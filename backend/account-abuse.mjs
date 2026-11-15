import { createHmac } from 'node:crypto';

export const ACCOUNT_RATE = `
for i = 1, 3 do
  local raw = redis.call('GET', KEYS[i])
  if raw then
    local n = tonumber(raw)
    if not n or n < 0 or n % 1 ~= 0 or redis.call('PTTL', KEYS[i]) < 1 then
      return redis.error_reply('invalid_rate_state')
    end
    if n >= tonumber(ARGV[i]) then return 0 end
  end
end
for i = 1, 3 do
  local n = redis.call('INCR', KEYS[i])
  if n == 1 then redis.call('EXPIRE', KEYS[i], tonumber(ARGV[i + 3])) end
end
return 1
`;

export function createAccountPreAuth({ redis, prefix, addressKey, trustedAddress }) {
  if (typeof redis?.command !== 'function' || typeof trustedAddress !== 'function'
      || !/^[a-zA-Z0-9:_-]{1,64}:$/.test(prefix ?? '')
      || !/^[a-f0-9]{64}$/.test(addressKey ?? '')) throw new Error('invalid_account_config');
  return async req => {
    // The hosting adapter must supply the verified address, not a client header.
    const address = trustedAddress(req);
    if (typeof address !== 'string' || !address || address.length > 128)
      throw new Error('access_unavailable');
    const key = createHmac('sha256', addressKey).update(address).digest('hex');
    const result = await redis.command(['EVAL', ACCOUNT_RATE, 3,
      `${prefix}account-rate:ip:${key}`, `${prefix}account-rate:global-minute`,
      `${prefix}account-rate:global-day`, 20, 500, 10000, 60, 60, 86400]);
    if (result !== 0 && result !== 1) throw new Error('access_unavailable');
    return result === 1;
  };
}
