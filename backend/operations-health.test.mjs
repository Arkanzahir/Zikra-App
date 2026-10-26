import test from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import { once } from 'node:events';
import { checkDeployment } from './operations-health.mjs';
import { createVercelHandler } from './vercel-handler.mjs';

const origin = 'https://example.vercel.app';
test('monitor rejects unsafe origins before any request', async () => {
  let calls = 0;
  for (const value of ['http://example.vercel.app', 'https://user:secret@example.vercel.app',
    'https://example.vercel.app?token=private', 'https://example.vercel.app/x',
    'https://example.vercel.app.evil.test', 'https://localhost', 'https://example.vercel.app#private']) {
    await assert.rejects(checkDeployment(value, { fetchImpl: async () => { calls++; } }));
  }
  assert.equal(calls, 0);
});

test('monitor enters the production Redis auth path without quota or provider calls', async t => {
  let lookups = 0, inference = 0;
  const handler = createVercelHandler({ FEATHERLESS_API_KEY: 'synthetic-provider-key-for-ops-tests', FEATHERLESS_MODEL: 'test/model',
    UPSTASH_REDIS_REST_URL: 'https://synthetic.upstash.io', UPSTASH_REDIS_REST_TOKEN: 'synthetic-redis-ops-test-token' }, {
    redis: { command: async args => { assert.equal(args[0], 'GET'); assert.match(args[1], /^zikra:v1:client:[a-f0-9]{64}$/); lookups++; return null; } },
    fetchImpl: async () => { inference++; throw new Error('No inference expected'); }
  });
  const server = http.createServer(handler); server.listen(0, '127.0.0.1'); await once(server, 'listening');
  t.after(() => { server.closeAllConnections(); server.close(); });
  const report = await checkDeployment(origin, { fetchImpl: (url, options) => {
    assert.equal(options.redirect, 'error');
    assert.ok(options.signal instanceof AbortSignal);
    assert.ok(url.endsWith('/health') || url.endsWith('/v1/capabilities'));
    return fetch(`http://127.0.0.1:${server.address().port}${new URL(url).pathname}`, options);
  } });
  assert.equal(report.passed, true); assert.equal(lookups, 1); assert.equal(inference, 0);
  assert.equal(report.credentialsRequired, false); assert.equal(report.inferenceChecked, false);
  assert.ok(!JSON.stringify(report).includes('synthetic-provider'));
});

test('health alone cannot hide Redis failure or an unknown credential gaining access', async () => {
  for (const [status, value] of [[503, { error: 'access_unavailable' }], [200, { imageGeneration: true }],
    [401, { error: 'unauthorized', private: 'PRIVATE-RESPONSE-CONTENT' }]]) {
    const report = await checkDeployment(origin, { fetchImpl: async url => url.endsWith('/health')
      ? Response.json({ status: 'ok' }) : Response.json(value, { status }) });
    assert.equal(report.liveness.passed, true); assert.equal(report.passed, false);
    assert.ok(!JSON.stringify(report).includes('PRIVATE-RESPONSE-CONTENT'));
  }
});

test('invalid, oversized, redirected and failed probes produce bounded metadata only', async () => {
  for (const fetchImpl of [async () => new Response('{PRIVATE'), async () => new Response('PRIVATE'.repeat(1000)),
    async () => new Response('', { status: 302, headers: { Location: 'https://evil.test' } }),
    async () => { throw new Error('PRIVATE-CREDENTIAL'); }]) {
    const report = await checkDeployment(origin, { fetchImpl });
    assert.equal(report.passed, false); assert.ok(!JSON.stringify(report).includes('PRIVATE'));
  }
});

test('deadline includes waiting for response body', async t => {
  const server = http.createServer((req, res) => { res.writeHead(200, { 'Content-Type': 'application/json' }); res.write('{'); });
  server.listen(0, '127.0.0.1'); await once(server, 'listening');
  t.after(() => { server.closeAllConnections(); server.close(); });
  const start = performance.now();
  const report = await checkDeployment(origin, { timeoutMs: 100,
    fetchImpl: (url, options) => fetch(`http://127.0.0.1:${server.address().port}`, options) });
  assert.equal(report.passed, false); assert.ok(performance.now() - start < 3000);
});
