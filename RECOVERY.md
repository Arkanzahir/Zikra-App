# Pemulihan Study/Hifz — 18 Oktober 2026

**Penyempurnaan terbaru:** Study/Hifz kini memakai susunan agenda: judul ivory/Pine, progres kecil per kategori, satu kegiatan belum selesai yang ditonjolkan, daftar ringan dan bagian Selesai yang bisa dilipat. Header XML dipakai bersama; konten utama bisa digulir untuk layar kecil/teks besar. Lihat [validasi dan preview agenda](android/validation/agenda/README.md). Bagian pemulihan di bawah mencatat keadaan sebelum penyempurnaan ini; fitur lokal lama lainnya tetap belum pulih.

## Sumber dan batas pemulihan

Saat pemeriksaan filesystem aktif, checkout utama belum tersedia dan kemudian diklon dari `Arkanzahir/Zikra-App`, commit publik `d19e95bb9eef80afba6d6c2b02291d56716a76fa`. Pengguna mengonfirmasi **belum ada salinan lain**, lalu menunjukkan riwayat pekerjaan di environment yang sama dan path `/workspace/Zikra-App/android/validation/screens/onboarding-garden/preview.html`. Path tersebut belum ditemukan di filesystem yang saat ini dapat diakses. Ini membatasi status pada **belum ditemukan**, bukan bukti bahwa pekerjaan hilang permanen atau environment pasti berganti.

Parallax orbit lama memang ada di Git (`65666c2`), termasuk `LearningOrbitView` dan GIF validasi. Versi diorama taman serta langit Home yang diperlihatkan pengguna belum ditemukan pada kedua checkout, reflog/objek Git yang tersedia, arsip kerja atau APK yang dapat diakses. Kode itu belum dapat diklaim pulih maupun termasuk dalam push terbaru. `prototype_zikra` tetap referensi lama tanpa perubahan. Petunjuk menjalankan sumber yang tersedia ada di [BUILDING.md](android/BUILDING.md).

Pekerjaan baru ada pada branch `recovery/study-hifz-refresh`. Snapshot sumber dan Git bundle dibuat terpisah agar pekerjaan tidak hanya bergantung pada instance ini. Jangan reset ke main atau menimpa branch pengguna. Build APK tidak mencadangkan data pribadi di perangkat.

## Yang dibangun kembali pada tahap ini

- **Study Hub:** header judul/kembali tanpa mic atau `+` duplikat; tanggal lokal dinamis menggantikan strip pekan statis; ringkasan progres sage, kategori, kartu checklist dan satu tombol tambah.
- **Diary Hifz:** permukaan ivory, ringkasan Pine/Camel; kartu menampilkan surah, rentang dan jumlah ayat. Target tersimpan di SQLite, bisa diedit, dicentang dan dihapus. Katalog metadata 114 surah diambil dari endpoint publik Quran.com, divalidasi urutan/totalnya dan hanya disimpan dalam memori. Fallback empat surah lokal + metadata target tersimpan tetap bisa dipakai saat gagal jaringan.
- **Editor native:** bottom dialog yang dapat digulir; kategori, validasi nama/rentang, catatan opsional dan draft yang bertahan saat Activity dibuat ulang. Dikte menggunakan layanan Android, mengisi draft tanpa menyimpan otomatis. Mic diletakkan di editor dengan label fungsi.
- **Migrasi SQLite v1→v2:** menambah kolom nullable, mempertahankan ID, tanggal, riwayat dan status. Target lama tidak diberi surah/ayat rekaan; pengguna melihat “Lengkapi target”. Mengubah catatan mempertahankan status; mengubah surah/rentang mereset centang target tersebut.
- **Buka bacaan:** empat surah bawaan dibuka di reader native pada ayat awal; target lain berlabel **Quran.com ↗** dan membuka browser eksternal. Ini **belum** pemulihan reader native 114 surah atau unduhan offline penuh.
- **Navigasi bersama:** Home dan halaman pemakai `NavDock` memakai komponen XML datar 64dp, tujuan Home/Quran/AI/Study/Profil. Diary Hifz dapat kembali ke Study lewat dock. Komposisi hero Home dan master branding tidak diganti. Protector masih memakai navigasi lamanya.

