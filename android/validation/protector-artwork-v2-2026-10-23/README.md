# Protector artwork correction — 23 October 2026

APK version 1.5.1 (13). Three newly generated assets replace the close-up city and repeated cloud cutouts. Composition follows the user's sunny/rainy reference: distant skyline around 75% of hero height, foreground trees, continuous storm sky, fine down-left animated rain. Sunny clouds and the storm field drift independently of the stationary city. Rainy foliage is desaturated and cooled; existing night tint and approximate moon phase remain.

## Evidence

- `ProtectorSunnyV2.png`, `ProtectorRainV2.png`: actual native Android screenshots at 720 × 1600, density 320, from this APK. Weather values are explicitly marked visual examples, not observations. Map/report loading in these images is not a new live-data verification.
- `ProtectorRainMotionV2.png`: later frame of the same rainy scene.
- `device.txt`: one targeted instrumentation test passed. It captures both weather states and asserts that the animation clock advances while active and stops when paused.
- `build.txt`: offline build, test APK and lint succeeded, one worker, 512 MB Gradle heap. Lint: zero errors, 682 warnings. No full regression suite repeated for an artwork correction.
- Crash buffer empty. One emulator used (1536 MB, two cores), then shut down. No physical phone tested.

APK: `D:/Download/Zikra-App-main/Zikra-Protector-v1.5.1-debug.apk`, 18,426,321 bytes.

SHA-256: `53d641d08fb1f4a8669026ccb1587b5da6723d7d776fa6398298fcf5ee153a39`.

Built from the existing isolated release tree `.local/concept-release-v130/android`, with these seven changed files copied from source: app/build.gradle, ProtectorWeatherView.java, ProtectorReleaseTest.java, protector-weather.txt, and the three weather PNG resources. Unrelated in-progress Quran audio edits in the main working tree were excluded. Source/build file hashes verified equal.

Source artwork and complete built-in imagegen prompts: `design/assets/protector-weather-v2/`. PNG pixels preserved unchanged. Android decodes the three images at half resolution (about 4.5 MiB total bitmap pixels). No image generation at runtime.

The scene is much closer in skyline scale and placement; it is a newly generated illustration, not a pixel-identical crop of the concept. Renderer and actual UI remain separate. The illustration is decorative, not a geographical photograph of the selected city.
