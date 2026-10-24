import test from 'node:test';
import assert from 'node:assert/strict';
import { once } from 'node:events';
import { validateSelection, providerReply } from './assistant-contract.mjs';
import { createGateway } from './server.mjs';
const id = '8da51639-4899-42f0-9dc4-66723b358667';
test('selected context rejects hidden fields, invalid prayer time and unconsented actions', () => {
  assert.deepEqual(validateSelection({}).context, {});
  assert.equal(validateSelection({ context: { gps: 'private' } }), null);
  assert.equal(validateSelection({ context: { tasks: [{ id: 1, title: 'Test', dueAt: 0, completed: false }] } }), null);
  assert.equal(validateSelection({ features: { taskProposals: true } }), null);
  assert.equal(validateSelection({ context: { prayer: { date: '2026-10-25', zone: 'Asia/Jakarta',
    items: [{ name: 'Subuh', time: '24:99' }] } } }), null);
  const content = JSON.stringify({ reply: 'Usulan belum disimpan.', proposal: { kind: 'add_task', title: 'Belajar', dueAt: 0 } });
  assert.ok(!providerReply(content, validateSelection({})).proposal);
  assert.equal(providerReply(content, validateSelection({ requestId: id, features: { taskProposals: true } })).proposal.id, id);
});
test('edit can only propose a selected incomplete task and cannot complete/delete it', () => {
  const selection = validateSelection({ requestId: id, features: { taskProposals: true }, context: {
    tasks: [{ id: '1', title: 'Original', dueAt: 0, completed: false }] } });
  const proposal = { kind: 'edit_task', title: 'Changed', dueAt: 0, taskId: '1' };
  const parse = () => providerReply(JSON.stringify({ reply: 'Tinjau.', proposal }), selection);
  assert.equal(parse().proposal.expectedTitle, 'Original');
  proposal.taskId = '2'; assert.ok(!parse().proposal);
  proposal.taskId = '1'; proposal.completed = true; assert.ok(!parse().proposal);
  delete proposal.completed; selection.context.tasks[0].completed = true; assert.ok(!parse().proposal);
});
test('source failure never calls model; valid source is returned separately from model summary', async t => {
  let calls = 0;
  const server = createGateway({ apiKey: 'synthetic-server-provider-key', model: 'test/model',
    clientToken: 'private-test-code-32-characters-long', fetchImpl: async () => {
      calls++; return Response.json({ choices: [{ message: { content: 'Ringkasan dari petikan.' } }] });
    }, sourceFetchImpl: async url => url.endsWith('/114') ? new Response('gone', { status: 503 })
      : Response.json({ code: 200, data: { nomor: 113, tafsir: [{ ayat: 1, teks: 'Synthetic public source for the test only.' }] } }) });
  server.listen(0, '127.0.0.1'); await once(server, 'listening');
  t.after(() => new Promise(resolve => { server.closeAllConnections(); server.close(resolve); }));
  async function send(chapter) {
    return fetch(`http://127.0.0.1:${server.address().port}/v1/assistant`, { method: 'POST',
      headers: { Authorization: 'Bearer private-test-code-32-characters-long', 'Content-Type': 'application/json' },
      body: JSON.stringify({ messages: [{ role: 'user', content: 'Jelaskan ayat' }], verse: { chapter, number: 1 } }) });
  }
  const failed = await send(114); assert.equal(failed.status, 503); assert.equal(calls, 0);
  assert.deepEqual(await failed.json(), { error: 'source_unavailable' });
  const success = await send(113), body = await success.json();
  assert.equal(success.status, 200); assert.equal(calls, 1);
  assert.equal(body.source.url, 'https://equran.id/tafsir/113');
  assert.equal(body.reply, 'Ringkasan dari petikan.');
});
