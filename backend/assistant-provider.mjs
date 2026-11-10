/** Server-selected endpoints only. No request body can choose a host, key or model. */
export function providerRequest(config, messages, proposals) {
  const cloudflare = config.provider === 'cloudflare';
  const body = { model: config.model, messages, temperature: proposals ? 0 : 0.5, stream: false };
  if (cloudflare) Object.assign(body, { max_completion_tokens: 512, reasoning_effort: 'low', store: false });
  else body.max_tokens = 512;
  return {
    url: cloudflare
      ? `https://api.cloudflare.com/client/v4/accounts/${config.accountId}/ai/v1/chat/completions`
      : 'https://api.featherless.ai/v1/chat/completions',
    headers: { Authorization: `Bearer ${config.apiKey}`, 'Content-Type': 'application/json',
      ...(!cloudflare ? { 'X-Title': 'Zikra', 'HTTP-Referer': 'https://github.com/Arkanzahir/Zikra-App' } : {}) },
    body: JSON.stringify(body),
  };
}

export function providerDailyQuota(data) {
  return (Array.isArray(data?.errors) && data.errors.some(error => Number(error.code) === 3036))
    || Number(data?.error?.code) === 3036;
}
