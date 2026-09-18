import { Router } from 'express';

import { env } from '../config/env';
import { formatLocalTime, normalizeIp, resolvePublicIp } from '../serverInfo';

/**
 * The three APIs M1 requires (server IP, server time, owner name), plus a
 * client-IP echo so the app can show the caller's address without needing a
 * device permission to enumerate its own interfaces.
 */
export function createInfoRouter(): Router {
  const router = Router();

  // Interface GET /api/server-ip
  router.get('/server-ip', (_req, res, next) => {
    resolvePublicIp(env.serverPublicIp)
      .then((ip) => res.json({ ip }))
      .catch(next);
  });

  // Interface GET /api/server-time
  router.get('/server-time', (_req, res) => {
    res.json({ time: formatLocalTime(new Date()) });
  });

  // Interface GET /api/name
  router.get('/name', (_req, res) => {
    res.json({ firstName: env.ownerFirstName, lastName: env.ownerLastName });
  });

  // Interface GET /api/client-ip
  router.get('/client-ip', (req, res) => {
    res.json({ ip: normalizeIp(req.ip ?? '') });
  });

  return router;
}
