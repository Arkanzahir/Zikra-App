# D01 supplemental — Kemenag timetable through myQuran, beta 1.9.18

28 October 2026. The user selected Kemenag as the Indonesia timetable reference.
This checkpoint uses the **public myQuran v3 API**, whose metadata declares
Kemenag Bimas Islam as its source. It is not a claim of direct authenticated
Kemenag API access. [Provider metadata and independent comparison](source-comparison.json),
[package/unit/lint audit](audit.json), [APK/source receipt](release.json),
[native results](native-results.json), [upgrade](upgrade.json), [isolation](preservation.json).

## Result

- `PrayerSchedule` resolves a complete cached city timetable first. Home,
  countdown, daily/weekly prayer screens, the seven-day alarm plan and opted-in
  AI context share this resolver. Imported times are used exactly, with **no
  second +2-minute adjustment**. Solar illustration keeps its own astronomical
  calculation; the sky is not shifted to the timetable's rounded sunrise.
- A bundled public catalog supports region selection without a network request.
  Initial Sukolilo/Surabaya resolves unambiguously to KOTA SURABAYA. Ambiguous
  city/kabupaten names and unnamed device fixes are not guessed by proximity;
  the source dialog permits an explicit searchable region choice. Choices are
  retained per location. Different province time zones are rejected. Location
  coordinates, Qibla, weather and permission settings are not changed.
- Foreground entry/location changes and a persisted, network-constrained
  twelve-hour Android job refresh the current and next month. Successful tables
  are refreshed at most once daily unless the user requests an update. Failed
  automatic attempts are throttled for fifteen minutes in the running process.
  Periodic jobs are best effort and may be deferred by Android; exact alarm
  delivery itself does not wait for the network or refresh job.
- Whole-month date coverage, region ID/name, strict times, daily order, response
  size and province/time-zone compatibility are validated before an atomic file
  replacement. Failed downloads preserve previous valid tables. Missing or
  invalid dates use the existing **clearly labelled local fallback**, including
  its original rounding/+2-minute policy. Cached future dates remain usable
  offline across process restarts and month boundaries. Public cached months
  are bounded to 48 files.
- Source, region, coverage and update time can be inspected from Home or the
  prayer screen. Dialog content/actions scroll together at large font sizes.
  Updating a timetable invalidates stale alarm intents. A prayer already
  delivered on the same selected local date is not replayed just because its
  minute changes.
- HTTPS verifies the normal certificate/hostname, redirects are rejected and
  responses are bounded. Only the public region ID and month are requested;
  no GPS coordinates, account credentials or personal records are uploaded.

## Source comparison

`tools/verify_prayer_provider.mjs` reads the same public `/api/sihat/` endpoints
used by the [official Bimas Islam page](https://bimasislam.kemenag.go.id/jadwal-sholat).
Their opaque region IDs are obtained from the public province/city selectors,
not manufactured or decoded. No login, API key or third-party message was used.

For **Surabaya, Makassar and Jayapura in October and November 2026**, all
**1,098 compared events** (Subuh, Terbit, Zuhur, Asar, Maghrib, Isya) match exactly.
These are six city-month samples, not certification of every provider region,
date, calculation method or future response.

On 28 October, the compared Surabaya table is **03:45 / 05:00 / 11:16 / 14:28 /
17:26 / 18:37 WIB**. NU Online's Maghrib 17:23 is a different published method;
the app does not subtract minutes to imitate that number or label NU data as
Kemenag. Public provider fixtures used by unit tests are saved under
`android/app/src/test/resources/prayer/`, not bundled monthly tables in the APK.

## Verification

**153 unit tests passed, 4 skipped; lint 0 errors / 818 warnings.**
**10 final native executions passed** in the task-owned API37.1 emulator:

1. Original 1.9.17 APK installed, populated synthetic fixtures seeded, then
   upgraded with `install -r` to 1.9.18. Five databases, old row/preference values,
   durable files and dummy encrypted credential access survived 12 activity
   openings and a process restart. No real phone records were involved.
2. The Android HTTP client contacted the real provider, downloaded current and
   next month, and returned exactly the provider's times without an adjustment.
3. Wi-Fi/mobile data were disabled and the process restarted. Native code
   confirmed **no active network**. At simulated local 17:25 on 28 October,
   Home showed Maghrib17:26 and countdown00:01:00, matching the alarm plan.
   October31→November1 resolved the cached next-month Subuh.
4. Unknown device-region names and a corrupt uncached month retained labelled
   local fallback without damaging the valid October table. The persisted
   refresh job was registered. Unit tests also reject wrong IDs/months,
   missing dates, invalid times, ordering and incompatible time zones.
5. A previously constructed intent became invalid when its timetable signature
   changed. Same-day delivery was suppressed even when the minute differed;
   the next date was not suppressed. This is scheduling-policy verification,
   not a new real speaker/Doze/boot delivery test.
6. Normal and 200%/320dp Home/source dialog were checked. Every source action
   could be scrolled fully into view; a real injected touch on Close dismissed
   the dialog. [Home](native/home-offline.png), [source](native/source-offline.png),
   [large font actions](native/large-source-offline.png).

The helper was removed, fixture keystore restored and emulator stopped. The
candidate is a private debug beta below100,000,000bytes, using the existing
debug signer and the selected private Subuh/Alafasy assets. MP3s, private env,
user backups and APK binaries are not committed.

## Earlier attempts and remaining limits

[Earlier attempts](earlier-attempts.json) are not counted as final passes.
The emulator initially had an unready telephony/DNS service; a direct API
diagnostic exposed `UnknownHostException`. The host runner was corrected to
wait for DNS readiness. Review also found a forced update could be lost behind
an active automatic request, so forced requests are coalesced and callbacks
wait for completion. No TLS verification was disabled.

The first large-font dialog screenshot showed clipped actions despite a
simplistic earlier test passing. [Preserved failed appearance](earlier/large-source-clipped.png).
The dialog was changed and the final test asserts full action visibility and
actual dismissal. Repeated directory pulls initially selected stale host-side
screenshots; final evidence uses the newly captured files, with hashes in audit.

Physical-phone delivery, long idle/battery/vendor behavior and live GPS region
recognition on this new build remain untested. A region timetable is not a
claim of point-specific elevation correction. Unknown/ambiguous names may need
manual region selection; new provider regions require a catalog update.
If both downloaded months are exhausted offline, the explicit local fallback
is used. No full D02/D02a/F/G, B01, C or public-release gate is closed by this
supplement. Approved artwork, AI design, PROJECT_HANDOFF WIP, audio stash and
private configuration remain protected.
