# Zikra — audit Prayer, Qibla, Weather, Earthquake, Protector

Audit date: 19 October 2026 (Asia/Jakarta). Read-only repository inspection, starting HEAD `9582592` on `recovery/study-hifz-refresh`. No app/source modifications, no Gradle, no adb. Source HTTP downloads in `/workspace/research-safety-sources/`; status JSON preserves each requested and final URL. TLS verification and inherited proxy untouched. No new credentials or providers are necessary for the recommended first pass.

## Main conclusion

The practical value is already present: computed five-prayer timetable/reminders; true-north corrected compass; hourly model forecasts; official last earthquake report; safe external dial/share/maps intents. The largest improvement is to make origin, location and data state clear and readable, with resilient return/retry behavior. Current screens contain many 9–11sp labels and low contrast, and are all pinned to Sukolilo. Do not present these as a general-location service. Keep the original master logo and shared Pine header; feature details should not introduce a separate app-like navigation hierarchy.

## Reference images inspected

- `design/references/qibla-rayhan-raju.png`: Rayhan Raju, Islamic Prayer App UI. Relevant qualities: compact location line, large uncomplicated compass, clear calibration action; the prayer page uses a restrained next-prayer card and readable daily list. Do not copy example Dhaka location/148° bearing or imply mosque discovery from the static example.
- `design/references/bmkg-user-reference.png`: Naufal Fathichul, Info-BMKG Redesign. Relevant qualities: clear weather number, simple dated chart, a map plus grounded earthquake detail and a concise warning entry. Its blue cyan identity need not replace Zikra branding. In current Zikra this became another blue sub-app; bring navigation/header/buttons back to Pine while retaining weather icon colors and meaningful amber warnings.

## Priority actions, grounded in files

### P1 — Stop suggesting global/local certainty the app does not have

- **Current:** `PrayerSchedule` fixed `LATITUDE=-7.28`, `LONGITUDE=112.79`, `ZONE=Asia/Jakarta`. `QiblaActivity` fixed294°, declination at the same place; `activity_qibla.xml` says “Masjid sekitar” though its single mosque is fixed ITS. Weather uses the same coordinates, no location permission. `ProtectorActivity` maps always opens ITS, and WhatsApp draft says “Lokasi saya:” without a location.
- **Recommended this pass:** keep explicit “Acuan Sukolilo, Surabaya · WIB” or “Lokasi jadwal: Sukolilo” in all relevant places. Rename “Masjid sekitar” to “Masjid acuan” and label fixed ITS directions. Label WhatsApp “Tulis pesan bantuan”/draft with no implied attached location; keep ACTION_VIEW, do not send automatically. Emergency112 should open dialer, as it already does. If introducing a new location selector later, unify prayer, Qibla, sky, cache keys and timezone in one actual model; do not change only one screen.
- **Files:** `QiblaActivity.java`, `activity_qibla.xml`, `ProtectorActivity.java`, `activity_protector.xml`, `BaseActivity.java` (shared emergency wording), `activity_prayer.xml`.
- **Tests:** assert fixed-reference labels and actual maps query; denied/missing external handler gives existing Toast. Native UI for main values remains readable under enlarged text.

### P1 — Preserve valid results while refreshing, make retry an actual labelled action

- **Current:** `WeatherActivity.refresh()` and `ProtectorActivity.refresh()` clear all values/chart/map before every request. Weather cache quickly repopulates, but earthquake has no cache and loses its valid report on temporary failure. Both use header ↻ plus text “Ketuk ↻”; an explicit retry action would be easier to understand. Weather's stored/stale status displays “pembaruan belum tersedia” while a new request is still in flight.
- **Recommended:** first load has progress; subsequent refresh keeps already-rendered valid values with a lightweight “Memperbarui…” state. On failure with old data, retain it and show last successful retrieval plus “Pembaruan gagal · Coba lagi.” No-data errors must not use safe/clear sky/zero magnitude defaults. Weather forecast origin stays **Open-Meteo · Prakiraan model**, distinguish forecast valid time from retrieval time.
- **Files:** `WeatherActivity.java`, `ProtectorActivity.java`, `EarthquakeActivity.java`, `OpenMeteoClient.java`, layouts. Earthquake caching is optional only if saved/fetched timestamp and last-report status are obvious; no fake fresh warnings.
- **Tests:** fixture-first verified success→refresh failure retains previous dated values; first-load failure renders explicit retry; invalid/expired cache refused; overlapping/cancelled generation does not update paused/destroyed Activity.

### P1 — Earthquake foreground lifecycle and response limits

