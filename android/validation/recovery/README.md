# Verifikasi pemulihan Study/Hifz — 18 Oktober 2026

Toolchain: Temurin JDK 17, Gradle wrapper 8.9, AGP 8.7.3, compile/target SDK 35, min 26. Device: AOSP API 28, x86_64 software rendering, emulator-5558, 540×1170/210dpi. Bukan hasil HP fisik.

- **Build APK, APK tes dan lint:** lulus. [Log build](build.txt). Percobaan awal menemukan lima `MissingInflatedId` pada listener dock Home lama; listener duplikat dihapus karena komponen `NavDock` sudah menangani klik. Hasil akhir tidak memiliki error lint; peringatan nonfatal masih ada.
- **12 tes instrumentasi terarah:** lulus, tidak ada skip/failure. [Log perangkat](device-tests.txt). Tiga tes baru mencakup migrasi v1, persistensi/rentang/status dan batas tanggal; lima tes SQLite baseline; empat tes Study/Hifz mencakup katalog live 114, respons invalid, validasi/draft/rekreasi/batal dikte, simpan kegiatan, kartu target, centang, Intent eksternal dan reader lokal mulai ayat 2.
- **Setup tersimpan dijalankan ulang:** lulus, termasuk build incremental sebagai pemeriksaan idempotensi setup. [Log](setup-repeatability.txt). [Startup emulator](startup.txt) diuji terhadap AVD yang sudah boot; cold boot asli dilakukan pada instance ini sebelum tes (sekitar 221 detik).

Perintah utama dari root repo:

```bash
source /workspace/setup-zikra/env.sh
./android/gradlew -p android assembleDebug assembleDebugAndroidTest lintDebug --console=plain
adb -s emulator-5558 install -r android/app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5558 install -r android/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5558 shell am instrument -w -r -e class 'com.zikra.app.data.HifzRecoveryStorageTest,com.zikra.app.data.ZikraDbHelperTest,com.zikra.app.StudyRecoveryTest' com.zikra.app.test/android.test.InstrumentationTestRunner
```

Tes mengambil PNG fisik dengan `screencap` setelah frame siap, lalu menghapus target contoh tes dan mengembalikan bookmark sebelumnya. Semua lima PNG diperiksa secara visual. Pemeriksaan read-only SQLite setelah suite: `quick_check=ok`, versi schema 2, nol baris sementara berprefiks `ZZ_RECOVERY_`. Master logo tetap ber-SHA256 `7fa6744a626bbb27e1d944c18090730a64d37658a0e8f5aaea5212f14d52d963`, sama dengan baseline. [Preview HTML mandiri](preview.html) memuat gambar sebagai data URI sehingga tidak bergantung pada path gambar relatif; [PDF](preview.pdf) dapat dibuka terpisah.

| Layar | PNG asli |
| --- | --- |
| Study Hub | [study.png](screenshots/study.png) |
| Editor kegiatan | [study-editor.png](screenshots/study-editor.png) |
| Diary Hifz | [hifz.png](screenshots/hifz.png) |
| Editor target surah/ayat | [hifz-editor.png](screenshots/hifz-editor.png) |
| Reader empat surah bawaan, dimulai ayat target | [local-reading.png](screenshots/local-reading.png) |

**Batas bukti:** katalog metadata live berhasil dari APK melalui proxy/CA emulator; tidak ada download/persistensi teks 114 surah. Intent Quran.com diperiksa URL-nya, browser eksternal tidak diuji end-to-end. Dikte dites dengan hasil Intent simulasi dan hasil batal, bukan rekaman/pengenalan suara nyata. Cold boot menampilkan ANR **System UI**; proses System UI dimulai ulang sebelum suite, lalu 12 tes selesai. Animasi sistem dimatikan selama tes untuk kestabilan emulator dan `animator_duration_scale` dikembalikan ke 1 sesudahnya. Belum ada pengukuran FPS, baterai, IME fisik, sensor, audio, alarm atau regresi seluruh aplikasi/API 35.

Log/screenshot historis di folder lain berasal dari commit/sesi lama. Jangan memakai hasil tersebut sebagai bukti pemulihan fitur yang tidak ada di kode ini. Lihat [inventaris pemulihan](../../../RECOVERY.md).
