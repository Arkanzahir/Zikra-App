# Zikra shared interface / Home audit

Research snapshot: 19 October 2026, Asia/Jakarta. Baseline `9582592`; read-only source audit. No Gradle, adb or app-source edits during this phase.

## Primary references, fetched with inherited proxy and verified TLS

All returned HTTP **200**. Local source snapshots: `/workspace/research-foundation-sources/`.

- Android accessibility principles: https://developer.android.com/guide/topics/ui/accessibility/principles — clear purpose, controls with useful labels, distinguish decorative images from interactive controls.
- Android accessibility implementation: https://developer.android.com/guide/topics/ui/accessibility/apps — interactive focusable/touch area at least **48 × 48 dp**; enough text/background contrast. Applies directly to existing Views/XML; no Compose rewrite required.
- Android 14 font scaling: https://developer.android.com/about/versions/14/features#non-linear-font-scaling — support up to **200%** font size, use `sp`, avoid fixed heights that assume text size.
- WCAG 2.2 contrast understanding: https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html — normal text **4.5:1**, large text **3:1**. Treat as practical measurement guidance, not a blanket compliance claim.
- WCAG resize text: https://www.w3.org/WAI/WCAG22/Understanding/resize-text.html — 200% text growth without losing content/actions.
- Material 3 navigation guidance: https://m3.material.io/components/navigation-bar/guidelines — page reachable 200, client-rendered page; do not infer detailed recommendations from the shell alone. Existing five top-level destinations can remain; priority is clear selected state and legible labels.

## Concrete findings

Paths below are relative to `android/app/src/main/` unless stated otherwise.

1. **Incorrect Home destination after Study became academic-only.** `res/layout/activity_home.xml:638` says “Buka Checklist Ibadah”, while `java/com/zikra/app/HomeActivity.java:116` opens unscoped academic Study. Home heading also mixes “Ibadah & Belajar”, `HomeActivity.java:162` counts all categories. Label, scope and destination must agree. Route the Home ibadah summary to `StudyActivity` with `CATEGORY_IBADAH`; academic work already has a dedicated shortcut/dock destination.
2. **Small active targets.** `activity_home.xml:70–86`: `homeSkySource` has only 24 dp minimum height, `homeHijri` fixed 32 dp (40 dp on tall screens, `values/home_dimensions.xml:8`, `values-h740dp/home_dimensions.xml:8`). They open dialogs/calendar and should each be at least 48 dp with no overlapping hit boxes. Existing header icons already satisfy 48 dp; shared header `view_feature_header.xml:5,15` is a good basis to reuse.
3. **Muted contrast and CTA.** `values/colors.xml:10` `#7C8B8A` measures **3.55:1 on white**, **3.28:1 on #F4F6F8**. Used for small Home progress/last-reading labels (`activity_home.xml:623,682`). `home_cta.xml:3` uses amber `#CF9447`, with white text (`activity_home.xml:642`): **2.63:1**. Suggested muted `#63766D` gives **4.84:1 on white**; Pine/white **13.58:1**. Compute actual background pairs for other feature surfaces rather than blindly replacing every color.
4. **Too much tiny/fixed typography for broad age groups.** Home shortcut labels 9 sp (`activity_home.xml:356ff`), prayer names 8 sp (`:140ff`), progress 10 sp (`:624`), dock overridden to 10 sp (`NavDock.java:103`), header subtitle 11 sp (`view_feature_header.xml:13`). Raise practical secondary text to 12–14 sp with expandable rows. Fixed 24 dp countdown/progress and fixed 62/68 dp shortcut rows risk clipping at 200%; let text-bearing rows wrap with minimum heights. Keep large clock but verify fit on narrow screens.
5. **Shared selected-state discoverability.** `NavDock.java:98` correctly uses `setSelected`; children marked decorative prevent duplicate focus. Active destination is only color difference (`:135–140`), no icon/label shape/weight difference. Add a modest active indicator or bold label and a native accessibility role/selected state; do not add a floating pill or new chrome. Main content currently reserves fixed 64/80 dp while dock is fixed64 (`NavDock.java:28,122`): if large-font dock grows, all docked screens need one shared measured inset to prevent overlap.
6. **Base-level assumptions.** `BaseActivity.java:70–71` opts all users into shake shortcut by default; dialog uses hard-coded ITS destination (`:116–129`). Not an emergency service; the UI should explain when setup is needed, and should not imply personalized family/location routing. Keep dialer/chooser intent behavior; no automatic messaging. Parent should coordinate with settings/prayer agent before changing behavior.
7. **Research docs contradict newest decisions.** `design/README.md:1` correctly says static illustrations; `:11–15` still says orbit/parallax, role/minat selection. Parent owns current onboarding/branding docs and should retire conflicting active instructions without deleting historical evidence.

## Proposed implementation priorities (five bounded changes)

1. Make Home's main checklist card **Ibadah today** with category-scoped count and correct scoped destination; leave academic shortcut clearly separate. Improve “continue reading” row minimum height and text hierarchy.
2. Standardize accessible foundation: Pine primary action, dark-muted supporting text, consistent 48 dp actions, shared header subtitle13sp, dock labels12sp plus non-color selected marker. Preserve Java/XML and current destinations.
3. Home typography/layout: secondary12sp where practical, prayer names10–11sp, flexible heights for200% text, maintain established Pine skyline and flat shortcut grid (do not add generic gradients/decorative hero cards).
4. Keep Home ambient motion because user previously asked for it, retaining existing `AmbientMotionPolicy` pause/reduced-motion/battery handling. Onboarding remains entirely static as latest user requested.
5. Add a small, meaningful foundation regression test: 320 dp width at 200% text; main CTA and dock fully visible and ≥48dp; no dock/content overlap; Home CTA opens Ibadah, not academics; selected destination exposed. Existing relevant tests already cover Home sky pause/reduced motion (`RebuiltHomeTest`), keyboard/dock separation (`NativePolishTest`), but **only onboarding tests exercise larger text and only at130%** (`StaticOnboardingTest:102`).

## Visual review

Inspected `android/validation/static-onboarding/screens/StaticOnboarding1.png`, `android/validation/rebuilt/screens/RebuiltHomeLive.png` and supplied `design/references/home-target-suhayel.png`. Onboarding indeed has a rectangular art crop separated by a large blank header gap; parent is handling seamless full-width composition and removing only wordmark dot. Home has a useful stable hierarchy and genuine data; improve legibility and card purpose rather than redesigning it into an unrelated aesthetic. Master crescent + book + spark remains untouched.

## Verification boundaries

This is research + static audit, not a claim that current APK is accessible or every feature works. Proposed implementation needs focused JVM/device checks plus actual screenshots. Software emulator cannot establish physical-device smoothness, battery performance, real compass accuracy, or production API access.
