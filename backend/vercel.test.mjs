import test from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import { once } from 'node:events';
import { spawn } from 'node:child_process';
import { createInterface } from 'node:readline';
import { fileURLToPath } from 'node:url';
import { randomBytes, randomUUID } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { createVercelHandler, readVercelConfig } from './vercel-handler.mjs';
import { RedisRest, RedisAccess, readRedisConfig, tokenHash, ISSUE, REVOKE } from './redis-access.mjs';

const env = { FEATHERLESS_API_KEY: 'synthetic-provider-key-testing', FEATHERLESS_MODEL: 'test/model',
  UPSTASH_REDIS_REST_URL: 'https://synthetic-test.upstash.io', UPSTASH_REDIS_REST_TOKEN: 'synthetic-redis-test-token' };
const auth = token => `Bearer ${token}`;
const response = () => Response.json({ choices: [{ message: { content: 'Baik.' } }] });

test('Vercel configuration never falls back to shared tokens or local files', () => {
  assert.equal(readVercelConfig(env).dailyLimit, 100);
  assert.equal(readRedisConfig({ KV_REST_API_URL: env.UPSTASH_REDIS_REST_URL,
    KV_REST_API_TOKEN: env.UPSTASH_REDIS_REST_TOKEN }).token, env.UPSTASH_REDIS_REST_TOKEN);
  for (const change of [
    { UPSTASH_REDIS_REST_URL: undefined }, { UPSTASH_REDIS_REST_URL: 'http://synthetic-test.upstash.io' },
    { UPSTASH_REDIS_REST_URL: 'https://synthetic-test.upstash.io.evil.example' },
    { UPSTASH_REDIS_REST_URL: 'https://user@synthetic-test.upstash.io' },
    { UPSTASH_REDIS_REST_URL: 'https://synthetic-test.upstash.io/path' },
    { UPSTASH_REDIS_REST_URL: 'https://synthetic-test.upstash.io?token=private' },
    { UPSTASH_REDIS_REST_TOKEN: 'private\r\nvalue' }, { ZIKRA_REDIS_PREFIX: 'bad namespace' },
    { NODE_TLS_REJECT_UNAUTHORIZED: '0' }, { ZIKRA_DAILY_LIMIT: '0' },
    { ZIKRA_DAILY_LIMIT: '1001' }, { FEATHERLESS_API_KEY: '' }
  ]) assert.throws(() => readVercelConfig({ ...env, ...change }), error => {
    assert.ok(!error.message.includes(env.UPSTASH_REDIS_REST_TOKEN)); return true;
  });
  assert.throws(() => createVercelHandler({ FEATHERLESS_API_KEY: env.FEATHERLESS_API_KEY,
    FEATHERLESS_MODEL: env.FEATHERLESS_MODEL, ZIKRA_CLIENT_TOKEN: 'old-private-token-32-characters-long' }));
});

test('Redis transport uses a bounded HTTPS POST, header-only credentials and no redirects', async () => {
  const config = readRedisConfig(env);
  const redis = new RedisRest({ ...config, fetchImpl: async (url, request) => {
    assert.equal(url, env.UPSTASH_REDIS_REST_URL);
    assert.equal(request.headers.Authorization, `Bearer ${env.UPSTASH_REDIS_REST_TOKEN}`);
    assert.equal(request.redirect, 'error'); assert.equal(request.method, 'POST');
    assert.deepEqual(JSON.parse(request.body), ['GET', 'example']);
    assert.ok(!request.body.includes(env.UPSTASH_REDIS_REST_TOKEN));
    return Response.json({ result: null });
  } });
  assert.equal(await redis.command(['GET', 'example']), null);
  for (const fake of [() => Response.json({ error: 'sensitive-details' }),
    () => new Response('private-details', { status: 401 }), () => new Response('{'),
    () => new Response('x'.repeat(65537)), () => { throw new Error('private-details'); }]) {
    await assert.rejects(new RedisRest({ ...config, fetchImpl: fake }).command(['GET', 'example']),
      { message: 'access_unavailable' });
  }
});

