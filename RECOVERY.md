# Pemulihan dan status Zikra — 19 Oktober 2026 (WIB)

## Status terbaru

Tahap onboarding berikutnya selesai: pengantar, judul 32sp dan paragraf manfaat,
komposisi teks seimbang, kembali di kiri dan langkah di kanan. Ilustrasi serta master
logo tidak diubah. Build/lint dan lima skenario onboarding unik lulus; [bukti terbaru](android/validation/onboarding-copy-2026-10-19/README.md).
[Rencana berikutnya](design/IMPROVEMENT_PLAN.md): lokasi bersama/masjid, lalu murottal/
hafalan dan integrasi asisten. Fitur rencana tersebut belum diimplementasikan tahap ini.

Branch `recovery/study-hifz-refresh` memuat pembangunan ulang dari riwayat chat dan screenshot pengguna. Sumber asli lokal versi taman/Home belum ditemukan; implementasi baru tidak diklaim identik dengan kode atau piksel aslinya. Checkpoint disimpan ke GitHub per tahap, tanpa menggabungkan main otomatis.

| Bagian | Kondisi saat ini |
| --- | --- |
| Study/Hifz | Study khusus akademik, checklist ibadah dari Waktu Sholat dan Diary Hifz terpisah. Tugas akademik/target Hifz tetap dapat diakses lintas tanggal; ibadah mengikuti hari lokal. Hari baru kosong, tanpa 24 contoh atau 8 progres selesai palsu. Data dan status lama tetap dipertahankan. CRUD/SQLite v2, draft editor, navigasi dan metadata 114 surah offline. |
| Branding | File master JPG tidak berubah; simbol bulan sabit, buku dan spark tetap sama. Varian Pine/putih memakai geometri/alpha identik; XML logo bersama pada splash/onboarding/Home/Tentang, simbol saja pada adaptive icon. Wordmark ditulis `zikra` tanpa titik; Profil memakai avatar. |
| Onboarding | Tiga adegan ilustrasi 2D statis 3:2 memenuhi lebar bagian atas: belajar di meja, perempuan bermukena di sajadah, dan membaca Quran dengan rehal. Judul dua baris, footer 01 / 03, tanpa animasi/parallax/transisi isi. ImageView hanya memuat gambar aktif dengan sampling sesuai display. Tanpa pertanyaan jenjang/minat; preferensi lama dipertahankan. |
| Home | Logo/lokasi kiri atas, siang/malam, busur matahari berdasarkan terbit/transit/terbenam, fase bulan USNO harian atau perkiraan lokal berlabel, parallax dan animasi masuk. |
| Quran | Katalog ringkas dan reader terang sesuai referensi Suhayel; ukuran tulisan Arab/terjemahan tersimpan, tema terang/malam/otomatis dan lompat ayat dengan validasi rentang. Pencarian Arab mengabaikan harakat/tatweel/variasi alef. Reader native 114 surah public v4, Utsmani/terjemahan resource 33, 50 ayat per bagian, bookmark kunci stabil, resume/recreation, delapan bagian cache; empat surah bawaan. Semua target Hifz menuju reader native. |
| Cuaca/Protector | Open-Meteo untuk prakiraan model Sukolilo, refresh 10 menit/cache maksimal enam jam bertimestamp. Gempa/peringatan tetap BMKG. Header XML sama dengan Study/Hifz; grafik memakai geometri tersimpan. |

Ronde 19 Oktober dimulai dari audit sumber/referensi dan panduan aksesibilitas resmi, lalu implementasi terarah pada onboarding, keterbacaan, navigasi, rencana lintas tanggal, reader dan penanganan data jaringan. **Build/lint, 33 tes JVM dan 47 metode emulator unik lulus.** [Laporan ronde ini](android/validation/quality-2026-10-19/README.md) memuat 22 PNG aktual, kegagalan awal/retest dan diagnosis ANR renderer/SystemUI emulator. Label navigasi serta Lewati tetap utuh pada font besar. Ini belum bukti HP fisik atau runtime API 35.

## Riwayat bukti sebelum ronde ini

[Validasi statis](android/validation/static-onboarding/README.md) merekam onboarding 18 Oktober sebelum perbaikan komposisi terbaru; [validasi karakter/Study/Quran](android/validation/character-refresh/README.md) merekam checkpoint 3D yang sudah diganti; [validasi pembangunan ulang](android/validation/rebuilt/README.md) menyimpan checkpoint sebelumnya. Catatan [pemulihan awal](android/validation/recovery/README.md) dan [agenda](android/validation/agenda/README.md) dipertahankan sebagai riwayat, bukan status terbaru semua fitur.

## Yang belum selesai atau belum terbukti

- Mushaf sesuai layout edisi, murottal dan unduhan Quran penuh belum tersedia. Pengguna belum mempunyai akses production Content Sync. Cache bagian yang dibuka bukan offline seluruh Quran.
- Posisi/orientasi bulan, awan dan bintang merupakan ilustrasi; fraksi terang dari data harian bukan pengamatan langsung atau penetapan awal bulan. Sumber/fallback dijelaskan saat label Langit diketuk. Lokasi masih Sukolilo/WIB.
- Kelancaran, baterai, sensor Kiblat/tilt, haptik, alarm/izin, layanan suara dan runtime Android API 35 belum diverifikasi di HP fisik. Emulator API 28 dengan software rendering tidak membuktikan performa HP.
- AI masih berbasis aturan lokal; tidak ada penilaian hafalan AI. Doa/Hadis tetap koleksi terbatas. Pilihan lokasi dan pengembangan fitur lain perlu tahap tersendiri.
- Uji agenda lama yang terhambat ANR tetap historis. Ronde terbaru memeriksa scoped onboarding320×560dp/font200%, Home sekitar320×720dp/font200% dan asisten200%. SystemUI sempat ANR; pemulihan backend dan pengujian ulang dicatat, bukan dihitung sebagai metode baru. Belum regresi seluruh kondisi/perangkat.

## Sumber dan riwayat penelusuran

Saat pemeriksaan filesystem aktif, checkout utama belum tersedia lalu diklon dari `Arkanzahir/Zikra-App` commit publik `d19e95bb9eef80afba6d6c2b02291d56716a76fa`. Pengguna mengonfirmasi belum ada salinan lain dan menunjukkan riwayat environment serta path lama `android/validation/screens/onboarding-garden/preview.html` dan `home-entrance/home-entrance.mp4`. Path itu belum ditemukan pada filesystem, kedua checkout, reflog/objek Git, arsip atau APK yang dapat diakses. Ini bukti **belum ditemukan**, bukan bukti hilang permanen atau environment pasti berganti.

Orbit lama memang ada di Git (`65666c2`, `LearningOrbitView`). Pengguna kemudian menyetujui pembangunan ulang dari chat; spesifikasi yang diterapkan mengikuti arahan terbaru, termasuk penghapusan jenjang/minat dan logo kiri atas. Aset taman yang baru bukan aset asli dari video. `prototype_zikra` tetap referensi web lama tanpa perubahan.

## Melanjutkan sesi

Gunakan checkout dan branch yang sudah ada; baca dokumen ini, README root dan [BUILDING.md](android/BUILDING.md). Jangan reset/pull otomatis, menghapus perubahan pengguna atau mengklaim fitur lengkap hanya karena build lulus. Java/XML native; tanpa login dahulu. Simpan checkpoint kode ke GitHub dan buat cadangan terpisah. Setup [tools/cloud](tools/cloud/README.md) tidak boleh mengubah source/dependensi/lockfile.
