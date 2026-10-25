import { isIP } from 'node:net';
import { isAbsolute } from 'node:path';

export class ConfigError extends Error {}

export function validateProvider({ apiKey, model, visionModel }) {
  // Reject whitespace/control characters before constructing provider headers.
  if (typeof apiKey !== 'string' || !/^[!-~]{16,512}$/.test(apiKey))
    throw new ConfigError('Invalid FEATHERLESS_API_KEY.');
  if (typeof model !== 'string' || !/^[A-Za-z0-9][A-Za-z0-9._/-]{0,199}$/.test(model))
    throw new ConfigError('Invalid FEATHERLESS_MODEL.');
  if(visionModel!==undefined&&visionModel!==null&&(typeof visionModel!=='string'||! /^[A-Za-z0-9][A-Za-z0-9._/-]{0,199}$/.test(visionModel)))throw new ConfigError('Invalid FEATHERLESS_VISION_MODEL.');
}

export function validateCredentials({ apiKey, model, visionModel, clientToken }) {
  validateProvider({ apiKey, model, visionModel });
  if (typeof clientToken !== 'string' || !/^[A-Za-z0-9._~+\/=-]{32,256}$/.test(clientToken)
      || clientToken.startsWith('sk-') || clientToken === apiKey)
    throw new ConfigError('Invalid ZIKRA_CLIENT_TOKEN; use a separate random 32+ character token.');
}

export function readConfig(env = process.env) {
  const config = { apiKey: env.FEATHERLESS_API_KEY, model: env.FEATHERLESS_MODEL, visionModel: env.FEATHERLESS_VISION_MODEL,
    clientToken: env.ZIKRA_CLIENT_TOKEN, host: env.HOST ?? '127.0.0.1',
    port: Number(env.PORT ?? '8787'), accessMode: env.ZIKRA_ACCESS_MODE ?? 'private' };
  if (config.accessMode === 'private') validateCredentials(config);
  else if (config.accessMode === 'installation') {
    validateProvider(config);
    config.clientsFile = env.ZIKRA_CLIENTS_FILE;
    config.usageFile = env.ZIKRA_USAGE_FILE;
    config.dailyLimit = Number(env.ZIKRA_DAILY_LIMIT ?? '100');
    if (![config.clientsFile, config.usageFile].every(path => typeof path === 'string' && isAbsolute(path))
        || config.clientsFile === config.usageFile)
      throw new ConfigError('Configure separate absolute ZIKRA_CLIENTS_FILE and ZIKRA_USAGE_FILE.');
    if (!Number.isInteger(config.dailyLimit) || config.dailyLimit < 1 || config.dailyLimit > 1000)
      throw new ConfigError('Invalid ZIKRA_DAILY_LIMIT; use 1 to 1000.');
  } else throw new ConfigError('Invalid ZIKRA_ACCESS_MODE.');
  if (config.host !== 'localhost' && !isIP(config.host))
    throw new ConfigError('Invalid HOST; use an IP address or localhost.');
  if (!/^\d{1,5}$/.test(String(env.PORT ?? '8787'))
      || !Number.isInteger(config.port) || config.port < 1 || config.port > 65535)
    throw new ConfigError('Invalid PORT; use an integer from 1 to 65535.');
  if (env.NODE_TLS_REJECT_UNAUTHORIZED === '0')
    throw new ConfigError('TLS verification must remain enabled.');
  return Object.freeze(config);
}
