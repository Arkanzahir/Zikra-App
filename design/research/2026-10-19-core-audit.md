# Core UX/function audit — 19 October 2026 WIB

Audit only, app unchanged. Source inspected at branch `recovery/study-hifz-refresh`, reported HEAD `9582592`. Read README/RECOVERY/API_PLAN and Quran/Study/Hifz/storage tests; opened actual `home-quran-suhayel.png` and Diary Hifz screenshot. No Gradle/ADB or backend changes.

## Official research reached

- https://developer.android.com/guide/topics/ui/accessibility/apps — HTTP 200, full readable official Android document. Recommends minimum 48×48dp interactive targets; contrast at least 4.5:1 for normal small text, 3:1 for larger text; clear purpose descriptions for icon controls. Native Java/XML can apply these rules; Compose snippets are examples, not a migration instruction.
- https://www.w3.org/WAI/WCAG22/Understanding/resize-text.html — HTTP 200. Text should be resizable to 200% without losing content or functionality. Use as an accessibility design benchmark alongside Android font scaling, not a claim of web conformance for this native app.
- https://m3.material.io/foundations/accessible-design/overview — HTTP 200, but fetched page was a JS shell. Do not claim its recommendations were directly verified from the response.

## What already works and should remain

Quran validates chapter/verse keys, full page sequence/count and translation resource 33 before rendering; background networking/cancellation, bounded atomic cache, 114 offline chapter metadata, 4 bundled surahs, native Hifz links. Do not replace with fake downloaded/offline/mushaf/audio features.

Study scope is now academic only. Ibadah and Hifz are preserved in SQLite and use scoped entry points. CRUD, draft recreation, 200-character input cap, invalid ranges and completion state exist. Do not erase or silently recategorize these rows.

## Ranked concrete issues

### 1. High: app shows invented progress and auto-fills school-specific examples

`ZikraDbHelper.databaseForDay()` inserts 24 prototype example rows on every new date. On the first date, indices 0–7 are marked completed even though the user has never used the app. This also inserts physics reports/mobile programming for users who may not be students in those subjects.

Recommended implementation this pass: change new-day initialization to no synthetic completions and no academic/Hifz homework examples. Start truly empty and guide the user to add their first real task/target; optional explicitly chosen templates can be a separate future feature. Preserve already stored historical/user rows. If daily prayer basics remain, they must start incomplete and be visibly a checklist template, not inferred worship.

Proposed files: `data/ZikraDbHelper.java`, storage instrumentation tests; Study empty state; README actual behavior. Regression: fresh DB has 0 completed; day ledger means deleting all does not reseed; old v1/v2 rows, completion and Hifz data survive upgrade; no hard-coded "8 of 24" anywhere. Product choice: don't preserve the prototype count just to satisfy old tests.

### 2. High: tasks disappear from accessible UI at local midnight

`getTasks()` queries only today's date. User-created academic tasks and Hifz targets remain in the database but are never accessible through any history/day-selection UI the next day; Study date is a nonclickable label. This is not durable planning. `updateTask`, `updateHifzTask`, `setCompleted`, `deleteTask` constrain the current day; if an editor stays open across midnight, saving can change zero rows while the UI still says "saved".

Safe implementation options, choose one deliberately: (A) add a real date selector/history to the existing daily ledger and pass selected date into scoped operations; (B) model academic/Hifz targets as persistent until completed with dates for creation/completion. Do not implement carryover that duplicates rows or silently copies completed worship tasks. At minimum, prevent false "saved" for stale midnight editors by reporting update success and offering to add the draft to today's list.

Proposed files: `ZikraDbHelper`, Task model if dates exposed, `StudyActivity`, agenda date control, storage/recreation tests. Meaningful tests require injected clock: create day A, advance day B, show/edit old date or persistent target; preserve IDs/completion; no data loss and no duplicate reseeding. This is more valuable than adding ornamental cards.

### 3. High/medium: reader lacks adjustable text and long-surah verse jump

Quran reader settings only contain light/dark/ambient. Arabic stays 29sp, translation 14sp. User must scroll through up to 50 long ayat before bottom pagination; there is no way to open e.g. Al-Baqarah 280 except a prior bookmark/Hifz link. Existing target extras and `QuranPage.pageFor` make a genuine verse jump inexpensive and preserve validated pagination.

Recommended this pass: native reading settings dialog with separately persisted Arabic and translation size (bounded sensible choices), and "Lompat ke ayat" numeric input validated against chapter count. Re-render/rescale current content and retain verse position/theme. Show the chosen range clearly, e.g. `Ayat 251–286` instead of implying actual mushaf pages. Keep the catalog light and flat, close to supplied Suhayel reference. Do not add dead Page/Juz/Hizb tabs.

Proposed files: `QuranActivity`, reader settings XML/new strings and `reading_item_ayah.xml`, `RebuiltQuranTest`. Regression: text sizes survive recreation, bookmark still opens same stable key; valid jump 280 maps to page6 and highlights/scrolls to 2:280, invalid 0/287 stays in dialog with accessible error; light/night persist and large font doesn't clip header controls.

### 4. Medium: searching Arabic names only works for exact bundled diacritics

`renderCatalog` compares unnormalized `surah.arabic` with the Latin-normalized query. Most people type `البقرة` without diacritics, while metadata is vocalized. Normalize Arabic combining marks/tatweel and common alef forms on both sides while preserving original display text. Latin already ignores spaces/apostrophes/hyphens, but not all transliteration variants.

Proposed files: extract small pure `SurahSearch` helper and JVM tests, invoke in `QuranActivity`; keep full114 list immutable. Regression: `Al Mulk`, `al-mulk`, `67`, `البقرة`/`ٱلْبَقَرَةِ` return intended chapter; blank returns114 and nonsense yields clear empty state.

### 5. Medium: small metadata and rigid reader header

Study/Hifz/Quran use many 9–12sp labels, which are too small as a primary means of understanding state. Reader's fixed120dp Arabic title + weighted metadata can squeeze the Latin area at 320dp/font200%. Surah row reserves90dp Arabic +36dp badge+margin. Increase essential metadata to12–14sp, allow wrapping, use minHeight instead of fixed row heights, do not rely on color to signal completion, and keep icon taps48dp. Verify using actual small-screen/font-scaled APK screenshots once, not a screenshot mockup.

Proposed files: `study_agenda_header`, `item_hifz_target`, `item_task`, `activity_quran`, `reading_item_surah`, reader typography/themes. Check contrast ratios programmatically for semantic text colors, then actual view bounds with system font scale2.0. Touch/interaction checks are more useful than XML-only snapshot assertions.

### 6. Medium: metadata/docs contradict live implementation

`android/API_PLAN.md` still says Open-Meteo unintegrated in the provider table/body and says API access failed, despite current live integration/status banner. README historical snippets likewise refer to old garden. Fix current status sections and clearly separate retained historical validation. No build required for documentation-only edits.

## Scope to avoid

No auth/backend secrets, external generic "AI" claims, audio/TTS pretending to be murottal, mushaf promises without edition/layout data, full offline download promises, or unnecessary API changes. Current cache source label is honest and should stay. Functional UI can improve substantially using local state/native APIs already available.
