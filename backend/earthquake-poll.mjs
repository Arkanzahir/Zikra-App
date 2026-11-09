import { randomUUID, timingSafeEqual } from 'node:crypto';
import { loadQuakes, MAX_ALERT_AGE_MS } from './earthquake-feed.mjs';
import { createFcmSender, readPushConfig } from './earthquake-fcm.mjs';
import { RedisRest, readRedisConfig } from './redis-access.mjs';

const RETENTION = 7 * 24 * 60 * 60;
export const RELEASE_POLL = `if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) end return 0`;
export const BOOTSTRAP_QUAKES = `
if redis.call('EXISTS', KEYS[1]) == 1 then return 0 end
for i = 2, #KEYS do redis.call('SET', KEYS[i], 'baseline', 'EX', ARGV[2], 'NX') end
redis.call('SET', KEYS[1], ARGV[1]); return 1
`;

/** Durable at-most-once reservation; clients also deduplicate before presenting a notification. */
export async function pollEarthquakes({ redis, prefix, send, load = loadQuakes, clock = Date.now }) {
  const lease = randomUUID(), key = prefix + 'e02:poll';
  if (await redis.command(['SET', key, lease, 'NX', 'PX', 55000]) !== 'OK') return { state: 'busy', accepted: 0 };
  try {
    if (await redis.command(['SET', prefix + 'e02:interval', '1', 'NX', 'EX', 60]) !== 'OK') return { state: 'throttled', accepted: 0 };
    const started = clock(), events = await load({ now: started });
    if (!events.length || events.length > 90) throw new Error('invalid_poll_batch');
    const initialized = await redis.command(['EVAL', BOOTSTRAP_QUAKES, events.length + 1,
      prefix + 'e02:initialized', ...events.map(event => prefix + 'e02:event:' + event.id), started, RETENTION]);
    if (initialized === 1) return { state: 'baseline', accepted: 0 };
    if (initialized !== 0) throw new Error('invalid_poll_storage');
    const pending = events.filter(event => event.at <= started && started - event.at <= MAX_ALERT_AGE_MS);
    // Validate cloud authorization before reserving any new event.
    await send.prepare?.();
    let accepted = 0, uncertain = 0;
    for (const event of pending) {
      if (accepted + uncertain >= 3 || clock() - started > 40000) break;
      const eventKey = prefix + 'e02:event:' + event.id;
      if (await redis.command(['SET', eventKey, 'reserved', 'NX', 'EX', RETENTION]) !== 'OK') continue;
      try {
        const receipt = await send(event);
        // If this write fails after FCM accepted, the reservation remains and prevents resending.
        await redis.command(['SET', eventKey, 'accepted:' + receipt, 'EX', RETENTION]); accepted++;
      } catch (error) {
        if (error.notAccepted === true) {
          await redis.command(['DEL', eventKey]);
          throw new Error('push_temporarily_rejected');
        }
        uncertain++; break;
      }
    }
    return { state: uncertain ? 'delivery_uncertain' : 'checked', accepted, uncertain };
  } finally { await redis.command(['EVAL', RELEASE_POLL, 1, key, lease]); }
}

function authorize(header, expected) {
  if (typeof expected !== 'string' || !/^[a-f0-9]{64}$/.test(expected) || typeof header !== 'string') return false;
  const supplied = Buffer.from(header), target = Buffer.from('Bearer ' + expected);
  return supplied.length === target.length && timingSafeEqual(supplied, target);
}

export function createEarthquakePollHandler(env = process.env, overrides = {}) {
  return async (req, res) => {
    const respond = (status, value) => {
      res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff' });
      res.end(JSON.stringify(value));
    };
    if (req.method !== 'POST') return respond(405, { error: 'method_not_allowed' });
    if (!authorize(req.headers.authorization, env.ZIKRA_QUAKE_CRON_SECRET)) return respond(401, { error: 'unauthorized' });
    try {
      const config = readRedisConfig(env);
      const send = overrides.send ?? createFcmSender(readPushConfig(env));
      const result = await pollEarthquakes({ redis: overrides.redis ?? new RedisRest(config), prefix: config.prefix,
        send, load: overrides.load, clock: overrides.clock });
      respond(result.state === 'delivery_uncertain' ? 503 : 200, result);
    } catch { respond(503, { error: 'earthquake_push_unavailable' }); }
  };
}

export default createEarthquakePollHandler();
