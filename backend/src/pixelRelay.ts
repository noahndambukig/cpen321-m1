import type { Server } from 'node:http';

import { WebSocket, WebSocketServer, type RawData } from 'ws';

/** Path the Android app connects to. */
export const PIXEL_RELAY_PATH = '/ws/pixels';

const RECONNECT_BASE_MS = 1_000;
const RECONNECT_MAX_MS = 30_000;

/**
 * Longest believable quiet period. The course server pauses about five seconds
 * between images, so silence beyond this means the link is dead rather than idle.
 */
const STALL_TIMEOUT_MS = 20_000;
const STALL_CHECK_MS = 5_000;

/**
 * Relays the course pixel stream to this server's own websocket clients.
 *
 * One upstream connection is shared by every app client rather than opening one
 * per client, and each frame is forwarded byte-for-byte: M1 requires the relay to
 * add no batching, delay, or reformatting. Nothing is buffered, so a client that
 * connects mid-image sees the remainder of that image only.
 */
export function attachPixelRelay(
  server: Server,
  upstreamUrl: string,
  options: { stallTimeoutMs?: number; stallCheckMs?: number } = {},
): () => void {
  const stallTimeoutMs = options.stallTimeoutMs ?? STALL_TIMEOUT_MS;
  const stallCheckMs = options.stallCheckMs ?? STALL_CHECK_MS;

  const wss = new WebSocketServer({ server, path: PIXEL_RELAY_PATH });

  let upstream: WebSocket | undefined;
  let reconnectDelay = RECONNECT_BASE_MS;
  let reconnectTimer: NodeJS.Timeout | undefined;
  let stopped = false;
  let lastFrameAt = Date.now();

  function broadcast(data: RawData, isBinary: boolean): void {
    lastFrameAt = Date.now();
    for (const client of wss.clients) {
      if (client.readyState === WebSocket.OPEN) {
        client.send(data, { binary: isBinary });
      }
    }
  }

  /**
   * Watches for a half-open connection: the peer disappears without sending a
   * FIN or RST, so no 'close' event ever fires and the socket sits "open"
   * delivering nothing. Reconnect-on-close alone cannot recover from this, and it
   * is invisible from outside because /health still answers and clients still
   * connect. Observed in production after the upstream rotated its certificate.
   */
  function checkForStall(): void {
    if (stopped || upstream === undefined) {
      return;
    }
    if (Date.now() - lastFrameAt > stallTimeoutMs) {
      console.warn(
        `Pixel relay saw no frames for ${stallTimeoutMs}ms; assuming the link is ` +
          'half-open and reconnecting',
      );
      // terminate, not close: a half-open socket will not complete a handshake.
      upstream.terminate();
    }
  }

  const stallTimer = setInterval(checkForStall, stallCheckMs);
  // Do not hold the process open just for the watchdog.
  stallTimer.unref?.();

  function connect(): void {
    if (stopped) {
      return;
    }

    const socket = new WebSocket(upstreamUrl);
    upstream = socket;
    // Reset on connect, so a slow handshake is not read as an immediate stall.
    lastFrameAt = Date.now();

    socket.on('open', () => {
      reconnectDelay = RECONNECT_BASE_MS;
      lastFrameAt = Date.now();
      console.log(`Pixel relay connected to ${upstreamUrl}`);
    });

    socket.on('message', broadcast);

    // The ws library answers pings automatically; an explicit pong also proves
    // the link is alive even during the pause between images.
    socket.on('pong', () => {
      lastFrameAt = Date.now();
    });

    socket.on('error', (error) => {
      console.error(`Pixel relay upstream error: ${error.message}`);
    });

    socket.on('close', () => {
      if (stopped) {
        return;
      }
      console.warn(`Pixel relay upstream closed; retrying in ${reconnectDelay}ms`);
      reconnectTimer = setTimeout(connect, reconnectDelay);
      reconnectDelay = Math.min(reconnectDelay * 2, RECONNECT_MAX_MS);
    });
  }

  connect();

  return () => {
    stopped = true;
    clearInterval(stallTimer);
    if (reconnectTimer !== undefined) {
      clearTimeout(reconnectTimer);
    }
    upstream?.close();
    wss.close();
  };
}
