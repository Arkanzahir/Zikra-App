const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { createHash } = require('node:crypto');
const withoutProviderLinks = require('./privacy-links.cjs');

const source = path.join(__dirname, 'dist');
const scratch = path.join(__dirname, '..', '.local');
const read = file => fs.readFileSync(path.join(source, file), 'utf8');
const publishPolicies = process.argv.includes('--public-policies');
const apkOption = process.argv.indexOf('--apk');
let download;
if (apkOption !== -1) {
  assert(publishPolicies && process.argv[apkOption + 1], 'Use --public-policies --apk <signed APK>');
  const manifest = require('../docs/beta/internal-artifact.json');
  assert.match(manifest.version, /^\d+\.\d+\.\d+$/);
  const data = fs.readFileSync(process.argv[apkOption + 1]);
  assert.equal(data.length, manifest.bytes);
  assert.equal(createHash('sha256').update(data).digest('hex'), manifest.sha256);
  download = { data, version: manifest.version, file: `downloads/Zikra-v${manifest.version}-beta.apk` };
}

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

let privacy = replaceOnce(read('privacy.html'), /<a href="\.\/#beta">Versi beta<\/a>/,
  '<a href="./#beta">Versi beta</a><a href="terms.html">Ketentuan Penggunaan</a>');
let terms = fs.readFileSync(path.join(__dirname, 'templates', 'terms.html'), 'utf8');
if (publishPolicies) {
  privacy = replaceOnce(privacy, /<p class="notice">[^<]*<\/p>/,
    '<p class="notice">Diperbarui 24 November 2026. Penerbitnya Zikra. Akses AI online melalui akun beta ditujukan untuk usia 18 tahun ke atas.</p>');
  privacy = replaceOnce(privacy, /<h2>Bacaan dan Murottal<\/h2>/,
    '<h2>Bacaan dan Murottal</h2>\n<p>Zikra memakai layanan konten Quran Foundation tanpa login Quran ID. Akun Zikra untuk AI online menggunakan Firebase Authentication. Posisi baca dan bookmark tetap tersimpan di perangkat.</p>');
  terms = replaceOnce(terms, /<p class="notice">[^<]*<\/p>/,
    '<p class="notice">Diperbarui 24 November 2026. Ketentuan ini berlaku ketika kamu memakai beta Android yang diterbitkan oleh Zikra.</p>');
  terms = replaceOnce(terms, /draf privasi Zikra/, 'kebijakan privasi Zikra');
}
if (download) {
  home = replaceOnce(home, /<section id="beta"[\s\S]*?<\/section>/, `<section id="beta" class="beta-section wrap section-space">
<div class="beta-copy"><h2>Unduh Zikra Beta</h2><p>Beta gratis untuk Android. Unduh paket resmi dan ikuti petunjuk pemasangan.</p>
<a class="button" href="${download.file}" download>Unduh APK ${download.version}</a></div>
<aside class="release"><h3>Versi ${download.version}</h3><p>${(download.data.length / 1e6).toLocaleString('id-ID', { maximumFractionDigits: 1 })} MB</p></aside>
</section>`);
  home = replaceOnce(home, /SAAT UNDUHAN SUDAH DIBUKA/, 'PEMASANGAN ANDROID');
  home = replaceOnce(home, /Unduhan publik belum dibuka\. Halaman ini merupakan pratinjau website beta; belum ada paket yang ditawarkan untuk pemasangan umum\./,
    'Ya. Pilih Unduh APK pada bagian beta dan ikuti petunjuk pemasangan Android.');
  home = replaceOnce(home, /Pratinjau · belum dipublikasikan/, 'Zikra Beta untuk Android');
  home = replaceOnce(home, /Akun AI18\+ — sedang disiapkan<\/h3><p>[^<]*<\/p>/,
    'Akun AI 18+</h3><p>Verifikasi email dan konfirmasi usiamu untuk memakai AI online. Layanan mengikuti kuota beta.</p>');
  home = replaceOnce(home, /AI online beta18\+ masih disiapkan dan memiliki kuota/, 'AI online untuk pengguna 18+ memiliki kuota');
  home = replaceOnce(home, /Bagaimana akses Zikra AI nantinya\?<\/summary><p>[^<]*<\/p>/,
    'Bagaimana cara memakai Zikra AI?</summary><p>Masuk dengan akun beremail terverifikasi dan konfirmasi bahwa usiamu minimal 18 tahun. AI online mengikuti kuota beta.</p>');
}
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
  const localHtml = withoutProviderLinks(name, html);
  const assetHtml = name === 'index.html' && download
    ? localHtml.replace(`href="${download.file}"`, '') : localHtml;
  assert(!/<(?:script|audio|video|source|track)\b|\.apk\b|https?:\/\/|\bmedia\/|#video\b|#transkrip\b/i.test(assetHtml), name);
  assert(html.includes('name="robots" content="noindex"'));
  const ids = anchorIds(html);
  for (const [, url] of localHtml.matchAll(/(?:href|src|poster)="([^"]+)"/g)) {
    if (url.startsWith('mailto:') || url === './') continue;
    if (url.startsWith('#')) { assert(ids.has(url.slice(1)), `${name}: ${url}`); continue; }
    if (url.startsWith('./#')) { assert(homeIds.has(url.slice(3)), `${name}: ${url}`); continue; }
    const target = url.split('?')[0];
    if (pages.has(target) || target === 'style.css' || target === download?.file) continue;
    addAsset(target);
  }
}
assert(!/https?:\/\/|@import/i.test(css));
for (const [, target] of css.matchAll(/url\(['"]?([^'"\)]+)['"]?\)/g)) addAsset(target);
if (!download) assert.match(home, /<button[^>]*disabled>Unduhan belum dibuka<\/button>/);
assert.match(terms, publishPolicies ? /Ketentuan ini berlaku/ : /Draf untuk ditinjau/);

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
if (publishPolicies) {
  const config = require('./vercel.json');
  if (download) {
    const target = path.join(out, download.file);
    fs.mkdirSync(path.dirname(target), { recursive: true });
    fs.writeFileSync(target, download.data);
    files.push(download.file);
    config.headers.push({
      source: '/' + download.file,
      headers: [
        { key: 'Content-Type', value: 'application/vnd.android.package-archive' },
        { key: 'Content-Disposition', value: `attachment; filename="${path.basename(download.file)}"` },
      ],
    });
  }
  fs.writeFileSync(path.join(out, 'vercel.json'), JSON.stringify(config, null, 2) + '\n');
  files.push('vercel.json');
}
const bytes = files.reduce((sum, file) => sum + fs.statSync(path.join(out, file)).size, 0);
console.log(JSON.stringify({ review: out, files: files.length, bytes, includesPromo: false, includesApk: Boolean(download) }));
