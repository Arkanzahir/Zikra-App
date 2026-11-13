import { randomUUID, randomBytes, createHash, createCipheriv, createDecipheriv } from 'node:crypto';
import { gzipSync, gunzipSync } from 'node:zlib';

const TTL = 8 * 86400, CHUNK = 350000, MAX = 96 * 1024 * 1024;
export const RELEASE_SYNC_LOCK = "if redis.call('GET',KEYS[1])==ARGV[1] then return redis.call('DEL',KEYS[1]) end return 0";
export const PUBLISH_SYNC = "if redis.call('GET',KEYS[1])~=ARGV[1] then return 0 end redis.call('SET',KEYS[2],ARGV[2],'EX',ARGV[3]);return 1";
const hash = bytes => createHash('sha256').update(bytes).digest('hex');

/** Private content cache. Immutable chunks are installed before a fenced pointer swap. */
export class QuranSyncCache {
  constructor(redis, prefix = 'zikra:v1:', encryptionKey) {
    if (!/^[a-zA-Z0-9:_-]{1,64}:$/.test(prefix)) throw Error('invalid_sync_cache_prefix');
    this.redis = redis; this.prefix = prefix + 'quran-sync-v2:';
    if (!/^[a-f0-9]{64}$/.test(encryptionKey ?? '')) throw Error('invalid_quran_cache_key');
    this.key = Buffer.from(encryptionKey, 'hex');
  }
  async acquire() {
    const lease = randomUUID();
    return await this.redis.command(['SET', this.prefix + 'lock', lease, 'NX', 'EX', 120]) === 'OK' ? lease : null;
  }
  async release(lease) {
    await this.redis.command(['EVAL', RELEASE_SYNC_LOCK, 1, this.prefix + 'lock', lease]);
  }
  async load({ deadline = Date.now() + 20000 } = {}) {
    const raw = await this.redis.command(['GET', this.prefix + 'current']);
    if (raw === null) return null;
    if (typeof raw !== 'string' || raw.length > 512) throw Error('invalid_sync_cache');
    const meta = JSON.parse(raw);
    if (meta.schema !== 2 || meta.encryption !== 'aes-256-gcm' || !/^[a-f0-9-]{36}$/.test(meta.generation) || !/^[a-f0-9]{64}$/.test(meta.sha256)
        || !Number.isInteger(meta.parts) || meta.parts < 1 || meta.parts > 200
        || !Number.isInteger(meta.bytes) || meta.bytes < 1 || meta.bytes > MAX) throw Error('invalid_sync_cache');
    const parts = [];
    for (let offset = 0; offset < meta.parts; offset += 4) {
      if (Date.now() >= deadline) throw Error('sync_cache_deadline');
      const batch = await Promise.all(Array.from({ length: Math.min(4, meta.parts - offset) }, (_, n) =>
        this.redis.command(['GET', this.prefix + meta.generation + ':' + (offset + n)])));
      for (const text of batch) {
        if (typeof text !== 'string' || text.length > 470000) throw Error('missing_sync_chunk');
        const part = Buffer.from(text, 'base64');
        if (part.length > CHUNK || part.toString('base64') !== text) throw Error('invalid_sync_chunk');
        parts.push(part);
      }
    }
    const bytes = Buffer.concat(parts);
    if (bytes.length !== meta.bytes || hash(bytes) !== meta.sha256) throw Error('invalid_sync_cache_hash');
    if (bytes.length < 29) throw Error('invalid_sync_ciphertext');
    const decipher = createDecipheriv('aes-256-gcm', this.key, bytes.subarray(0, 12));
    decipher.setAAD(Buffer.from('zikra-quran-cache-v2')); decipher.setAuthTag(bytes.subarray(12, 28));
    const compressed = Buffer.concat([decipher.update(bytes.subarray(28)), decipher.final()]);
    const value = JSON.parse(gunzipSync(compressed, { maxOutputLength: MAX }).toString('utf8'));
    if (value.schema !== 1 || value.filter !== 'mushafs:1;quran_core:1;translations:33'
        || !Array.isArray(value.resources) || !Array.isArray(value.unavailable)) throw Error('invalid_sync_cache');
    const state = { token: value.token, checkedAt: value.checkedAt, unavailable: new Set(value.unavailable),
      resources: new Map(value.resources.map(([name, rows]) => {
        const map = new Map(rows);
        if (map.size !== rows.length) throw Error('duplicate_sync_cache_record');
        return [name, map];
      })) };
    if (state.resources.size !== value.resources.length || state.unavailable.size !== value.unavailable.length)
      throw Error('invalid_sync_cache');
    return state;
  }
  async publish(state, lease, { deadline = Date.now() + 20000 } = {}) {
    const json = Buffer.from(JSON.stringify({ schema: 1, filter: 'mushafs:1;quran_core:1;translations:33',
      token: state.token, checkedAt: state.checkedAt, unavailable: [...state.unavailable],
      resources: [...state.resources].map(([key, rows]) => [key, [...rows]]) }));
    if (json.length > MAX) throw Error('sync_cache_budget');
    const nonce = randomBytes(12), cipher = createCipheriv('aes-256-gcm', this.key, nonce);
    cipher.setAAD(Buffer.from('zikra-quran-cache-v2'));
    const encrypted = Buffer.concat([cipher.update(gzipSync(json)), cipher.final()]);
    const bytes = Buffer.concat([nonce, cipher.getAuthTag(), encrypted]);
    const generation = randomUUID(), parts = Math.ceil(bytes.length / CHUNK);
    if (parts > 200) throw Error('sync_cache_budget');
    for (let offset = 0; offset < parts; offset += 4) {
      if (Date.now() >= deadline) throw Error('sync_cache_deadline');
      const results = await Promise.all(Array.from({ length: Math.min(4, parts - offset) }, (_, n) => {
        const i = offset + n;
        return this.redis.command(['SET', this.prefix + generation + ':' + i,
          bytes.subarray(i * CHUNK, (i + 1) * CHUNK).toString('base64'), 'EX', TTL]);
      }));
      if (results.some(value => value !== 'OK')) throw Error('sync_cache_write_failed');
    }
    if (Date.now() >= deadline) throw Error('sync_cache_deadline');
    const meta = JSON.stringify({ schema: 2, encryption: 'aes-256-gcm', generation, parts, bytes: bytes.length, sha256: hash(bytes) });
    if (await this.redis.command(['EVAL', PUBLISH_SYNC, 2, this.prefix + 'lock', this.prefix + 'current', lease, meta, TTL]) !== 1)
      throw Error('sync_cache_lease_lost');
  }
}
