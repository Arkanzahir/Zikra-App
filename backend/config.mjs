import { isIP } from 'node:net';

export class ConfigError extends Error {}

export function validateCredentials({ apiKey, model, clientToken }) {
  // Reject whitespace/control characters before constructing provider headers.
  if (typeof apiKey !== 'string' || !/^[!-~]{16,512}$/.test(apiKey))
    throw new ConfigError('Invalid FEATHERLESS_API_KEY.');
  if (typeof model !== 'string' || !/^[A-Za-z0-9][A-Za-z0-9._/-]{0,199}$/.test(model))
    throw new ConfigError('Invalid FEATHERLESS_MODEL.');
  if (typeof clientToken !== 'string' || !/^[A-Za-z0-9._~+\/=-]{32,256}$/.test(clientToken)
      || clientToken.startsWith('sk-') || clientToken === apiKey)
    throw new ConfigError('Invalid ZIKRA_CLIENT_TOKEN; use a separate random 32+ character token.');
}

export function readConfig(env = process.env) {
  const config = { apiKey: env.FEATHERLESS_API_KEY, model: env.FEATHERLESS_MODEL,
    clientToken: env.ZIKRA_CLIENT_TOKEN, host: env.HOST ?? '127.0.0.1',
    port: Number(env.PORT ?? '8787') };
  validateCredentials(config);
  if (config.host !== 'localhost' && !isIP(config.host))
    throw new ConfigError('Invalid HOST; use an IP address or localhost.');
  if (!/^\d{1,5}$/.test(String(env.PORT ?? '8787'))
      || !Number.isInteger(config.port) || config.port < 1 || config.port > 65535)
    throw new ConfigError('Invalid PORT; use an integer from 1 to 65535.');
  if (env.NODE_TLS_REJECT_UNAUTHORIZED === '0')
    throw new ConfigError('TLS verification must remain enabled.');
  return Object.freeze(config);
}
