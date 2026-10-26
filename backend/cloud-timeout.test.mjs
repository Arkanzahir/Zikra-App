import test from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import { once } from 'node:events';
import { runTimeoutFixture } from './test-support/cloud-timeout-fixture.mjs';
import fixtureApi from './test-support/cloud-timeout-api.mjs';

test('cloud fixture uses real HTTP abort before headers and during body, then explicit retry', async () => {
  for (const stage of ['headers', 'body']) {
    const report = await runTimeoutFixture(stage, { providerTimeoutMs: 150 });
    assert.equal(report.passed, true, JSON.stringify(report));
    assert.equal(report.usesProductionDefaultDeadline, false);
    assert.equal(report.externalInferenceRequests, 0);
  }
  await assert.rejects(runTimeoutFixture('unknown'), /invalid_fixture_stage/);
  await assert.rejects(runTimeoutFixture('headers', { providerTimeoutMs: 0 }), /invalid_fixture_deadline/);
});

test('preview fixture denies missing/expired credentials and invalid stage without work', async t => {
  const keys = ['ZIKRA_TIMEOUT_FIXTURE_TOKEN', 'ZIKRA_TIMEOUT_FIXTURE_EXPIRES'];
  const old = Object.fromEntries(keys.map(key => [key, process.env[key]]));
  t.after(() => { for (const key of keys) {
    if (old[key] === undefined) delete process.env[key]; else process.env[key] = old[key];
  } });
  const server = http.createServer(fixtureApi);
  server.listen(0, '127.0.0.1'); await once(server, 'listening');
  t.after(() => new Promise(resolve => { server.closeAllConnections(); server.close(resolve); }));
  const url = `http://127.0.0.1:${server.address().port}`;
  delete process.env.ZIKRA_TIMEOUT_FIXTURE_TOKEN;
  assert.equal((await fetch(url)).status, 503);
  process.env.ZIKRA_TIMEOUT_FIXTURE_TOKEN = 'a'.repeat(64);
  process.env.ZIKRA_TIMEOUT_FIXTURE_EXPIRES = String(Date.now() + 60000);
  assert.equal((await fetch(url)).status, 401);
  const headers = { Authorization: `Bearer ${'a'.repeat(64)}` };
  assert.equal((await fetch(url, { headers })).status, 405);
  assert.equal((await fetch(url, { method: 'POST', headers })).status, 400);
  process.env.ZIKRA_TIMEOUT_FIXTURE_EXPIRES = String(Date.now() - 1);
  assert.equal((await fetch(url, { headers })).status, 503);
});
