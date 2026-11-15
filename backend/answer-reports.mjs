import { createHash } from 'node:crypto';

const REASONS = new Set(['unsafe', 'inaccurate', 'other']);
const SAVE = `
local hash = redis.call('GET', KEYS[3])
local raw = hash and redis.call('GET', ARGV[2] .. 'client:' .. hash)
if not raw then return -1 end
local client = cjson.decode(raw)
if client.revoked ~= false or client.id ~= ARGV[3] then return -1 end
if redis.call('EXISTS', KEYS[1]) == 1 then return 1 end
if tonumber(redis.call('GET', KEYS[2]) or 0) >= 5 then return 0 end
redis.call('SET', KEYS[1], ARGV[1], 'EX', 604800)
if redis.call('INCR', KEYS[2]) == 1 then redis.call('EXPIRE', KEYS[2], 86400) end
return 1`;

// The moderation record contains only the fields explicitly reviewed by the user.
export async function saveAnswerReport(redis, prefix, identity, body) {
  if (!body || Object.keys(body).length !== 2 || !REASONS.has(body.reason)
      || typeof body.excerpt !== 'string' || !body.excerpt.trim() || body.excerpt.length > 500)
    return 400;
  const record = JSON.stringify({ reason: body.reason, excerpt: body.excerpt.trim() });
  const digest = createHash('sha256').update(identity).update('\0').update(record).digest('hex');
  const result = await redis.command(['EVAL', SAVE, 3, prefix + 'answer-report:' + identity + ':' + digest,
    prefix + 'report-rate:' + identity, prefix + 'installation:' + identity, record, prefix, identity]);
  if (result === 1) return 201;
  if (result === -1) return 401;
  if (result === 0) return 429;
  throw new Error('report_unavailable');
}
