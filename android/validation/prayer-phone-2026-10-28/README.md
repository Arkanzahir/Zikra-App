# D01/D02/D02a — physical phone follow-up, beta1.9.22

28 October2026, POCO F7/API36. This checkpoint preserves the successful morning
tests rather than requiring the user to repeat them. The source-label change
first tested on APK1.9.19/code46 changes
the displayed source wording to **Kemenag** as requested. The actual transport
remains the public myQuran v3 timetable declaring Kemenag as its source; technical
provenance and [independent source comparison](../prayer-source-2026-10-28/README.md)
remain explicit in code/evidence. This is not a claim of direct authenticated
Kemenag API access. Monthly data and solar-fallback behavior are unchanged.
Beta1.9.21/code48 improves audio focus and the regular audio onset; the final
1.9.22/code49 uses the user's later replacement regular recording.

## Physical results

- Morning1.9.15: a real AlarmManager exact alarm scheduled30seconds ahead fired
  while the display was off and keyguard locked. It started the automatic
  foreground player303ms after the scheduled epoch, displayed a Stop action and
  stopped after the user's notification action. The user confirmed hearing it
  with the display off. [Native proof](morning-alarm-check.json),
  [morning preservation](morning-preservation.json). The trigger was a synthetic
  test epoch, not a change to the published prayer timetable. USB charging and
  instrumentation were present; this is not an overnight/vendor-Doze test.
- The morning user check also confirmed both then-selected Makkah/Subuh previews
  were audible and Stop worked. That regular Makkah recording is historical;
  the current selection is Alafasy and is evaluated separately below.
-1.9.19 was installed over the phone's1.9.15 with `install -r`, without removing
  the main app. Internal data and the old installed APK were backed up privately
  first. The user confirmed Sukolilo/Kota Surabaya. Only the generic display
  name was replaced by that confirmed name; coordinates, zone, fix timestamp,
  accuracy and origin were retained. The Surabaya region was explicitly chosen
  and the current/next months downloaded. [Region/source check](region-check.json).
- All **20 combinations** of five prayer slots and four modes passed physical
  native checks. Future plan epochs agree with the shared timetable resolver,
  including the next month. OFF removes the slot; the master switch removes the
  plan and prevents a notice. Ordinary notification and vibration modes did not
  start adzan; the vibration channel has no sound. This checks channel behavior,
  not a user's subjective confirmation of feeling vibration.
  [Mode results](modes-check.json).
  These20cases were run on1.9.21. A final1.9.22 private phone snapshot independently
  checked all30persisted future alarm epochs/signatures against the cached
  Surabaya city table after the user's GPS refresh. [Persisted plan](persisted-plan.json).

## Playback and volume

The phone exposed a usability problem: incoming notification sounds could request
`AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK`, which the old player treated like exclusive
focus loss and stopped the whole recording.1.9.20 changes only this case to
20% player gain while playback continues; focus gain restores100%. Other
transient/permanent focus losses still stop, and later gain does not restart
stopped audio. DND, ringer, alarm volume, permissions and quiet-class rules remain
respected. Phone tests use actual Android focus requests, not direct invocation
of the player's focus listener.

On1.9.20 both selected recordings completed from beginning to end on the phone:
regular157,237ms and Subuh187,272ms. [Full-play proof](full-play-1.9.20.json).
These results are attributed to1.9.20, rather than relabelled as a new full
playback on1.9.21.

The user then reported an abrupt loud onset specifically on the regular audio.
Local decoding of the original MP3 confirmed differing level dynamics; no speed
change is configured in the player.1.9.21 adds a600ms linear VolumeShaper entrance
only for non-Subuh playback. It multiplies independently of notification ducking,
so focus gain cannot skip the entrance curve. It never edits/normalizes the MP3,
changes system volume, or alters the Subuh start. The source recording's other
dynamic/encoding characteristics remain; this is not audio remastering.

[Physical focus/entrance results](focus-check-1.9.21.json) sampled actual
VolumeShaper gain0→1, then verified continuing position during MAY_DUCK, stronger
loss stopping playback and no restart on focus gain. The user still found the
regular recording's onset unsuitable during a separate ordinary30second preview
on1.9.21. Passing engine checks was not treated as subjective audio acceptance.

