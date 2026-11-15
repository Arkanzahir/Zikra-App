import test from 'node:test';
import assert from 'node:assert/strict';
import { inspectAnswerReports, resolveAnswerReport } from './answer-report-admin.mjs';

const id = 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa:' + 'b'.repeat(64);
test('report inspection deduplicates scan results and retains only allowed fields', async () => {
  const redis = { command: async ([action]) => action === 'SCAN'
    ? ['0', ['test:answer-report:' + id, 'test:answer-report:' + id]]
    : JSON.stringify({ reason: 'unsafe', excerpt: 'Synthetic', unwanted: 'excluded' }) };
  assert.deepEqual(await inspectAnswerReports(redis, 'test:'), {
    reports: [{ reference: id, reason: 'unsafe', excerpt: 'Synthetic' }], truncated: false });
});
test('report inspection bounds nonterminating scan and rejects namespace escape', async () => {
  let count = 0;
  const redis = { command: async () => { count++; return ['1', []]; } };
  assert.deepEqual(await inspectAnswerReports(redis, 'test:'), { reports: [], truncated: true });
  assert.equal(count, 20);
  await assert.rejects(inspectAnswerReports({command: async () => ['0', ['other:key']]}, 'test:'));
});
test('resolution requires exact reference and confirmation before deleting one report', async () => {
  let command;
  const redis = { command: async args => { command = args; return 1; } };
  for (const [target, confirm] of [['*', '*'], [id, 'different']])
    await assert.rejects(resolveAnswerReport(redis, 'test:', target, confirm));
  assert.equal(command, undefined);
  assert.deepEqual(await resolveAnswerReport(redis, 'test:', id, id), { resolved: true, alreadyAbsent: false });
  assert.deepEqual(command, ['DEL', 'test:answer-report:' + id]);
});
