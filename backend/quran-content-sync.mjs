// Protocol engine only. Secrets, transport and persistence are supplied by the server.
const GROUPS = new Map([['quran_core', 1], ['translations', 33], ['mushafs', 1]]);
export const QURAN_SYNC_FILTER = 'mushafs:1;quran_core:1;translations:33';
const MAX_PAGES = 100, MAX_ROWS = 200000;
const object = value => value && typeof value === 'object' && !Array.isArray(value);
const integer = value => Number.isSafeInteger(value) && value >= 0;
function resource(group, id) {
  if (GROUPS.get(group) !== id) throw new Error('unexpected_sync_resource');
  return `${group}:${id}`;
}
function identity(group, row) {
  if (!object(row) || !integer(row.id) || row.id < 1) throw new Error('invalid_sync_record');
  if (group === 'translations') {
    if (row.resource_id !== 33 || row.resource_content_id !== 33
        || typeof row.text !== 'string' || !row.text.trim()
        || !/^[1-9][0-9]{0,2}:[1-9][0-9]{0,2}$/.test(row.verse_key)) throw new Error('invalid_translation');
    return `translation:${row.id}`;
  }
  if (group === 'mushafs') {
    if (!['mushaf', 'mushaf_page', 'mushaf_word'].includes(row.record_type)) throw new Error('invalid_mushaf_record_type');
    return `${row.record_type}:${row.id}`;
  }
  if (!['chapter', 'verse', 'word', 'juz', 'hizb', 'rub_el_hizb'].includes(row.record_type))
    throw new Error('invalid_quran_record_type');
  return `${row.record_type}:${row.id}`;
}

