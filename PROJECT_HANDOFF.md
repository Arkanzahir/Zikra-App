# Handoff Zikra — 21 Oktober 2026 (WIB)

## Terbaru: proporsi dan jarak bacaan dirapikan

Reservasi judul dalam badan dihapus ketika panel luar sudah memuat judul itu.
Judul lebih kecil, panel antar-surah lebih tipis, celah antarkata dibatasi dan
baris pendek dipusatkan. Medali lebih bulat; halaman pembuka tidak dipaksa
merenggang memenuhi semua ruang. Huruf/ayat dan pembagian baris sumber tetap.
[Bukti native](android/validation/mushaf-spacing-2026-10-21/README.md).
Build/lint, 80 JVM, 4 metode Android lulus; satu diulang pada teks sistem 200%.
APK terbaru `D:/Download/Zikra-App-main/Zikra-Mushaf-Tidy-20261021-debug.apk`.

## Terbaru: judul surah pas kotak dan nomor ayat lebih jelas

Panel atas memakai “سُورَةُ + nama” dengan ukuran otomatis menurut lebar/tinggi
kotak aman. Awal surah berikutnya pada halaman sama memakai panel teal senada.
Medali angka kini teal kontras dengan latar terang; slot diperlebar berdasarkan
digit dan ukuran baris dihitung ulang. Batas tinggi font menjaga jarak harakat.
Urutan kata/ayat dan baris tetap dari sumber, tetapi jarak horizontal berubah.
[Bukti dan validasi](android/validation/mushaf-panels-2026-10-21/README.md).
Build/lint, 80 JVM dan 4 metode Android unik lulus; satu diulang pada font sistem
200%. APK terbaru `D:/Download/Zikra-App-main/Zikra-Mushaf-Panels-20261021-debug.apk`.

## Terbaru: bingkai teal memakai gambar pengguna

Pengguna mengizinkan langsung memakai gambar Pinterest yang diunggah. JPEG utuh
disimpan sebagai drawable-nodpi; renderer mengambil ornamen tepi/panel dan memakai
judul dari aplikasi di tengah panel atas, bukan kaligrafi bitmap. Teks/glyph tetap
terpisah. Margin aman dan warna kertas disesuaikan, mode malam memakai filter.
[Bukti native, asal gambar, hasil tes](android/validation/mushaf-teal-2026-10-21/README.md).
Build/lint dan 80 JVM lulus; 4 metode Android unik lulus, satu diulang pada teks
sistem 200%. APK terbaru `D:/Download/Zikra-App-main/Zikra-Mushaf-Teal-20261021-debug.apk`.
Batas resolusi JPEG, Android 12+/online dan sorotan kata belum aktif tetap dicatat.

## Terbaru: fokus bentuk ornamen referensi

Sudut bersulur/daun, finial floral pada judul dan medali angka bersudut emas pucat
berlapis. Native path, font tidak diubah. Slot/nomor ayat memakai metadata sumber;
tata letak baris tidak diganti. [Bukti dan temuan](android/validation/mushaf-ornaments-2026-10-21/README.md).
Pengujian medali tiga digit juga menemukan spasi U+0020 pada code_v2 halaman 49
yang tidak ada glyph fontnya; renderer kini memberi advance spasi dan melewati
penggambaran karakter pemisah tersebut. Huruf bacaan tetap utuh.
Build/lint dan 80 JVM lulus, empat metode Android final lulus termasuk halaman 49
dan playback asli. Log gagal awal/interupsi emulator disimpan terpisah. APK terbaru
`D:/Download/Zikra-App-main/Zikra-Mushaf-Ornaments-20261021-debug.apk`.

## Terbaru: desain mengikuti referensi yang dikirim ulang

Header kecil Aa/⋮, surah kiri–juz kanan, bingkai melengkung halus, ornamen judul
dan mini player tipis. Bingkai mengikuti tinggi viewport; penanda sage ayat aktif
menyatu per baris. Seekbar dibuka lewat ⋮ → posisi audio; atribusi dipindahkan ke
⋮ → Sumber Mushaf. [Screenshot native dan bukti](android/validation/mushaf-reference-2026-10-21/README.md).
Build/lint dan 78 JVM lulus. Empat metode Android unik lulus, termasuk playback
asli/posisi native dan jeda pada mini player; satu diulang pada teks sistem 200%.
APK terbaru `D:/Download/Zikra-App-main/Zikra-Mushaf-Reference-20261021-debug.apk`.
Batas Android 12+/online dan sorotan kata belum terhubung tetap berlaku.

## Terbaru: poles tampilan sesuai arah referensi pengguna

Bingkai sage bersudut geometris, ornamen judul sand gold, kertas ivory,
metadata terpusat, halaman pembuka lebih lapang dan player khusus Mushaf lebih
ringkas. Glyph/baris sumber tetap utuh. [Bukti native dan hasil tes](android/validation/mushaf-polish-2026-10-21/README.md).
Build/lint, 78 JVM dan tiga metode Mushaf lulus; satu metode diulang pada font
sistem 200%. Batas Android 12+/online dan sorotan kata belum terhubung tetap sama.
APK baru di `D:/Download/Zikra-App-main/Zikra-Mushaf-Polished-20261021-debug.apk`.

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
