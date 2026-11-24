const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const source = path.join(__dirname, 'dist');
const scratch = path.join(__dirname, '..', '.local');
const read = file => fs.readFileSync(path.join(source, file), 'utf8');

function replaceOnce(html, pattern, replacement) {
  assert.equal([...html.matchAll(new RegExp(pattern.source, 'g'))].length, 1,
    `Expected exactly one source match for ${pattern}`);
  return html.replace(pattern, replacement);
}

let home = read('index.html');
home = replaceOnce(home, /<section class="promo wrap" id="video"[^>]*>[\s\S]*?<\/section>/, '');
home = replaceOnce(home, /<a href="#video">Video<\/a>/, '');
home = replaceOnce(home, /<a class="button secondary" href="#video">[^<]*<span[^>]*>[^<]*<\/span><\/a>/,
  '<a class="button secondary" href="#beta">Status unduhan</a>');
home = replaceOnce(home, /class="feature-panel panel-ai"/, 'class="feature-panel panel-ai panel-ai-static"');
home = replaceOnce(home, /<img class="panel-demo" src="media\/ai-demo\.jpg"[^>]*>/,
  '<p><a class="text-link" href="#akun">Status akses AI online ↗</a></p>');
home = replaceOnce(home, /<a href="privacy\.html">Privasi & batas layanan<\/a>/,
  '<a href="privacy.html">Privasi</a><a href="terms.html">Ketentuan Penggunaan</a>');

const privacy = replaceOnce(read('privacy.html'), /<a href="\.\/#beta">Versi beta<\/a>/,
  '<a href="./#beta">Versi beta</a><a href="terms.html">Ketentuan Penggunaan</a>');
const terms = fs.readFileSync(path.join(__dirname, 'templates', 'terms.html'), 'utf8');
const css = read('style.css') + '\n.panel-ai-static { justify-content: center; padding-bottom: clamp(24px, 4vw, 38px); }\n';
const pages = new Map([['index.html', home], ['privacy.html', privacy], ['terms.html', terms]]);
const assets = new Set(['assets/FONT-LICENSE.txt']);
const anchorIds = html => new Set([...html.matchAll(/\bid="([^"]+)"/g)].map(match => match[1]));
const homeIds = anchorIds(home);

function addAsset(target) {
  assert.match(target, /^assets\/(?:[\w-]+\/)*[\w.-]+\.(?:png|svg|ttf|txt)$/);
  assert(fs.lstatSync(path.join(source, target)).isFile(), `Expected local asset ${target}`);
  assets.add(target);
}

for (const [name, html] of pages) {
  assert(!/<(?:script|audio|video|source|track)\b|\.apk\b|https?:\/\/|\bmedia\/|#video\b|#transkrip\b/i.test(html), name);
  assert(html.includes('name="robots" content="noindex"'));
  const ids = anchorIds(html);
  for (const [, url] of html.matchAll(/(?:href|src|poster)="([^"]+)"/g)) {
    if (url.startsWith('mailto:') || url === './') continue;
    if (url.startsWith('#')) { assert(ids.has(url.slice(1)), `${name}: ${url}`); continue; }
    if (url.startsWith('./#')) { assert(homeIds.has(url.slice(3)), `${name}: ${url}`); continue; }
    const target = url.split('?')[0];
    if (pages.has(target) || target === 'style.css') continue;
    addAsset(target);
  }
}
assert(!/https?:\/\/|@import/i.test(css));
for (const [, target] of css.matchAll(/url\(['"]?([^'"\)]+)['"]?\)/g)) addAsset(target);
assert.match(home, /<button[^>]*disabled>Unduhan belum dibuka<\/button>/);
assert.match(terms, /Draf untuk ditinjau/);

fs.mkdirSync(scratch, { recursive: true });
const out = fs.mkdtempSync(path.join(scratch, 'zikra-public-review-'));
for (const [name, html] of pages) fs.writeFileSync(path.join(out, name), html);
fs.writeFileSync(path.join(out, 'style.css'), css);
for (const asset of assets) {
  const target = path.join(out, asset);
  fs.mkdirSync(path.dirname(target), { recursive: true });
  fs.copyFileSync(path.join(source, asset), target);
}
const files = [...pages.keys(), 'style.css', ...assets];
const bytes = files.reduce((sum, file) => sum + fs.statSync(path.join(out, file)).size, 0);
console.log(JSON.stringify({ review: out, files: files.length, bytes, includesPromo: false, includesApk: false }));
