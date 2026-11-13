import { randomBytes, randomUUID } from 'node:crypto';
import { writeFileSync } from 'node:fs';
import { privateOperatorPath } from './operator-files.mjs';
import { RedisRest, readRedisConfig, tokenHash, ISSUE, REVOKE } from './redis-access.mjs';

try {
  const config = readRedisConfig(), redis = new RedisRest(config);
  const [command, target, ...extra] = process.argv.slice(2);
  if (extra.length) throw new Error();
  if (command === 'issue' && target) {
    const output = privateOperatorPath(target);
    const token = randomBytes(32).toString('hex'), id = randomUUID(), hash = tokenHash(token);
    writeFileSync(output, `ZIKRA_CLIENT_TOKEN=${token}\n`, { mode: 0o600, flag: 'wx' });
    const result = await redis.command(['EVAL', ISSUE, 2, `${config.prefix}client:${hash}`,
      `${config.prefix}installation:${id}`, JSON.stringify({ id, revoked: false }), hash]);
    if (result !== 1) throw new Error();
    console.log(JSON.stringify({ id, issued: true, tokenSavedPrivately: true }));
  } else if (command === 'revoke' && /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/.test(target ?? '')) {
    const hash = await redis.command(['GET', `${config.prefix}installation:${target}`]);
    if (!/^[a-f0-9]{64}$/.test(hash ?? '')) throw new Error();
    if (await redis.command(['EVAL', REVOKE, 1, `${config.prefix}client:${hash}`, target]) !== 1) throw new Error();
    console.log(JSON.stringify({ revoked: true }));
  } else throw new Error();
} catch {
  console.error('Redis client administration failed; no secret printed. A failed issue may leave an unused private token file.');
  process.exitCode = 1;
}
