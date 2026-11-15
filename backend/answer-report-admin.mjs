import { writeFileSync } from 'node:fs';
import { pathToFileURL } from 'node:url';
import { RedisRest, readRedisConfig } from './redis-access.mjs';
import { privateOperatorPath } from './operator-files.mjs';

const reference = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}:[0-9a-f]{64}$/;
function reportPrefix(prefix) {
  if (!/^[a-zA-Z0-9:_-]{1,64}:$/.test(prefix)) throw new Error('invalid_namespace');
  return prefix + 'answer-report:';
}

/** Bounded scan. Excerpts go only into an explicit private output file, never stdout. */
export async function inspectAnswerReports(redis, prefix) {
  const base = reportPrefix(prefix);
  const reports = [];
  const seen = new Set();
  let cursor = '0';
  for (let step = 0; step < 20; step++) {
    const page = await redis.command(['SCAN', cursor, 'MATCH', base + '*', 'COUNT', 20]);
    if (!Array.isArray(page) || page.length !== 2 || !/^[0-9]+$/.test(String(page[0]))
        || !Array.isArray(page[1])) throw new Error('invalid_scan');
    cursor = String(page[0]);
    for (const key of page[1]) {
      if (typeof key !== 'string' || !key.startsWith(base)) throw new Error('invalid_report_key');
      const id = key.slice(base.length);
      if (!reference.test(id)) throw new Error('invalid_report_key');
      if (seen.has(id)) continue;
      seen.add(id);
      const raw = await redis.command(['GET', key]);
      if (raw === null) continue;
      const value = JSON.parse(raw);
      if (!['unsafe', 'inaccurate', 'other'].includes(value.reason)
          || typeof value.excerpt !== 'string' || value.excerpt.length > 500)
        throw new Error('invalid_report');
      reports.push({ reference: id, reason: value.reason, excerpt: value.excerpt });
      if (reports.length >= 50) return { reports, truncated: true };
    }
    if (cursor === '0') return { reports, truncated: false };
  }
  return { reports, truncated: true };
}

export async function resolveAnswerReport(redis, prefix, id, confirmation) {
  if (!reference.test(id ?? '') || confirmation !== id) throw new Error('invalid_confirmation');
  const removed = await redis.command(['DEL', reportPrefix(prefix) + id]);
  if (removed !== 0 && removed !== 1) throw new Error('invalid_delete');
  return { resolved: removed === 1, alreadyAbsent: removed === 0 };
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  try {
    const [action, target, flag, confirmation, ...extra] = process.argv.slice(2);
    if (extra.length) throw new Error('invalid_arguments');
    const config = readRedisConfig();
    const redis = new RedisRest(config);
    if (action === 'inspect' && flag === undefined) {
      const output = privateOperatorPath(target);
      const result = await inspectAnswerReports(redis, config.prefix);
      writeFileSync(output, JSON.stringify(result, null, 2), { flag: 'wx', mode: 0o600 });
      console.log(JSON.stringify({ count: result.reports.length, truncated: result.truncated, savedPrivately: true }));
    } else if (action === 'resolve' && flag === '--confirm-report') {
      console.log(JSON.stringify(await resolveAnswerReport(redis, config.prefix, target, confirmation)));
    } else throw new Error('invalid_arguments');
  } catch {
    console.error('Report operation refused or unavailable; no report content or credentials printed.');
    process.exitCode = 1;
  }
}
