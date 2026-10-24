import { issueClient, revokeClient } from './access.mjs';

try {
  const [command, registryFile, target] = process.argv.slice(2);
  if (command === 'issue' && registryFile && target) {
    const id = issueClient(registryFile, target);
    console.log(JSON.stringify({ id, issued: true, tokenSavedPrivately: true }));
  } else if (command === 'revoke' && registryFile && target) {
    revokeClient(registryFile, target);
    console.log(JSON.stringify({ revoked: true }));
  } else throw new Error('invalid_command');
} catch {
  console.error('Client administration failed. Check command, private paths and permissions; no secret printed.');
  process.exitCode = 1;
}
