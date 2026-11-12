import test from 'node:test';
import assert from 'node:assert/strict';
import { createQuranSyncTransport } from './quran-sync-transport.mjs';
const env = { QF_ENV: 'production', QF_CLIENT_ID: 'synthetic-client', QF_CLIENT_SECRET: 'synthetic-secret-for-test-only' };
const path = '/api/v4/resources/sync?bootstrap=true';
test('fixed-origin transport authorizes server-side and reuses bounded token', async () => {
  const calls = [];
  const read = createQuranSyncTransport(env, { fetchImpl: async (url, options) => {
    calls.push(url); assert.equal(options.redirect, 'error'); assert.ok(options.signal);
    if (url.includes('/oauth2/token')) return Response.json({ access_token: 'synthetic-access', expires_in: 300 });
    assert.equal(options.headers['x-auth-token'], 'synthetic-access');
    return Response.json({ sync: {} });
  } });
  await read(path); await read(path); assert.equal(calls.length, 3);
  assert.ok(calls.every(url => !url.includes(env.QF_CLIENT_SECRET)));
});
test('hostile paths are rejected before sending authorization anywhere', async () => {
  let calls = 0;
  const read = createQuranSyncTransport(env, { fetchImpl: async () => { calls++; } });
  await assert.rejects(read('https://evil.test/api/v4/resources/sync')); assert.equal(calls, 0);
});
test('token expiry and unauthorized response cannot leave stale credentials in use', async () => {
  let auth = 0, content = 0;
  const read = createQuranSyncTransport(env, { fetchImpl: async url => {
    if (url.includes('/oauth2/token')) { auth++; return Response.json({ access_token: 'synthetic', expires_in: 60 }); }
    content++; return content === 1 ? Response.json({}, { status: 401 }) : Response.json({ sync: {} });
  } });
  await assert.rejects(read(path)); await read(path); assert.equal(auth, 2);
});
test('unknown environment, malformed authorization and oversized content fail closed', async () => {
  assert.throws(() => createQuranSyncTransport({ ...env, QF_ENV: 'prelive' }));
  const read = createQuranSyncTransport(env, { fetchImpl: async () => Response.json({ access_token: 'synthetic', expires_in: 0 }) });
  await assert.rejects(read(path));
  const oversized = createQuranSyncTransport(env, { fetchImpl: async url => url.includes('/oauth2/token')
    ? Response.json({ access_token: 'synthetic', expires_in: 300 })
    : new Response('{}', { headers: { 'Content-Length': '100000000' } }) });
  await assert.rejects(oversized(path));
});
