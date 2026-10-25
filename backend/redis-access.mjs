import { createHash, randomUUID } from 'node:crypto';
import { ConfigError } from './config.mjs';

const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;
export const tokenHash = token => createHash('sha256').update(token).digest('hex');
const hashFromAuth = auth => typeof auth === 'string' && /^Bearer [a-f0-9]{64}$/.test(auth)
  ? tokenHash(auth.slice(7)) : null;

export function readRedisConfig(env = process.env) {
  let url;
  try { url = new URL(env.UPSTASH_REDIS_REST_URL ?? env.KV_REST_API_URL); }
  catch { throw new ConfigError('Invalid UPSTASH_REDIS_REST_URL.'); }
  if (url.protocol !== 'https:' || !/^[a-z0-9][a-z0-9-]*\.upstash\.io$/.test(url.hostname)
      || url.username || url.password || url.port || url.search || url.hash || url.pathname !== '/')
    throw new ConfigError('Use the HTTPS Upstash Redis REST endpoint without credentials or paths.');
  const token = env.UPSTASH_REDIS_REST_TOKEN ?? env.KV_REST_API_TOKEN, prefix = env.ZIKRA_REDIS_PREFIX ?? 'zikra:v1:';
  if (typeof token !== 'string' || !/^[A-Za-z0-9._~+\/=-]{16,2048}$/.test(token))
    throw new ConfigError('Invalid UPSTASH_REDIS_REST_TOKEN.');
  if (!/^[a-zA-Z0-9:_-]{1,64}:$/.test(prefix)) throw new ConfigError('Invalid ZIKRA_REDIS_PREFIX.');
  if (env.NODE_TLS_REJECT_UNAUTHORIZED === '0') throw new ConfigError('TLS verification must remain enabled.');
  return { url: url.origin, token, prefix };
}

export class RedisRest {
  constructor({ url, token, fetchImpl = fetch }) { this.url = url; this.token = token; this.fetchImpl = fetchImpl; }
  async command(args) {
    try {
      const response = await this.fetchImpl(this.url, { method: 'POST', redirect: 'error',
        headers: { Authorization: `Bearer ${this.token}`, 'Content-Type': 'application/json' },
        body: JSON.stringify(args), signal: AbortSignal.timeout(5000) });
      if (!response.ok) { await response.body?.cancel(); throw new Error(); }
      const chunks = []; let bytes = 0;
      for await (const chunk of response.body) {
        bytes += chunk.length;
        if (bytes > 65536) throw new Error();
        chunks.push(chunk);
      }
      const data = JSON.parse(Buffer.concat(chunks).toString('utf8'));
      if (!data || Object.hasOwn(data, 'error') || !Object.hasOwn(data, 'result')) throw new Error();
      return data.result;
    } catch { throw new Error('access_unavailable'); }
  }
}

// Redis time, counters and leases are shared across cold starts and replicas.
export const ACQUIRE = `
local raw = redis.call('GET', KEYS[1])
if not raw then return {-1} end
local ok, client = pcall(cjson.decode, raw)
if not ok or type(client) ~= 'table' or type(client.revoked) ~= 'boolean' then
  return redis.error_reply('invalid_registry')
end
if client.revoked or client.id ~= ARGV[1] then return {-1} end
local clock = redis.call('TIME')
local now = tonumber(clock[1]) * 1000 + math.floor(tonumber(clock[2]) / 1000)
local day = math.floor(now / 86400000)
local state = redis.call('HMGET', KEYS[2], 'day', 'count', 'minute', 'minuteCount')
local function integer(value)
  return value and tonumber(value) and tonumber(value) >= 0 and tonumber(value) % 1 == 0
end
local count, minuteCount = 0, 0
local minute = math.floor(now / 60000)
if state[1] then
  for i = 1, 4 do if not integer(state[i]) then return redis.error_reply('invalid_usage') end end
  if tonumber(state[1]) == day then count = tonumber(state[2]) end
  if tonumber(state[3]) == minute then minuteCount = tonumber(state[4]) end
elseif state[2] or state[3] or state[4] then return redis.error_reply('invalid_usage') end
redis.call('ZREMRANGEBYSCORE', KEYS[3], '-inf', now)
redis.call('ZREMRANGEBYSCORE', KEYS[4], '-inf', now)
if count >= tonumber(ARGV[3]) or minuteCount >= 10 then return {0, 'quota_exceeded'} end
if redis.call('ZCARD', KEYS[3]) >= 2 or redis.call('ZCARD', KEYS[4]) >= 1 then return {0, 'try_later'} end
redis.call('HSET', KEYS[2], 'day', day, 'count', count + 1, 'minute', minute, 'minuteCount', minuteCount + 1)
redis.call('EXPIRE', KEYS[2], 172800)
redis.call('ZADD', KEYS[3], now + 55000, ARGV[2])
redis.call('ZADD', KEYS[4], now + 55000, ARGV[2])
redis.call('EXPIRE', KEYS[3], 60)
redis.call('EXPIRE', KEYS[4], 60)
return {1}
`;

