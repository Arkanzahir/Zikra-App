# Feature screen design refresh — 19 October 2026

Scope: Study Hub, Diary Hifz, the offline companion, and the Quran reader. Home,
onboarding, database contents, prayer calculations, location and audio services
are unchanged.

## Changes

- Study uses a shorter heading and a compact light task card. Hifz retains a pine
  practice card with the chapter's bundled Arabic name and a clearly labelled
  target-completion summary. Empty plans do not show a meaningless 0% bar.
- Featured task controls stack for narrow screens or larger system text.
- The companion uses pine/sage throughout. The two entry actions are smaller,
  the voice indicator is compact, and the introduction disappears after the
  first message. The introduction and topic shortcuts are also removed from the
  keyboard viewport. Local history, drafts, voice fallback and Study navigation
  remain available.
- Quran verses have one overflow control containing Play, Mark last read,
  Copy and Share. Last-read text remains visible. Quran content, pagination,
  theme, reading sizes, range playback and downloads are unchanged.
- Existing reader bookmark regression coverage now exercises the visible menu.
  Existing assistant coverage checks that the introduction stays hidden after
  sending and recreation; the enlarged-text test isolates/restores its history.

## Validation and limits

- All Android app resources compiled and linked successfully using the installed
  Android `aapt2` and API 35 framework resources.
- Java 17 syntax parsing passed for all 112 production and test source files.
- `git diff --check` passed.
- Full Gradle build, lint, JVM tests and device instrumentation were **not run**.
  This machine lacks the pinned Gradle 8.9 and exact project dependencies, and
  shell network access is unavailable. Source files were retrieved through the
  connected GitHub tool. Syntax/resource checks do not prove Java type checking
  or runtime behavior.
- The accompanying HTML preview uses sample data and mirrors the proposed
  hierarchy; it is not an APK screenshot or a substitute for native review.

Before merging, run from `android/`:

```sh
./gradlew assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug
```

Then run the affected `StudyRecoveryTest`, `UtilitiesPolishTest`,
`NativePolishTest`, `RebuiltQuranTest` and `QuranAudioTest` paths on a test device.
Check normal and 200% text, 320/360dp widths, empty/populated plans, editor
recreation, keyboard/composer visibility, verse menu bookmarking, playback,
night mode, and scrolling. Use test data rather than altering a user's plans.

