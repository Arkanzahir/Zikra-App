const endpoint = 'https://zikra-ai.vercel.app/v1/earthquakes/poll';

// No public trigger, provider credentials, event contents or automatic retries.
export async function poll(env, fetchImpl = fetch) {
  if (!/^[a-f0-9]{64}$/.test(env.ZIKRA_QUAKE_CRON_SECRET ?? '')) {
    throw new Error('earthquake_cron_configuration');
  }
  const response = await fetchImpl(endpoint, {
    method: 'POST',
    redirect: 'manual',
    headers: { Authorization: 'Bearer ' + env.ZIKRA_QUAKE_CRON_SECRET },
    signal: AbortSignal.timeout(55000),
  });
  await response.body?.cancel();
  if (response.status !== 200) throw new Error('earthquake_poll_unavailable');
}

export default {
  fetch() { return new Response(null, { status: 404 }); },
  async scheduled(controller, env) {
    controller.noRetry();
    await poll(env);
  },
};
