# Pemulihan Zikra — 18 Oktober 2026

## Status terbaru

Branch `recovery/study-hifz-refresh` memuat pembangunan ulang dari riwayat chat dan screenshot pengguna. Sumber asli lokal versi taman/Home belum ditemukan; implementasi baru tidak diklaim identik dengan kode atau piksel aslinya. Checkpoint disimpan ke GitHub per tahap, tanpa menggabungkan main otomatis.

| Bagian | Kondisi saat ini |
| --- | --- |
| Study/Hifz | Study khusus akademik, checklist ibadah dari Waktu Sholat dan Diary Hifz terpisah; data lama tidak dihapus. Agenda Pine/ivory, target surah/rentang, CRUD, SQLite v2 mempertahankan data, draft editor dan navigasi bersama. Metadata pemilihan 114 surah tersedia offline. |
| Branding | Master JPG tetap, simbol Pine/putih dengan geometri/alpha identik; XML logo bersama pada splash/onboarding/Home/Tentang, simbol saja pada adaptive icon. Profil memakai avatar. |
| Onboarding | Tiga ilustrasi 2D statis: belajar di meja, perempuan bermukena di sajadah, dan membaca Quran dengan rehal. Tanpa animasi/parallax/transisi isi. ImageView native hanya memuat gambar aktif dengan sampling sesuai display. Tidak ada pertanyaan jenjang/minat; preferensi lama dipertahankan. |
| Home | Logo/lokasi kiri atas, siang/malam, busur matahari berdasarkan terbit/transit/terbenam, fase bulan USNO harian atau perkiraan lokal berlabel, parallax dan animasi masuk. |
| Quran | Katalog ringkas dan reader terang sesuai referensi Suhayel; mode malam opsional melalui pengaturan bacaan. Reader native 114 surah melalui public v4 API, Arab Utsmani/terjemahan resource 33, 50 ayat per bagian, bookmark kunci stabil, resume/recreation, delapan bagian cache; empat surah bawaan. Semua target Hifz menuju reader native. |
| Cuaca/Protector | Open-Meteo untuk prakiraan model Sukolilo, refresh 10 menit/cache maksimal enam jam bertimestamp. Gempa/peringatan tetap BMKG. Header XML sama dengan Study/Hifz; grafik memakai geometri tersimpan. |

Bukti onboarding terbaru ada di [validasi statis](android/validation/static-onboarding/README.md); [validasi karakter/Study/Quran](android/validation/character-refresh/README.md) merekam checkpoint 3D yang sudah diganti; [validasi pembangunan ulang](android/validation/rebuilt/README.md) menyimpan checkpoint sebelumnya. Catatan [pemulihan awal](android/validation/recovery/README.md) dan [agenda](android/validation/agenda/README.md) tetap dipertahankan sebagai riwayat, bukan status terbaru semua fitur.

## Yang belum selesai atau belum terbukti

- Mushaf sesuai layout edisi, murottal dan unduhan Quran penuh belum tersedia. Pengguna belum mempunyai akses production Content Sync. Cache bagian yang dibuka bukan offline seluruh Quran.
- Posisi/orientasi bulan, awan dan bintang merupakan ilustrasi; fraksi terang dari data harian bukan pengamatan langsung atau penetapan awal bulan. Sumber/fallback dijelaskan saat label Langit diketuk. Lokasi masih Sukolilo/WIB.
- Kelancaran, baterai, sensor Kiblat/tilt, haptik, alarm/izin, layanan suara dan runtime Android API 35 belum diverifikasi di HP fisik. Emulator API 28 dengan software rendering tidak membuktikan performa HP.
- AI masih berbasis aturan lokal; tidak ada penilaian hafalan AI. Doa/Hadis tetap koleksi terbatas. Pilihan lokasi dan pengembangan fitur lain perlu tahap tersendiri.
- Tes layar kecil/font besar agenda lama terhambat ANR System UI emulator; jangan menyebutnya lulus. Suite yang lulus dicatat per checkpoint; belum regresi menyeluruh semua fitur.

## Sumber dan riwayat penelusuran

Saat pemeriksaan filesystem aktif, checkout utama belum tersedia lalu diklon dari `Arkanzahir/Zikra-App` commit publik `d19e95bb9eef80afba6d6c2b02291d56716a76fa`. Pengguna mengonfirmasi belum ada salinan lain dan menunjukkan riwayat environment serta path lama `android/validation/screens/onboarding-garden/preview.html` dan `home-entrance/home-entrance.mp4`. Path itu belum ditemukan pada filesystem, kedua checkout, reflog/objek Git, arsip atau APK yang dapat diakses. Ini bukti **belum ditemukan**, bukan bukti hilang permanen atau environment pasti berganti.

Orbit lama memang ada di Git (`65666c2`, `LearningOrbitView`). Pengguna kemudian menyetujui pembangunan ulang dari chat; spesifikasi yang diterapkan mengikuti arahan terbaru, termasuk penghapusan jenjang/minat dan logo kiri atas. Aset taman yang baru bukan aset asli dari video. `prototype_zikra` tetap referensi web lama tanpa perubahan.

## Melanjutkan sesi

Gunakan checkout dan branch yang sudah ada; baca dokumen ini, README root dan [BUILDING.md](android/BUILDING.md). Jangan reset/pull otomatis, menghapus perubahan pengguna atau mengklaim fitur lengkap hanya karena build lulus. Java/XML native; tanpa login dahulu. Simpan checkpoint kode ke GitHub dan buat cadangan terpisah. Setup [tools/cloud](tools/cloud/README.md) tidak boleh mengubah source/dependensi/lockfile.
