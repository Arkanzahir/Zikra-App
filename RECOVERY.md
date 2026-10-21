> **Study beranimasi 1.4.0 — 22 Oktober 2026:** aset ilustrasi dibuat dahulu, lalu dipasang ke agenda/fokus/istirahat. Tangan menulis dan siklus cangkir mengikuti timer; 7 tes Android lulus, termasuk jeda, lifecycle dan font 200%. [Screenshot, video, build dan batas](android/validation/study-layered-motion-2026-10-22/README.md). Emulator ditutup; HP fisik belum diuji.

> **Home/Quran 1.3.2 — 22 Oktober 2026:** ilustrasi masjid baru, proporsi Home dan daftar surah mengikuti referensi lebih dekat. [Screenshot APK, build dan batas](android/validation/reference-fidelity-2026-10-22/README.md). Study/AI belum setara ilustrasi konsep.

> **Emulator / 1.3.1 — 22 Oktober 2026:** screenshot native Home/Study/AI/Quran, layout fokus lebih ringkas dan tombol tetap terlihat. Gerak break/pause dan font 200%: 4 tes Android lulus. [Bukti, video dan batas](android/validation/emulator-layout-2026-10-22/README.md). Emulator ditutup setelah tes.

> **Konsep pilihan pengguna — 1.3.0:** Home/langit, Study/fokus/break, katalog Quran dan AI diperbarui. Reader mendukung warna kata saat sumber/timing cocok; jalur Featherless siap dikonfigurasi, belum live. [Build, 96 JVM, 9 tes backend dan batas visual/HP](android/validation/concept-refresh-2026-10-21/README.md).

> **Orb bergerak 1.2.1 — 21 Oktober 2026:** pita warna mengalir, kilau dan denyut halus pada Pendamping Zikra. [Build dan batas](android/validation/orb-motion-2026-10-21/README.md). Build/lint lulus; gerak belum diuji di HP.

> **Tampilan native 1.2 — 21 Oktober 2026:** Home/Quran/Study/Pendamping Zikra diperbarui; Mushaf dan gulir otomatis lebih mudah ditemukan. [Hasil build, referensi dan batas pengujian](android/validation/visual-refresh-2026-10-21/README.md). Pengguna memilih APK dahulu; belum diuji visual di HP. Sorotan kata khusus Mushaf; adzan penuh dan AI generatif belum tersedia.

> **Checkpoint sorotan kata — 21 Oktober 2026:** sorotan emas Mushaf kini mengikuti rekaman/timing asli tiga qari dan posisi decoder. [Bukti, pengujian dan batas](android/validation/mushaf-word-sync-2026-10-21/README.md). Online, belum audit semua ayat/HP fisik. Status belum ada/blocked di bawah adalah riwayat sebelum checkpoint terbaru.

# Pemulihan dan status Zikra — 20 Oktober 2026 (WIB)

## Status terbaru

21 Oktober: berdasarkan `486a35d`, desain katalog/reader/panel audio Quran sudah
diterapkan di Java/XML. Data, parser, service audio dan unduhan tetap dipakai.
[Bukti terbaru](android/validation/quran-reader-panel-2026-10-21/README.md).
Mushaf dan sorotan kata masih belum tersedia: akses API/CDN tetap ditolak proxy
dan data layout/timing/izin font belum terverifikasi. Catatan di bawah historis.

Revisi berikutnya berdasarkan `b8f3a04`: pose cangkir juga dipakai saat jeda fokus;
lanjut kembali membaca, tanpa mengubah fase atau sisa waktu.
[Bukti](android/validation/focus-pause-pose-2026-10-20/README.md).
[Acuan Quran/Mushaf yang disepakati](design/QURAN_READER_DESIGN.md) kini disimpan;
reader baru, Mushaf dan sorotan kata masih rencana, bukan fitur APK pada checkpoint ini.

Revisi setelah `7f9d60a`: pengguna menilai gerak/posisi karakter belum sesuai referensi.
Susunan kini angka di atas karakter, progres tipis, gerak badan/kepala/tangan/buku
lebih terlihat dan pose istirahat duduk. [Bukti terbaru](android/validation/focus-motion-placement-2026-10-20/README.md).
Catatan cincin progres besar di bawah adalah hasil terdahulu yang sudah direvisi.

Main kini mencakup checkpoint `f48fd31` dan desain Fokus/Pomodoro terbaru berdasarkan
referensi gerak pengguna: karakter daun membaca/istirahat, ring progres, gerak napas/
kedip dan penghormatan pengaturan animasi/penghemat daya. Pengguna meminta push langsung
ke main setelah validasi. [Bukti terbaru](android/validation/focus-motion-2026-10-20/README.md).
Hitungan monotonic, alarm, tugas dan SQLite v3 dipertahankan. Baca hash remote terbaru;
catatan branch/commit di bawah merupakan urutan checkpoint sebelum perapian animasi ini.


