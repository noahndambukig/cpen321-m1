import 'dotenv/config';

const rawPort = process.env.PORT;
const port =
  rawPort === undefined || rawPort === ''
    ? 3000
    : Number.parseInt(rawPort, 10);

if (Number.isNaN(port) || port < 1 || port > 65535) {
  throw new Error(`Invalid PORT: ${rawPort}`);
}

function required(name: string, fallback: string): string {
  const value = process.env[name];
  return value === undefined || value === '' ? fallback : value;
}

export const env = {
  port,
  /** Name returned by GET /api/name. */
  ownerFirstName: required('OWNER_FIRST_NAME', 'Noah'),
  ownerLastName: required('OWNER_LAST_NAME', 'Ndambuki'),
  /**
   * Public address of this server. Set after deploying — a cloud VM behind NAT
   * cannot discover it from its own interfaces. Left unset, the server falls
   * back to an external reflector lookup.
   */
  serverPublicIp: process.env.SERVER_PUBLIC_IP,
  /** Course-provided pixel stream that Button 2 relays to the app. */
  pixelStreamUrl: required('PIXEL_STREAM_URL', 'wss://8.229.22.124'),
} as const;