test('Vercel entry points pin paths and deployment excludes private config and admin/test code', () => {
  const config = JSON.parse(readFileSync(new URL('./vercel.json', import.meta.url)));
  assert.equal(config.functions['api/*.js'].supportsCancellation, true);
  assert.equal(config.functions['api/*.js'].maxDuration, 60);
  assert.deepEqual(config.rewrites, [{ source: '/health', destination: '/api/health' },
    { source: '/v1/assistant', destination: '/api/assistant' }]);
  for (const [entry, path] of [['health', '/health'], ['assistant', '/v1/assistant']])
    assert.ok(readFileSync(new URL(`./api/${entry}.js`, import.meta.url), 'utf8').includes(`req.url = '${path}'`));
  const allowlist = readFileSync(new URL('./.vercelignore', import.meta.url), 'utf8');
  assert.equal(allowlist.split(/\r?\n/)[0], '*');
  assert.ok(!allowlist.includes('!.env')); assert.ok(!allowlist.includes('!*.mjs'));
});

async function fixture(t) {
  const child = spawn(process.env.ZIKRA_REDIS_TEST_PYTHON,
    [fileURLToPath(new URL('./test-support/redis-fixture.py', import.meta.url))],
    { stdio: ['pipe', 'pipe', 'pipe'], windowsHide: true });
  const pending = [];
  const fail = () => { while (pending.length) pending.shift().reject(new Error('Redis fixture unavailable')); };
  child.on('error', fail); child.on('exit', fail);
  const lines = createInterface({ input: child.stdout });
  lines.on('line', line => {
    const request = pending.shift(); if (!request) return;
    const data = JSON.parse(line);
    if (data.error) request.reject(new Error('access_unavailable')); else request.resolve(data.result);
  });
  t.after(() => { child.stdin.end(); child.kill(); lines.close(); });
  return { command: args => new Promise((resolve, reject) => {
    pending.push({ resolve, reject }); child.stdin.write(JSON.stringify(args) + '\n');
  }) };
}
async function issue(redis, prefix = 'zikra:v1:') {
  const token = randomBytes(32).toString('hex'), id = randomUUID(), hash = tokenHash(token);
  assert.equal(await redis.command(['EVAL', ISSUE, 2, `${prefix}client:${hash}`, `${prefix}installation:${id}`,
    JSON.stringify({ id, revoked: false }), hash]), 1);
  return { id, token, hash };
}
const integration = { skip: !process.env.ZIKRA_REDIS_TEST_PYTHON, timeout: 15000 };

test('production Lua: quotas and revocation persist across new Vercel instances', integration, async t => {
  const redis = await fixture(t), client = await issue(redis);
  const first = new RedisAccess({ redis, dailyLimit: 2 });
  assert.equal(await first.identify(auth(client.token)), client.id);
  assert.equal(await first.identify('Bearer shared-private-code-32-characters'), null);
  const grant = await first.acquire(client.id, auth(client.token)); assert.ok(grant.allowed);
  await first.release(client.id, grant.lease);
  const second = new RedisAccess({ redis, dailyLimit: 2 });
  const next = await second.acquire(client.id, auth(client.token)); assert.ok(next.allowed);
  await second.release(client.id, next.lease);
  assert.deepEqual(await first.acquire(client.id, auth(client.token)),
    { allowed: false, status: 429, error: 'quota_exceeded' });
  const clock = await redis.command(['TIME']), day = Math.floor(Number(clock[0]) / 86400);
  await redis.command(['HSET', `zikra:v1:usage:${client.id}`, 'day', day - 1, 'minute', 0]);
  const reset = await second.acquire(client.id, auth(client.token)); assert.ok(reset.allowed);
  await second.release(client.id, reset.lease);
  assert.equal(await redis.command(['EVAL', REVOKE, 1, `zikra:v1:client:${client.hash}`, client.id]), 1);
  assert.equal(await first.identify(auth(client.token)), null);
  assert.equal((await second.acquire(client.id, auth(client.token))).status, 401);
});

