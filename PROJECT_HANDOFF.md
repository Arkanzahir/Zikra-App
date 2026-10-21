# Handoff Zikra — 21 Oktober 2026 (WIB)

## Terbaru: Mushaf online dan audio qari asli

Commit implementasi `83f520c65849c1c10feb20913c18a4e81419145c` berhasil dipush ke
`origin/main`; hash remote diverifikasi sama setelah push. Catatan ini disimpan
dalam commit dokumentasi berikutnya. Tidak ada force push atau kredensial dalam Git.

[Laporan/preview terbaru](android/validation/mushaf-reader-2026-10-21/README.md).
Mode Mushaf QCF V2 / mushaf 1 sudah dapat dibuka dari reader Aa → Baca Mushaf.
Native glyph/baris/nomor inline, RTL, lompat halaman/surah/juz, simpan posisi dan
perpindahan ayat ke Terjemahan, zoom/malam serta player lama terhubung.
**Mushaf sementara Android 12+/API 31+**, karena Canvas.drawGlyphs; entry disembunyikan
pada API 26–30. Minimum aplikasi/reader lama tetap 26. Tidak ada fallback glyph palsu.

Halaman tetap online public v4 (sumber reader lama); JSON baru tidak dipersistenkan.
Font CDN resmi Foundation dicache maksimal delapan file/refresh tujuh hari, tanpa
modifikasi/bundle terpisah. Akun developer aktif pengguna dan atribusi di app memenuhi
prasyarat font yang diperiksa. Rahasia Production tetap .local, tidak di APK/Git.
Gateway terautentikasi/backend dan Content Sync belum diintegrasikan.

Build/APK tes/lint dan 78 JVM lulus. Lima metode Android unik lulus di AVD uji baru
API 37: tiga Mushaf dan dua audio asli. Streaming Alafasy/Husary/Minshawi, seek,
background/layar mati, unduhan lengkap Al-Fatihah/Alafasy, repair rusak/parsial dan
repeat/range dengan jaringan mati kini terbukti di emulator. Tidak ada penilaian
pelafalan manusia/HP/headset/telepon. Dua run streaming yang dihentikan adalah
kegagalan helper, bukan hasil lulus; log dan perbaikannya tersimpan.

Screenshot normal/Al-Mulk/604/malam/zoom/teks sistem 200% adalah APK native,
bukan konsep. Belum audit visual semua 604 halaman. Sorotan kata emas masih belum
terhubung; hanya penanda ayat sage. WordTimeline tetap fondasi untuk adapter berikutnya.
Jangan menganggap screenshot menunjukkan sinkron kata atau menyimpulkan data timing
QF cocok dengan CDN lama hanya karena nama qari sama.

Berikutnya: pairing recording/timing (termasuk PCM/offset bila hash berbeda), adapter
player dan end-marker, lifecycle/recreation, uji HP/API 31/35, backend/Content Sync.
Semua bagian status di bawah adalah riwayat sebelum checkpoint Mushaf ini.

## Akses Foundation Production berhasil — 06:59 WIB

Pengguna kini memiliki app Developer Console API-access-only / Server dan telah
mengisi kredensial Production di `.local/quran-foundation.env` (Git-ignored).
Jangan membaca nilainya ke output/chat atau memasukkannya ke APK/commit.
[Bukti terbaru](android/validation/foundation-access-2026-10-21/README.md): token
200; chapters 200/114; Al-Mulk 200/30; mushaf 1 halaman 1 200/36 glyph dengan baris;
recitation 7 ayat 1:1 200/4 segmen. Script tools/Test-QuranFoundation.ps1 menyimpan
ringkasan tanpa token, secret atau respons mentah.

Pernyataan akun/akses belum ada pada bagian sebelumnya di bawah adalah riwayat,
bukan penghambat terkini. Akses Content Sync belum diuji. Lanjut audit page-layout
API dan font resmi, lalu loader/renderer/perpindahan mode/posisi serta backend
untuk menjaga secret di server. Jumlah words termasuk marker; timing kata bersuara
harus dipetakan dengan benar. Ini belum pembuktian UI Mushaf atau audio Android.

## Pembaruan fondasi Mushaf

[Laporan terbaru](android/validation/mushaf-foundation-2026-10-21/README.md):
model halaman, indeks ayat/halaman satu edisi dan lookup timing berdasarkan hash
recording/posisi player ditambahkan, dengan **17 tes baru; total 69 JVM lulus**,
APK aplikasi/tes dan lint lulus. Belum renderer/loader/adapter player atau layar
Mushaf yang dapat dibuka. Tidak ada preview Android baru. Fixture sintetis untuk
validasi struktur/logika, bukan data Quran/timing asli.

Gambar acuan tambahan disimpan dan ditautkan dari design/QURAN_READER_DESIGN.md.
Pengguna menunda tes HP dan mengonfirmasi belum memiliki akun Foundation.
Kandidat konkret QUL: layout 10 (V2 1421H/604 halaman), font 249, script 61.
Panduan font menyebut bundling native; izin paket/aset eksplisit, versi/hash dan
korpus penuh belum diverifikasi. Tidak ada data/font dari paket yang diimpor.
Lanjutkan izin/akses dan audit korpus, baru loader/renderer/persistensi dan
adapter player. Tes playback asli tetap belum selesai; jangan mengulang emulator
API 37 tanpa menyelesaikan hambatan yang dicatat di bawah.

