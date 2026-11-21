import {saveAnswerReport} from './answer-reports.mjs';
import { ConfigError, readProviderConfig } from './config.mjs';
import { RedisAccess, RedisRest, readRedisConfig } from './redis-access.mjs';
import { createRequestHandler } from './server.mjs';
import {readImageConfig,createImageService} from './image-generation.mjs';

export function readVercelConfig(env = process.env) {
  const config = readProviderConfig(env);
  const redis = readRedisConfig(env), dailyLimit = Number(env.ZIKRA_DAILY_LIMIT ?? '100');
  if (!Number.isInteger(dailyLimit) || dailyLimit < 1 || dailyLimit > 1000)
    throw new ConfigError('Invalid ZIKRA_DAILY_LIMIT; use 1 to 1000.');
  return { ...config, redis, dailyLimit };
}

export function createVercelHandler(env = process.env, { redis, fetchImpl, sourceFetchImpl,
  adultAccounts, accountPreAuth, onRejection = event => console.warn(JSON.stringify(event)) } = {}) {
  const config = readVercelConfig(env);
  if (env.ZIKRA_ADULT_ACCOUNTS_REQUIRED !== undefined && !['true','false'].includes(env.ZIKRA_ADULT_ACCOUNTS_REQUIRED))
    throw new ConfigError('Invalid adult account activation.');
  const accountRequired=env.ZIKRA_ADULT_ACCOUNTS_REQUIRED==='true';
  const selfServiceValue = env.ZIKRA_ADULT_ACCOUNTS_SELF_SERVICE;
  if (selfServiceValue !== undefined && !['true', 'false'].includes(selfServiceValue))
    throw new ConfigError('Invalid adult account activation.');
  const accountSelfService = selfServiceValue === 'true';
  const accountsUsed = accountRequired || accountSelfService;
  if(accountsUsed&&env.ZIKRA_ADULT_ACCOUNTS_ENABLED!=='true')throw new ConfigError('Adult account endpoints must be enabled.');
  if(accountsUsed&&typeof adultAccounts?.authorize!=='function')throw new ConfigError('Adult account identity service required.');
  const accessStore = new RedisAccess({ redis: redis ?? new RedisRest(config.redis),
    prefix: config.redis.prefix, dailyLimit: config.dailyLimit });
  // An image configuration failure must not disable existing text/vision chat.
  let imageConfig;try{imageConfig=readImageConfig(env);}catch{imageConfig=null;}
  const imageService=createImageService({config:imageConfig,redis:accessStore.redis,prefix:config.redis.prefix,fetchImpl});
  const reportAnswer = env.ZIKRA_REPORTS_ENABLED === 'true'
    ? (identity, body, account) => saveAnswerReport(accessStore.redis, config.redis.prefix, identity, body, account) : null;
  return createRequestHandler({ ...(accountsUsed?{adultAccessGate:token=>adultAccounts.authorize(token)}:{}),
    accountRequired, accountSelfService, accountPreAuth, reportAnswer, ...config, accessMode: 'installation', accessStore, fetchImpl, sourceFetchImpl,imageService,onRejection });
}

let handler;
export default async function vercelHandler(req, res) {
  try {
    if(!handler){
      const runtime = async () => (await import('./adult-account-runtime.mjs')).getAdultRuntime();
      handler = createVercelHandler(process.env, {
        adultAccounts: { authorize: async token => (await runtime()).service.authorize(token) },
        accountPreAuth: async req => (await runtime()).beforeAuth(req)
      });
    }
  }
  catch {
    res.writeHead(503, { 'Content-Type': 'application/json; charset=utf-8',
      'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff' });
    res.end('{"error":"access_unavailable"}'); return;
  }
  return handler(req, res);
}
