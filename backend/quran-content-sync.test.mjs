import test from 'node:test';
import assert from 'node:assert/strict';
import { QuranContentSync, syncPath } from './quran-content-sync.mjs';

const snapshot = group => ({ resource_group: group, resource_id: group === 'translations' ? 33 : 1,
  schema_version: 1, sync_sequence: 10, records: group === 'translations'
    ? [{ id: 1, resource_id: 33, resource_content_id: 33, verse_key: '1:1', text: 'Synthetic fixture' }]
    : group === 'mushafs' ? [{ id: 1, record_type: 'mushaf' }]
    : [{ id: 1, record_type: 'verse', verse_key: '1:1', text_uthmani: 'Synthetic fixture' }] });
const page = (mutations = [
  { sequence: 1, type: 'RESOURCE_CREATE', resource_group: 'quran_core', resource_id: 1,
    snapshot_url: '/api/v4/resources/snapshots/quran_core/1' },
  { sequence: 2, type: 'RESOURCE_CREATE', resource_group: 'translations', resource_id: 33,
    snapshot_url: '/api/v4/resources/snapshots/translations/33' },
  { sequence: 3, type: 'RESOURCE_CREATE', resource_group: 'mushafs', resource_id: 1,
    snapshot_url: '/api/v4/resources/snapshots/mushafs/1' }
], token = 'synthetic-checkpoint') => ({ sync: {
  mutations, sync_until_sequence: 10, has_more: false, next_page_url: null, next_sync_token: token } });
const change = (type, extra = {}) => ({ sequence: 2, type, resource_group: 'translations', resource_id: 33, ...extra });
function harness() {
  const calls = [], controller = { page: page(), fail: false };
  const engine = new QuranContentSync({ clock: () => 123, read: async path => {
    calls.push(path);
    if (path.startsWith('/api/v4/resources/sync')) return structuredClone(controller.page);
    if (controller.fail) throw Error('synthetic-network-failure');
    return snapshot(path.includes('/mushafs/') ? 'mushafs' : path.endsWith('/33') ? 'translations' : 'quran_core');
  } });
  return { calls, controller, engine };
}

test('bootstrap fetches both requested snapshots before publishing final checkpoint', async () => {
  const { engine, calls } = harness();
  const result = await engine.refresh();
  assert.equal(result.token, 'synthetic-checkpoint'); assert.equal(result.resources.size, 3);
  assert.equal(calls.length, 4); assert.equal(result.checkedAt, 123);
  result.resources.clear(); assert.equal(engine.state().resources.size, 3);
});
test('failed snapshot leaves prior generation and checkpoint untouched', async () => {
  const { engine, controller } = harness(); await engine.refresh(); const before = engine.state();
  controller.page = page([change('RESOURCE_INVALIDATE', { snapshot_url: '/api/v4/resources/snapshots/translations/33' })], 'new');
  controller.fail = true;
  await assert.rejects(engine.refresh()); assert.deepEqual(engine.state(), before);
});
test('row mutations apply in sequence and preserve exact source text', async () => {
  const { engine, controller } = harness(); await engine.refresh();
  controller.page = page([change('ROW_UPDATE', { record_type: 'translation', record_key: '1',
    data: { ...snapshot('translations').records[0], text: '  Original\ntext  ' } })], 'next');
  const value = await engine.refresh();
  assert.equal(value.resources.get('translations:33').get('translation:1').text, '  Original\ntext  ');
});
test('resource delete removes source and advances checkpoint without resurrecting it', async () => {
  const { engine, controller } = harness(); await engine.refresh();
  controller.page = page([change('RESOURCE_DELETE')], 'deleted');
  assert.equal((await engine.refresh()).resources.has('translations:33'), false);
});
test('hostile snapshot URL cannot receive transport credentials', async () => {
  const { engine, controller, calls } = harness();
  controller.page = page([change('RESOURCE_CREATE', { snapshot_url: 'https://evil.test/private' })]);
  await assert.rejects(engine.refresh()); assert.equal(calls.length, 1);
});
test('rejects missing final token, foreign resources and unordered events without commit', async () => {
  for (const value of [page([], ''), page([change('RESOURCE_UPDATE', { resource_id: 99 })]),
    page([change('RESOURCE_DELETE'), change('RESOURCE_DELETE')])]) {
    const { engine, controller } = harness(); controller.page = value;
    await assert.rejects(engine.refresh()); assert.equal(engine.state().token, null);
  }
});
test('pagination loop and changing upper bound roll back all pages', async () => {
  let count = 0;
  const engine = new QuranContentSync({ read: async () => ({ sync: { ...page().sync,
    has_more: true, next_page_url: '/api/v4/resources/sync?cursor=synthetic',
    sync_until_sequence: ++count === 1 ? 10 : 11 } }) });
  await assert.rejects(engine.refresh()); assert.equal(engine.state().token, null);
});
test('aborted run never commits and next attempt can recover', async () => {
  const { engine } = harness(); const controller = new AbortController(); controller.abort();
  await assert.rejects(engine.refresh({ signal: controller.signal }));
  assert.equal(engine.state().token, null); assert.ok((await engine.refresh()).token);
});
test('concurrent refreshes share one run', async () => {
  const { engine, calls } = harness();
  await Promise.all([engine.refresh(), engine.refresh()]); assert.equal(calls.length, 4);
});
test('path validation rejects external, traversal, unrelated endpoints and fragments', () => {
  for (const value of ['//evil.test/api/v4/resources/sync', '/api/v4/resources/../private',
    '/api/v4/resources/snapshots/translations/34', '/api/v4/resources/sync#token',
    '/api/v4/resources/sync\\evil']) assert.throws(() => syncPath(value));
});
test('generation validation failure preserves complete previous generation', async () => {
  const engine = new QuranContentSync({ read: async path => path.includes('snapshots')
    ? snapshot(path.includes('/mushafs/') ? 'mushafs' : path.endsWith('33') ? 'translations' : 'quran_core') : page(),
    validateGeneration: () => { throw Error('incomplete-canonical-verses'); } });
  await assert.rejects(engine.refresh()); assert.equal(engine.state().token, null);
});
test('incomplete bootstrap cannot silently publish a partial resource set', async () => {
  const { engine, controller } = harness(); controller.page = page([]);
  await assert.rejects(engine.refresh()); assert.equal(engine.state().resources.size, 0);
});
test('bootstrap resource enumeration may have descending historical sequences', async () => {
  const { engine, controller } = harness();
  controller.page = page([
    change('RESOURCE_CREATE', { sequence: 9, resource_group: 'quran_core', resource_id: 1,
      snapshot_url: '/api/v4/resources/snapshots/quran_core/1' }),
    change('RESOURCE_CREATE', { sequence: 3, snapshot_url: '/api/v4/resources/snapshots/translations/33' }),
    change('RESOURCE_CREATE', { sequence: 1, resource_group: 'mushafs', resource_id: 1, snapshot_url: '/api/v4/resources/snapshots/mushafs/1' })
  ]);
  assert.equal((await engine.refresh()).resources.size, 3);
  controller.page = page([change('RESOURCE_UPDATE', { sequence: 9 }), change('RESOURCE_UPDATE', { sequence: 3 })]);
  const before = engine.state();
  await assert.rejects(engine.refresh()); assert.deepEqual(engine.state(), before);
});
