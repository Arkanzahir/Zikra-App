import test from 'node:test';
import assert from 'node:assert/strict';
import { QuranSyncCache, PUBLISH_SYNC, RELEASE_SYNC_LOCK } from './quran-sync-cache.mjs';
import { RedisRest } from './redis-access.mjs';
import { redisFixture } from './test-support/redis-fixture.mjs';

function fixture() {
  const data = new Map();
  const redis = { command: async ([command, ...args]) => {
    if (command === 'GET') return data.get(args[0]) ?? null;
    if (command === 'SET') {
      if (args.includes('NX') && data.has(args[0])) return null;
      data.set(args[0], args[1]); return 'OK';
    }
    if (command === 'EVAL') {
      const [script, , key, ...values] = args;
      if (script === RELEASE_SYNC_LOCK) return data.get(key) === values[0] ? Number(data.delete(key)) : 0;
      if (script === PUBLISH_SYNC) {
        const [pointer, lease, meta] = values;
        if (data.get(key) !== lease) return 0;
        data.set(pointer, meta); return 1;
      }
    }
    throw Error('unexpected-command');
  } };
  return { data, redis, cache: new QuranSyncCache(redis, 'zikra:v1:', '1'.repeat(64)) };
}
const state = { token: 'synthetic-private-checkpoint', checkedAt: 123, unavailable: new Set(),
  resources: new Map([['quran_core:1', new Map([['verse:1', { id: 1, text: '  source\ntext  ' }]])]]) };

test('durable generation restores checkpoint and exact source text', async () => {
  const { cache } = fixture(); const lease = await cache.acquire();
  await cache.publish(state, lease); assert.deepEqual(await cache.load(), state); await cache.release(lease);
});
test('expired writer cannot replace a valid published generation', async () => {
  const { cache, data } = fixture(); const lease = await cache.acquire(); await cache.publish(state, lease);
  data.set(cache.prefix + 'lock', 'different-owner');
  await assert.rejects(cache.publish({ ...state, token: 'wrong' }, lease));
  assert.equal((await cache.load()).token, state.token);
});
test('missing/corrupted chunks never become accepted state', async () => {
  const { cache, data } = fixture(); const lease = await cache.acquire(); await cache.publish(state, lease);
  const chunk = [...data.keys()].find(key => /:0$/.test(key)); data.set(chunk, 'AAAA');
  await assert.rejects(cache.load()); data.delete(chunk); await assert.rejects(cache.load());
});
test('partial write leaves old pointer and lock release cannot affect another owner', async () => {
  const { cache, data } = fixture(); const lease = await cache.acquire(); await cache.publish(state, lease);
  const original = data.get(cache.prefix + 'current'); data.set(cache.prefix + 'lock', 'other');
  await cache.release(lease); assert.equal(data.get(cache.prefix + 'lock'), 'other');
  assert.equal(data.get(cache.prefix + 'current'), original); assert.equal(await cache.acquire(), null);
});
test('Redis content response budget is opt-in; ordinary access checks keep old bound', async () => {
  const config = { url: 'https://synthetic.upstash.io', token: 'synthetic',
    fetchImpl: async () => Response.json({ result: 'x'.repeat(70000) }) };
  await assert.rejects(new RedisRest(config).command(['GET', 'synthetic']));
  assert.equal((await new RedisRest({ ...config, responseLimit: 524288 }).command(['GET', 'synthetic'])).length, 70000);
  assert.throws(() => new RedisRest({ ...config, responseLimit: 1000000 }));
});

test('cache ciphertext hides source/checkpoint and wrong keys cannot restore it', async () => {
  const { cache, data, redis } = fixture(); const lease = await cache.acquire();
  await cache.publish(state, lease);
  assert.ok(![...data.values()].join('').includes(state.token));
  await assert.rejects(new QuranSyncCache(redis, 'zikra:v1:', '2'.repeat(64)).load());
  assert.deepEqual(await cache.load(), state);
});

test('production publication Lua fences stale writers on Redis', {
  skip: !process.env.ZIKRA_REDIS_TEST_PYTHON
}, async t => {
  const redis = await redisFixture(t);
  const cache = new QuranSyncCache(redis, 'quran-test:', '3'.repeat(64));
  const lease = await cache.acquire(); await cache.publish(state, lease);
  assert.deepEqual(await cache.load(), state);
  await redis.command(['SET', cache.prefix + 'lock', 'next-owner']);
  await assert.rejects(cache.publish({ ...state, token: 'stale' }, lease));
  await cache.release(lease);
  assert.equal(await redis.command(['GET', cache.prefix + 'lock']), 'next-owner');
  assert.equal((await cache.load()).token, state.token);
});
