import { randomBytes, randomUUID } from 'node:crypto';
import { writeFileSync } from 'node:fs';
import { isAbsolute } from 'node:path';
import { RedisRest, readRedisConfig, tokenHash, ISSUE, REVOKE } from './redis-access.mjs';

try {
  const config = readRedisConfig(), redis = new RedisRest(config);
  const [command, target] = process.argv.slice(2);
  if (command === 'issue' && target && isAbsolute(target)) {
    const token = randomBytes(32).toString('hex'), id = randomUUID(), hash = tokenHash(token);
    writeFileSync(target, `ZIKRA_CLIENT_TOKEN=${token}\n`, { mode: 0o600, flag: 'wx' });
    const result = await redis.command(['EVAL', ISSUE, 2, `${config.prefix}client:${hash}`,
      `${config.prefix}installation:${id}`, JSON.stringify({ id, revoked: false }), hash]);
    if (result !== 1) throw new Error();
    console.log(JSON.stringify({ id, issued: true, tokenSavedPrivately: true }));
  } else if (command === 'revoke' && /^[0-9a-f-]{36}$/.test(target ?? '')) {
    const hash = await redis.command(['GET', `${config.prefix}installation:${target}`]);
    if (!/^[a-f0-9]{64}$/.test(hash ?? '')) throw new Error();
    if (await redis.command(['EVAL', REVOKE, 1, `${config.prefix}client:${hash}`, target]) !== 1) throw new Error();
    console.log(JSON.stringify({ revoked: true }));
  } else throw new Error();
} catch {
  console.error('Redis client administration failed; no secret printed. A failed issue may leave an unused private token file.');
  process.exitCode = 1;
}
