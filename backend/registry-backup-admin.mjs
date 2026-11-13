import { randomBytes, createHash } from 'node:crypto';
import { writeFileSync, openSync, readSync, fstatSync, closeSync } from 'node:fs';
import { privateOperatorPath as privatePath } from './operator-files.mjs';
import { RedisRest, readRedisConfig } from './redis-access.mjs';
import { snapshotRegistry, encryptSnapshot, decryptSnapshot, restoreRegistry } from './registry-backup.mjs';

function readEnvelope(path) {
  const fd = openSync(privatePath(path), 'r');
  try {
    const info = fstatSync(fd);
    if (!info.isFile() || info.size > 50000) throw new Error();
    const buffer = Buffer.alloc(50001); let bytes = 0, count;
    while ((count = readSync(fd, buffer, bytes, buffer.length - bytes, null)) > 0) {
      bytes += count; if (bytes > 50000) throw new Error();
    }
    return JSON.parse(buffer.subarray(0, bytes).toString('utf8'));
  } finally { closeSync(fd); }
}

try {
  const [command, path, targetPrefix, ...extra] = process.argv.slice(2);
  if (extra.length || !path) throw new Error();
  if (command === 'init-key' && !targetPrefix) {
    writeFileSync(privatePath(path), `ZIKRA_BACKUP_KEY=${randomBytes(32).toString('hex')}\n`, { flag: 'wx', mode: 0o600 });
    console.log(JSON.stringify({ keySavedPrivately: true, keyPrinted: false }));
  } else if (command === 'backup' && !targetPrefix) {
    const output = privatePath(path), secret = process.env.ZIKRA_BACKUP_KEY;
    if (!/^[a-f0-9]{64}$/.test(secret ?? '')) throw new Error();
    const config = readRedisConfig(), snapshot = await snapshotRegistry(new RedisRest(config), config.prefix);
    const data = JSON.stringify(encryptSnapshot(snapshot, secret)) + '\n';
    writeFileSync(output, data, { flag: 'wx', mode: 0o600 });
    console.log(JSON.stringify({ encryptedBackupSaved: true, installations: snapshot.clients.length,
      bytes: Buffer.byteLength(data), sha256: createHash('sha256').update(data).digest('hex'), plaintextWritten: false }));
  } else if (command === 'inspect' && !targetPrefix) {
    const snapshot = decryptSnapshot(readEnvelope(path), process.env.ZIKRA_BACKUP_KEY);
    console.log(JSON.stringify({ valid: true, createdAt: snapshot.createdAt, installations: snapshot.clients.length,
      revokedInSnapshot: snapshot.clients.filter(client => client.revoked).length, identifiersPrinted: false }));
  } else if (command === 'restore' && targetPrefix) {
    const snapshot = decryptSnapshot(readEnvelope(path), process.env.ZIKRA_BACKUP_KEY), config = readRedisConfig();
    console.log(JSON.stringify(await restoreRegistry(new RedisRest(config), snapshot, targetPrefix, config.prefix)));
  } else throw new Error();
} catch {
  console.error('Registry backup operation failed; credentials, identifiers and plaintext are not printed. Existing files are never overwritten.');
  process.exitCode = 1;
}
