import { realpathSync } from 'node:fs';
import { isAbsolute, relative, resolve, dirname, basename, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = realpathSync(fileURLToPath(new URL('../', import.meta.url)));

/** Resolve the real parent before checking, so a directory link cannot bypass repository exclusion. */
export function privateOperatorPath(value) {
  if (typeof value !== 'string' || !isAbsolute(value)) throw new Error('private_path_required');
  const canonical = resolve(realpathSync(dirname(value)), basename(value));
  const location = relative(root, canonical);
  const inside = location !== '..' && !location.startsWith('..' + sep) && !isAbsolute(location);
  if (inside && !location.startsWith('.local' + sep)) throw new Error('repository_output_forbidden');
  return canonical;
}
