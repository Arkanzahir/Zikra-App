## Checkpoint tata letak1.4.2 — 22 Oktober 2026

- Acuan baru `design/approved-2026-10-21/comfortable-layout-reference.png`. Reader header Arab/bismillah terpusat, metadata toolbar, nomor/speaker kiri/Arab kanan, arti bawah; menu lewat nomor/longpress; player ringkas+seek48dp di atas dock dan strip gulir/sorotan bawah.
- Home urutan nama salat/waktu/countdown, hero304dp/ikon50dp. Study kalender tanggal dahulu, hero132dp/statistik72dp dan timeline tugas; tidak mengarang durasi/prioritas harian.
- `android/validation/comfortable-layout-2026-10-22/`: build/lint22detik;6metode native lulus29.581detik termasuk Alafasy asli/katalog;3rerun setelah kontras7.079detik dan2Reader final4.698detik. JVM tidak diulang. Font200/malam dilihat langsung; emulator ditutup. HP belum diuji.
- APK luar root `Zikra-Layout-Rapi-v1.4.2-debug.apk`, code10. Ilustrasi masjid/karakter lama dipertahankan. Kecepatan audio/label prioritas konsep belum menjadi fitur; Protector/Hadis/Doa belum dipoles.
- Build terisolasi `.local/concept-release-v130/android` menghindari WIP prefetch audio. Pertahankan file service/timing/MushafReaderTest/tool audit dan catatan private sebagai WIP terpisah. Featherless belum dihosting.

## Checkpoint Quran 1.4.1 — 22 Oktober 2026

- Mini-player katalog ditambahkan (judul/audio dari service aktif, kontrol antrean dan progres, membuka ayat terdengar); tema malam/font200, tanpa player kedua.
- Tombol Lanjut ikuti ayat sesudah sentuhan manual. ArabicWordMap mendukung alif penuh vs superscript alif eksplisit; seluruh kata tetap diperiksa, mismatch fallback ayat.
- `android/validation/quran-catalog-audio-2026-10-22/`: build/lint34detik, 93 JVM lulus/3 optional host-fixture dilewati, 5 native/22.44detik lulus. Alafasy Al-Fatihah2–3 asli: offset kata berubah, follow ayat3, pause/seek; kontrol/layout juga memakai WAV sintetis berlabel. Tidak audit semua qari/ayat/HP.
- APK luar root `Zikra-Quran-Player-v1.4.1-debug.apk`, code9. Emulator ditutup. Protector/Hadis/Doa belum dipoles ronde ini.
- Build memakai `.local/concept-release-v130/android` stabil. Pertahankan eksperimen audio, MushafReaderTest, tools timing dan catatan private lokal sebagai WIP; tidak termasuk paket/commit ini. Featherless belum dihosting.

## Checkpoint Study 1.4.0 — 22 Oktober 2026

- Aset generated-first, prompt lengkap: `design/study-layered-assets-2026-10-22.md`.
- Ruangan tetap, karakter dua pose, siku/pergelangan menulis; cangkir naik-minum-turun dalam 8 detik. Rig 2D.
- Agenda dua metrik aktual (Fokus sesi ini), inline tambah/editor asli dan kalender aktif. Timer gear durasi, akhiri sesi tidak menyelesaikan tugas, font besar caption di luar ring.
- Bukti: `android/validation/study-layered-motion-2026-10-22/README.md`. Build/lint29detik, final paket8detik; 7 tes native/33.963detik lulus. Fixture dikembalikan teardown. Emulator ditutup, HP belum diuji. Tidak ada tes ulang playback/API.
- APK luar repo: `D:\Download\Zikra-App-main\Zikra-Study-Animated-v1.4.0-debug.apk`. Versi8/1.4.0.
- Build terisolasi `.local/concept-release-v130/android` menghindari eksperimen prefetch audio yang masih WIP. Jangan stage MushafReaderTest, QuranAudioService, TimedAudioRepository, tool timing dan catatan private lokal tanpa validasi.
- Home/Quran versi1.3.2 tetap; mini-player katalog masih backlog. Featherless belum dihosting; rahasia tetap hanya lokal.

# Handoff Zikra — 21 Oktober 2026 (WIB)

## Terbaru: Home/Quran lebih dekat referensi — 1.3.2, 22 Oktober 2026

Masjid detail PNG transparan dengan langit/awan/celestial native terpisah; hero
Home/pintasan/kartu ibadah dipadatkan. Quran: buku berlengkung emas, hero136dp,
baris surah56dp; Juz membuka pemilih Mushaf, Penanda membuka last_chapter/last_ayah.
Bukti asli di android/validation/reference-fidelity-2026-10-22/. Build/lint35detik
lulus; cek navigasi manual, font200% masih wrapping, bukan audit aksesibilitas
lulus. Tidak menguji ulang audio/gerak/HP. Emulator dimatikan, adb kosong.
APK luar repo Zikra-Home-Quran-v1.3.2-debug.apk, source terisolasi tanpa prefetch.
Ilustrasi Study dan orb masih belum setara referensi. Jangan klaim keseluruhan
konsep sudah diterapkan identik. Metadata gambar/prompt di design/reference-fidelity-2026-10-22.md.

