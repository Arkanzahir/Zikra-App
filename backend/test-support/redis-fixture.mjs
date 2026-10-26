import { spawn } from 'node:child_process';
import { createInterface } from 'node:readline';
import { fileURLToPath } from 'node:url';

/** Shared test-only adapter: real production Lua executed locally by fakeredis/Lupa. */
export async function redisFixture(t) {
  const child = spawn(process.env.ZIKRA_REDIS_TEST_PYTHON,
    [fileURLToPath(new URL('./redis-fixture.py', import.meta.url))],
    { stdio: ['pipe', 'pipe', 'pipe'], windowsHide: true });
  const pending = []; let closed = false;
  const fail = () => { closed = true; while (pending.length) pending.shift().reject(new Error('Redis fixture unavailable')); };
  child.on('error', fail); child.on('exit', fail); child.stderr.resume();
  const lines = createInterface({ input: child.stdout });
  lines.on('line', line => {
    const request = pending.shift(); if (!request) return;
    try {
      const data = JSON.parse(line);
      if (data.error) request.reject(new Error('access_unavailable')); else request.resolve(data.result);
    } catch { request.reject(new Error('Redis fixture unavailable')); }
  });
  t.after(() => { child.stdin.end(); child.kill(); lines.close(); });
  return { command: args => new Promise((resolve, reject) => {
    if (closed) { reject(new Error('Redis fixture unavailable')); return; }
    pending.push({ resolve, reject }); child.stdin.write(JSON.stringify(args) + '\n');
  }) };
}
