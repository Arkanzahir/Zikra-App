import test from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import { once } from 'node:events';
import { createQuranHandler } from './quran-sync-handler.mjs';
const token = 'a'.repeat(64), id = '11111111-1111-4111-8111-111111111111';
const env = { ZIKRA_QURAN_SYNC_ENABLED: 'true', UPSTASH_REDIS_REST_URL: 'https://synthetic.upstash.io',
  UPSTASH_REDIS_REST_TOKEN: 'synthetic-redis-credential' };
async function setup(t, extra = {}, revoked = false) {
  const calls = [];
  const handler = createQuranHandler({ ...env, ...extra }, {
    redis: { command: async args => {
      assert.equal(args[0], 'GET'); return JSON.stringify({ id, revoked });
    } }, service: { page: async (...args) => { calls.push(args); return { verses: [], pagination: {}, zikra_sync: {} }; } }
  });
  const server = http.createServer(handler); server.listen(0, '127.0.0.1'); await once(server, 'listening');
  t.after(() => { server.closeAllConnections(); server.close(); });
  return { calls, request: (path, options = {}) => fetch('http://127.0.0.1:' + server.address().port + path,
    { headers: { Authorization: 'Bearer ' + token }, ...options }) };
}
test('only authorized installations can access content; no provider consent/header or inference is used', async t => {
  const { calls, request } = await setup(t);
  const response = await request('/v1/quran?chapter=67&page=1');
  assert.equal(response.status, 200); assert.deepEqual(calls, [[67, 1, id]]);
  assert.equal(response.headers.get('cache-control'), 'no-store');
  assert.equal((await request('/v1/quran?chapter=1&page=1', { headers: {} })).status, 401);
  assert.equal(calls.length, 1);
});
test('disabled route and revoked installation do not invoke source work', async t => {
  const disabled = await setup(t, { ZIKRA_QURAN_SYNC_ENABLED: 'false' });
  assert.equal((await disabled.request('/v1/quran?chapter=1&page=1')).status, 503);
  assert.equal(disabled.calls.length, 0);
  const revoked = await setup(t, {}, true);
  assert.equal((await revoked.request('/v1/quran?chapter=1&page=1')).status, 401);
  assert.equal(revoked.calls.length, 0);
});
test('duplicate/unrelated query fields, other routes and POST cannot reach source', async t => {
  const { calls, request } = await setup(t);
  for (const path of ['/v1/quran?chapter=1&chapter=2&page=1', '/v1/quran?chapter=1&page=1&url=https://evil.test',
    '/v1/quran?chapter=1&page=0', '/v1/other?chapter=1&page=1']) {
    assert.equal((await request(path)).status, 400);
  }
  assert.equal((await request('/v1/quran', { method: 'POST' })).status, 405);
  assert.equal(calls.length, 0);
});
