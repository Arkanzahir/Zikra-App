# Halaman harian native — 24 Oktober 2026

APK debug **1.6.0 (14)**. Onboarding, Hadis, Doa Harian dan Profil sudah diterapkan ke Java/XML Android, bukan gambar konsep yang dipasang sebagai satu layar.

## Perubahan

- Onboarding: tiga aset produksi terpisah dengan suasana masjid, membaca dan belajar; teks asli, langkah maju/mundur, Lewati dan tombol tetap di bawah. Preferensi pendidikan lama dipertahankan. Gerak tetap berupa parallax sentuhan/gulir yang sudah tersedia.
- Hadis: hero perpustakaan, sampul kitab yang digambar native, filter Bukhari/Muslim/Tersimpan dan pencarian. Halaman baca tersendiri menyediakan ukuran teks, simpan, salin dan bagikan; bacaan terakhir diperbarui ketika kembali.
- Doa: ilustrasi jendela, pencarian dan kategori, kartu yang dapat dibuka untuk membaca. Kontrol pelafalan dan siklus TTS sebelumnya dipertahankan.
- Profil: latar mint dengan garis ilustrasi meja/buku yang halus; header mengikuti tinggi isi. Nama lokal bisa diubah, nama kosong ditolak, inisial mengikuti nama. Progres berasal dari database Study, bukan angka contoh. Jalur ke pengaturan/lokasi/Protector dan fitur lain tetap tersedia.
- Tipografi UI memakai Plus Jakarta Sans yang sudah dibundel. Sampul kitab dan teks Arab Hadis memakai serif perangkat; tidak mengklaim font Arab identik dengan konsep.

## Screenshot dari APK

Diambil pada emulator Medium_Phone, 720 × 1600, density 320. Semua gambar memperlihatkan native view.

| Layar | Bukti |
| --- | --- |
| Sambutan | [Welcome](DailyOnboardingWelcome.png) |
| Membaca | [Read](DailyOnboardingRead.png) |
| Belajar | [Study](DailyOnboardingStudy.png) |
| Koleksi Hadis | [Hadis](DailyHadithOverview.png) |
| Pembaca Hadis | [Reader](DailyHadithReader.png) |
| Daftar Doa | [Doa](DailyDuaOverview.png) |
| Doa dibuka | [Doa expanded](DailyDuaExpanded.png) |
| Profil awal | [Profil](DailyProfile.png) |
| Nama diperbarui | [Profil dengan nama tersimpan](DailyProfileNamed.png) |

## Pemeriksaan

`assembleDebug`, `assembleDebugAndroidTest`, `lintDebug` berhasil. Lint: **0 error, 680 warning** pada seluruh aplikasi; warning tidak disembunyikan. Build dibatasi satu worker dengan heap 512 MB.

Empat metode instrumentasi pada APK final lulus (**11,749 detik**):

1. Tiga langkah onboarding, ilustrasi sesuai langkah, tombol akhir dan kembali; preferensi pendidikan tidak berubah.
2. Memilih Muslim menghasilkan lima kutipan; pencarian kosong/pulih, membuka reader, perubahan bookmark persisten dan pembaruan bacaan terakhir.
3. Membuka isi Doa beserta sumber Taha 20:114, pencarian kosong/pulih tanpa memerlukan layanan suara.
4. Nama Profil kosong ditolak; nama valid tersimpan, muncul kembali setelah Activity dibuka ulang, serta caption progres tugas berasal dari alur Study.

Log: [build](build.txt), [instrumentasi](instrumentation.txt), [hash sumber/APK](release.json). Buffer crash kosong sesudah tes. Satu emulator diberi parameter RAM 1536 MB dan dua core, tetapi log gambar Android 37.1 menunjukkan RAM dinaikkan otomatis menjadi 4096 MB. Emulator sudah ditutup setelah screenshot diambil. Tidak menggunakan HP pengguna. Koreksi kapasitas ini diketahui saat pemeriksaan [revisi Profil 1.6.1](../profile-background-2026-10-24/README.md).

APK lokal: `D:\Download\Zikra-App-main\Zikra-Daily-v1.6.0-debug.apk`, **31.242.144 byte**.

SHA-256: `14897b3148d42d11502841faf33a8d2746ac36b930d21a754d2d1badc8d0a0c8`.

## Batas

- Hadis masih **tujuh kutipan lokal**, bukan kitab lengkap atau integrasi API Hadis. Label ini tetap ditampilkan di aplikasi.
- Doa masih empat pilihan lokal. Suara TTS bergantung pada mesin dan paket bahasa Arab Android; keluaran suara tidak diuji pada putaran ini.
- Tidak menguji HP fisik, TalkBack, atau seluruh variasi ukuran font. Screenshot merupakan ukuran dan font standar emulator tersebut. Desain mengikuti arah konsep, bukan klaim identik piksel.
- Perubahan audio Quran yang belum selesai di working tree tidak dimasukkan ke APK ini. APK dibangun dari snapshot baseline ditambah 32 file lingkup perubahan; 447 file produksi terlacak dibandingkan dengan checkout, dengan normalisasi LF/CRLF dan pengecualian dua file audio tersebut. Detail pengecualian dan hash tersedia di `release.json`.
- Gerak tangan menulis/minum pada Study tetap milik implementasi terdahulu; putaran ini tidak mengubah atau menguji ulang animasi tersebut. AI/Featherless, cuaca dan peta gempa juga tidak diubah.

## Sumber aset

Ilustrasi produksi di [onboarding-production-v1](../../../design/assets/onboarding-production-v1/PROMPTS.md) dan [daily-pages-v2](../../../design/assets/daily-pages-v2/PROMPTS.md). PNG asli disalin tanpa penyuntingan bitmap ke drawable-nodpi. Semua label, teks Arab/terjemahan, kartu, navigasi dan tombol adalah view Android.
