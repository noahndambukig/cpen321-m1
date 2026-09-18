import express, { type Express } from 'express';

import { createInfoRouter } from './routes/info';

export function createApp(): Express {
  const app = express();

  // Report the caller's address rather than a proxy's when deployed behind one.
  app.set('trust proxy', true);

  app.get('/health', (_req, res) => {
    res.json({ status: 'ok' });
  });

  app.use('/api', createInfoRouter());

  app.use((_req, res) => {
    res.status(404).json({ error: 'Not Found' });
  });

  return app;
}
