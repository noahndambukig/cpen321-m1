import request from 'supertest';

import { createApp } from '../../src/app';
import { resetPublicIpCache } from '../../src/serverInfo';

describe('Mocked: GET /api/server-ip', () => {
  beforeEach(() => {
    resetPublicIpCache();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  // Mocked behavior: the public-IP reflector returns a valid address
  // Input: GET request to /api/server-ip
  // Expected status code: 200
  // Expected behavior: the reflector's answer is used verbatim
  // Expected output: { ip: "203.0.113.7" }
  test('Reflector responds', async () => {
    jest.spyOn(global, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ ip: '203.0.113.7' }), { status: 200 }),
    );

    const response = await request(createApp()).get('/api/server-ip');

    expect(response.status).toBe(200);
    expect(response.body).toEqual({ ip: '203.0.113.7' });
  });

  // Mocked behavior: the reflector is unreachable (network error)
  // Input: GET request to /api/server-ip
  // Expected status code: 200
  // Expected behavior: the error is handled and a local address is substituted
  //                    rather than surfacing a 5xx to the app
  // Expected output: { ip: <non-empty string> }
  test('Reflector unreachable', async () => {
    jest.spyOn(global, 'fetch').mockRejectedValue(new Error('ENETUNREACH'));

    const response = await request(createApp()).get('/api/server-ip');

    expect(response.status).toBe(200);
    expect(typeof response.body.ip).toBe('string');
    expect(response.body.ip.length).toBeGreaterThan(0);
  });

  // Mocked behavior: the reflector answers with a non-200 status
  // Input: GET request to /api/server-ip
  // Expected status code: 200
  // Expected behavior: the bad response is ignored in favour of the fallback
  // Expected output: { ip: <non-empty string> }
  test('Reflector returns an error status', async () => {
    jest.spyOn(global, 'fetch').mockResolvedValue(new Response('', { status: 503 }));

    const response = await request(createApp()).get('/api/server-ip');

    expect(response.status).toBe(200);
    expect(typeof response.body.ip).toBe('string');
    expect(response.body.ip.length).toBeGreaterThan(0);
  });

  // Mocked behavior: reflector answers once; a second call must not re-query
  // Input: two GET requests to /api/server-ip
  // Expected status code: 200
  // Expected behavior: the lookup is memoized, so fetch is called exactly once
  // Expected output: the same address both times
  test('Lookup is memoized', async () => {
    const fetchSpy = jest.spyOn(global, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ ip: '203.0.113.7' }), { status: 200 }),
    );
    const app = createApp();

    const first = await request(app).get('/api/server-ip');
    const second = await request(app).get('/api/server-ip');

    expect(first.body).toEqual({ ip: '203.0.113.7' });
    expect(second.body).toEqual({ ip: '203.0.113.7' });
    expect(fetchSpy).toHaveBeenCalledTimes(1);
  });
});
