import test from 'node:test';
import assert from 'node:assert/strict';
import { QuranSyncService } from './quran-sync-service.mjs';
const id = '11111111-1111-4111-8111-111111111111';
const state = () => ({ checkedAt: 1000000000, token: 'synthetic', unavailable: new Set(), resources: new Map([
  ['quran_core:1', new Map([['chapter:1', { record_type: 'chapter', chapter_number: 1, verses_count: 1 }],
    ['verse:1', { record_type: 'verse', chapter_id: 1, verse_number: 1, verse_key: '1:1', text_uthmani: 'Original  text' }]])],
  ['translations:33', new Map([['translation:1', { verse_key: '1:1', text: 'Exact\nmeaning' }]])]
]) });
function service(options = {}) {
  return new QuranSyncService({ redis: { command: async () => 1 }, clock: () => 1000000001,
    cache: { load: async () => state() }, engineFactory: () => ({ restore() {} }), ...options });
}
test('page response contains only selected content and public provenance, never checkpoint', async () => {
  const value = await service().page(1, 1, id);
  assert.equal(value.verses.length, 1); assert.equal(value.verses[0].text_uthmani, 'Original  text');
  assert.equal(value.verses[0].translations[0].text, 'Exact\nmeaning');
  assert.equal(value.pagination.next_page, null); assert.equal(value.zikra_sync.schema, 1);
  assert.ok(!JSON.stringify(value).includes('synthetic'));
});
test('invalid pages and unauthorized identity never fetch content', async () => {
  const value = service({ cache: { load: async () => { throw Error('must-not-load'); } } });
  await assert.rejects(value.page(0, 1, id), { status: 400 });
  await assert.rejects(value.page(1, 1, 'unknown'), { status: 401 });
});
test('Quran rate quota is distinct from AI inference and rejects before content work', async () => {
  const calls = [];
  const value = service({ redis: { command: async args => { calls.push(args); return 61; } } });
  await assert.rejects(value.page(1, 1, id), { status: 429 });
  assert.equal(calls.length, 1); assert.ok(calls[0][3].includes('quran-sync:rate:'));
});
test('cold restart reads durable generation without downloading new snapshots', async () => {
  let restored = 0;
  const value = service({ engineFactory: () => ({ restore() { restored++; }, refresh() { throw Error('no-sync-needed'); } }) });
  await value.page(1, 1, id); await value.page(1, 1, id); assert.equal(restored, 1);
});
test('known deleted source stays unavailable and is not substituted with old content', async () => {
  const deleted = state(); deleted.unavailable.add('translations:33'); deleted.resources.delete('translations:33');
  await assert.rejects(service({ cache: { load: async () => deleted } }).page(1, 1, id), { message: 'quran_resource_unavailable' });
});
test('failed refresh or publication does not replace last complete generation', async () => {
  const old = state(); old.checkedAt = 1;
  const value = service({ cache: { load: async () => old, acquire: async () => 'lease',
    publish: async () => { throw Error('storage-offline'); }, release: async () => {} },
    engineFactory: () => ({ restore() {}, refresh: async () => state() }) });
  const page = await value.page(1, 1, id);
  assert.equal(page.zikra_sync.checked_at, 1); assert.equal(page.zikra_sync.retained_during_outage, true);
});

function layoutState() {
  const value = state(), core = value.resources.get('quran_core:1');
  core.set('verse:1', { ...core.get('verse:1'), id: 1, juz_number: 1 });
  core.set('juz:1', { first_verse_id: 1 });
  core.set('word:10', { text_imlaei: 'accessible text' });
  core.set('word:11', { text_imlaei: '' });
  value.resources.set('mushafs:1', new Map([
    ['a', { record_type: 'mushaf_word', verse_id: 1, word_id: 10, page_number: 1,
      position_in_page: 1, position_in_verse: 1, line_number: 2, text: '\ufc41', char_type_name: 'word' }],
    ['b', { record_type: 'mushaf_word', verse_id: 1, word_id: 11, page_number: 2,
      position_in_page: 1, position_in_verse: 2, line_number: 1, text: '\ufc42', char_type_name: 'end' }]
  ])); return value;
}
test('Mushaf preserves source glyphs and resolves split verses, word anchors and juz', async () => {
  const value = service({ cache: { load: async () => layoutState() } });
  const page = await value.mushaf(1, id);
  assert.equal(page.verses[0].words[0].code_v2, '\ufc41');
  assert.equal(page.verses[0].words[0].text_uthmani, 'accessible text');
  assert.equal((await value.locate({ chapter: 1, verse: 1, preferred: 2 }, id)).page_number, 2);
  assert.equal((await value.locate({ chapter: 1, verse: 1, position: 1, preferred: 2 }, id)).page_number, 1);
  assert.equal((await value.locate({ juz: 1 }, id)).page_number, 1);
  await assert.rejects(value.locate({ chapter: 1, verse: 1, position: 3 }, id), { status: 400 });
  await assert.rejects(value.locate({ juz: 1, chapter: 1 }, id), { status: 400 });
  await assert.rejects(value.mushaf(605, id), { status: 400 });
});
