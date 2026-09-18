import request from 'supertest';

import { createApp } from '../../src/app';
import { formatLocalTime } from '../../src/serverInfo';

/** The exact shape M1 specifies: 24-hour clock, GMT-relative offset. */
const TIME_FORMAT = /^([01]\d|2[0-3]):[0-5]\d:[0-5]\d GMT[+-]\d{2}:\d{2}$/;
const IPV4 = /^(\d{1,3}\.){3}\d{1,3}$/;

// Interface GET /api/server-time
describe('Unmocked: GET /api/server-time', () => {
  // Input: GET request to /api/server-time
  // Expected status code: 200
  // Expected behavior: server reports its own local time at call time
  // Expected output: { time: "hh:mm:ss GMT+hh:mm" }
  test('Returns 24-hour GMT-relative time', async () => {
    const response = await request(createApp()).get('/api/server-time');

    expect(response.status).toBe(200);
    expect(typeof response.body.time).toBe('string');
    expect(response.body.time).toMatch(TIME_FORMAT);
  });

  // Input: two GET requests separated by a state change in the clock
  // Expected status code: 200
  // Expected behavior: the time is evaluated per call rather than at startup
  // Expected output: a value consistent with the clock at the second call
  test('Reflects the clock at call time', async () => {
    const before = formatLocalTime(new Date());
    const response = await request(createApp()).get('/api/server-time');
    const after = formatLocalTime(new Date());

    expect(response.status).toBe(200);
    // Same offset, and the clock lies within the window the call spanned.
    expect(response.body.time.slice(9)).toBe(before.slice(9));
    expect(response.body.time >= before && response.body.time <= after).toBe(true);
  });
});

// Interface GET /api/name
describe('Unmocked: GET /api/name', () => {
  // Input: GET request to /api/name
  // Expected status code: 200
  // Expected behavior: returns the configured owner name
  // Expected output: { firstName: string, lastName: string }, both non-empty
  test('Returns first and last name', async () => {
    const response = await request(createApp()).get('/api/name');

    expect(response.status).toBe(200);
    expect(typeof response.body.firstName).toBe('string');
    expect(typeof response.body.lastName).toBe('string');
    expect(response.body.firstName.length).toBeGreaterThan(0);
    expect(response.body.lastName.length).toBeGreaterThan(0);
  });
});

// Interface GET /api/client-ip
describe('Unmocked: GET /api/client-ip', () => {
  // Input: GET request to /api/client-ip over loopback
  // Expected status code: 200
  // Expected behavior: echoes the caller's address, IPv4-mapped prefix stripped
  // Expected output: { ip: "127.0.0.1" }
  test('Echoes the caller address', async () => {
    const response = await request(createApp()).get('/api/client-ip');

    expect(response.status).toBe(200);
    expect(response.body.ip).toMatch(IPV4);
    expect(response.body.ip).not.toContain('::ffff:');
  });
});

// Interface GET /api/server-ip
describe('Unmocked: GET /api/server-ip', () => {
  // Input: GET request to /api/server-ip
  // Expected status code: 200
  // Expected behavior: resolves an address, falling back to a local interface
  //                    when no reflector is reachable
  // Expected output: { ip: <non-empty string> }
  test('Returns an address', async () => {
    const response = await request(createApp()).get('/api/server-ip');

    expect(response.status).toBe(200);
    expect(typeof response.body.ip).toBe('string');
    expect(response.body.ip.length).toBeGreaterThan(0);
  });
});