- **Current:** `EarthquakeActivity` calls `refresh()` only onCreate and closes only onDestroy. Leaving for Maps/browser does not cancel request; returning to an old instance never updates the last report. `BmkgClient` can parse an unbounded `body.string()` whereas weather deliberately caps262144bytes. `BmkgClient` already guards generation/finishing/destroyed and has25s call timeout.
- **Recommended:** align foreground lifecycle with Weather: onResume stale/initial refresh and onPause cancel/reset; retain last-rendered report while fetching. Apply a reasonable BMKG JSON body limit before string allocation. Keep parser coordinate validation, source event time in WIB, national last-report wording, disabled map action when no valid coordinates. Do not market autogempa as personalized warning or push alert.
- **Files:** `EarthquakeActivity.java`, `BmkgClient.java`.
- **Tests:** return-from-map triggers/permits refresh; paused Activity does not mutate; malformed and oversized payload fails cleanly; missing coordinates keeps map action disabled. Existing parser fixtures and `RebuiltWeatherTest` should assert actual report rather than sleep3500 then screenshot.

### P1 — Readability, touch targets and large text

- **Current:** essential date/source/weather/prayer footnotes9–11sp. Prayer mode container38dp including6dp padding →32dp clickable tabs. Fixed127dp prayer hero and82dp rise/set metric block risk clipping at fontScale1.3–2.0. Data rows/body labels often inline hardcoded colors. `#A1ADA4` on `#FCFDFB`≈2.28:1, `#9BA89E`≈2.42:1, `#718198` onwhite≈3.97:1, `#8492A6`onwhite≈3.16:1 (below4.5 for small copy). Existing bell/map/refresh targets mostly48dp; keep those.
- **Recommended:** core body14–16sp, supporting essential12–13sp, headings18–22sp, controls min48dp. Darken secondary ink to shared muted token that meets4.5. Use wrap_content plusminHeight for hero/content; fontScale testing should not shrink text. Make chart accessible via contentDescription summarizing range/dates and actual forecast list; map/compass have text alternatives. Avoid live-region announcements on every compass heading or countdown tick; announce state (alignment/low accuracy) sparingly.
- **Files:** `activity_prayer.xml`, `prayer_day.xml`, `prayer_item.xml`, `PrayerActivity.java` dynamic week text, `activity_qibla.xml`, `activity_weather.xml`, `weather_forecast_item.xml`, `activity_earthquake.xml`, `activity_protector.xml`, protector resource styles/colors, `TemperatureChartView.java`.
- **Tests:** native320dp width and fontScale1.3/2.0 screenshot/readability; controls visible and >=48dp; important source, coordinates, retry reachable by scroll; focusable icon controls have meaningful labels. No broad visual snapshot tests mirroring every margin.

### P2 — Consistent identity/navigation

- `FeatureHeader` already used for Protector/Weather/Earthquake and accessible48dp icons; use it in Prayer/Qibla too, keeping titles/back. Current `protector_navigation.xml` is a second blue bottom-navigation language inside Zikra, and labels10sp. It is dashboard-only; entering weather/quake removes it. Prefer one app navigation scheme or clearly contextual labelled links, not a second app brand.
- Keep useful content hierarchy: next prayer→daily list→reminder settings; compass→accuracy/calibration→fixed reference; weather→hourly timeline→source; earthquake→magnitude/eventtime/area→source/maps. Don't add decorative heroes above data people need.

### P2 — Reminders are structurally sound; test current Android before broader claims

- `PrayerReminders` already: five unique immutable PendingIntents; notification permission handling; channel and user-block checks; exact alarm capability check; safe inexact fallback and SecurityException handling; reschedule on boot/time/timezone/app upgrade; duplication guard; no premature notifications; stale delivery cutoff30min; foreground settings reflect permission state.
- Android14 fresh installs targeting33+ do not pregrant exact alarms. Existing fallback is correct; do not auto-request it on startup or switch to `USE_EXACT_ALARM` casually. Existing permission/result logic should be exercised on API35, not inferred from API28 instrumentation.
- Prayer method is explicit calculation (Subuh20°, Isya18°, Asr1x,2min ihtiyati), not official Kemenag feed. Keep accurate source wording, optionally make small details an expandable “Metode perhitungan” entry so ordinary users are not overwhelmed. `prayerSunset` currently displays Maghrib with2min precaution as “Terbenam”; physically sunset and prayer precaution are different. Label “Maghrib” here or calculate/return true sunset separately.
- Test deniedPOST_NOTIFICATIONS, permission revoked/channel disabled, exact denied→inexact, reboot/time change, delivery duplicates/future/stale, no physical-device performance or guaranteed alarm punctuality claims.

### P2 — Compass robustness and accessible states

- Good present behavior: true-north magnetic declination correction, display rotation remapping, circular smoothing,10Hz rendering bound,5° entry/8° exit hysteresis,lowaccuracy warning/haptic debouncing, missing sensor fallback, listenerremovedonPause. Keep this.
- Missing sensor fallback is honest and static. An accelerometer+magnetometer fallback could broaden compatibility later, but is scope expansion and would need physical-device calibration. First make no-sensor/low-accuracy/active/aligned clearly distinct and legible. Screen reader shouldn't receive rapid continuous status announcements (current polite live region changes≈350ms).
- `QiblaCompassView` allocates paths in draw; low-risk optimization optional if profiling shows concern, not priority over usability.

