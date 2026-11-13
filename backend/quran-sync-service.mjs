import { QuranSyncCache } from './quran-sync-cache.mjs';
import { createQuranSyncEngine } from './quran-sync-transport.mjs';

const FRESH = 6 * 86400000;
export const QURAN_RATE = "local n=redis.call('INCR',KEYS[1]);if n==1 then redis.call('EXPIRE',KEYS[1],60) end;return n";
function failure(code, status = 503) { const error = Error(code); error.status = status; return error; }

export class QuranSyncService {
  constructor({ redis, prefix = 'zikra:v1:', env, clock = Date.now, cache, engineFactory }) {
    this.redis = redis; this.prefix = prefix; this.clock = clock;
    this.cache = cache ?? new QuranSyncCache(redis, prefix, env?.ZIKRA_QURAN_CACHE_KEY);
    this.engineFactory = engineFactory ?? (() => createQuranSyncEngine(env, { clock }));
    this.current = null; this.pending = null; this.retryAt = 0;
  }
  async state() {
    if (this.current && this.clock() - this.current.checkedAt < FRESH) return this.current;
    if (this.pending) return this.pending;
    if (this.clock() < this.retryAt) {
      if (this.current) return this.current;
      throw failure('quran_sync_unavailable');
    }
    const work = this.refresh(); this.pending = work;
    try { return await work; }
    finally { if (this.pending === work) this.pending = null; }
  }
  async refresh() {
    let lease;
    const deadline = Date.now() + 50000;
    try {
      let stored = null;
      try { stored = await this.cache.load({ deadline }); }
      catch { /* Corrupt/rotated cache is never served; a fenced bootstrap may replace it. */ }
      if (stored) {
        const validator = this.engineFactory(); validator.restore(stored);
        if (stored.checkedAt > this.clock() + 60000) throw failure('quran_cache_clock');
        this.current = stored;
        if (this.clock() - stored.checkedAt < FRESH) return stored;
      }
      lease = await this.cache.acquire();
      if (!lease) {
        if (this.current) return this.current;
        throw failure('quran_sync_busy');
      }
      const candidate = this.engineFactory();
      if (this.current) candidate.restore(this.current);
      const next = await candidate.refresh({ signal: AbortSignal.timeout(Math.max(1, deadline - Date.now())) });
      if (next.unavailable.size) this.current = next;
      await this.cache.publish(next, lease, { deadline });
      this.current = next; this.retryAt = 0;
      return next;
    } catch (error) {
      this.retryAt = this.clock() + 60000;
      // Authorization loss is not a connectivity outage permitting stale content.
      if (error.status === 401 || error.status === 403) { this.current = null; throw failure('quran_source_access_unavailable'); }
      if (this.current) return this.current;
      throw failure('quran_sync_unavailable');
    } finally {
      if (lease) { try { await this.cache.release(lease); } catch { /* Lock has a bounded TTL. */ } }
    }
  }
  async page(chapter, page, identity) {
    if (!Number.isInteger(chapter) || chapter < 1 || chapter > 114 || !Number.isInteger(page) || page < 1 || page > 6)
      throw failure('invalid_quran_page', 400);
    const state = await this.available(identity);
    const core = state.resources.get('quran_core:1'), translations = state.resources.get('translations:33');
    if (!core || !translations) throw failure('quran_resource_unavailable');
    const info = [...core.values()].find(row => row.record_type === 'chapter' && row.chapter_number === chapter);
    const pages = Math.ceil(info.verses_count / 50);
    if (page > pages) throw failure('invalid_quran_page', 400);
    const meanings = new Map([...translations.values()].map(row => [row.verse_key, row]));
    const verses = [...core.values()].filter(row => row.record_type === 'verse' && row.chapter_id === chapter
      && row.verse_number >= (page - 1) * 50 + 1 && row.verse_number <= page * 50)
      .sort((a, b) => a.verse_number - b.verse_number).map(row => ({ verse_number: row.verse_number,
        verse_key: row.verse_key, text_uthmani: row.text_uthmani,
        translations: [{ resource_id: 33, text: meanings.get(row.verse_key).text }] }));
    return { verses, pagination: { per_page: 50, current_page: page, total_pages: pages,
      total_records: info.verses_count, next_page: page < pages ? page + 1 : null },
      zikra_sync: this.metadata(state) };
  }
  metadata(state) {
    return { schema: 1, checked_at: state.checkedAt, source: 'Quran Foundation Content Sync', translation: 33,
      retained_during_outage: this.clock() - state.checkedAt >= FRESH };
  }
  async available(identity) {
    if (!/^[a-f0-9-]{36}$/.test(identity)) throw failure('unauthorized', 401);
    const usage = await this.redis.command(['EVAL', QURAN_RATE, 1, this.prefix + 'quran-sync:rate:' + identity]);
    if (!Number.isInteger(usage) || usage < 1) throw failure('quran_access_unavailable');
    if (usage > 60) throw failure('quran_rate_limited', 429);
    const state = await this.state();
    if (state.unavailable.size) throw failure('quran_resource_unavailable');
    return state;
  }
  async mushaf(page, identity) {
    if (!Number.isInteger(page) || page < 1 || page > 604) throw failure('invalid_quran_page', 400);
    const state = await this.available(identity), core = state.resources.get('quran_core:1'), layout = state.resources.get('mushafs:1');
    if (!core || !layout) throw failure('quran_resource_unavailable');
    const rows = [...layout.values()].filter(row => row.record_type === 'mushaf_word' && row.page_number === page)
      .sort((a, b) => a.position_in_page - b.position_in_page);
    const verses = new Map();
    for (const row of rows) {
      const verse = core.get(`verse:${row.verse_id}`), word = core.get(`word:${row.word_id}`);
      if (!verse || !word) throw failure('quran_resource_unavailable');
      if (!verses.has(verse.id)) verses.set(verse.id, { verse_key: verse.verse_key, juz_number: verse.juz_number, words: [] });
      verses.get(verse.id).words.push({ position: row.position_in_verse, page_number: page, line_number: row.line_number,
        code_v2: row.text, char_type_name: row.char_type_name, text_uthmani: word.text_imlaei ?? '' });
    }
    if (!verses.size) throw failure('quran_resource_unavailable');
    return { verses: [...verses.values()], pagination: { per_page: verses.size, current_page: 1,
      total_pages: 1, total_records: verses.size, next_page: null }, zikra_sync: this.metadata(state) };
  }
  async locate({ chapter = 0, verse = 0, position = 0, preferred = 0, juz = 0 }, identity) {
    if (![chapter, verse, position, preferred, juz].every(Number.isInteger)
        || chapter < 0 || chapter > 114 || verse < 0 || verse > 286 || position < 0 || position > 512
        || preferred < 0 || preferred > 604 || juz < 0 || juz > 30
        || (juz ? chapter || verse || position || preferred : !chapter || !verse))
      throw failure('invalid_quran_page', 400);
    const state = await this.available(identity), core = state.resources.get('quran_core:1'), layout = state.resources.get('mushafs:1');
    if (!core || !layout) throw failure('quran_resource_unavailable');
    const target = juz ? core.get(`juz:${juz}`)?.first_verse_id
      : [...core.values()].find(row => row.record_type === 'verse' && row.verse_key === `${chapter}:${verse}`)?.id;
    const words = [...layout.values()].filter(row => row.record_type === 'mushaf_word' && row.verse_id === target)
      .sort((a, b) => a.position_in_verse - b.position_in_verse);
    const found = position ? words.find(row => row.position_in_verse === position)
      : words.find(row => row.page_number === preferred) ?? words[0];
    if (!found) throw failure('invalid_quran_page', 400);
    return { page_number: found.page_number, zikra_sync: this.metadata(state) };
  }
}
