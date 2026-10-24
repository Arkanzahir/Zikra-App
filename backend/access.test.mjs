import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtempSync, readFileSync, writeFileSync, rmSync } from 'node:fs';
import { join, resolve, dirname, basename } from 'node:path';
import { tmpdir } from 'node:os';
import { once } from 'node:events';
import { issueClient, revokeClient, InstallationAccess } from './access.mjs';
import { createGateway } from './server.mjs';

function fixture(t) {
  const directory = mkdtempSync(join(tmpdir(), 'zikra-access-'));
  t.after(() => {
    assert.equal(dirname(resolve(directory)), resolve(tmpdir()));
    assert.ok(basename(directory).startsWith('zikra-access-'));
    rmSync(directory, { recursive: true, force: true });
  });
  const clientsFile = join(directory, 'clients.json'), usageFile = join(directory, 'usage.json');
  const tokenFile = join(directory, 'token.env'), id = issueClient(clientsFile, tokenFile);
  return { clientsFile, usageFile, id, token: readFileSync(tokenFile, 'utf8').trim().split('=')[1] };
}
test('installation tokens are hashed, independently revocable and not shared client credentials', t => {
  const options = fixture(t), access = new InstallationAccess(options);
  assert.ok(!readFileSync(options.clientsFile, 'utf8').includes(options.token));
  assert.equal(access.identify(`Bearer ${options.token}`), options.id);
  assert.equal(access.identify('Bearer private-test-code-32-characters-long'), null);
  revokeClient(options.clientsFile, options.id);
  assert.equal(access.identify(`Bearer ${options.token}`), null);
});
test('daily and minute quotas survive process recreation, reset at UTC day boundary and fail closed on corruption', t => {
  const options = fixture(t), access = new InstallationAccess({ ...options, dailyLimit: 2 });
  const time = Date.parse('2026-10-25T01:00:00Z');
  assert.ok(access.consume(options.id, time));
  assert.ok(new InstallationAccess({ ...options, dailyLimit: 2 }).consume(options.id, time));
  assert.equal(new InstallationAccess({ ...options, dailyLimit: 2 }).consume(options.id, time), false);
  assert.ok(access.consume(options.id, time + 86400000));
  const minute = new InstallationAccess({ ...options, dailyLimit: 100 });
  for (let i = 0; i < 9; i++) assert.ok(minute.consume(options.id, time + 86400000));
  assert.equal(new InstallationAccess({ ...options, dailyLimit: 100 }).consume(options.id, time + 86400000), false);
  assert.ok(minute.consume(options.id, time + 86460000));
  writeFileSync(options.usageFile, '{');
  assert.throws(() => access.consume(options.id, time));
});
test('installation gateway enforces revocation/quotas before provider and isolates clients behind one proxy', async t => {
  const options = fixture(t), secondFile = join(options.clientsFile, '..', 'second.env');
  issueClient(options.clientsFile, secondFile);
  const second = readFileSync(secondFile, 'utf8').trim().split('=')[1];
  let calls = 0;
  const server = createGateway({ ...options, accessMode: 'installation', dailyLimit: 1,
    apiKey: 'synthetic-server-only-key', model: 'test/model',
    fetchImpl: async () => { calls++; return Response.json({ choices: [{ message: { content: 'Baik.' } }] }); } });
  server.listen(0, '127.0.0.1'); await once(server, 'listening');
  t.after(() => new Promise(resolve => { server.closeAllConnections(); server.close(resolve); }));
  async function send(token) {
    const response = await fetch(`http://127.0.0.1:${server.address().port}/v1/assistant`, {
      method: 'POST', headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json',
        'X-Forwarded-For': '203.0.113.10' }, body: JSON.stringify({ messages: [{ role: 'user', content: 'Halo' }] }) });
    await response.arrayBuffer(); return response.status;
  }
  assert.equal(await send(options.token), 200);
  assert.equal(await send(options.token), 429);
  assert.equal(await send(second), 200);
  revokeClient(options.clientsFile, options.id);
  assert.equal(await send(options.token), 401);
  assert.equal(calls, 2);
  writeFileSync(options.clientsFile, '{}');
  assert.equal(await send(second), 503);
  assert.equal(calls, 2);
});
