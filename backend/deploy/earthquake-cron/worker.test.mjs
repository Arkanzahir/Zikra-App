import { test } from 'node:test';
import assert from 'node:assert/strict';
import worker, { poll } from './worker.mjs';

const env = { ZIKRA_QUAKE_CRON_SECRET: 'a'.repeat(64) };

test('public HTTP requests cannot trigger a poll', async () => {
  for (const method of ['GET', 'POST', 'PUT']) {
    assert.equal((await worker.fetch(new Request('https://worker.invalid/', { method }))).status, 404);
  }
});

test('configuration fails closed before sending any request', async () => {
  let calls = 0;
  for (const secret of [undefined, '', 'a'.repeat(63), 'A'.repeat(64), 'x'.repeat(64)]) {
    await assert.rejects(poll({ ZIKRA_QUAKE_CRON_SECRET: secret }, () => { calls++; }), /configuration/);
  }
  assert.equal(calls, 0);
});

test('only the fixed production endpoint receives the trigger credential', async () => {
  let calls = 0, canceled = false;
  await poll({ ...env, ZIKRA_QUAKE_BACKEND_ORIGIN: 'https://attacker.invalid' }, async (url, options) => {
    calls++;
    assert.equal(url, 'https://zikra-ai.vercel.app/v1/earthquakes/poll');
    assert.equal(options.method, 'POST');
    assert.equal(options.redirect, 'manual');
    assert.deepEqual(options.headers, { Authorization: 'Bearer ' + env.ZIKRA_QUAKE_CRON_SECRET });
    assert.equal(options.body, undefined);
    assert.ok(options.signal instanceof AbortSignal);
    return { status: 200, body: { cancel: async () => { canceled = true; } } };
  });
  assert.equal(calls, 1);
  assert.equal(canceled, true);
});

test('redirects and service failures are not retried or read into logs', async () => {
  for (const status of [302, 401, 429, 500, 503]) {
    let calls = 0, canceled = false;
    await assert.rejects(poll(env, async () => {
      calls++;
      return { status, body: { cancel: async () => { canceled = true; } }, text() { throw Error('must not read body'); } };
    }), /^Error: earthquake_poll_unavailable$/);
    assert.equal(calls, 1);
    assert.equal(canceled, true);
  }
});

test('scheduled handler completes one poll and disables scheduler retries', async () => {
  const original = globalThis.fetch;
  let requests = 0, noRetry = 0;
  globalThis.fetch = async () => { requests++; return new Response(null, { status: 200 }); };
  try {
    await worker.scheduled({ noRetry() { noRetry++; } }, env);
    assert.equal(requests, 1);
    assert.equal(noRetry, 1);
  } finally { globalThis.fetch = original; }
});
