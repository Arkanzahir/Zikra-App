import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { resolve, join } from 'node:path';
import { createHash } from 'node:crypto';

// Public snapshots stay outside the application and Git. At most two page requests at once.
const output = resolve(process.argv[2] ?? '.local/quran-d03-audit');
const metadataFolder = join(output, 'public');
const corpusFolder = join(output, 'corpus');
await mkdir(metadataFolder, { recursive: true });
await mkdir(corpusFolder, { recursive: true });
const sha256 = bytes => createHash('sha256').update(bytes).digest('hex');

async function snapshot(url, destination, limit) {
  let bytes;
  try {
    bytes = await readFile(destination);
  } catch (error) {
    if (error.code !== 'ENOENT') throw error;
    const response = await fetch(url, {
      signal: AbortSignal.timeout(18_000),
      redirect: 'error',
      headers: { 'User-Agent': 'Zikra-D03-Audit/1.0' },
    });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const chunks = [];
    let length = 0;
    for await (const chunk of response.body) {
      length += chunk.length;
      if (length > limit) throw new Error('Response size limit');
      chunks.push(chunk);
    }
    bytes = Buffer.concat(chunks);
    JSON.parse(bytes.toString('utf8'));
    await writeFile(destination, bytes);
  }
  if (bytes.length > limit) throw new Error('Snapshot size limit');
  return { json: JSON.parse(bytes.toString('utf8')), bytes: bytes.length, sha256: sha256(bytes) };
}

const sources = [
  ['chapters', 'https://api.quran.com/api/v4/chapters?language=id'],
  ['recitations', 'https://api.quran.com/api/v4/resources/recitations?language=id'],
  ['meta', 'https://api.alquran.cloud/v1/meta'],
  ['cdn-editions', 'https://api.alquran.cloud/v1/edition/format/audio'],
];
const metadata = {};
for (const [name, url] of sources) {
  metadata[name] = await snapshot(url, join(metadataFolder, `${name}.json`), 2 * 1024 * 1024);
}
const chapters = metadata.chapters.json.chapters;
const independent = metadata.meta.json.data.surahs.references;
if (chapters.length !== 114 || independent.length !== 114) throw new Error('Incomplete catalog');
const catalog = chapters.map((chapter, index) => ({
  chapter: chapter.id,
  count: chapter.verses_count,
  independentCount: independent[index].numberOfAyahs,
  matches: chapter.id === index + 1 && chapter.id === independent[index].number
    && chapter.verses_count === independent[index].numberOfAyahs,
}));
if (catalog.some(chapter => !chapter.matches)) throw new Error('Catalog mismatch');
const jobs = chapters.flatMap(chapter => Array.from(
  { length: Math.ceil(chapter.verses_count / 50) },
  (_, index) => ({ chapter: chapter.id, page: index + 1, count: chapter.verses_count }),
));
const pages = [];
let cursor = 0;
async function auditPage(job) {
  const file = `chapter-${job.chapter}-page-${job.page}.json`;
  const url = `https://api.quran.com/api/v4/verses/by_chapter/${job.chapter}`
    + `?language=id&translations=33&fields=text_uthmani&per_page=50&page=${job.page}`;
  for (let attempt = 0; attempt < 2; attempt++) {
    try {
      const result = await snapshot(url, join(corpusFolder, file), 524_288);
      const { pagination: p, verses } = result.json;
      if (p.current_page !== job.page || p.per_page !== 50 || p.total_records !== job.count
          || p.total_pages !== Math.ceil(job.count / 50)
          || (job.page < p.total_pages ? p.next_page !== job.page + 1 : p.next_page !== null)
          || verses.length !== Math.min(50, job.count - (job.page - 1) * 50)) {
        throw new Error('Pagination mismatch');
      }
      verses.forEach((verse, index) => {
        const number = (job.page - 1) * 50 + index + 1;
        if (verse.verse_number !== number || verse.verse_key !== `${job.chapter}:${number}`
            || !verse.text_uthmani?.trim()
            || !verse.translations.some(t => t.resource_id === 33 && t.text?.trim())) {
          throw new Error('Verse identity or text missing');
        }
      });
      pages.push({ ...job, file, bytes: result.bytes, sha256: result.sha256, verses: verses.length });
      return;
    } catch (error) {
      if (attempt === 1) pages.push({ ...job, error: error.message });
    }
  }
}
await Promise.all([0, 1].map(async () => {
  while (cursor < jobs.length) await auditPage(jobs[cursor++]);
}));
pages.sort((a, b) => a.chapter - b.chapter || a.page - b.page);
const reciters = [[7, 'ar.alafasy'], [6, 'ar.husary'], [9, 'ar.minshawi']].map(([id, edition]) => {
  const record = metadata.recitations.json.recitations.find(row => row.id === id);
  const cdn = metadata['cdn-editions'].json.data.find(row => row.identifier === edition);
  if (!record || !cdn || cdn.language !== 'ar' || cdn.format !== 'audio') throw new Error('Missing reciter');
  return {
    id, cdnEdition: edition, name: record.reciter_name, style: record.style,
    cdnName: cdn.englishName, cdnApiType: cdn.type,
    cdnDocumentation: 'https://alquran.cloud/cdn',
    metadataNote: cdn.type === 'translation'
      ? 'API type differs from official CDN documentation, which lists this edition as Murattal. Never reuse Quran.com word timing for these CDN bytes.'
      : null,
  };
});
const report = {
  checkedUtc: new Date().toISOString(),
  scope: 'Identity, counts, pagination and nonempty text; not editorial or phonetic certification.',
  sources: sources.map(([name, url]) => ({ name, url, sha256: metadata[name].sha256 })),
  catalog, reciters, pages,
  totalVerses: pages.reduce((sum, page) => sum + (page.verses ?? 0), 0),
  errors: pages.filter(page => page.error).length,
};
await writeFile(join(output, 'corpus-audit.json'), JSON.stringify(report, null, 2) + '\n');
console.log(JSON.stringify({ pages: pages.length, verses: report.totalVerses, errors: report.errors }));
if (report.errors || report.totalVerses !== 6236) process.exitCode = 1;
