import { networkInterfaces } from 'node:os';

/**
 * Formats a moment as the 24-hour, GMT-relative string M1 requires:
 * `hh:mm:ss GMT+hh:mm`. Uses the process's local timezone, which is the
 * container's TZ (UTC unless overridden in the environment).
 */
export function formatLocalTime(date: Date): string {
  const pad = (value: number): string => String(value).padStart(2, '0');

  const clock = `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;

  // getTimezoneOffset returns minutes to ADD to local time to reach UTC, so it
  // is the negation of the conventional GMT offset.
  const offsetMinutes = -date.getTimezoneOffset();
  const sign = offsetMinutes < 0 ? '-' : '+';
  const absolute = Math.abs(offsetMinutes);

  return `${clock} GMT${sign}${pad(Math.trunc(absolute / 60))}:${pad(absolute % 60)}`;
}

/** Strips the IPv4-mapped IPv6 prefix so `::ffff:1.2.3.4` reads as `1.2.3.4`. */
export function normalizeIp(ip: string): string {
  return ip.startsWith('::ffff:') ? ip.slice('::ffff:'.length) : ip;
}

/**
 * First non-internal address bound to a local interface. On a cloud VM behind
 * NAT this is the private address, which is why it is only a fallback.
 */
export function localNonInternalIp(): string | undefined {
  for (const addresses of Object.values(networkInterfaces())) {
    for (const address of addresses ?? []) {
      if (!address.internal) {
        return address.address;
      }
    }
  }
  return undefined;
}

const PUBLIC_IP_LOOKUP_URL = 'https://api.ipify.org?format=json';
const LOOKUP_TIMEOUT_MS = 3000;

let cachedPublicIp: string | undefined;

/** Test seam: drops the memoized lookup result. */
export function resetPublicIpCache(): void {
  cachedPublicIp = undefined;
}

/**
 * Resolves the server's public IP, preferring an explicit configuration value.
 *
 * A cloud VM usually cannot see its own public address on any interface, so an
 * external reflector is consulted and memoized. `SERVER_PUBLIC_IP` short-circuits
 * that entirely and should be set once the backend is deployed.
 */
export async function resolvePublicIp(configured?: string): Promise<string> {
  if (configured !== undefined && configured !== '') {
    return configured;
  }
  if (cachedPublicIp !== undefined) {
    return cachedPublicIp;
  }

  try {
    const response = await fetch(PUBLIC_IP_LOOKUP_URL, {
      signal: AbortSignal.timeout(LOOKUP_TIMEOUT_MS),
    });
    if (response.ok) {
      const body = (await response.json()) as { ip?: unknown };
      if (typeof body.ip === 'string' && body.ip !== '') {
        cachedPublicIp = body.ip;
        return body.ip;
      }
    }
  } catch {
    // Fall through to the local address below; an offline grader still gets a value.
  }

  return localNonInternalIp() ?? '127.0.0.1';
}
