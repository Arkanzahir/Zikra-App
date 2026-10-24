import { spawn } from 'node:child_process';
import { once } from 'node:events';
import { createServer } from 'node:net';
import { fileURLToPath } from 'node:url';
import { setTimeout as delay } from 'node:timers/promises';
import { readConfig } from './config.mjs';

// Only fixed synthetic text leaves this machine; credentials and replies are never printed.
async function verify() {
  const config = readConfig();
  const report = { runtime: process.version, scope: 'private-loopback-not-production',
    configValid: true, starts: [], liveRequested: process.argv.includes('--live'),
    keyRotationVerified: false, publicHostingVerified: false };
  const reservation = createServer();
  reservation.listen(0, '127.0.0.1');
  await once(reservation, 'listening');
  const port = reservation.address().port;
  await new Promise(resolve => reservation.close(resolve));
  const origin = `http://127.0.0.1:${port}`;
  for (let index = 0; index < 2; index++) {
    const child = spawn(process.execPath, [fileURLToPath(new URL('./server.mjs', import.meta.url))], {
      env: { ...process.env, HOST: '127.0.0.1', PORT: String(port) },
      stdio: ['ignore', 'pipe', 'pipe'], windowsHide: true
    });
    // Retain only whether application logs exposed a private value, never raw output.
    let logTail = '', logSecretFound = false;
    const secretValues = [config.apiKey, config.clientToken];
    const tailSize = Math.max(...secretValues.map(value => value.length));
    const checkLogs = chunk => {
      const text = logTail + chunk.toString('utf8');
      logSecretFound ||= secretValues.some(value => text.includes(value));
      logTail = text.slice(-tailSize);
    };
    child.stdout.on('data', checkLogs); child.stderr.on('data', checkLogs);
    let childError = false;
    child.on('error', () => { childError = true; });
    const exited = once(child, 'close');
    const result = { healthStatus: null, healthMinimal: false, unauthorizedStatus: null,
      processStopped: false, logsContainSecret: false };
    try {
      for (let attempt = 0; attempt < 50; attempt++) {
        if (childError || child.exitCode !== null) throw new Error('startup_failed');
        try {
          const response = await fetch(`${origin}/health`, { signal: AbortSignal.timeout(500),
            redirect: 'error' });
          result.healthStatus = response.status;
          result.healthMinimal = JSON.stringify(await response.json()) === '{"status":"ok"}';
          break;
        } catch { await delay(100); }
      }
      if (result.healthStatus !== 200 || !result.healthMinimal) throw new Error('health_failed');
      const unauthorized = await fetch(`${origin}/v1/assistant`, { method: 'POST',
        headers: { 'Content-Type': 'application/json' }, body: '{"messages":[]}',
        redirect: 'error', signal: AbortSignal.timeout(2000) });
      result.unauthorizedStatus = unauthorized.status;
      await unauthorized.arrayBuffer();
      if (index === 0 && report.liveRequested) {
        const response = await fetch(`${origin}/v1/assistant`, {
          method: 'POST', redirect: 'error', signal: AbortSignal.timeout(40000),
          headers: { Authorization: `Bearer ${config.clientToken}`, 'Content-Type': 'application/json' },
          body: JSON.stringify({ messages: [{ role: 'user', content: 'Balas singkat: siap.' }] })
        });
        const body = await response.json();
        report.liveGatewayStatus = response.status;
        report.liveReplyValid = response.status === 200 && body.provider === 'featherless'
          && typeof body.reply === 'string' && body.reply.trim().length > 0;
        // Only the gateway's fixed error codes may appear in the report.
        if (!report.liveReplyValid) report.liveError = ['provider_busy', 'provider_unavailable',
          'invalid_provider_response', 'try_later'].includes(body.error) ? body.error : 'check_failed';
      }
    } finally {
      if (child.exitCode === null && child.signalCode === null) child.kill('SIGTERM');
      let deadline;
      try {
        await Promise.race([exited, new Promise((_resolve, reject) => {
          deadline = setTimeout(() => { child.kill('SIGKILL'); reject(new Error('stop_timeout')); }, 45000);
        })]);
        result.processStopped = true;
      } finally { clearTimeout(deadline); }
      result.logsContainSecret = logSecretFound;
      result.stopMode = process.platform === 'win32' ? 'windows-process-termination' : 'SIGTERM';
      report.starts.push(result);
    }
  }
  report.passed = report.starts.every(result => result.healthStatus === 200 && result.healthMinimal
    && result.unauthorizedStatus === 401 && result.processStopped && !result.logsContainSecret)
    && (!report.liveRequested || report.liveReplyValid);
  return report;
}

try {
  const report = await verify();
  console.log(JSON.stringify(report, null, 2));
  if (!report.passed) process.exitCode = 1;
} catch {
  console.log(JSON.stringify({ passed: false, error: 'private_check_failed_no_sensitive_details' }));
  process.exitCode = 1;
}
