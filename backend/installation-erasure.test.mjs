import test from 'node:test';
import assert from 'node:assert/strict';
import { randomBytes, randomUUID } from 'node:crypto';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { ISSUE, RedisAccess, tokenHash } from './redis-access.mjs';
import { manageInstallationErasure, MAX_ERASURE_REQUESTS } from './installation-erasure.mjs';
import { redisFixture } from './test-support/redis-fixture.mjs';

const integration = { skip: !process.env.ZIKRA_REDIS_TEST_PYTHON, timeout: 20000 };
async function fixture(t) {
  const redis = await redisFixture(t), prefix = 'erasure:', id = randomUUID();
  const token = randomBytes(32).toString('hex'), hash = tokenHash(token);
  await redis.command(['EVAL', ISSUE, 2, prefix + 'client:' + hash, prefix + 'installation:' + id,
    JSON.stringify({ id, revoked: false }), hash]);
  return { redis, prefix, id, token, hash, manage: action => manageInstallationErasure(redis, prefix, id, action) };
}
async function settle(f) {
  const key = f.prefix + 'client:' + f.hash;
  const client = JSON.parse(await f.redis.command(['GET', key]));
  client.erasureRequestedAt -= 121;
  await f.redis.command(['SET', key, JSON.stringify(client)]);
}

test('erasure validates exact identifiers and namespace before accessing Redis', async () => {
  const redis = { command() { throw new Error('must not access'); } };
  for (const [prefix, id, action] of [['bad*:', randomUUID(), 'inspect'], ['ok:', '*', 'erase'],
    ['ok:', randomUUID(), 'DEL'], ['ok:', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'prepare']])
    await assert.rejects(manageInstallationErasure(redis, prefix, id, action), /invalid_erasure_request/);
});

test('inspection is read-only; prepare revokes, waits and is idempotent', integration, async t => {
  const f = await fixture(t), key = f.prefix + 'client:' + f.hash;
  const before = await f.redis.command(['GET', key]);
  const report = await f.manage('inspect');
  assert.equal(report.revoked, false); assert.equal(report.prepared, false);
  assert.equal(await f.redis.command(['GET', key]), before);
  assert.equal((await f.manage('erase')).state, 'not_ready');
  assert.equal((await f.manage('prepare')).state, 'prepared');
  const prepared = await f.redis.command(['GET', key]);
  assert.equal((await f.manage('prepare')).state, 'prepared');
  assert.equal(await f.redis.command(['GET', key]), prepared);
  assert.equal(await new RedisAccess({ redis: f.redis, prefix: f.prefix }).identify('Bearer ' + f.token), null);
  assert.equal((await f.manage('erase')).state, 'not_ready');
  assert.equal((await f.manage('inspect')).providerDataDeleted, false);
});

test('erasure deletes only one paired installation and its scoped metadata', integration, async t => {
  const f = await fixture(t), other = randomUUID();
  const selected = [f.prefix + 'usage:' + f.id, f.prefix + 'images:user:' + f.id,
    f.prefix + 'images:request:' + f.id + ':' + randomUUID(), f.prefix + 'quran-sync:rate:' + f.id];
  const retained = [f.prefix + 'images:request:' + other + ':' + randomUUID(), f.prefix + 'images:global',
    f.prefix + 'e02:initialized', f.prefix + 'quran-sync:current', 'unrelated:private'];
  for (const key of [...selected, ...retained]) await f.redis.command(['SET', key, 'private-value']);
  await f.manage('prepare'); await settle(f);
  const result = await f.manage('erase');
  assert.equal(result.state, 'erased'); assert.equal(result.removedKeys, 6);
  for (const key of selected) assert.equal(await f.redis.command(['EXISTS', key]), 0);
  for (const key of retained) assert.equal(await f.redis.command(['GET', key]), 'private-value');
  assert.equal(await f.redis.command(['EXISTS', f.prefix + 'installation:' + f.id]), 0);
  assert.equal(await f.redis.command(['EXISTS', f.prefix + 'client:' + f.hash]), 0);
  assert.equal(await new RedisAccess({ redis: f.redis, prefix: f.prefix }).identify('Bearer ' + f.token), null);
  assert.equal((await f.manage('erase')).state, 'missing');
  for (const secret of [f.token, f.id, f.hash, 'private-value']) assert.ok(!JSON.stringify(result).includes(secret));
});

test('in-flight lease blocks deletion; expired own lease does not remove global capacity', integration, async t => {
  const f = await fixture(t), access = new RedisAccess({ redis: f.redis, prefix: f.prefix });
  const grant = await access.acquire(f.id, 'Bearer ' + f.token); assert.equal(grant.allowed, true);
  await f.manage('prepare'); await settle(f);
  assert.equal((await f.manage('erase')).state, 'not_ready');
  await f.redis.command(['ZADD', f.prefix + 'active:' + f.id, 0, grant.lease]);
  assert.equal((await f.manage('erase')).state, 'erased');
  assert.equal(await f.redis.command(['ZCARD', f.prefix + 'active']), 1);
});

test('corrupt or mismatched registry never causes deletion or cross-client revocation', integration, async t => {
  const f = await fixture(t), key = f.prefix + 'client:' + f.hash;
  for (const raw of ['{', JSON.stringify({ id: randomUUID(), revoked: false }),
    JSON.stringify({ id: f.id, revoked: false, erasureRequestedAt: 'old' })]) {
    await f.redis.command(['SET', key, raw]);
    await assert.rejects(f.manage('prepare'));
    assert.equal(await f.redis.command(['GET', key]), raw);
  }
  await f.redis.command(['SET', f.prefix + 'installation:' + f.id, '*']);
  await assert.rejects(f.manage('erase'));
});

test('oversized scope fails before any deletion', integration, async t => {
  const f = await fixture(t); await f.manage('prepare'); await settle(f);
  for (let i = 0; i <= MAX_ERASURE_REQUESTS; i++)
    await f.redis.command(['SET', f.prefix + 'images:request:' + f.id + ':' + i, 'attempted']);
  await assert.rejects(f.manage('erase'));
  assert.equal(await f.redis.command(['EXISTS', f.prefix + 'installation:' + f.id]), 1);
  assert.equal(await f.redis.command(['EXISTS', f.prefix + 'images:request:' + f.id + ':0']), 1);
});

test('CLI refuses mutation without repeated exact installation and prints no raw arguments', () => {
  const path = fileURLToPath(new URL('./installation-erasure-admin.mjs', import.meta.url));
  const id = randomUUID(), secret = 'PRIVATE-CLI-CREDENTIAL';
  for (const args of [['erase', id], ['prepare', id], ['erase', id, '--confirm-installation', randomUUID()],
    ['inspect', id, '--confirm-installation', id]]) {
    const result = spawnSync(process.execPath, [path, ...args], { encoding: 'utf8', windowsHide: true,
      env: { ...process.env, UPSTASH_REDIS_REST_TOKEN: secret }, timeout: 5000 });
    assert.equal(result.status, 1);
    assert.ok(!result.stdout.includes(id)); assert.ok(!result.stderr.includes(id));
    assert.ok(!result.stderr.includes(secret));
  }
});
