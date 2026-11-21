import { createHash, randomUUID } from 'node:crypto';
import { ConfigError } from './config.mjs';
import { ADULT_POLICY } from './adult-accounts.mjs';

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
  constructor({ url, token, fetchImpl = fetch, responseLimit = 65536 }) {
    if (!Number.isInteger(responseLimit) || responseLimit < 1 || responseLimit > 524288)
      throw new ConfigError('Invalid Redis response limit.');
    this.url = url; this.token = token; this.fetchImpl = fetchImpl; this.responseLimit = responseLimit;
  }
  async command(args) {
    try {
      const response = await this.fetchImpl(this.url, { method: 'POST', redirect: 'error',
        headers: { Authorization: `Bearer ${this.token}`, 'Content-Type': 'application/json' },
        body: JSON.stringify(args), signal: AbortSignal.timeout(5000) });
      if (!response.ok) { await response.body?.cancel(); throw new Error(); }
      const chunks = []; let bytes = 0;
      for await (const chunk of response.body) {
        bytes += chunk.length;
        if (bytes > this.responseLimit) throw new Error();
        chunks.push(chunk);
      }
      const data = JSON.parse(Buffer.concat(chunks).toString('utf8'));
      if (!data || Object.hasOwn(data, 'error') || !Object.hasOwn(data, 'result')) throw new Error();
      return data.result;
    } catch { throw new Error('access_unavailable'); }
  }
}

const accountId = /^[a-f0-9]{64}$/;
export const adultRecordKey = (prefix, id) => prefix + 'adult:account:' + id;

// Called inside the same Redis transaction as quota acquisition or report storage.
export const ADULT_RECORD_GUARD = `
local function adult_allowed(key, id, revision, policy)
  local raw = redis.call('GET', key)
  if not raw then return false end
  local record = cjson.decode(raw)
  if type(record) ~= 'table' then return false end
  local clock = redis.call('TIME')
  local now = tonumber(clock[1]) * 1000 + math.floor(tonumber(clock[2]) / 1000)
  return record.schema == 2 and record.id == id and record.revision == tonumber(revision)
    and record.state == 'active' and record.ageBand == 'adult' and record.policy == policy
    and record.assurance == 'self_declared' and tonumber(record.expiresAt) > now
end
`;

// Redis time, counters and leases are shared across cold starts and replicas.
export const ACQUIRE = `
${ADULT_RECORD_GUARD}
if ARGV[4] then
  if not adult_allowed(KEYS[1], ARGV[1], ARGV[4], ARGV[5]) then return {-2} end
else
  local raw = redis.call('GET', KEYS[1])
  if not raw then return {-1} end
  local ok, client = pcall(cjson.decode, raw)
  if not ok or type(client) ~= 'table' or type(client.revoked) ~= 'boolean' then
    return redis.error_reply('invalid_registry')
end
  if client.revoked or client.id ~= ARGV[1] then return {-1} end
end
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
if count >= tonumber(ARGV[3]) then return {0, 'quota_exceeded'} end
if minuteCount >= 10 then return {0, 'rate_limited'} end
if redis.call('ZCARD', KEYS[4]) >= 1 then return {0, 'try_later', 'installation'} end
if redis.call('ZCARD', KEYS[3]) >= 2 then return {0, 'try_later', 'global'} end
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
    return this.acquireFor(id, this.prefix + 'client:' + hash);
  }
  async acquireAccount({ accountId: id, revision }) {
    if (!accountId.test(id) || !Number.isSafeInteger(revision) || revision < 1)
      return { allowed: false, status: 403, error: 'adult_confirmation_required' };
    return this.acquireFor(id, adultRecordKey(this.prefix, id), [revision, ADULT_POLICY]);
  }
  async acquireFor(id, key, accountArgs = []) {
    const lease = randomUUID();
    const result = await this.redis.command(['EVAL', ACQUIRE, 4, key,
      `${this.prefix}usage:${id}`, `${this.prefix}active`, `${this.prefix}active:${id}`,
      id, lease, this.dailyLimit, ...accountArgs]);
    if (Array.isArray(result) && result.length === 1 && result[0] === 1) return { allowed: true, lease };
    if (Array.isArray(result) && result.length === 1 && result[0] === -1)
      return { allowed: false, status: 401, error: 'unauthorized' };
    if (Array.isArray(result) && result.length === 1 && result[0] === -2)
      return { allowed: false, status: 403, error: 'adult_confirmation_required' };
    if (Array.isArray(result) && result.length === 3 && result[0] === 0
        && result[1] === 'try_later' && ['installation', 'global'].includes(result[2]))
      return { allowed: false, status: 429, error: 'try_later', queue: result[2] };
    if (Array.isArray(result) && result.length === 2 && result[0] === 0
        && ['try_later', 'quota_exceeded', 'rate_limited'].includes(result[1]))
      return { allowed: false, status: 429, error: result[1] };
    throw new Error('invalid_access_response');
  }
  async release(id, lease) {
    if ((!uuid.test(id) && !accountId.test(id)) || !uuid.test(lease)) throw new Error('invalid_lease');
    await this.redis.command(['EVAL', RELEASE, 2, `${this.prefix}active`, `${this.prefix}active:${id}`, lease]);
  }
}