test('production Lua: concurrent processes share two global slots, one per installation and expiring leases', integration, async t => {
  const redis = await fixture(t), clients = await Promise.all([issue(redis), issue(redis), issue(redis)]);
  const stores = clients.map(() => new RedisAccess({ redis }));
  const grants = await Promise.all(Array.from({ length: 12 }, (_, i) => {
    const n = i % 3; return stores[n].acquire(clients[n].id, auth(clients[n].token));
  }));
  assert.equal(grants.filter(g => g.allowed).length, 2);
  for (let i = 0; i < grants.length; i++) if (grants[i].allowed)
    await stores[i % 3].release(clients[i % 3].id, grants[i].lease);
  const current = await stores[0].acquire(clients[0].id, auth(clients[0].token)); assert.ok(current.allowed);
  await stores[0].release(clients[0].id, randomUUID());
  assert.equal((await stores[0].acquire(clients[0].id, auth(clients[0].token))).error, 'try_later');
  await redis.command(['ZADD', 'zikra:v1:active', 0, current.lease]);
  await redis.command(['ZADD', `zikra:v1:active:${clients[0].id}`, 0, current.lease]);
  const recovered = await stores[0].acquire(clients[0].id, auth(clients[0].token)); assert.ok(recovered.allowed);
  await stores[0].release(clients[0].id, recovered.lease);
});

test('production Lua: minute quota, namespaces, storage corruption and malformed responses fail closed', integration, async t => {
  const redis = await fixture(t), client = await issue(redis), access = new RedisAccess({ redis });
  for (let i = 0; i < 10; i++) {
    const grant = await access.acquire(client.id, auth(client.token)); assert.ok(grant.allowed);
    await access.release(client.id, grant.lease);
  }
  assert.equal((await access.acquire(client.id, auth(client.token))).error, 'quota_exceeded');
  assert.equal(await new RedisAccess({ redis, prefix: 'preview:' }).identify(auth(client.token)), null);
  await redis.command(['HSET', `zikra:v1:usage:${client.id}`, 'minute', 0]);
  const reset = await access.acquire(client.id, auth(client.token)); assert.ok(reset.allowed);
  await access.release(client.id, reset.lease);
  await redis.command(['HSET', `zikra:v1:usage:${client.id}`, 'count', 'invalid']);
  await assert.rejects(access.acquire(client.id, auth(client.token)));
  await redis.command(['SET', `zikra:v1:client:${client.hash}`, '{}']);
  await assert.rejects(access.identify(auth(client.token)));
  const invalid = new RedisAccess({ redis: { command: async () => [1, 'unexpected'] } });
  await assert.rejects(invalid.acquire(client.id, auth(client.token)));
});

test('Vercel HTTP: two handlers share access state, invalid input releases slots, failures never call provider', integration, async t => {
  const redis = await fixture(t), client = await issue(redis); let calls = 0;
  const options = { redis, fetchImpl: async () => { calls++; return response(); } };
  const servers = [createVercelHandler(env, options), createVercelHandler(env, options)].map(handler => http.createServer(handler));
  for (const server of servers) {
    server.listen(0, '127.0.0.1'); await once(server, 'listening');
    t.after(() => new Promise(resolve => { server.closeAllConnections(); server.close(resolve); }));
  }
  const send = (index, body, token = client.token) => fetch(`http://127.0.0.1:${servers[index].address().port}/v1/assistant`,
    { method: 'POST', headers: { Authorization: auth(token), 'Content-Type': 'application/json' }, body });
  const health = await fetch(`http://127.0.0.1:${servers[0].address().port}/health`);
  assert.deepEqual(await health.json(), { status: 'ok' });
  assert.equal((await send(0, '{')).status, 400);
  const valid = JSON.stringify({ messages: [{ role: 'user', content: 'Halo' }] });
  assert.equal((await send(1, valid)).status, 200);
  assert.equal((await send(0, valid, randomBytes(32).toString('hex'))).status, 401);
  await redis.command(['EVAL', REVOKE, 1, `zikra:v1:client:${client.hash}`, client.id]);
  assert.equal((await send(1, valid)).status, 401);
  await redis.command(['SET', `zikra:v1:client:${client.hash}`, '{']);
  const unavailable = await send(0, valid); assert.equal(unavailable.status, 503);
  assert.deepEqual(await unavailable.json(), { error: 'access_unavailable' }); assert.equal(calls, 1);
});