Tahap berikutnya di `feature/map-audio-study-2026-10-20`, berdasarkan `2b13539`:
status/coba ulang peta saat tile gagal, perbaikan pengunduhan ulang cache audio rusak,
Study berurutan tenggat, timer fokus/jeda dan pengingat tugas opsional. SQLite v3
menambah `due_at`/`study_reminder` secara bertahap dari v1/v2 tanpa membuang tabel/data.
[Bukti tahap ini](android/validation/map-audio-study-2026-10-20/README.md).
Provider OSM/Overpass/CDN qari masih ditolak proxy cloud (CONNECT 403); status live belum terverifikasi.


GitHub diperiksa pada 20 Oktober 2026 (WIB): `main` dan
`feature/location-quran-audio` sama-sama `1045fa75113a14553c3bb59b0df83e35b6f90fa0`;
`recovery/study-hifz-refresh` tetap `d9d2ed4b5fb39606b659a708300a00b92fba7126`.
Merge ke main sebelumnya diminta pengguna dan telah berhasil. Pembacaan Git remote
berhasil pada sesi ini; kemampuan push harus dibuktikan oleh hasil push aktual.

Lokasi otomatis/manual, jadwal/cuaca/Kiblat bersama dan masjid OSM sudah ada.
Pemutar Quran per ayat, qari, background/MediaSession/notifikasi, unduhan surah,
gulir mandiri/follow serta latihan rentang Hifz sudah masuk main pada `1045fa7`.
[Bukti 19 Oktober](android/validation/location-quran-2026-10-19/README.md):
46 tes JVM dan 20 metode API 28 lulus; audio uji WAV sintetis, bukan rekaman Quran.
CDN qari live/full download serta runtime API 35 dan HP fisik belum terbukti.

Checkpoint sebelumnya `feature/location-polish-2026-10-20` berangkat dari `1045fa7`:
latar masjid/lokasi netral `#F4F6F8`, kartu lokasi ringkas, indikator loading kota,
pembatalan lookup saat memilih kota manual, status pembatasan layanan dan
pemulihan hasil masjid kosong. [Validasi tahap ini](android/validation/location-polish-2026-10-20/README.md).
[Rencana aktif](design/IMPROVEMENT_PLAN.md) sudah tersimpan sebagai file.

Branch recovery adalah checkpoint lama, bukan satu-satunya sumber terbaru.
Sumber asli lokal versi taman/Home belum ditemukan; implementasi baru tidak
diklaim identik dengan kode atau piksel aslinya. Pertahankan seluruh perubahan/data.

| Bagian | Kondisi saat ini |
| --- | --- |
| Study/Hifz | Study khusus akademik, checklist ibadah dari Waktu Sholat dan Diary Hifz terpisah. Tugas akademik/target Hifz tetap dapat diakses lintas tanggal; ibadah mengikuti hari lokal. Hari baru kosong, tanpa 24 contoh atau 8 progres selesai palsu. Data dan status lama tetap dipertahankan. CRUD/SQLite v3, draft editor, navigasi dan metadata 114 surah offline. |
| Branding | File master JPG tidak berubah; simbol bulan sabit, buku dan spark tetap sama. Varian Pine/putih memakai geometri/alpha identik; XML logo bersama pada splash/onboarding/Home/Tentang, simbol saja pada adaptive icon. Wordmark ditulis `zikra` tanpa titik; Profil memakai avatar. |
| Onboarding | Tiga ilustrasi 2D 3:2: belajar di meja, perempuan bermukena di sajadah, membaca Quran dengan rehal. Parallax gambar utuh/teks saat sentuh/gulir; karakter tetap diam, tanpa sensor/loop idle. Animasi nonaktif/penghemat daya menghentikan efek. Judul/paragraf, kembali dan langkah 01 / 03. ImageView hanya memuat gambar aktif dengan sampling sesuai display. Tanpa jenjang/minat; preferensi lama dipertahankan. |
| Home | Lokasi/zona dipilih pengguna, logo/lokasi kiri atas, siang/malam, busur matahari berdasarkan terbit/transit/terbenam, fase bulan USNO harian atau perkiraan lokal berlabel, parallax dan animasi masuk. |
| Quran | Katalog ringkas dan reader terang sesuai referensi Suhayel; ukuran tulisan Arab/terjemahan tersimpan, tema terang/malam/otomatis dan lompat ayat dengan validasi rentang. Pencarian Arab mengabaikan harakat/tatweel/variasi alef. Reader native 114 surah public v4, Utsmani/terjemahan resource 33, 50 ayat per bagian, bookmark kunci stabil, resume/recreation, delapan bagian cache; empat surah bawaan. Semua target Hifz menuju reader native. |
| Cuaca/Protector | Open-Meteo untuk prakiraan model lokasi pilihan dengan cache terpisah maksimal empat lokasi, refresh 10 menit/cache maksimal enam jam bertimestamp. Gempa/peringatan tetap BMKG. Header XML sama dengan Study/Hifz; grafik memakai geometri tersimpan. |

