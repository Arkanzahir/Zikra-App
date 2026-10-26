import { createCipheriv, createDecipheriv, randomBytes } from 'node:crypto';
import { IMAGE_GLOBAL_DAILY } from './image-generation.mjs';

export const MAX_BACKUP_CLIENTS = 100;
const aad = Buffer.from('zikra-registry-backup-v1');
const uuid = /^[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}$/;
const hash = /^[a-f0-9]{64}$/;
const prefixPattern = /^[a-zA-Z0-9:_-]{1,64}:$/;

// Atomic, bounded registry-only snapshot. Counters, leases, prompts and raw tokens are excluded.
export const SNAPSHOT_REGISTRY = `
local prefix = ARGV[1]
local function scan(suffix)
  local cursor, keys, unique = '0', {}, {}
  for page = 1, 64 do
    local batch = redis.call('SCAN', cursor, 'MATCH', prefix .. suffix .. '*', 'COUNT', 100)
    cursor = batch[1]
    for _, key in ipairs(batch[2]) do
      if not unique[key] then
        unique[key] = true
        table.insert(keys, key)
        if #keys > ${MAX_BACKUP_CLIENTS} then return nil end
      end
    end
    if cursor == '0' then return keys end
  end
  return nil
end
local clients, installations = scan('client:'), scan('installation:')
if not clients or not installations then return redis.error_reply('backup_capacity') end
if #clients ~= #installations then return redis.error_reply('invalid_registry') end
local rows, seen = {}, {}
for _, key in ipairs(clients) do
  local tokenHash = string.sub(key, string.len(prefix .. 'client:') + 1)
  local raw = redis.call('GET', key)
  if not raw or #raw > 512 or #tokenHash ~= 64 or not string.match(tokenHash, '^[a-f0-9]+$') then
    return redis.error_reply('invalid_registry')
  end
  local ok, client = pcall(cjson.decode, raw)
  if not ok or type(client) ~= 'table' or type(client.id) ~= 'string' or type(client.revoked) ~= 'boolean'
      or seen[client.id] or redis.call('GET', prefix .. 'installation:' .. client.id) ~= tokenHash then
    return redis.error_reply('invalid_registry')
  end
  seen[client.id] = true
  table.insert(rows, {client.id, tokenHash, client.revoked and 1 or 0})
end
for _, key in ipairs(installations) do
  if not seen[string.sub(key, string.len(prefix .. 'installation:') + 1)] then
    return redis.error_reply('invalid_registry')
  end
end
local clock = redis.call('TIME')
return {clock[1], rows}
`;

// Restores only to an empty namespace. Every imported credential stays revoked.
export const RESTORE_REGISTRY = `
local prefix, rows = ARGV[1], cjson.decode(ARGV[2])
if type(rows) ~= 'table' or #rows > ${MAX_BACKUP_CLIENTS} then return redis.error_reply('invalid_backup') end
local cursor = '0'
for page = 1, 64 do
  local batch = redis.call('SCAN', cursor, 'MATCH', prefix .. '*', 'COUNT', 100)
  if #batch[2] > 0 then return redis.error_reply('restore_target_not_empty') end
  cursor = batch[1]
  if cursor == '0' then break end
  if page == 64 then return redis.error_reply('restore_scan_capacity') end
end
local ids, hashes = {}, {}
for _, client in ipairs(rows) do
  if type(client) ~= 'table' or type(client.id) ~= 'string' or type(client.hash) ~= 'string'
      or #client.id ~= 36 or #client.hash ~= 64 or not string.match(client.hash, '^[a-f0-9]+$')
      or ids[client.id] or hashes[client.hash] then return redis.error_reply('invalid_backup') end
  ids[client.id], hashes[client.hash] = true, true
end
for _, client in ipairs(rows) do
  redis.call('SET', prefix .. 'client:' .. client.hash, cjson.encode({id=client.id, revoked=true}))
  redis.call('SET', prefix .. 'installation:' .. client.id, client.hash)
end
local clock = redis.call('TIME')
redis.call('HSET', prefix .. 'images:global', 'day', math.floor(tonumber(clock[1]) / 86400), 'count', ${IMAGE_GLOBAL_DAILY})
redis.call('EXPIRE', prefix .. 'images:global', 172800)
return #rows
`;

function prefix(value) {
  if (typeof value !== 'string' || !prefixPattern.test(value)) throw new Error('invalid_backup_prefix');
  return value;
}

