import test from 'node:test';
import assert from 'node:assert/strict';
import { once } from 'node:events';
import { createGateway, validateMessages } from './server.mjs';

const token = 'private-test-code-32-characters-long';
const message = { role: 'user', content: 'Bantu atur belajar.' };
const json = (value, status = 200) => new Response(JSON.stringify(value), { status });
async function setup(t, fetchImpl) {
  const server = createGateway({ apiKey: 'server-only-test-key', model: 'test/model',
    clientToken: token, fetchImpl });
  server.listen(0, '127.0.0.1'); await once(server, 'listening');
  t.after(() => new Promise(resolve => { server.closeAllConnections();server.close(resolve); }));
  return async (messages = [message], auth = token) => fetch(
    `http://127.0.0.1:${server.address().port}/v1/assistant`,
    { method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${auth}` },
      body: JSON.stringify({ messages }) });
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
  assert.equal((await send()).status, 413);
});
test('rate limit prevents unlimited upstream requests', async t => {
  let requests = 0;
  const send = await setup(t, () => { requests++;return json({ choices: [{ message: { content: 'Baik.' } }] }); });
  for (let i = 0; i < 10; i++) assert.equal((await send()).status, 200);
  assert.equal((await send()).status, 429); assert.equal(requests, 10);
});