Contoh Al-Mulk 3–7 dan Al-Ikhlas 2–4 pada screenshot adalah data sementara dari tes yang dibersihkan setelah pengujian. Progres awal 8/24 tetap berasal dari data contoh baseline, bukan riwayat pengguna yang berhasil dipulihkan.

## Persyaratan pengguna yang masih harus dipulihkan terpisah

Ini catatan pekerjaan yang pernah diminta/disetujui, **bukan klaim kemampuan kode saat ini**:

1. **Branding:** satu master `design/branding/zikra-logo-primary.jpg` (bulan sabit + buku + spark), bentuk/spark dan proporsi tetap. Pengguna mengizinkan konversi piksel deterministik untuk PNG transparan/putih; hindari menggambar logo baru. Simbol + “zikra.” pada splash/onboarding; logo lengkap ringkas Home dan Tentang; ikon aplikasi simbol saja; halaman fitur judul/kembali, Profil avatar. Pine `#15332F`, putih pada latar gelap; resource terpusat dan komponen XML bersama; dokumentasikan aturan. Tahap ini tidak mengubah master atau membuat varian baru.
2. **Onboarding:** alur cerita menarik dengan parallax/elemen masuk, logo kiri atas; pengguna membatalkan pilihan jenjang/minat. Kode baseline masih memuat alur orbit/jenjang/minat lama. Gunakan acuan Blend wrk dan video pengguna setelah benar-benar dilihat; jangan mengarang isi video yang belum diakses.
3. **Quran Foundation:** pulihkan fondasi katalog/reader 114 surah native, bookmark/lanjut bacaan, pagination, error/offline dan sumber/atribusi terverifikasi. Pengguna belum punya akses production Content Sync. Jangan menaruh secret di APK atau mengklaim akses production tersedia. Mushaf dengan layout edisi yang benar, murottal/play-pause dan offline penuh masih pekerjaan lanjutan.
4. **Home:** penataan logo/lokasi yang lebih rapi, langit siang/malam, matahari mengikuti waktu, fase bulan berdasarkan tanggal/sumber astronomi, parallax dan animasi masuk dengan lifecycle/reduced-motion. Implementasi USNO/animasi yang disebut di percakapan lama tidak ditemukan di remote; baseline masih hero/lentera lama. Belum ada bukti performa/baterai HP fisik.
5. **Cuaca dan konsistensi fitur lain:** Open-Meteo, cache bertimestamp dan penanganan kegagalan; gempa/peringatan resmi tetap BMKG. Header Study/Hifz/Cuaca/Gempa/Protector, dialog/kartu perlu ditinjau bersama setelah pemulihan. Versi baseline masih endpoint cuaca BMKG; keberhasilan lama tidak menjadi bukti versi ini.
6. **Fitur lanjutan:** pilihan lokasi jadwal/cuaca, uji Kiblat/sensor/izin alarm di HP, konten Doa/Hadis bersumber, backend AI. Belum ada evaluasi bacaan hafalan AI. Jangan menjadikan daftar ini perluasan otomatis tugas desain Study/Hifz saat ini.

## Melanjutkan sesi

Baca dokumen ini sebelum bagian historis README/VALIDATION. Pertahankan branch pemulihan dan perubahan pengguna. Lihat [validasi tahap ini](android/validation/recovery/README.md) dan [setup](tools/cloud/README.md). Gunakan Java/XML; jangan pindah ke prototype web. Kerjakan satu tahap konkret berikutnya: pulihkan reader Quran native lengkap, lalu branding/onboarding/Home sesuai arahan pengguna yang terbaru.
