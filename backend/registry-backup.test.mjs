import test from 'node:test';
import assert from 'node:assert/strict';
import { randomBytes, randomUUID } from 'node:crypto';
import { mkdtempSync, readFileSync, writeFileSync, rmSync, existsSync, realpathSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, dirname, resolve, basename } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';
import { redisFixture } from './test-support/redis-fixture.mjs';
import { ISSUE, RedisAccess, tokenHash } from './redis-access.mjs';
import { RESERVE_IMAGE } from './image-generation.mjs';
import { MAX_BACKUP_CLIENTS, validateSnapshot, snapshotRegistry, encryptSnapshot, decryptSnapshot, restoreRegistry } from './registry-backup.mjs';

const secret = randomBytes(32).toString('hex');
const example = () => ({ schema: 1, prefix: 'fixture:source:', createdAt: '2026-10-26T00:00:00.000Z',
  clients: [{ id: randomUUID(), hash: randomBytes(32).toString('hex'), revoked: false }] });
const integration = { skip: !process.env.ZIKRA_REDIS_TEST_PYTHON, timeout: 20000 };

test('encrypted snapshots round-trip with authenticated fresh nonces and no plaintext identifiers', () => {
  const original = example(), first = encryptSnapshot(original, secret), second = encryptSnapshot(original, secret);
  assert.notEqual(first.nonce, second.nonce); assert.notEqual(first.data, second.data);
  assert.deepEqual(decryptSnapshot(first, secret), original);
  const text = JSON.stringify(first);
  for (const privateValue of [secret, original.prefix, original.clients[0].id, original.clients[0].hash]) assert.ok(!text.includes(privateValue));
  for (const field of ['nonce', 'tag', 'data']) {
    const changed = { ...first }, bytes = Buffer.from(first[field], 'base64'); bytes[0] ^= 1; changed[field] = bytes.toString('base64');
    assert.throws(() => decryptSnapshot(changed, secret));
  }
  assert.throws(() => decryptSnapshot(first, randomBytes(32).toString('hex')));
});

test('backup schema, capacity and canonical ciphertext are bounded before restoration', () => {
  const original = example();
  for (const changed of [{ ...original, rawToken: 'private' }, { ...original, prefix: 'unsafe*:' },
    { ...original, createdAt: 'invalid' }, { ...original, clients: [...original.clients, ...original.clients] },
    { ...original, clients: [{ ...original.clients[0], revoked: 'false' }] },
    { ...original, clients: Array.from({ length: MAX_BACKUP_CLIENTS + 1 }, () => example().clients[0]) }]) {
    assert.throws(() => validateSnapshot(changed));
  }
  const encrypted = encryptSnapshot(original, secret);
  for (const changed of [{ ...encrypted, data: encrypted.data + '\n' }, { ...encrypted, nonce: 'AA==' },
    { ...encrypted, tag: '' }, { ...encrypted, data: 'A'.repeat(50000) }, { ...encrypted, private: 'extra' }]) {
    assert.throws(() => decryptSnapshot(changed, secret));
  }
});

async function issue(redis, prefix) {
  const token = randomBytes(32).toString('hex'), id = randomUUID(), hash = tokenHash(token);
  assert.equal(await redis.command(['EVAL', ISSUE, 2, prefix + 'client:' + hash, prefix + 'installation:' + id,
    JSON.stringify({ id, revoked: false }), hash]), 1);
  return { token, id, hash };
}

test('real Lua exports paired registry metadata and ignores usage or accidental extra content', integration, async t => {
  const redis = await redisFixture(t), prefix = 'source:', client = await issue(redis, prefix);
  await redis.command(['SET', prefix + 'client:' + client.hash, JSON.stringify({ id: client.id, revoked: false, unexpected: 'PRIVATE-NOT-IN-BACKUP' })]);
  await redis.command(['HSET', prefix + 'usage:' + client.id, 'count', 10]);
  await redis.command(['SET', 'unrelated:private', 'PRIVATE-UNRELATED']);
  const snapshot = await snapshotRegistry(redis, prefix);
  assert.deepEqual(snapshot.clients, [{ id: client.id, hash: client.hash, revoked: false }]);
  assert.ok(!JSON.stringify(snapshot).includes('PRIVATE')); assert.ok(!JSON.stringify(snapshot).includes(client.token));
  assert.equal(await redis.command(['GET', 'unrelated:private']), 'PRIVATE-UNRELATED');
});

