const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;
export const ERASURE_SETTLE_SECONDS = 120;
export const MAX_ERASURE_REQUESTS = 1000;

// One atomic operation validates the index pair before any change. Shared keys are never deleted.
export const INSTALLATION_ERASURE = `
local prefix, id, action = ARGV[1], ARGV[2], ARGV[3]
local index = prefix .. 'installation:' .. id
local hash = redis.call('GET', index)
if not hash then return {'missing'} end
if string.len(hash) ~= 64 or not string.match(hash, '^[a-f0-9]+$') then return redis.error_reply('invalid_registry') end
local key = prefix .. 'client:' .. hash
local raw = redis.call('GET', key)
if not raw then return redis.error_reply('invalid_registry') end
local ok, client = pcall(cjson.decode, raw)
if not ok or type(client) ~= 'table' or client.id ~= id or type(client.revoked) ~= 'boolean' then
  return redis.error_reply('invalid_registry')
end
local clock = redis.call('TIME')
local now = tonumber(clock[1])
local requested = client.erasureRequestedAt
if requested ~= nil and (type(requested) ~= 'number' or requested < 0 or requested % 1 ~= 0 or requested > now) then
  return redis.error_reply('invalid_registry')
end
if action == 'prepare' then
  client.revoked = true
  if requested == nil then client.erasureRequestedAt = now end
  redis.call('SET', key, cjson.encode(client))
  return {'prepared'}
end
local keys = {key, index, prefix .. 'usage:' .. id, prefix .. 'active:' .. id,
  prefix .. 'images:user:' .. id, prefix .. 'quran-sync:rate:' .. id}
local seen, cursor, steps = {}, '0', 0
repeat
  local batch = redis.call('SCAN', cursor, 'MATCH', prefix .. 'images:request:' .. id .. ':*', 'COUNT', 100)
  cursor = tostring(batch[1]); steps = steps + 1
  for _, item in ipairs(batch[2]) do
    if not seen[item] then
      seen[item] = true
      table.insert(keys, item)
      if #keys > ${MAX_ERASURE_REQUESTS + 6} then return redis.error_reply('erasure_too_large') end
    end
  end
  if steps >= 1000 and cursor ~= '0' then return redis.error_reply('erasure_too_large') end
until cursor == '0'
local live = redis.call('ZCOUNT', prefix .. 'active:' .. id, now * 1000, '+inf')
local count = 0
for _, item in ipairs(keys) do count = count + redis.call('EXISTS', item) end
local wait = requested and math.max(0, requested + ${ERASURE_SETTLE_SECONDS} - now) or ${ERASURE_SETTLE_SECONDS}
if action == 'inspect' then return {'inspected', client.revoked and 1 or 0, requested and 1 or 0, wait, live, count} end
if not client.revoked or not requested or wait > 0 or live > 0 then return {'not_ready'} end
if action ~= 'erase' then return redis.error_reply('invalid_action') end
local removed = 0
for _, item in ipairs(keys) do removed = removed + redis.call('DEL', item) end
return {'erased', removed}
`;

export async function manageInstallationErasure(redis, prefix, id, action) {
  if (!/^[a-zA-Z0-9:_-]{1,64}:$/.test(prefix) || !uuid.test(id)
      || !['inspect', 'prepare', 'erase'].includes(action)) throw new Error('invalid_erasure_request');
  const value = await redis.command(['EVAL', INSTALLATION_ERASURE, 0, prefix, id, action]);
  if (!Array.isArray(value)) throw new Error('invalid_erasure_response');
  if (value.length === 1 && ['missing', 'prepared', 'not_ready'].includes(value[0]))
    return { state: value[0], providerDataDeleted: false };
  if (value[0] === 'erased' && value.length === 2 && Number.isInteger(value[1]) && value[1] >= 0)
    return { state: 'erased', removedKeys: value[1], providerDataDeleted: false };
  if (value[0] === 'inspected' && value.length === 6 && value.slice(1).every(n => Number.isInteger(n) && n >= 0))
    return { state: 'inspected', revoked: value[1] === 1, prepared: value[2] === 1,
      waitSeconds: value[3], activeRequests: value[4], scopedKeys: value[5], providerDataDeleted: false };
  throw new Error('invalid_erasure_response');
}
