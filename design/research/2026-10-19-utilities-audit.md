# Utilities audit — 19 October 2026 (Jakarta)

Research/audit only. No Android source edits, Gradle tasks or adb actions performed.

## References actually inspected

- `design/references/ibadah-isha-manzoor.png`: useful small editorial headers, calm light backgrounds, readily scanned lists, compact prayer/calendar hierarchy. Keep the hierarchy and spacing; do not reproduce tiny screenshot text sizes or invent additional collections.
- `design/references/ai-voice-reference.png`: separate written/voice entry points and clear conversation layout are useful. The model/voice-analysis presentation would misrepresent this app's deterministic local assistant; retain explicit offline capability labeling.
- Android accessibility, HTTPS fetched successfully (200): https://developer.android.com/guide/topics/ui/accessibility/apps — recommends interactive targets at least 48 × 48 dp; ordinary small text contrast at least 4.5:1, larger text 3:1.
- Android 14 font scaling, HTTPS fetched successfully (200): https://developer.android.com/about/versions/14/features#non-linear-font-scaling — use sp, accommodate up to 200% font scaling, test maximum text size, avoid fixed text container heights.
- Android XML font docs, HTTPS 200 with redirect: https://developer.android.com/develop/ui/views/text-and-emoji/fonts-in-xml — native XML-compatible typography.

## User-visible issues, prioritized

1. **Wrong Hifz route in actual assistant response.** `OfflineAssistant.reply` tells the user to use the Hifz category in Study Hub, which is now academic-only. It must name Diary Hifz instead. The response also unnecessarily assumes college/dosen/kampus; replace with broadly understandable belajar/guru/layanan konseling language.
2. **Invented identity across Profile and AI.** Profile XML displays "Arkan Firdaus", avatar A and a Surabaya caption; AI XML/empty thread/greeting reply says Arkan for every user. No name editor exists. Remove the assumed identity: neutral "Profil lokal"/"Pengguna Zikra" and an avatar icon; greet without a fixed name. Show a role only when saved from older onboarding, without requiring role selection again. Existing `roleLabel()` is dead code and the existing `NativePolishTest#testProfileHeaderReflectsSelectedLearningRole` contradicts current implementation.
3. **Readability.** Numerous 9–12sp subtitles, sources, assistant messages, chips and profile labels; light gray #92A19A on white is 2.70:1, #8B9B91 on #F7F9F7 is 2.76:1, white text on green #57927E is 3.61:1, white on assistant purple #A489C8 is 3.00:1. These fail small-text 4.5:1 guidance. Primary Pine on white is 13.58:1. Use a darker shared secondary ink (suggest #52665F), 14sp readable body/captions, 16sp principal rows, and dark Pine selected chips.
4. **Small or clipped controls.** Dua category chips are 40dp/10sp; Hadith filters 37dp; calendar dates 42dp; AI action tiles have fixed 132dp height even though their stack includes several text lines/margins/padding (likely truncation, worse at enlarged font). Use 48dp minimum wrap-content chips and >=48dp calendar cells; tiles wrap-content + minHeight; rows should grow when fonts scale.
5. **Headers feel like different products.** Dua left 21sp header with decorative heart; Hadith/Calendar custom 66dp, Profile centered 16sp on green, AI centered purple header plus generic avatar. Shared `ui/FeatureHeader` exists but binds only a Protector-oriented action; do not expand it simultaneously with another owner. These pages can reuse the future general shared header or at least align white/Pine title/back, 48dp buttons and 20dp text now.
6. **Profile progress navigation is misleading after academic separation.** The total shows global checklist progress (Study + Ibadah + Hifz), but tapping opens academic Study. Either label it "Tugas akademik" and calculate the Study subset, or keep global summary and add clearly named destinations to all three. Prefer academic progress when linking Study.
7. **AI content hierarchy.** Purple gradient, large voice presentation, two fixed tiles and small chat bubbles put decoration above use. Retain purple only as modest feature accent or unify Pine; show concise "Panduan offline" explanation and useful topic starters; voice is Android speech-to-text, not Quran recitation evaluation. The code already waits for manual send after recognition and handles missing speech service; preserve this behavior. History is bounded (24 messages, 1600 chars/message), malformed state ignored, destroy cleanup exists.
8. **Hadith hero interaction.** Hero has a click listener but lacks explicit focusable/clickable XML semantics/ripple/accessibility label. Add clear "Baca hadis" action or visible row behavior. Search/empty/save states already exist and should be preserved; only seven curated excerpts exist, not a complete corpus.
9. **Dua content remains only four entries.** Existing exact source labels and TTS fallback are good. Don't present it as a comprehensive library or real reciter audio. Keep category/search/expand behavior and Arabic TTS availability message. Raise source and translated text sizes.
10. **Calendar limits are explicit but visually buried.** Umm al-Qura estimate disclaimer already exists. Move short "Perkiraan Umm al-Qura" near calendar heading, retain full note. Current date is computed once in field at Activity creation; update onResume if date crossed midnight. Fixed Jakarta date should be documented until actual location/time-zone support is implemented by a separate owner.

## Proposed bounded implementation ownership

- Activities: `DuaActivity.java`, `HadithActivity.java`, `CalendarActivity.java`, `ProfileActivity.java`, `AiActivity.java`.
- Local assistant logic: `assistant/OfflineAssistant.java` (incorrect route, neutral greeting/age assumptions only; do not add remote AI claims).
- Utility layouts: `activity_dua.xml`, `reading_item_dua.xml`, `activity_hadith.xml`, `hadith_item.xml`, `activity_calendar.xml`, `activity_profile.xml`, `activity_ai.xml`.
- Utility-specific resources only, if needed. Avoid `FeatureHeader.java`, shared values and shared components until parent assigns an owner.
- No DB migration, deletion, reseed, new cloud account, or API contract change needed for this batch.

## Relevant verification proposal

- JVM: existing calendar boundary/upcoming tests; add a pure `OfflineAssistantTest` that asserts Hifz routes to Diary, no fixed identity, blank/oversize rejection and safety/offline limits.
- Device: Hadith search empty/recovery and bookmark survival, Dua expand/search with no TTS, Calendar move/today restore and selected date recreation, local Profile no assumed name + academic subtotal, AI send/history restore and composer above keyboard; 200% font + small screen checks on representative utility layouts. Existing `ReferenceFlowTest` and `NativePolishTest` supply pieces but should not be quoted as current passes before running.
- Screens: at least Profile, AI, Dua expanded and Calendar with native APK at normal text scale; one large-font screenshot showing principal controls remain reachable. Physical-device voice behavior and performance remain unverified by emulator.