The user then supplied **Adzan-Misyari-Rasyid.mp3** as the replacement. Final
1.9.22 includes that exact3,713,152byte file, not a normalized/retimed derivative;
Subuh is unchanged. Full local decoding succeeds, about232seconds, below the
five-minute safety bound. The user confirmed the new ordinary30second preview
was **more stable**. [Preview/user confirmation](listening-1.9.22.json).
The final phone check sought near the end of each recording and verified normal
completion/cleanup; Subuh has no entrance shaper. [End check](completion-1.9.22.json).
This is **not** a claim that the new232second recording was played fully from
beginning to end on the phone; the full pair proof above belongs to1.9.20 and
its older regular recording.

## Audio and source

The final selected regular recording was fetched from the publisher's linked
public URL and matches the user's file byte-for-byte:3,713,152bytes/SHA256
`545da9c890916ce918ed0e82df9b5e37169aafd638cdb3dfe61d41caa39b016f`.
[Source match](selected-audio-source.json). The Subuh source match is retained
in the [previous audio audit](../adhan-source-d02-2026-10-27/README.md).
The [previous Alafasy2match](previous-alafasy-source.json) is historical.

The publisher labels the final recording as Mishari Rasyid and permits free
downloads. This check has not established a separate license for bundling the
recordings in public app distribution. File identity does not establish every
lafaz, performer identity or ownership. MP3s remain private debug-build inputs,
not repository assets. No third-party message or permission request was sent.

## Earlier attempts and scope

The first two mode attempts ran before Application.onCreate completed, so they
resolved the solar fallback before the application installed the timetable
provider. A diagnostic confirmed provider absent before the main-thread barrier
and present after it. Waiting for application startup in the helper resolved
the discrepancy; the same APK then passed all20cases. No fixed minute offset
or weakened equality assertion was introduced. This was a test-startup race,
not evidence of a one-minute production timetable correction.

The first natural-completion attempt stopped after about42seconds on notification
focus loss. On the second attempt, the regular recording finished but Subuh
stopped on another notification at about59seconds. These are not two successful
pair runs. Private Android audio-event evidence identified notification usage
and MAY_DUCK requests, motivating the1.9.20 correction; no unrelated package
identities or notification contents are published. Its subsequent full pair run
passed. A1.9.21 helper's initial800ms wall-clock expectation for full soft-start
gain was too early for the device's buffered audio timeline; the final helper
samples the real VolumeShaper gain until completion with a bounded wait instead.
An additional1.9.21 end-check invocation timed out on an existing-activity
startup wait; opening Splash allowed its assertions to finish. The helper now
opens Home without waiting for a new Activity instance when an existing task
can be reused. The final1.9.22 end check returned normally and passed; the old
host timeout is not counted as a successful runner execution.

[APK/source receipt](release.json). Source checkpoint`c94b020807d0b293729dd678146c5f9fa8384998`.

## Package and limits

[Package audit](package-audit.json):43,616,987bytes, below100,000,000;
SHA256`935a155168899e4d9dacb18d95101e720cea256830b5f4dea1fb3fd2954da066`.
Build/helper/lint succeeded;153unit tests passed/4skipped, lint0errors/818warnings.
Known current private
values were absent from decompressed APK contents. Both bundled debug audio
entries match the user-selected files. Protected WIP, stash/archive, private
configuration, approved logo/Home art and AI implementation retain their hashes.
Previous1.9.18emulator results remain separately attributed; no new emulator or
overnight phone delivery result is claimed.

[Preservation](preservation.json) checked25original internal files and17database
tables. Original content is retained; the chat's access timestamp changed while
its messages/draft/title remained unchanged. The user confirmed refreshing GPS
and granting location permissions during the session; those newer choices are
kept, rather than restored to the old fix. Other original preferences, durable
files, system settings and other permission grants are retained. No whole
database was restored over newer data. The helper/own test files were removed
and Zikra reopened; the main app was never uninstalled. Raw backups remain private.

## UI observations retained for follow-up

The user noticed faint night lamps, Home-only theme selection and the absence
of bottom navigation in AI. These observations are recorded, not claimed fixed
by this audio checkpoint. After Isya the current Home strip follows tomorrow's
next-prayer date: Surabaya28October Maghrib17:26 and29October17:27. The actual
phone cache contains both values. The missing visible date context should be
made clearer; no arbitrary minute correction was applied.

**D02/D02a remain open** for source/distribution terms and the remaining physical
vendor/reboot/Doze/interaction cases. D01's qualified beta result remains intact.
A–D and public release are not collectively complete; B01/C02/C04/C07 retain
their own gates. Private backups, raw phone logs/screens, GPS, databases,
credentials, APKs and MP3s are excluded from Git.