const RELEASE = `redis.call('ZREM', KEYS[1], ARGV[1]); redis.call('ZREM', KEYS[2], ARGV[1]); return 1`;
export const ISSUE = `
if redis.call('EXISTS', KEYS[1]) == 1 or redis.call('EXISTS', KEYS[2]) == 1 then return 0 end
redis.call('SET', KEYS[1], ARGV[1]); redis.call('SET', KEYS[2], ARGV[2]); return 1
`;
export const REVOKE = `
local raw = redis.call('GET', KEYS[1])
if not raw then return 0 end
local client = cjson.decode(raw)
if client.id ~= ARGV[1] then return 0 end
client.revoked = true; redis.call('SET', KEYS[1], cjson.encode(client)); return 1
`;

export class RedisAccess {
  constructor({ redis, prefix = 'zikra:v1:', dailyLimit = 100 }) {
    if (!Number.isInteger(dailyLimit) || dailyLimit < 1 || dailyLimit > 1000) throw new ConfigError('Invalid ZIKRA_DAILY_LIMIT.');
    this.redis = redis; this.prefix = prefix; this.dailyLimit = dailyLimit;
  }
  async identify(auth) {
    const hash = hashFromAuth(auth);
    if (!hash) return null;
    const raw = await this.redis.command(['GET', `${this.prefix}client:${hash}`]);
    if (raw === null) return null;
    let client;
    try { client = JSON.parse(raw); } catch { throw new Error('invalid_registry'); }
    if (!uuid.test(client?.id) || typeof client.revoked !== 'boolean') throw new Error('invalid_registry');
    return client.revoked ? null : client.id;
  }
  async acquire(id, auth) {
    const hash = hashFromAuth(auth);
    if (!hash || !uuid.test(id)) return { allowed: false, status: 401, error: 'unauthorized' };
    const lease = randomUUID();
    const result = await this.redis.command(['EVAL', ACQUIRE, 4, `${this.prefix}client:${hash}`,
      `${this.prefix}usage:${id}`, `${this.prefix}active`, `${this.prefix}active:${id}`,
      id, lease, this.dailyLimit]);
    if (Array.isArray(result) && result.length === 1 && result[0] === 1) return { allowed: true, lease };
    if (Array.isArray(result) && result.length === 1 && result[0] === -1)
      return { allowed: false, status: 401, error: 'unauthorized' };
    if (Array.isArray(result) && result.length === 2 && result[0] === 0
        && ['try_later', 'quota_exceeded'].includes(result[1]))
      return { allowed: false, status: 429, error: result[1] };
    throw new Error('invalid_access_response');
  }
  async release(id, lease) {
    if (!uuid.test(id) || !uuid.test(lease)) throw new Error('invalid_lease');
    await this.redis.command(['EVAL', RELEASE, 2, `${this.prefix}active`, `${this.prefix}active:${id}`, lease]);
  }
}
