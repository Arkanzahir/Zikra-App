import { once } from 'node:events';
import { createHash, randomUUID } from 'node:crypto';
import { writeFileSync } from 'node:fs';
import { createGateway } from '../../../backend/server.mjs';
import { readConfig } from '../../../backend/config.mjs';
import { loadTafsir } from '../../../backend/tafsir.mjs';

const config = readConfig();
const server = createGateway({ ...config, accessMode: 'private' });
const report = { dateWib: '2026-10-25', scope: 'synthetic-private-loopback; no APK, hosting or microphone claim',
  runtime: process.version, keyRotated: false, providerRequests: 0 };
try {
  server.listen(0, '127.0.0.1'); await once(server, 'listening');
  async function send(value) {
    report.providerRequests++;
    const response = await fetch(`http://127.0.0.1:${server.address().port}/v1/assistant`, {
      method: 'POST', signal: AbortSignal.timeout(40000), redirect: 'error',
      headers: { Authorization: `Bearer ${config.clientToken}`, 'Content-Type': 'application/json' },
      body: JSON.stringify(value)
    });
    return { status: response.status, body: await response.json() };
  }
  const proposal = await send({ requestId: randomUUID(), features: { taskProposals: true },
    context: { name: 'Pengguna Uji' }, messages: [{ role: 'user',
      content: 'Usulkan satu tugas baru berjudul Belajar 15 menit, tanpa tenggat. Jangan simpan tugas; saya akan meninjaunya.' }] });
  if (!proposal.body.proposal) writeFileSync(new URL('../../../.local/c-ai-proposal-debug.json', import.meta.url), JSON.stringify(proposal.body));
  report.taskProposal = { status: proposal.status, replyValid: typeof proposal.body.reply === 'string',
    proposalPresent: !!proposal.body.proposal, kind: proposal.body.proposal?.kind ?? null,
    expectedTitle: proposal.body.proposal?.title === 'Belajar 15 menit', noDeadline: proposal.body.proposal?.dueAt === 0 };
  const source = await loadTafsir({ chapter: 1, number: 2 }, fetch, AbortSignal.timeout(15000));
  report.source = { fetched: true, chapter: source.chapter, number: source.number,
    url: source.url, chars: source.text.length,
    sha256: createHash('sha256').update(source.text).digest('hex'), truncated: source.truncated };
  const explanation = await send({ verse: { chapter: 1, number: 2 }, messages: [{ role: 'user',
    content: 'Ringkas makna ayat dalam satu kalimat berdasarkan petikan tafsir. Jangan menambahkan kutipan atau sumber baru.' }] });
  report.tadabbur = { status: explanation.status, replyValid: typeof explanation.body.reply === 'string'
    && explanation.body.reply.trim().length > 0,
    sourceMatches: explanation.body.source?.text === source.text && explanation.body.source?.url === source.url };
  report.passed = report.taskProposal.status === 200 && report.taskProposal.proposalPresent
    && report.taskProposal.kind === 'add_task' && report.taskProposal.expectedTitle && report.taskProposal.noDeadline
    && report.tadabbur.status === 200 && report.tadabbur.replyValid && report.tadabbur.sourceMatches;
} catch {
  report.passed = false; report.error = 'feature_check_failed_no_private_details';
} finally {
  server.closeAllConnections(); await new Promise(resolve => server.close(resolve));
}
writeFileSync(new URL('./live-local.json', import.meta.url), JSON.stringify(report, null, 2) + '\n');
console.log(JSON.stringify(report, null, 2));
if (!report.passed) process.exitCode = 1;
