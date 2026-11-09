import { pathToFileURL } from 'node:url';

export async function triggerEarthquakePoll(env = process.env, fetchImpl = fetch) {
  const origin = env.ZIKRA_QUAKE_BACKEND_ORIGIN ?? 'https://zikra-ai.vercel.app';
  if (origin !== 'https://zikra-ai.vercel.app' || !/^[a-f0-9]{64}$/.test(env.ZIKRA_QUAKE_CRON_SECRET ?? '')) throw new Error('poll_configuration_missing');
  const response = await fetchImpl(origin + '/v1/earthquakes/poll', { method: 'POST', redirect: 'error',
    headers: { Authorization: 'Bearer ' + env.ZIKRA_QUAKE_CRON_SECRET }, signal: AbortSignal.timeout(55000) });
  // Never print upstream bodies, credentials, or event payloads in Actions logs.
  if (response.status !== 200) { await response.body?.cancel(); throw new Error('earthquake_poll_failed'); }
  await response.body?.cancel();
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  try { await triggerEarthquakePoll(); console.log('Earthquake poll completed; no delivery guarantee.'); }
  catch { console.error('Earthquake poll unavailable; inspect private service configuration.'); process.exitCode = 1; }
}
