import { RedisRest, readRedisConfig } from './redis-access.mjs';
import { manageInstallationErasure } from './installation-erasure.mjs';

try {
  const [action, id, flag, confirmation, ...extra] = process.argv.slice(2);
  if (extra.length || !['inspect', 'prepare', 'erase'].includes(action)
      || (action === 'inspect' ? flag !== undefined : flag !== '--confirm-installation' || confirmation !== id))
    throw new Error();
  const config = readRedisConfig();
  const result = await manageInstallationErasure(new RedisRest(config), config.prefix, id, action);
  console.log(JSON.stringify(result));
  if (result.state === 'not_ready' || result.state === 'missing') process.exitCode = 2;
} catch {
  console.error('Installation operation refused or unavailable; no identifiers, keys or credentials printed.');
  process.exitCode = 1;
}