export function syncPath(value) {
  if (typeof value !== 'string' || value.length > 16000 || value.includes('\\')
      || /[\r\n#]/.test(value) || !value.startsWith('/api/v4/resources/')) throw new Error('invalid_sync_path');
  const url = new URL(value, 'https://apis.quran.foundation');
  if (url.origin !== 'https://apis.quran.foundation' || url.hash
      || !(/^\/api\/v4\/resources\/sync$/.test(url.pathname)
        || /^\/api\/v4\/resources\/snapshots\/(quran_core\/1|translations\/33|mushafs\/1)$/.test(url.pathname)))
    throw new Error('invalid_sync_path');
  return url.pathname + url.search;
}

export function validateQuranGeneration(resources) {
  const core = resources.get('quran_core:1'), translations = resources.get('translations:33');
  if (!core || !translations || translations.size !== 6236) throw new Error('incomplete_quran_generation');
  const chapters = new Map(), verses = new Map(), meanings = new Set();
  for (const row of core.values()) {
    if (row.record_type === 'chapter') {
      if (!Number.isInteger(row.chapter_number) || row.chapter_number < 1 || row.chapter_number > 114
          || !Number.isInteger(row.verses_count) || row.verses_count < 1 || chapters.has(row.chapter_number))
        throw new Error('invalid_quran_chapters');
      chapters.set(row.chapter_number, row.verses_count);
    } else if (row.record_type === 'verse') {
      if (row.verse_key !== `${row.chapter_id}:${row.verse_number}` || verses.has(row.verse_key)
          || typeof row.text_uthmani !== 'string' || !row.text_uthmani.trim()) throw new Error('invalid_quran_verse');
      verses.set(row.verse_key, row);
    }
  }
  if (chapters.size !== 114 || verses.size !== 6236) throw new Error('incomplete_quran_generation');
  for (const [chapter, count] of chapters) for (let number = 1; number <= count; number++) {
    if (!verses.has(`${chapter}:${number}`)) throw new Error('missing_quran_verse');
  }
  if ([...chapters.values()].reduce((sum, count) => sum + count, 0) !== 6236) throw new Error('invalid_quran_counts');
  for (const row of translations.values()) {
    if (!verses.has(row.verse_key) || meanings.has(row.verse_key)
        || row.verse_key !== `${row.chapter_id}:${row.verse_number}`) throw new Error('invalid_translation_mapping');
    meanings.add(row.verse_key);
  }
  const layout = resources.get('mushafs:1');
  if (!layout) throw new Error('missing_mushaf_resource');
  const metadata = layout.get('mushaf:1'), pages = new Map(), words = new Set(), positions = new Map();
  if (metadata?.pages_count !== 604 || metadata?.lines_per_page !== 15 || metadata?.default_font_name !== 'v2')
    throw new Error('invalid_mushaf_edition');
  for (const row of layout.values()) {
    if (row.record_type === 'mushaf_page') {
      if (row.mushaf_id !== 1 || !Number.isInteger(row.page_number) || row.page_number < 1 || row.page_number > 604
          || pages.has(row.page_number)) throw new Error('invalid_mushaf_page');
      pages.set(row.page_number, row);
    } else if (row.record_type === 'mushaf_word') {
      const source = core.get(`word:${row.word_id}`);
      if (row.mushaf_id !== 1 || !source || source.verse_id !== row.verse_id || source.position !== row.position_in_verse
          || words.has(row.word_id) || typeof row.text !== 'string' || !row.text.trim() || row.text.length > 64
          || !Number.isInteger(row.page_number) || row.page_number < 1 || row.page_number > 604
          || !Number.isInteger(row.line_number) || row.line_number < 1 || row.line_number > 15
          || !Number.isInteger(row.position_in_page) || row.position_in_page < 1
          || !['word', 'end'].includes(row.char_type_name)) throw new Error('invalid_mushaf_word');
      words.add(row.word_id);
      const list = positions.get(row.page_number) ?? []; list.push(row); positions.set(row.page_number, list);
    }
  }
  if (pages.size !== 604 || words.size !== 83665 || positions.size !== 604) throw new Error('incomplete_mushaf');
  for (const list of positions.values()) {
    list.sort((a, b) => a.position_in_page - b.position_in_page);
    for (let i = 0; i < list.length; i++) if (list[i].position_in_page !== i + 1
        || (i > 0 && list[i].line_number < list[i - 1].line_number)) throw new Error('invalid_mushaf_order');
  }
}

/** Commit all resource changes and the final checkpoint together, never page by page.
 * A failed run leaves the previous complete generation available to readers.
 */
export class QuranContentSync {
  #state = Object.freeze({ token: null, resources: new Map(), unavailable: new Set(), checkedAt: 0 });
  #pending;
  constructor({ read, clock = Date.now, validateGeneration = () => {} }) {
    this.read = read; this.clock = clock; this.validateGeneration = validateGeneration;
  }
  state() {
    // Clone at the boundary so callers cannot rewrite a committed source generation.
    return structuredClone(this.#state);
  }
  restore(value) {
    if (this.#pending || !value || typeof value.token !== 'string' || !value.token || value.token.length > 12000
        || !integer(value.checkedAt) || !(value.resources instanceof Map) || !(value.unavailable instanceof Set))
      throw Error('invalid_sync_restore');
    for (const key of value.unavailable) if (!['quran_core:1', 'translations:33', 'mushafs:1'].includes(key)) throw Error('invalid_sync_restore');
    for (const [group, id] of GROUPS) {
      const key = `${group}:${id}`, rows = value.resources.get(key);
      if (value.unavailable.has(key)) { if (rows) throw Error('invalid_sync_restore'); continue; }
      if (!(rows instanceof Map) || rows.size > MAX_ROWS) throw Error('invalid_sync_restore');
      for (const [key, row] of rows) if (identity(group, row) !== key) throw Error('invalid_sync_restore');
    }
    if (value.resources.size + value.unavailable.size !== 3) throw Error('invalid_sync_restore');
    if (!value.unavailable.size) this.validateGeneration(value.resources);
    this.#state = Object.freeze(structuredClone(value));
  }
  refresh({ signal } = {}) {
    if (this.#pending) return this.#pending;
    const work = this.#run(signal);
    this.#pending = work;
    work.finally(() => { if (this.#pending === work) this.#pending = undefined; }).catch(() => {});
    return work;
  }
  async #run(signal) {
    const previous = this.#state;
    const unavailable = new Set(previous.unavailable);
    const staging = new Map([...previous.resources].map(([key, rows]) => [key, new Map(rows)]));
    const query = new URLSearchParams({ resources: QURAN_SYNC_FILTER, per_page: '100',
      ...(previous.token ? { sync_token: previous.token } : { bootstrap: 'true' }) });
    let path = '/api/v4/resources/sync?' + query, token, bound, sequence = -1;
    const visited = new Set(), bootstrapResources = new Set();
    const fetch = async value => {
      signal?.throwIfAborted();
      const data = await this.read(syncPath(value), signal);
      signal?.throwIfAborted();
      return data;
    };
    const snapshot = async (group, id, supplied) => {
      const key = resource(group, id), expected = `/api/v4/resources/snapshots/${group}/${id}`;
      if (supplied !== expected) throw new Error('snapshot_identity_mismatch');
      const value = await fetch(expected);
      if (!object(value) || value.resource_group !== group || value.resource_id !== id
          || value.schema_version !== 1 || !integer(value.sync_sequence)
          || !Array.isArray(value.records) || value.records.length < 1 || value.records.length > MAX_ROWS)
        throw new Error('invalid_sync_snapshot');
      const rows = new Map();
      for (const row of value.records) {
        const id = identity(group, row);
        if (rows.has(id)) throw new Error('duplicate_sync_record');
        rows.set(id, structuredClone(row));
      }
      staging.set(key, rows);
      unavailable.delete(key);
    };
    for (let page = 0; ; page++) {
      if (page >= MAX_PAGES || visited.has(path)) throw new Error('sync_page_budget');
      visited.add(path);
      const envelope = await fetch(path), value = envelope?.sync;
      if (!object(value) || !integer(value.sync_until_sequence) || typeof value.has_more !== 'boolean'
          || !Array.isArray(value.mutations) || value.mutations.length > 100)
        throw new Error('invalid_sync_envelope');
      if (bound !== undefined && bound !== value.sync_until_sequence) throw new Error('sync_bound_changed');
      bound = value.sync_until_sequence;
      for (const change of value.mutations) {
        if (!object(change) || !integer(change.sequence) || change.sequence > bound
            || (previous.token && change.sequence <= sequence))
          throw new Error('invalid_sync_order');
        sequence = change.sequence;
        const key = resource(change.resource_group, change.resource_id);
        // Bootstrap enumerates resources, not the ordered incremental mutation log.
        if (!previous.token) {
          if (change.type !== 'RESOURCE_CREATE' || bootstrapResources.has(key)) throw new Error('invalid_bootstrap_resource');
          bootstrapResources.add(key);
        }
        if (['RESOURCE_CREATE', 'RESOURCE_INVALIDATE'].includes(change.type)) {
          await snapshot(change.resource_group, change.resource_id, change.snapshot_url);
        } else if (change.type === 'RESOURCE_DELETE') {
          staging.delete(key);
          unavailable.add(key);
        } else if (change.type === 'RESOURCE_UPDATE') {
          if (!staging.has(key)) throw new Error('missing_sync_resource');
        } else if (['ROW_CREATE', 'ROW_UPDATE', 'ROW_DELETE'].includes(change.type)) {
          const rows = staging.get(key);
          if (!rows || typeof change.record_type !== 'string'
              || !/^[1-9][0-9]*$/.test(String(change.record_key))) throw new Error('invalid_row_identity');
          const id = `${change.record_type}:${change.record_key}`;
          if (change.type === 'ROW_DELETE') rows.delete(id);
          else {
            if (identity(change.resource_group, change.data) !== id) throw new Error('row_identity_mismatch');
            rows.set(id, structuredClone(change.data));
          }
          if (rows.size > MAX_ROWS) throw new Error('sync_row_budget');
        } else throw new Error('unknown_sync_mutation');
      }
      if (!value.has_more) {
        if (value.next_page_url != null || typeof value.next_sync_token !== 'string'
            || !value.next_sync_token || value.next_sync_token.length > 12000) throw new Error('missing_final_checkpoint');
        token = value.next_sync_token; break;
      }
      if (!value.next_page_url) throw new Error('missing_sync_page');
      path = syncPath(value.next_page_url);
    }
    if (!previous.token && bootstrapResources.size !== GROUPS.size)
      throw new Error('incomplete_bootstrap_resources');
    if (!unavailable.size) this.validateGeneration(staging);
    signal?.throwIfAborted();
    this.#state = Object.freeze({ token, resources: staging, unavailable, checkedAt: this.clock() });
    return this.state();
  }
}
