import { ConfigError, validateProvider } from './config.mjs';
import { RedisAccess, RedisRest, readRedisConfig } from './redis-access.mjs';
import { createRequestHandler } from './server.mjs';
import {readImageConfig,createImageService} from './image-generation.mjs';

export function readVercelConfig(env = process.env) {
  const config = { apiKey: env.FEATHERLESS_API_KEY, model: env.FEATHERLESS_MODEL, visionModel: env.FEATHERLESS_VISION_MODEL };
  validateProvider(config);
  const redis = readRedisConfig(env), dailyLimit = Number(env.ZIKRA_DAILY_LIMIT ?? '100');
  if (!Number.isInteger(dailyLimit) || dailyLimit < 1 || dailyLimit > 1000)
    throw new ConfigError('Invalid ZIKRA_DAILY_LIMIT; use 1 to 1000.');
  return { ...config, redis, dailyLimit };
}

export function createVercelHandler(env = process.env, { redis, fetchImpl, sourceFetchImpl } = {}) {
  const config = readVercelConfig(env);
  const accessStore = new RedisAccess({ redis: redis ?? new RedisRest(config.redis),
    prefix: config.redis.prefix, dailyLimit: config.dailyLimit });
  // An image configuration failure must not disable existing text/vision chat.
  let imageConfig;try{imageConfig=readImageConfig(env);}catch{imageConfig=null;}
  const imageService=createImageService({config:imageConfig,redis:accessStore.redis,prefix:config.redis.prefix,fetchImpl});
  return createRequestHandler({ ...config, accessMode: 'installation', accessStore, fetchImpl, sourceFetchImpl,imageService });
}

let handler;
export default async function vercelHandler(req, res) {
  try { handler ??= createVercelHandler(); }
  catch {
    res.writeHead(503, { 'Content-Type': 'application/json; charset=utf-8',
      'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff' });
    res.end('{"error":"access_unavailable"}'); return;
  }
  return handler(req, res);
}