Ronde kegunaan sebelumnya pada 19 Oktober dimulai dari audit sumber/referensi dan panduan aksesibilitas resmi, lalu implementasi terarah pada onboarding, keterbacaan, navigasi, rencana lintas tanggal, reader dan penanganan data jaringan. **Build/lint, 33 tes JVM dan 47 metode emulator unik lulus.** [Laporan ronde ini](android/validation/quality-2026-10-19/README.md) memuat 22 PNG aktual, kegagalan awal/retest dan diagnosis ANR renderer/SystemUI emulator. Label navigasi serta Lewati tetap utuh pada font besar. Ini belum bukti HP fisik atau runtime API 35.

## Riwayat bukti sebelum ronde ini

[Validasi statis](android/validation/static-onboarding/README.md) merekam onboarding 18 Oktober sebelum perbaikan komposisi terbaru; [validasi karakter/Study/Quran](android/validation/character-refresh/README.md) merekam checkpoint 3D yang sudah diganti; [validasi pembangunan ulang](android/validation/rebuilt/README.md) menyimpan checkpoint sebelumnya. Catatan [pemulihan awal](android/validation/recovery/README.md) dan [agenda](android/validation/agenda/README.md) dipertahankan sebagai riwayat, bukan status terbaru semua fitur.

## Yang belum selesai atau belum terbukti

- Mushaf sesuai layout edisi dan offline seluruh korpus otomatis belum tersedia. Pemutar/unduhan teks-audio per surah sudah ada di main; keberhasilan rekaman qari live dan unduhan jaringan penuh belum terbukti. Pengguna belum mempunyai akses production Content Sync. Cache delapan bagian biasa bukan offline seluruh Quran.
- Posisi/orientasi bulan, awan dan bintang merupakan ilustrasi; fraksi terang dari data harian bukan pengamatan langsung atau penetapan awal bulan. Sumber/fallback dijelaskan saat label Langit diketuk. Lokasi/zona mengikuti pilihan; acuan awal Sukolilo/WIB berlabel sampai pengguna menyimpan pilihan.
- Kelancaran, baterai, sensor Kiblat/tilt, haptik, alarm/izin, layanan suara dan runtime Android API 35 belum diverifikasi di HP fisik. Emulator API 28 dengan software rendering tidak membuktikan performa HP.
- AI masih berbasis aturan lokal; tidak ada penilaian hafalan AI. Doa/Hadis tetap koleksi terbatas. Pengembangan AI generatif dan fitur Quran lanjutan perlu tahap tersendiri.
- Uji agenda lama yang terhambat ANR tetap historis. Ronde terbaru memeriksa scoped onboarding320×560dp/font200%, Home sekitar320×720dp/font200% dan asisten200%. SystemUI sempat ANR; pemulihan backend dan pengujian ulang dicatat, bukan dihitung sebagai metode baru. Belum regresi seluruh kondisi/perangkat.

## Sumber dan riwayat penelusuran

Saat pemeriksaan filesystem aktif, checkout utama belum tersedia lalu diklon dari `Arkanzahir/Zikra-App` commit publik `d19e95bb9eef80afba6d6c2b02291d56716a76fa`. Pengguna mengonfirmasi belum ada salinan lain dan menunjukkan riwayat environment serta path lama `android/validation/screens/onboarding-garden/preview.html` dan `home-entrance/home-entrance.mp4`. Path itu belum ditemukan pada filesystem, kedua checkout, reflog/objek Git, arsip atau APK yang dapat diakses. Ini bukti **belum ditemukan**, bukan bukti hilang permanen atau environment pasti berganti.

Orbit lama memang ada di Git (`65666c2`, `LearningOrbitView`). Pengguna kemudian menyetujui pembangunan ulang dari chat; spesifikasi yang diterapkan mengikuti arahan terbaru, termasuk penghapusan jenjang/minat dan logo kiri atas. Aset taman yang baru bukan aset asli dari video. `prototype_zikra` tetap referensi web lama tanpa perubahan.

## Melanjutkan sesi

Gunakan checkout dan branch yang sudah ada; baca dokumen ini, README root dan [BUILDING.md](android/BUILDING.md). Jangan reset/pull otomatis, menghapus perubahan pengguna atau mengklaim fitur lengkap hanya karena build lulus. Java/XML native; tanpa login dahulu. Simpan checkpoint kode ke GitHub dan buat cadangan terpisah. Setup [tools/cloud](tools/cloud/README.md) tidak boleh mengubah source/dependensi/lockfile.