## Terbaru: emulator dan koreksi tampilan — 1.3.1, 22 Oktober 2026

Pengguna meminta emulator. Satu Medium_Phone berhasil boot dengan GPU host dan
Vulkan off, layar 720x1600/density320, lalu dihentikan setelah bukti selesai.
Home/Study/AI dipadatkan; tombol timer kini tetap di bawah. Bug karakter bergerak
saat pause diperbaiki dengan syarat running. Empat tes Android final lulus
(break aktif, piksel fokus/pause, teks200%, pause/recreate/resume). Build/lint
lulus27detik; APK luar root Zikra-Concept-Refresh-v1.3.1-debug.apk. Screenshot dan
video nyata: android/validation/emulator-layout-2026-10-22/. Tidak menguji ulang
audio/live Featherless atau HP. Jangan mengklaim gambar Canvas identik mockup.
Eksperimen prefetch audio/cache/MushafReaderTest tetap WIP, dikecualikan dari APK.

## Terbaru: konsep terpilih diterapkan — 1.3.0

Pengguna memilih mockup Home/Quran/AI dan Study/Fokus/Break, meminta langit serta
karakter bergerak dan warna teks mengikuti audio. Referensi tersimpan pada
design/approved-2026-10-21/. Home memakai sapaan sesuai waktu/countdown, Canvas
masjid/awan/matahari/bulan; Study mendapat strip tanggal/tugas dan ring timer,
ilustrasi siswa berlapis membaca/cangkir dengan tangan/kepala/uap yang terpisah.
Timer/SQLite lama dipertahankan. Jangan menyalin angka/tugas demo dari mockup.

Reader terjemahan kini dapat memakai timing rekaman yang sama dengan Mushaf dan
pencocokan SELURUH urutan kata Arab sebelum mewarnai kata. Ejaan/urutan/timing
mismatch kembali ke warna ayat; pilihan sorotan ayat/offline tersedia. Ornamen
Mushaf tetap. Belum uji playback/UI native versi ini.

Featherless: pengguna belum punya backend dan memilih siapkan jalur dulu.
Gateway Node + klien Android opt-in tersedia, provider key hanya di environment
server, kode gateway disimpan Android Keystore. Offline default, online belum
live/deploy. Lihat backend/README.md untuk batas pengujian pribadi/hosting.

APK Zikra-Concept-Refresh-v1.3.0-debug.apk (folder induk workspace), code 5,
sertifikat sama. Build akhir 32 detik, heap 768 MiB/1 worker/tanpa emulator:
96 JVM lulus tanpa skip, 9 tes backend mock lulus, tes Android berhasil dikompilasi,
lint 0 error/648 warning. Belum screenshot/rekaman HP dan belum klaim identik
mockup atau kelancaran animasi. [Laporan](android/validation/concept-refresh-2026-10-21/README.md).

Build terpisah dari eksperimen prefetch lama. Working tree service/cache audio,
MushafReaderTest, alat audit timing dan catatan storage lama tetap dipertahankan;
jangan hapus/commit perubahan lama itu tanpa meninjau lingkupnya. Berikutnya:
pasang sebagai pembaruan di HP (jangan uninstall), cek visual/teks besar dan
gerak/playback; hosting/model/key server diperlukan untuk uji Featherless live.

## Terbaru: orb bergerak — 1.2.1

Koreksi pengguna: orb referensi bergerak, sedangkan APK 1.2 masih statis.
AssistantOrbView kini menggambar pita mengalir, kilau, putaran dan denyut kecil;
target sekitar 20 fps, shader/objek gambar dipakai ulang. Animasi terikat lifecycle
AiActivity, visibilitas viewport, fokus serta kebijakan animasi/penghemat daya.
Tidak menambah izin mikrofon atau kemampuan AI generatif.
[Build dan batas](android/validation/orb-motion-2026-10-21/README.md).

APK `Zikra-Visual-Refresh-v1.2.1-debug.apk`, versionCode 4. Build/lint lulus dalam
34 detik (0 error, 608 warning), heap 768 MiB/satu worker/tanpa emulator. Belum ada
rekaman atau tes gerakan/lifecycle/kinerja di HP. Tes JVM 87 berasal dari v1.2,
tidak diulang untuk koreksi visual ini. Pengguna tetap memilih APK dahulu.
Eksperimen prefetch lama tidak ikut APK; perubahan lokal sebelumnya dipertahankan.

## Terbaru: pembaruan tampilan native 1.2

