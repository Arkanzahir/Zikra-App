import { createHash, randomBytes, randomUUID, timingSafeEqual } from 'node:crypto';
import { readFileSync, writeFileSync, renameSync, statSync } from 'node:fs';
import { isAbsolute } from 'node:path';

const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;
const hash = value => createHash('sha256').update(value).digest('hex');
function read(path, missing) {
  try {
    if (statSync(path).size > 512 * 1024) throw new Error('invalid_access_state');
    return JSON.parse(readFileSync(path, 'utf8'));
  } catch (error) { if (error.code === 'ENOENT' && missing) return missing; throw error; }
}
export function atomicJson(path, data) {
  const temporary = `${path}.${randomUUID()}.tmp`;
  writeFileSync(temporary, JSON.stringify(data), { mode: 0o600, flag: 'wx' });
  renameSync(temporary, path);
}
export function readRegistry(path) {
  const data = read(path);
  if (data?.version !== 1 || !Array.isArray(data.clients) || data.clients.length > 1000)
    throw new Error('invalid_registry');
  const ids = new Set(), hashes = new Set();
  for (const client of data.clients) {
    if (!uuid.test(client?.id) || !/^[a-f0-9]{64}$/.test(client?.tokenHash)
        || typeof client.revoked !== 'boolean' || ids.has(client.id) || hashes.has(client.tokenHash))
      throw new Error('invalid_registry');
    ids.add(client.id); hashes.add(client.tokenHash);
  }
  return data;
}
export function issueClient(registryFile, tokenFile) {
  if (![registryFile, tokenFile].every(isAbsolute) || registryFile === tokenFile)
    throw new Error('private_absolute_paths_required');
  let registry;
  try { registry = readRegistry(registryFile); }
  catch (error) { if (error.code !== 'ENOENT') throw error; registry = { version: 1, clients: [] }; }
  if (registry.clients.length >= 1000) throw new Error('registry_full');
  const token = randomBytes(32).toString('hex'), id = randomUUID();
  // The raw token is written once to an operator-only file, never to stdout or the registry.
  writeFileSync(tokenFile, `ZIKRA_CLIENT_TOKEN=${token}\n`, { mode: 0o600, flag: 'wx' });
  registry.clients.push({ id, tokenHash: hash(token), revoked: false });
  atomicJson(registryFile, registry);
  return id;
}
export function revokeClient(registryFile, id) {
  const registry = readRegistry(registryFile), client = registry.clients.find(item => item.id === id);
  if (!client) throw new Error('unknown_client');
  client.revoked = true;
  atomicJson(registryFile, registry);
}

/** One process and one persistent volume. Multi-replica quotas require a transactional shared store. */
export class InstallationAccess {
  constructor({ clientsFile, usageFile, dailyLimit = 100 }) {
    if (![clientsFile, usageFile].every(isAbsolute) || clientsFile === usageFile)
      throw new Error('private_absolute_paths_required');
    this.clientsFile = clientsFile; this.usageFile = usageFile; this.dailyLimit = dailyLimit;
    readRegistry(clientsFile);
  }
  identify(authorization) {
    if (typeof authorization !== 'string' || !/^Bearer [a-f0-9]{64}$/.test(authorization)) return null;
    const candidate = Buffer.from(hash(authorization.slice(7)), 'hex');
    return readRegistry(this.clientsFile).clients.find(item => !item.revoked
      && timingSafeEqual(candidate, Buffer.from(item.tokenHash, 'hex')))?.id ?? null;
  }
  consume(id, now) {
    const state = read(this.usageFile, { version: 1, clients: {} });
    if (state?.version !== 1 || !state.clients || typeof state.clients !== 'object'
        || Array.isArray(state.clients) || Object.keys(state.clients).length > 1000)
      throw new Error('invalid_usage');
    for (const [key, value] of Object.entries(state.clients)) {
      if (!uuid.test(key) || !/^\d{4}-\d{2}-\d{2}$/.test(value?.day)
          || !Number.isSafeInteger(value.count) || value.count < 0
          || !Number.isSafeInteger(value.minuteCount) || value.minuteCount < 0
          || !Number.isSafeInteger(value.minuteStart) || value.minuteStart < 0)
        throw new Error('invalid_usage');
    }
    const day = new Date(now).toISOString().slice(0, 10);
    for (const key of Object.keys(state.clients)) if (state.clients[key].day !== day) delete state.clients[key];
    const current = state.clients[id] ?? { day, count: 0, minuteStart: now, minuteCount: 0 };
    if (now - current.minuteStart >= 60000) { current.minuteStart = now; current.minuteCount = 0; }
    if (current.count >= this.dailyLimit || current.minuteCount >= 10) return false;
    current.count++; current.minuteCount++; state.clients[id] = current;
    atomicJson(this.usageFile, state);
    return true;
  }
}