test('real Lua rejects incomplete, corrupt and oversized registries instead of partial backups', integration, async t => {
  const redis = await redisFixture(t), client = await issue(redis, 'source:');
  await redis.command(['DEL', 'source:installation:' + client.id]);
  await assert.rejects(snapshotRegistry(redis, 'source:'));
  await redis.command(['SET', 'source:installation:' + client.id, client.hash]);
  await redis.command(['SET', 'source:client:' + client.hash, 'corrupt']);
  await assert.rejects(snapshotRegistry(redis, 'source:'));
  assert.equal(await redis.command(['GET', 'source:client:' + client.hash]), 'corrupt');
  for (let i = 0; i < MAX_BACKUP_CLIENTS; i++) await issue(redis, 'many:');
  assert.equal((await snapshotRegistry(redis, 'many:')).clients.length, MAX_BACKUP_CLIENTS);
  await issue(redis, 'many:');
  await assert.rejects(snapshotRegistry(redis, 'many:'));
});

test('restore never overwrites production or reactivates credentials revoked after a stale backup', integration, async t => {
  const redis = await redisFixture(t), client = await issue(redis, 'source:');
  const snapshot = await snapshotRegistry(redis, 'source:');
  const revoked = JSON.stringify({ id: client.id, revoked: true });
  await redis.command(['SET', 'source:client:' + client.hash, revoked]);
  await assert.rejects(restoreRegistry(redis, snapshot, 'source:', 'source:'));
  await assert.rejects(restoreRegistry(redis, snapshot, 'live:', 'live:'));
  await redis.command(['SET', 'occupied:do-not-touch', 'retained']);
  await assert.rejects(restoreRegistry(redis, snapshot, 'occupied:', 'source:'));
  assert.equal(await redis.command(['GET', 'occupied:do-not-touch']), 'retained');
  const report = await restoreRegistry(redis, snapshot, 'recovery:', 'source:');
  assert.equal(report.allImportedCredentialsRevoked, true); assert.equal(report.deploymentSwitched, false);
  assert.equal(await redis.command(['GET', 'source:client:' + client.hash]), revoked);
  assert.equal(await new RedisAccess({ redis, prefix: 'recovery:' }).identify('Bearer ' + client.token), null);
  assert.deepEqual(await redis.command(['EVAL', RESERVE_IMAGE, 3, 'recovery:images:global',
    'recovery:images:user:' + client.id, 'recovery:images:request:' + client.id + ':' + randomUUID()]), [0, 'image_global_quota']);
  await assert.rejects(restoreRegistry(redis, snapshot, 'recovery:', 'source:'));
});

test('operator CLI keeps keys and decoded content private, refuses overwrites and repository output', () => {
  const temporaryRoot = realpathSync(tmpdir()), directory = mkdtempSync(join(temporaryRoot, 'zikra-registry-test-'));
  try {
    const cli = fileURLToPath(new URL('./registry-backup-admin.mjs', import.meta.url));
    const run = args => spawnSync(process.execPath, [cli, ...args], { encoding: 'utf8', windowsHide: true,
      env: { ...process.env, ZIKRA_BACKUP_KEY: secret }, timeout: 10000 });
    const keyFile = join(directory, 'key.env'), first = run(['init-key', keyFile]);
    assert.equal(first.status, 0); const keyText = readFileSync(keyFile, 'utf8');
    assert.match(keyText, /^ZIKRA_BACKUP_KEY=[a-f0-9]{64}\n$/); assert.ok(!first.stdout.includes(keyText.split('=')[1].trim()));
    assert.notEqual(run(['init-key', keyFile]).status, 0); assert.equal(readFileSync(keyFile, 'utf8'), keyText);
    const publicOutput = fileURLToPath(new URL('./DO-NOT-CREATE-BACKUP-KEY.env', import.meta.url));
    assert.notEqual(run(['init-key', publicOutput]).status, 0); assert.equal(existsSync(publicOutput), false);
    const snapshot = example(), file = join(directory, 'snapshot.json');
    writeFileSync(file, JSON.stringify(encryptSnapshot(snapshot, secret)));
    const inspection = run(['inspect', file]); assert.equal(inspection.status, 0);
    for (const value of [secret, snapshot.clients[0].id, snapshot.clients[0].hash]) assert.ok(!inspection.stdout.includes(value));
    writeFileSync(file, 'x'.repeat(50001)); const oversized = run(['inspect', file]); assert.notEqual(oversized.status, 0);
    assert.ok(!oversized.stderr.includes(secret)); assert.ok(!oversized.stderr.includes('Error:'));
  } finally {
    assert.equal(dirname(resolve(directory)), temporaryRoot); assert.ok(basename(directory).startsWith('zikra-registry-test-'));
    rmSync(directory, { recursive: true });
  }
});
