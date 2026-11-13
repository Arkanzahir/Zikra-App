const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const root = path.join(__dirname, 'dist');
for (const name of ['index.html', 'privacy.html']) {
  const html = fs.readFileSync(path.join(root, name), 'utf8');
  assert(html.includes('lang="id"') && html.includes('name="viewport"'), name);
  for (const [, target] of html.matchAll(/(?:href|src)="([^"]+)"/g)) {
    if (/^(?:https?:|mailto:|#)/.test(target)) continue;
    const file = target.split('#')[0];
    assert(fs.existsSync(path.resolve(root, file)), `${name}: missing ${file}`);
  }
  assert(!/<script|\.apk|https?:\/\//i.test(html), 'No remote resources, scripts or APK links in HOLD preview');
}
const home = fs.readFileSync(path.join(root, 'index.html'), 'utf8');
assert(/<button[^>]*disabled>Unduhan belum dibuka<\/button>/.test(home));
assert(home.includes('arkanzahir46@gmail.com'));
console.log('PASS: local references, language/viewport, HOLD download and support contact');