export function validateSnapshot(value) {
  if (!value || Array.isArray(value) || Object.keys(value).sort().join(',') !== 'clients,createdAt,prefix,schema'
      || value.schema !== 1 || typeof value.createdAt !== 'string'
      || !Number.isFinite(Date.parse(value.createdAt)) || new Date(value.createdAt).toISOString() !== value.createdAt
      || !Array.isArray(value.clients) || value.clients.length > MAX_BACKUP_CLIENTS)
    throw new Error('invalid_registry_backup');
  prefix(value.prefix);
  const ids = new Set(), hashes = new Set();
  for (const client of value.clients) {
    if (!client || Array.isArray(client) || Object.keys(client).sort().join(',') !== 'hash,id,revoked'
        || !uuid.test(client.id) || !hash.test(client.hash) || typeof client.revoked !== 'boolean'
        || ids.has(client.id) || hashes.has(client.hash)) throw new Error('invalid_registry_backup');
    ids.add(client.id); hashes.add(client.hash);
  }
  return value;
}

export async function snapshotRegistry(redis, sourcePrefix) {
  prefix(sourcePrefix);
  const result = await redis.command(['EVAL', SNAPSHOT_REGISTRY, 0, sourcePrefix]);
  if (!Array.isArray(result) || result.length !== 2 || !/^\d+$/.test(String(result[0])) || !Array.isArray(result[1]))
    throw new Error('invalid_snapshot_result');
  const clients = result[1].map(row => {
    if (!Array.isArray(row) || row.length !== 3 || (row[2] !== 0 && row[2] !== 1)) throw new Error('invalid_snapshot_result');
    return { id: row[0], hash: row[1], revoked: row[2] === 1 };
  }).sort((a, b) => a.id.localeCompare(b.id));
  return validateSnapshot({ schema: 1, createdAt: new Date(Number(result[0]) * 1000).toISOString(), prefix: sourcePrefix, clients });
}

function key(value) {
  if (typeof value !== 'string' || !hash.test(value)) throw new Error('invalid_backup_key');
  return Buffer.from(value, 'hex');
}

function bytes(value, length, maximum = length) {
  if (typeof value !== 'string' || value.length > Math.ceil(maximum / 3) * 4 || !/^[A-Za-z0-9+/]*={0,2}$/.test(value))
    throw new Error('invalid_backup_envelope');
  const decoded = Buffer.from(value, 'base64');
  if (decoded.length < length || decoded.length > maximum || decoded.toString('base64') !== value)
    throw new Error('invalid_backup_envelope');
  return decoded;
}

export function encryptSnapshot(snapshot, secret) {
  validateSnapshot(snapshot);
  const nonce = randomBytes(12), cipher = createCipheriv('aes-256-gcm', key(secret), nonce);
  cipher.setAAD(aad);
  const data = Buffer.concat([cipher.update(JSON.stringify(snapshot), 'utf8'), cipher.final()]);
  return { format: 'zikra-registry-backup-v1', nonce: nonce.toString('base64'),
    tag: cipher.getAuthTag().toString('base64'), data: data.toString('base64') };
}

export function decryptSnapshot(envelope, secret) {
  if (!envelope || Array.isArray(envelope) || Object.keys(envelope).sort().join(',') !== 'data,format,nonce,tag'
      || envelope.format !== 'zikra-registry-backup-v1') throw new Error('invalid_backup_envelope');
  const decipher = createDecipheriv('aes-256-gcm', key(secret), bytes(envelope.nonce, 12));
  decipher.setAAD(aad); decipher.setAuthTag(bytes(envelope.tag, 16));
  const plain = Buffer.concat([decipher.update(bytes(envelope.data, 1, 32768)), decipher.final()]);
  return validateSnapshot(JSON.parse(plain.toString('utf8')));
}

export async function restoreRegistry(redis, snapshot, targetPrefix, livePrefix) {
  validateSnapshot(snapshot); prefix(targetPrefix); prefix(livePrefix);
  if (targetPrefix === livePrefix || targetPrefix === snapshot.prefix) throw new Error('live_restore_forbidden');
  const count = await redis.command(['EVAL', RESTORE_REGISTRY, 0, targetPrefix, JSON.stringify(snapshot.clients)]);
  if (count !== snapshot.clients.length) throw new Error('restore_not_confirmed');
  return { restoredClients: count, allImportedCredentialsRevoked: true, currentDayImageQuotaSealed: true,
    liveNamespaceModified: false, deploymentSwitched: false };
}