Home, katalog Quran, Study Hub, Fokus dan Pendamping Zikra diperbarui memakai
Pine/mint/cream/lavender, ilustrasi vektor dan orb yang langsung terlihat.
Mushaf kini mempunyai akses langsung di katalog/reader; tombol gulir otomatis
dan audio/ikuti ayat juga terlihat. Ringkasan Study tidak hilang saat kosong.
Pose jeda karakter boleh bergerak saat kebijakan animasi sistem mengizinkan.
[Hasil, referensi dan batas](android/validation/visual-refresh-2026-10-21/README.md).

APK `Zikra-Visual-Refresh-v1.2-debug.apk`: versionCode 3, minSdk 26, target 35.
Build sumber staged terpisah, 87 tes JVM dan lint lulus (0 error, 611 warning),
39 detik, heap 768 MiB/satu worker/tanpa emulator. HP tidak terhubung; pengguna
memilih Siapkan APK dulu. Belum ada screenshot atau pengujian UI/native HP untuk
revisi ini. Eksperimen prefetch dan tes tambahan lama tetap di working tree,
tidak ikut APK ini. Source APK memakai implementasi audio stabil sebelumnya.

Sorotan kata tetap khusus Mushaf (Android 12+/online/timing cocok); reader
terjemahan masih sorotan ayat. Adzan penuh belum tersedia, AI masih panduan lokal,
dan error masjid pada HP belum dibuktikan/ditangani live pada tahap desain ini.
Prioritas berikut: cek visual APK di HP, lalu fitur audio/masjid secara terarah.

## Terbaru: sorotan emas per kata aktif di Mushaf

Mushaf otomatis meminta audio/timing per ayat yang dipasangkan Quran.com untuk
Alafasy (7), Husary (6), Minshawi Murattal (9). File asli dihitung SHA256 lalu
diputar oleh decoder; jumlah kata dan durasi diverifikasi sebelum WordTimeline
diaktifkan. Isi glyph tersapu emas RTL menurut posisi aktual, termasuk seek,
jeda, pengulangan dan pindah halaman. Medali tidak diwarnai emas. Sentuhan manual
tetap menjeda mengikuti ayat. Reader biasa/unduhan lama mempertahankan provider
lama; timing tidak lengkap/gagal memakai sorotan ayat dengan keterangan di UI.
Maksimal tiga file audio cache sementara; timing di memori, tanpa token dalam APK.

[Bukti native, hash audio dan log](android/validation/mushaf-word-sync-2026-10-21/README.md).
Build/lint, 87 JVM dan 10 metode Android unik lulus: tujuh Mushaf memakai audio
asli, tiga regresi service lama memakai fixture WAV sintetis. Tes notifikasi
menunggu tombol yang sesuai state; izin notifikasi emulator diaktifkan khusus tes
dan dikembalikan. Log gagal awal disimpan. Belum tes HP fisik/seluruh ayat; online
dan ketersediaan timing sumber tetap diperlukan. Backend/Content Sync belum dibuat.
APK terbaru `D:/Download/Zikra-App-main/Zikra-Mushaf-WordSync-20261021-debug.apk`.

## Terbaru: basmalah lebih besar dan memakai glyph Mushaf

Basmalah serif sistem berukuran 0,59× huruf ayat diganti empat glyph asli QCF V2
Al-Fatihah 1:1, tanpa medali. Ukuran memakai batas tinta dan skala seragam agar
mengisi baris dengan rapi. Font p1 utuh dicache terpisah tujuh hari; tidak dibundel.
Al-Fatihah tidak mendapat basmalah ganda; At-Taubah tanpa tambahan, diperiksa pada
halaman 187. [Bukti dan pengujian](android/validation/mushaf-basmalah-2026-10-21/README.md).
Build/lint dan 80 JVM lulus; 5 metode Android unik lulus, satu diulang pada teks
sistem 200%. Kegagalan tes awal menemukan status siap sebelum posisi tersimpan;
urutan diperbaiki dan log awal disimpan. Belum diuji di HP fisik/seluruh 604 halaman.
APK terbaru `D:/Download/Zikra-App-main/Zikra-Mushaf-Basmalah-20261021-debug.apk`.

## Terbaru: jenis font judul diganti menjadi kaligrafi surah

Judul serif sistem diganti Surah Names v1 dari CDN Quran Foundation, sesuai
pemetaan resmi Quran.com (001–114 dan prefiks surah). Outline tinta diukur agar
kaligrafi pas di kotak, tanpa meregangkan bentuk. Font utuh dicache terpisah
tujuh hari dan tidak dibundel; font ayat QCF V2 tetap. [Bukti/sumber/hasil tes](android/validation/mushaf-title-font-2026-10-21/README.md).
Build/lint dan 80 JVM lulus; lima metode Android unik lulus, termasuk pemeriksaan
114 ligatur. Audit visual hanya halaman contoh, belum seluruh 604 halaman atau
semua nama. APK terbaru `D:/Download/Zikra-App-main/Zikra-Mushaf-Calligraphy-20261021-debug.apk`.

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
