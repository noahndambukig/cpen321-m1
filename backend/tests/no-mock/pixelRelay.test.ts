import { createServer, type Server } from 'node:http';
import type { AddressInfo } from 'node:net';

import { WebSocket, WebSocketServer } from 'ws';

import { attachPixelRelay, PIXEL_RELAY_PATH } from '../../src/pixelRelay';

/** Stand-in for the course pixel server, so tests need no network. */
function startFakeUpstream(): Promise<{ url: string; wss: WebSocketServer }> {
  return new Promise((resolve) => {
    const wss = new WebSocketServer({ port: 0 }, () => {
      const { port } = wss.address() as AddressInfo;
      resolve({ url: `ws://127.0.0.1:${port}`, wss });
    });
  });
}

function startRelay(
  upstreamUrl: string,
  options: { stallTimeoutMs?: number; stallCheckMs?: number } = {},
): Promise<{ url: string; server: Server; stop: () => void }> {
  return new Promise((resolve) => {
    const server = createServer();
    server.listen(0, () => {
      const { port } = server.address() as AddressInfo;
      const stop = attachPixelRelay(server, upstreamUrl, options);
      resolve({ url: `ws://127.0.0.1:${port}${PIXEL_RELAY_PATH}`, server, stop });
    });
  });
}

function connect(url: string): Promise<WebSocket> {
  return new Promise((resolve, reject) => {
    const ws = new WebSocket(url);
    ws.once('open', () => resolve(ws));
    ws.once('error', reject);
  });
}

/** Waits until the relay has an upstream connection to forward from. */
function waitForUpstreamClient(wss: WebSocketServer): Promise<WebSocket> {
  return new Promise((resolve) => {
    if (wss.clients.size > 0) {
      resolve([...wss.clients][0] as WebSocket);
      return;
    }
    wss.once('connection', (socket) => resolve(socket as WebSocket));
  });
}

// Interface WS /ws/pixels
describe('Unmocked: WS /ws/pixels', () => {
  let upstream: Awaited<ReturnType<typeof startFakeUpstream>>;
  let relay: Awaited<ReturnType<typeof startRelay>>;

  beforeEach(async () => {
    upstream = await startFakeUpstream();
    relay = await startRelay(upstream.url);
  });

  afterEach(async () => {
    relay.stop();
    await new Promise((r) => relay.server.close(r));
    await new Promise((r) => upstream.wss.close(r));
  });

  // Input: upstream emits a pixel frame
  // Expected behavior: the frame reaches the client byte-for-byte, since M1 forbids
  //                    batching, delaying or reformatting
  // Expected output: the identical JSON string
  test('Forwards frames verbatim', async () => {
    const client = await connect(relay.url);
    const source = await waitForUpstreamClient(upstream.wss);

    const frame = '{"x":7,"y":13,"color":"#3a2a1a"}';
    const received = new Promise<string>((resolve) => {
      client.once('message', (data) => resolve(data.toString()));
    });

    source.send(frame);
    expect(await received).toBe(frame);

    client.close();
  });

  // Input: upstream emits several frames while two clients are attached
  // Expected behavior: both clients see the same frames in the same order, because
  //                    one shared upstream connection is fanned out
  // Expected output: identical ordered sequences
  test('Fans one upstream out to every client in order', async () => {
    const a = await connect(relay.url);
    const b = await connect(relay.url);
    const source = await waitForUpstreamClient(upstream.wss);

    const frames = [
      '{"x":0,"y":0,"color":"#FFFFFF"}',
      '{"x":1,"y":2,"color":"#3a2a1a"}',
      '{"x":15,"y":15,"color":"#dff3ff"}',
    ];

    const collect = (ws: WebSocket): Promise<string[]> =>
      new Promise((resolve) => {
        const seen: string[] = [];
        ws.on('message', (data) => {
          seen.push(data.toString());
          if (seen.length === frames.length) resolve(seen);
        });
      });

    const both = Promise.all([collect(a), collect(b)]);
    for (const f of frames) source.send(f);
    const [seenA, seenB] = await both;

    expect(seenA).toEqual(frames);
    expect(seenB).toEqual(frames);

    a.close();
    b.close();
  });

  // Input: a client disconnects, then upstream emits another frame
  // Expected behavior: the remaining client still receives it; a dead socket does
  //                    not break the broadcast loop
  // Expected output: the surviving client gets the frame
  test('Survives a client disconnecting', async () => {
    const a = await connect(relay.url);
    const b = await connect(relay.url);
    const source = await waitForUpstreamClient(upstream.wss);

    await new Promise<void>((resolve) => {
      b.once('close', () => resolve());
      b.close();
    });

    const frame = '{"x":3,"y":4,"color":"#123456"}';
    const received = new Promise<string>((resolve) => {
      a.once('message', (data) => resolve(data.toString()));
    });

    source.send(frame);
    expect(await received).toBe(frame);

    a.close();
  });
});

// Interface WS /ws/pixels (stall recovery)
describe('Unmocked: WS /ws/pixels recovers from a half-open upstream', () => {
  // Input: the upstream stops sending but never closes the socket, which is what
  //        a vanished peer looks like when no FIN or RST arrives
  // Expected behavior: the watchdog notices the silence, tears the socket down and
  //                    reconnects, so frames resume
  // Expected output: the client receives a frame sent after the reconnect
  test('Reconnects when frames stop without a close', async () => {
    const upstream = await startFakeUpstream();
    // Short windows so the test does not wait 20 real seconds.
    const relay = await startRelay(upstream.url, {
      stallTimeoutMs: 400,
      stallCheckMs: 100,
    });

    try {
      const client = await connect(relay.url);
      const first = await waitForUpstreamClient(upstream.wss);

      // Go silent without closing, and make sure a close cannot fire: pause the
      // socket so the relay's own terminate is what breaks the link.
      first.pause();

      // The watchdog should terminate and reconnect, producing a second
      // connection on the fake upstream.
      const second = await new Promise<WebSocket>((resolve) => {
        upstream.wss.once('connection', (s) => resolve(s as WebSocket));
      });

      const frame = '{"x":9,"y":9,"color":"#ABCDEF"}';
      const received = new Promise<string>((resolve) => {
        client.once('message', (data) => resolve(data.toString()));
      });
      second.send(frame);

      expect(await received).toBe(frame);
      client.close();
    } finally {
      relay.stop();
      await new Promise((r) => relay.server.close(r));
      await new Promise((r) => upstream.wss.close(r));
    }
  }, 15_000);
});
