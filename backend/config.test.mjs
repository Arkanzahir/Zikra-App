import test from 'node:test';
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { ConfigError, readConfig } from './config.mjs';

const env = { FEATHERLESS_API_KEY: 'synthetic-provider-key-for-testing',
  FEATHERLESS_MODEL: 'test/model', ZIKRA_CLIENT_TOKEN: 'private-test-code-32-characters-long' };

test('private config defaults to loopback and validates explicit bind', () => {
  const config = readConfig(env);
  assert.equal(config.host, '127.0.0.1');
  assert.equal(config.port, 8787);
  assert.ok(Object.isFrozen(config));
  assert.equal(readConfig({ ...env, HOST: '0.0.0.0', PORT: '3000' }).port, 3000);
  const installation = { ...env, ZIKRA_ACCESS_MODE: 'installation',
    ZIKRA_CLIENTS_FILE: '/data/clients.json', ZIKRA_USAGE_FILE: '/data/usage.json' };
  assert.equal(readConfig({ ...installation, ZIKRA_CLIENT_TOKEN: undefined }).dailyLimit, 100);
  assert.throws(() => readConfig({ ...installation, ZIKRA_DAILY_LIMIT: '0' }), ConfigError);
  assert.throws(() => readConfig({ ...installation, ZIKRA_CLIENTS_FILE: 'relative.json' }), ConfigError);
  assert.throws(() => readConfig({ ...env, ZIKRA_ACCESS_MODE: 'unknown' }), ConfigError);
});

test('config rejects missing, control, whitespace and malformed values without echoing them', () => {
  for (const [name, values] of Object.entries({
    FEATHERLESS_API_KEY: [undefined, '', ' ', 'sensitive-value\r\nheader', ' key-with-leading-space'],
    FEATHERLESS_MODEL: ['', 'model with spaces', 'https://untrusted.example/model'],
    ZIKRA_CLIENT_TOKEN: ['', 'short', 'x'.repeat(32) + '\n', '!'.repeat(32),
      'sk-' + 'x'.repeat(32), env.FEATHERLESS_API_KEY],
    HOST: ['', 'https://untrusted.example', '127.0.0.1\n'],
    PORT: ['', '0', '-1', '65536', '8e3', '8787.1', ' 8787'],
    NODE_TLS_REJECT_UNAUTHORIZED: ['0']
  })) for (const value of values) {
    assert.throws(() => readConfig({ ...env, [name]: value }), error => {
      assert.ok(error instanceof ConfigError);
      assert.ok(!error.message.includes('sensitive-value'));
      assert.ok(!error.message.includes(env.FEATHERLESS_API_KEY));
      return true;
    });
  }
});

test('CLI check validates without listening or exposing credentials; invalid startup exits nonzero', () => {
  const entry = fileURLToPath(new URL('./server.mjs', import.meta.url));
  for (const valid of [true, false]) {
    const result = spawnSync(process.execPath, [entry, '--check-config'], {
      encoding: 'utf8', timeout: 5000,
      env: { ...process.env, ...env, HOST: '127.0.0.1', PORT: valid ? '8787' : 'invalid',
        NODE_TLS_REJECT_UNAUTHORIZED: '1' }
    });
    assert.equal(result.status, valid ? 0 : 1);
    for (const value of Object.values(env))
      assert.ok(!(result.stdout + result.stderr).includes(value));
    assert.ok(!(result.stdout + result.stderr).includes(' at '));
  }
});
