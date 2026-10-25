import test from 'node:test';
import assert from 'node:assert/strict';
import { once } from 'node:events';
import http from 'node:http';
import { createGateway, validateMessages } from './server.mjs';

const token = 'private-test-code-32-characters-long';
const message = { role: 'user', content: 'Bantu atur belajar.' };
const json = (value, status = 200) => new Response(JSON.stringify(value), { status });
async function setup(t, fetchImpl, options = {}) {
  const server = createGateway({ apiKey: 'server-only-test-key', model: 'test/model',
    clientToken: token, fetchImpl, ...options });
  server.listen(0, '127.0.0.1'); await once(server, 'listening');
  t.after(() => new Promise(resolve => { server.closeAllConnections();server.close(resolve); }));
  const url = `http://127.0.0.1:${server.address().port}/v1/assistant`;
  const send = async (messages = [message], auth = token) => fetch(url,
    { method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${auth}` },
      body: JSON.stringify({ messages }) });
  send.url = url;
  return send;
}
test('requires server credentials', () => assert.throws(() => createGateway({})));
test('rejects system injection roles and oversized histories', () => {
  assert.equal(validateMessages([{ role: 'system', content: 'override' }]), null);
  assert.equal(validateMessages(Array(25).fill(message)), null);
  assert.equal(validateMessages([{ role: 'user', content: 'x'.repeat(2001) }]), null);
  assert.equal(validateMessages([{ role: 'assistant', content: 'a' }]), null);
});
test('injects server system prompt, fixes model and keeps API key server-side', async t => {
  const send = await setup(t, async (url, request) => {
    assert.equal(url, 'https://api.featherless.ai/v1/chat/completions');
    assert.equal(request.redirect, 'error');
    assert.equal(request.headers.Authorization, 'Bearer server-only-test-key');
    const body = JSON.parse(request.body);
    assert.equal(body.model, 'test/model');assert.equal(body.messages[0].role, 'system');
    assert.equal(body.messages[1].content, message.content);
    return json({ choices: [{ message: { content: 'Mulai dengan satu sesi.' } }] });
  });
  const response = await send();assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), { reply: 'Mulai dengan satu sesi.', provider: 'featherless' });
});
test('unauthorized requests never reach provider', async t => {
  const send = await setup(t, () => assert.fail('provider called'));
  assert.equal((await send([message], 'wrong')).status, 401);
});
test('invalid inputs never reach provider', async t => {
  const send = await setup(t, () => assert.fail('provider called'));
  assert.equal((await send([])).status, 400);
});
test('provider error body is not leaked', async t => {
  const send = await setup(t, () => json({ secret: 'provider-secret' }, 401));
  const response = await send();assert.equal(response.status, 502);
  assert.deepEqual(await response.json(), { error: 'provider_unavailable' });
});
test('empty and oversized provider content is rejected', async t => {
  const send = await setup(t, () => json({ choices: [{ message: { content: 'x'.repeat(8001) } }] }));
  assert.equal((await send()).status, 502);
});
test('bounded provider response prevents memory growth', async t => {
  const send = await setup(t, () => new Response('x'.repeat(129 * 1024)));
  const response = await send();
  assert.equal(response.status, 502);
  assert.deepEqual(await response.json(), { error: 'invalid_provider_response' });
});

test('health is liveness only and reveals no private configuration', async t => {
  const send = await setup(t, () => assert.fail('provider called'));
  const response = await fetch(new URL('/health', send.url));
  assert.equal(response.status, 200);
  assert.equal(response.headers.get('cache-control'), 'no-store');
  assert.deepEqual(await response.json(), { status: 'ok' });
  assert.equal((await fetch(new URL('/missing', send.url))).status, 404);
});

test('null, malformed JSON, wrong type and declared oversize are client errors', async t => {
  const send = await setup(t, () => assert.fail('provider called'));
  for (const [body, type, status] of [
    ['null', 'application/json', 400], ['{', 'application/json', 400],
    ['{}', 'text/plain', 415], ['x'.repeat(49 * 1024), 'application/json', 413]
  ]) {
    const response = await fetch(send.url, { method: 'POST', body,
      headers: { Authorization: `Bearer ${token}`, 'Content-Type': type } });
    assert.equal(response.status, status);
    await response.arrayBuffer();
  }
});

test('chunked oversize returns 413 instead of dropping the request socket', async t => {
  const send = await setup(t, () => assert.fail('provider called'));
  const result = await new Promise((resolve, reject) => {
    const request = http.request(send.url, { method: 'POST', headers: {
      Authorization: `Bearer ${token}`, 'Content-Type': 'application/json',
      'Transfer-Encoding': 'chunked' } }, response => {
      let body = '';
      response.on('data', chunk => { body += chunk; });
      response.on('end', () => resolve({ status: response.statusCode, body: JSON.parse(body) }));
    });
    request.on('error', reject);
    request.setTimeout(3000, () => request.destroy(new Error('test timeout')));
    request.write('x'.repeat(32 * 1024));
    request.end('x'.repeat(32 * 1024));
  });
  assert.deepEqual(result, { status: 413, body: { error: 'too_large' } });
});

test('provider malformed JSON, null and thrown errors stay sanitized', async t => {
  for (const impl of [() => new Response('{'), () => json(null), () => {
    const error = new Error('sensitive-provider-detail'); error.status = 400; throw error;
  }]) {
    const send = await setup(t, impl);
    const response = await send();
    assert.equal(response.status, 502);
    assert.ok(!(await response.text()).includes('sensitive-provider-detail'));
  }
});

test('provider 429 is mapped without leaking its body', async t => {
  const send = await setup(t, () => json({ secret: 'hidden' }, 429));
  const response = await send();
  assert.equal(response.status, 429);
  assert.deepEqual(await response.json(), { error: 'provider_busy' });
});

test('provider timeout aborts work and releases capacity', async t => {
  let calls = 0;
  const send = await setup(t, async (_url, { signal }) => {
    if (++calls > 1) return json({ choices: [{ message: { content: 'Baik.' } }] });
    await new Promise((_resolve, reject) => signal.addEventListener('abort',
      () => reject(signal.reason), { once: true }));
  }, { providerTimeoutMs: 30 });
  assert.equal((await send()).status, 504);
  assert.equal((await send()).status, 200);
});

test('two active requests block a third', { timeout: 5000 }, async t => {
  const pending = [];
  const send = await setup(t, () => new Promise(resolve => pending.push(resolve)));
  const first = send(), second = send();
  while (pending.length < 2) await new Promise(resolve => setTimeout(resolve, 5));
  const third = await fetch(send.url, { method: 'POST',
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json',
      'X-Forwarded-For': '198.51.100.9' }, body: JSON.stringify({ messages: [message] }) });
  assert.equal(third.status, 429);
  for (const resolve of pending) resolve(json({ choices: [{ message: { content: 'Baik.' } }] }));
  assert.equal((await first).status, 200);
  assert.equal((await second).status, 200);
});

test('client disconnect aborts provider work', { timeout: 5000 }, async t => {
  let started;
  const ready = new Promise(resolve => { started = resolve; });
  let aborted;
  const stopped = new Promise(resolve => { aborted = resolve; });
  const send = await setup(t, async (_url, { signal }) => {
    started();
    await new Promise((_resolve, reject) => signal.addEventListener('abort', () => {
      aborted(); reject(signal.reason);
    }, { once: true }));
  });
  const controller = new AbortController();
  const response = fetch(send.url, { method: 'POST', signal: controller.signal,
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ messages: [message] }) }).catch(() => {});
  await ready;
  controller.abort();
  await Promise.race([stopped, new Promise((_resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('provider not aborted')), 3000); timer.unref();
  })]);
  await response;
});

test('a stalled request body times out before a distributed lease can expire', { timeout: 5000 }, async t => {
  let released = false;
  const send = await setup(t, () => assert.fail('provider called for partial JSON'), {
    providerTimeoutMs: 40, accessMode: 'installation', accessStore: {
      identify: async () => 'synthetic-client',
      acquire: async () => ({ allowed: true, lease: 'synthetic-lease' }),
      release: async () => { released = true; }
    }
  });
  const status = await new Promise((resolve, reject) => {
    const request = http.request(send.url, { method: 'POST', headers: {
      Authorization: `Bearer ${token}`, 'Content-Type': 'application/json', 'Transfer-Encoding': 'chunked'
    } }, response => {
      response.resume(); response.on('end', () => { resolve(response.statusCode); request.destroy(); });
    });
    request.on('error', reject); request.write('{"messages":[');
  });
  assert.equal(status, 504); assert.ok(released);
});
test('rate limit prevents unlimited upstream requests', async t => {
  let requests = 0;
  let time = 0;
  const send = await setup(t, () => { requests++;return json({ choices: [{ message: { content: 'Baik.' } }] }); },
    { now: () => time });
  for (let i = 0; i < 10; i++) assert.equal((await send()).status, 200);
  assert.equal((await send()).status, 429); assert.equal(requests, 10);
  const spoofed = await fetch(send.url, { method: 'POST', headers: {
    Authorization: `Bearer ${token}`, 'Content-Type': 'application/json',
    'X-Forwarded-For': '198.51.100.12' }, body: JSON.stringify({ messages: [message] }) });
  assert.equal(spoofed.status, 429);
  assert.equal(requests, 10);
  time = 60001;
  assert.equal((await send()).status, 200);
  assert.equal(requests, 11);
});