## Internet research — exact status and relevant facts

Network fetched via Python urllib default TLS/proxy,4workers maximum. Most requests succeeded once; some BMKG frontend requests subsequently403, which may reflect access/WAF behavior. This is evidence of endpoint availability during this run, not Android runtime proof.

| Source | Reachable result | Useful guidance |
|---|---|---|
| https://developer.android.com/guide/topics/ui/accessibility/apps |200|48×48dp touch targets, contrast and simple controls; API-specifiedsp and descriptions. Use4.5:1 for smallcopy.|
| https://developer.android.com/develop/ui/views/notifications/notification-permission |200→/develop/ui/compose/notifications/notification-permission|Android13+ runtimePOST_NOTIFICATIONS; platform behavior applies even though redirected docs examples are Compose. Do not migrate app from Java/XML.|
| https://developer.android.com/develop/background-work/services/alarms/schedule |200→/develop/background-work/services/alarms|Inexact alarms respectDoze; exact special access unavailable by default on current fresh installs; checkcanScheduleExactAlarms.|
| https://developer.android.com/develop/sensors-and-location/sensors/sensors_position |200|Sensor availability and magnetic-vs-true-north distinctions, rotationmatrix/orientation; physicalsensorvalidation needed.|
| https://data.bmkg.go.id/gempabumi/ |200|autogempa latest report,15events feeds,DateTimeUTC,Tanggal/JamWIB,coordinates latitude thenlongitude; attributionBMKG mandatory;60req/min/IP.|
| https://data.bmkg.go.id/prakiraan-cuaca/ |200|Officialforecastadm4,3days/3hour slots,twice-dailyupdates; mandatoryattribution60req/min/IP.|
| https://data.bmkg.go.id/DataMKG/TEWS/autogempa.json |200,410bytes|Actual national last-event payload. Parser fields/location order match docs.|
| https://api.bmkg.go.id/publik/prakiraan-cuaca?adm4=35.78.09.1001 |403|Don't swap working Open-Meteo weather to inaccessible official API or show mock weather as fallback.|
| https://www.bmkg.go.id/cuaca/peringatan-dini-cuaca.bmkg |First404, later403|Existing warning link may be outdated, but can't verify canonical replacement; frontend homepage and extensionless candidate also403. Keep accurate external-source entry/fallback to verified source page; do not claim replacement verified.|
| https://open-meteo.com/en/docs |200|Hourly timestamps and units match parser; current endpoint model conditions also available but not necessary to add another request. Data are model forecasts.|
| https://open-meteo.com/en/terms |200|FreeAPI noncommercial,less10000/day5000/hour600/min; CC-BY4.0. Review commercial terms if monetized later, not a blocker for current personal/noncommercial app.|
| https://api.open-meteo.com/v1/forecast?latitude=-7.28&longitude=112.79&hourly=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,wind_direction_10m&timezone=Asia%2FJakarta&forecast_days=3&wind_speed_unit=kmh |200,3149bytes|Existing productionURL works,72hourlyslots expected; no newkeyneeded.|
| https://gml.noaa.gov/grad/solcalc/calcdetails.html |200|Meeus-based solar calculation, physical refraction/weather caveats; NOAAcalculatorno longeractivelymaintained. Computed prayer schedule should retain method/local-reference qualification.|
| https://aladhan.com/prayer-times-api and https://aladhan.com/calculation-methods |200|Method selection matters; method20 isKemenag20°/18°. Not an official Indonesian published timetable source and no need to replace working offlinecalculation.|
| https://api.aladhan.com/v1/timings/19-10-2026?latitude=-7.28&longitude=112.79&method=20 |200|Reference comparative endpoint:03:47Fajr05:05sunrise11:14Dhuhr14:22Asr17:22Maghrib18:33Isha with no local2minihtiyati; methodparams20/18. Do not assert exact match without explicitly comparing calculation conventions.|

## Suggested implementation/test split

1. Parent foundations owner: sharedtypography,contrast,touch targets,Pineheader/backgroundandmainnavigation. Safetyfeatureowner scopes onlyitslayouts.
2. Safety owner: labeled fixed references, useful reliable refreshstate, Earthquakeforegroundlifecycle/bodybound, warninglink fallback, source/method affordances. No home/sky/global location edits.
3. One relevant verification round after source integrated: unitforecast/compass/prayerlogic + instrumentationactualno-data/stale/lifecycle/per-prayerreminders; screenshotdefaultandlargefont for Prayer,Qibla,Weather,Earthquake,Protector. API35permissionbehavior plus physicalcompass/Doze remain distinctfromAPI28 proof.
4. Publish screenshots/APK/StudioZIP with exacttested/pushedHEAD and explicit limitations. Do not use full-live provider request success as sole regression test.
