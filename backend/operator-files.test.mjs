import test from 'node:test';
import assert from 'node:assert/strict';
import { existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';
import { privateOperatorPath } from './operator-files.mjs';

test('operator paths allow private local outputs but refuse source files and relative paths', () => {
  const local = fileURLToPath(new URL('../.local/operator-path-test-never-written.env', import.meta.url));
  assert.equal(privateOperatorPath(local), local);
  for (const value of ['token.env', fileURLToPath(new URL('../README.md', import.meta.url)),
    fileURLToPath(new URL('./token-never-written.env', import.meta.url))])
    assert.throws(() => privateOperatorPath(value));
});

test('client issue refuses repository output before any token file or network request', () => {
  const cli = fileURLToPath(new URL('./redis-client-admin.mjs', import.meta.url));
  const target = fileURLToPath(new URL('./operator-path-test-never-written.env', import.meta.url));
  assert.equal(existsSync(target), false);
  const result = spawnSync(process.execPath, [cli, 'issue', target], { encoding: 'utf8', windowsHide: true,
    timeout: 2000, env: { ...process.env, UPSTASH_REDIS_REST_URL: 'https://fixture.upstash.io',
      UPSTASH_REDIS_REST_TOKEN: 'PRIVATE-FIXTURE-OPERATOR-TOKEN' } });
  assert.equal(result.status, 1); assert.equal(existsSync(target), false);
  assert.ok(!result.stderr.includes('PRIVATE-FIXTURE-OPERATOR-TOKEN'));
  assert.ok(!result.stderr.includes(target));
});