## Proyek/acuan

Arkanzahir/Zikra-App; Android Java/XML, JDK 17, compile/target SDK 35, min 26.
prototype_zikra hanya referensi. Root berisi dokumen ini dan android/. ZIP kini
memiliki .git/origin, main mengikuti origin/main tanpa checkout/reset source.
Baca README.md, RECOVERY.md, android/API_PLAN.md, design/IMPROVEMENT_PLAN.md,
design/QURAN_READER_DESIGN.md dan android/VALIDATION.md.

## Implementasi/bukti

Source produksi tidak diubah dalam audit lokal ini. Onboarding/Home, lokasi,
jadwal/cuaca/Kiblat, OSM, Study/Hifz, fokus, Quran reader/panel/audio/unduhan tetap
implementasi sebelumnya. Bukti historis: android/validation/quran-reader-panel-2026-10-21/
(52 JVM/18 metode API 28 saat checkpoint lama; audio sintetis).

[Laporan sesi lokal](android/validation/local-qari-2026-10-21/README.md) dan
[ringkasan](android/validation/local-qari-2026-10-21/summary.json): build/tes APK,
52 JVM dan lint dijalankan lokal. Dua tes live baru compiled, **nol live lulus**.
Tidak ada screenshot/rekaman baru atau klaim suara didengarkan.
[API/MP3/hash/timing](android/validation/local-qari-2026-10-21/providers.json).

## Pekerjaan/hambatan

Prioritas audio asli belum selesai. LiveQariPlaybackTest menyiapkan streaming tiga
qari/kontrol/layar mati/pergantian surah dan unduhan-repair/offline/repeat/range.
Hanya emulator uji; radio dimatikan sementara, file/preferensi dipulihkan.
Jangan clear data pengguna. Suite belum terbukti runtime; periksa teardown/hasil.

Emulator API 37 mengalami WHPX/offline; C sempat penuh. TEMP/TMP di D memungkinkan
boot dan pemasangan APK, tetapi instrumentasi crash sebelum hasil metode, lalu
ADB hilang. Stack trace tidak diperoleh; akar crash belum diketahui. Setelah
emulator berakhir C kembali sekitar 9 GB bebas. Runtime API 35/HP belum diuji.

Berikutnya: emulator stabil API 35 dengan ruang disk cukup, ambil crash buffer
segera, jalankan suite; lalu interupsi jaringan saat stream/transfer, headset,
telepon dan bukti Android aktual. Jangan menyebut compiled sebagai lulus.

## Sumber, izin dan keputusan

[Audit sumber](android/validation/local-qari-2026-10-21/SOURCE_AUDIT.md): QCF V2
kandidat, edisi belum final. QUL layout 12 adalah Indopak 610 halaman. Verifikasi
izin/version/hash data+font/layout, halaman awal/tengah/akhir serta Al-Mulk.
Jalur resmi QF font/gambar memerlukan akun developer aktif; API/Content Sync
memerlukan akses server. Cache teks lama perlu audit/migrasi sesuai syarat terbaru,
tanpa menghapus data otomatis. Tidak ada backend/secret baru.

Timing Alafasy 1:1 tersedia, hash recording berbeda dari MP3 aplikasi. Kecocokan
PCM/timeline/korpus belum terbukti. Mushaf/sorotan kata belum diimplementasikan;
jangan menebak timestamp. AI generatif/perluasan Study tetap tahap lain.

Pertahankan Pine #15332F, master design/branding/zikra-logo-primary.jpg
(bulan+buku+spark), Home/onboarding dan reader/panel yang disepakati. Mushaf harus
berbaris berkesinambungan dengan nomor inline, bukan kartu per ayat. Pertahankan
preferensi, bookmark/posisi, Hifz, tugas, unduhan valid dan SQLite.

## Build/tes

Ikuti android/BUILDING.md. JAVA_HOME JDK 17 dan ANDROID_HOME melalui environment
lokal; jangan commit path komputer/local.properties. Dari android/:

```text
gradlew.bat assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e class com.zikra.app.LiveQariPlaybackTest com.zikra.app.test/android.test.InstrumentationTestRunner
```

tools/Test-QuranProviders.ps1 -OutputPath <json> -TemporaryRoot <folder> menguji
host HTTP/signature/hash, bukan playback Android. APK lokal ignored:
android/app/build/outputs/apk/debug/app-debug.apk. Laporan/log ada pada tautan di atas.

## Commit/push

Baseline main/origin/main setelah fetch: 9f97bd44d473df8f4686f129a7be5212de09cdda.
Pengguna mengizinkan commit/push main tanpa force. Checkpoint audit
`49aabbbff384becf6f1fd1e37929d120fa9b2d7e` berhasil dipush ke main; hash lokal
dan `git ls-remote origin refs/heads/main` sama setelah push. Pembaruan handoff
ini adalah commit dokumentasi penerusnya; lihat `git log -1 -- PROJECT_HANDOFF.md`
untuk hash penerus dan verifikasi remote lagi sebelum pekerjaan selanjutnya.
