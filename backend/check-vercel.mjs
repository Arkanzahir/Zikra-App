// Synthetic smoke check only; run with a privately supplied installation token.
import { writeFileSync } from 'node:fs';

try {
  const [originValue, output, liveFlag] = process.argv.slice(2), origin = new URL(originValue);
  if (origin.protocol !== 'https:' || !/^[a-z0-9-]+\.vercel\.app$/.test(origin.hostname)
      || origin.username || origin.password || origin.port || origin.pathname !== '/'
      || origin.search || origin.hash || !output || (liveFlag && liveFlag !== '--live')) throw new Error();
  const report = { scope: 'public-https-synthetic-no-phone-claim', origin: origin.origin,
    liveRequested: liveFlag === '--live', health: null, unauthorized: null, completion: null };
  const request = async (path, options = {}) => {
    const response = await fetch(new URL(path, origin), { ...options, redirect: 'error', signal: AbortSignal.timeout(50000) });
    let bytes = 0; const chunks = [];
    for await (const chunk of response.body) {
      bytes += chunk.length; if (bytes > 65536) throw new Error(); chunks.push(chunk);
    }
    return { status: response.status, body: JSON.parse(Buffer.concat(chunks).toString('utf8')) };
  };
  const health = await request('/health');
  report.health = { status: health.status, minimal: JSON.stringify(health.body) === '{"status":"ok"}' };
  const synthetic = JSON.stringify({ messages: [{ role: 'user', content: 'Jawab singkat: siap membantu belajar.' }] });
  const denied = await request('/v1/assistant', { method: 'POST',
    headers: { 'Content-Type': 'application/json' }, body: synthetic });
  report.unauthorized = { status: denied.status, generic: denied.body.error === 'unauthorized' };
  if (report.liveRequested) {
    if (!/^[a-f0-9]{64}$/.test(process.env.ZIKRA_CLIENT_TOKEN ?? '')) throw new Error();
    const result = await request('/v1/assistant', { method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${process.env.ZIKRA_CLIENT_TOKEN}` }, body: synthetic });
    report.completion = { status: result.status, replyValid: typeof result.body.reply === 'string'
      && result.body.reply.trim().length > 0 && result.body.reply.length <= 8000,
      providerMatches: result.body.provider === 'featherless' };
  }
  report.passed = report.health.status === 200 && report.health.minimal
    && report.unauthorized.status === 401 && report.unauthorized.generic
    && (!report.liveRequested || (report.completion.status === 200 && report.completion.replyValid && report.completion.providerMatches));
  writeFileSync(output, JSON.stringify(report, null, 2) + '\n');
  console.log(JSON.stringify(report));
  if (!report.passed) process.exitCode = 1;
} catch {
  console.error('Vercel synthetic check failed; configuration, credentials and responses are not printed.');
  process.exitCode = 1;
}
