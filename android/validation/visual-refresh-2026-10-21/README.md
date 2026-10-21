# Tampilan Zikra 1.2 — 21 Oktober 2026

Pengguna meminta desain diprioritaskan setelah mencoba Home, Quran, Study Hub dan
Pendamping Zikra di HP. Screenshot pengguna memperlihatkan ruang kosong besar,
konten pendukung kurang menonjol, serta kontrol Mushaf/gulir yang sulit ditemukan.

## Yang berubah

| Bagian | Perubahan |
| --- | --- |
| Home | Ikon shortcut dalam lingkaran mint/emas/lavender/biru, kartu ibadah bernuansa hangat, kartu lanjut baca sage, dan kartu fokus Pine dengan ilustrasi buku serta aksi ke timer. Jam, langit, jadwal dan delapan tujuan yang ada dipertahankan. |
| Quran | Hero Pine dengan ilustrasi buku, tombol Buka Mushaf langsung di katalog, kartu surah lebih lapang, serta tombol Mushaf, Gulir otomatis dan Audio & ikuti ayat di reader. |
| Study | Header Pine bergambar, kartu fokus dengan deskripsi dan aksi nyata, ringkasan progres tetap terlihat saat kosong, serta ajakan awal yang lebih jelas. Tidak menambahkan tugas atau progres palsu. |
| Pendamping Zikra | Latar lavender/cream, orb Canvas yang sebelumnya hanya tampil setelah input suara kini terlihat di hero awal, kartu tulis/suara berwarna, dan header yang sekeluarga. Percakapan lokal/dikte tetap bekerja dengan mekanisme lama. |
| Fokus | Latar adegan mint hangat. Pose istirahat saat timer dijeda kini boleh tetap bernapas/beruap ketika kebijakan animasi sistem mengizinkan. Penjelasan keterbatasan gerak ditampilkan bila pengaturan sistem mematikan animasi. |
| Pengingat sholat | Keterangan di halaman pengingat membedakan suara notifikasi sistem dari adzan penuh. Tidak mengubah izin/alarm atau mengganti suara channel pengguna. |

Ilustrasi baru adalah vektor native kecil; tidak menambah library, video atau
animasi raster. Warna layout baru dipusatkan dalam resource. Konten, bookmark,
preferensi, tugas, unduhan dan database pengguna tidak direset.

## Referensi yang diperiksa

- [Home/Quran Suhayel](../../../design/references/home-quran-suhayel.png) dan
  [detail Home](../../../design/references/home-target-suhayel.png): gambar lokal
  dibuka; dipakai untuk ritme hero, strip ibadah, menu dan kartu progres.
- [Acuan AI pengguna](../../../design/references/ai-voice-reference.png): gambar
  lokal dibuka; arah lavender, orb dan kartu aksi menjadi acuan.
- [QuranApp](https://github.com/AlfaazPlus/QuranApp) dan
  [Quran for Android](https://github.com/quran/quran_android): dokumentasi primer
  diperiksa untuk pola navigasi bacaan/Mushaf/audio. Tidak mengimpor kode/asetnya.
- [Structured](https://structured.app/): penjelasan produk tentang rencana visual
  dan tugas diperiksa. Bukan klaim menguji aplikasi benchmark tersebut.

## Hasil aktual dan batas pengujian

APK: `Zikra-Visual-Refresh-v1.2-debug.apk`, versionCode 3, versionName 1.2,
minSdk 26 / target 35. SHA256:

`66B21BF737FC7AB8EC53DEE3D07A0EE303F8C24C41B54379626AE4D14835077A`

- Build, 87 tes JVM (15 suite, 0 kegagalan/error), serta lint selesai dalam 39 detik
  dari salinan sumber staged yang terpisah. Heap 768 MiB, satu worker, offline,
  tanpa emulator. [Log](build.txt) · [Hasil terstruktur dan hash tree sumber](result.json).
- Lint: 0 error, 611 warning; masih ada peringatan sumber/proyek lama dan resource.
  Tidak menyatakan proyek bebas warning atau mengklaim semua warning sudah diaudit.
- Tidak ada HP pada daftar adb. Pengguna memilih **Siapkan APK dulu**. Tidak ada
  screenshot native baru atau klaim pengujian visual, animasi, gesture, jaringan,
  notifikasi/adzan dan audio live pada HP untuk versi ini. Build bukan bukti
  persetujuan desain. Pemeriksaan visual/font besar tetap perlu di HP.
- Eksperimen prefetch audio, audit timing, dan tambahan tes native yang sudah ada
  sebelum ronde desain dipertahankan di working tree tetapi **tidak ikut APK ini**.
  Audio memakai checkpoint stabil WordSync sebelumnya.

## Jawaban untuk keluhan fitur

1. Mushaf sudah ada, sebelumnya tersimpan dalam pengaturan bacaan; sekarang punya
   akses langsung. Memerlukan Android 12+ dan koneksi. Tombol memberi penjelasan
   pada Android lebih lama. Layout/ornamen, font Quran dan metadata Mushaf tidak
   diubah oleh ronde desain ini.
2. Sorotan kata terverifikasi tetap khusus Mushaf dengan rekaman/timing yang cocok.
   Reader ayat-terjemahan masih memakai sorotan ayat. Ronde ini tidak mengklaim
   telah menambahkan karaoke kata ke reader biasa.
3. Gulir otomatis reader dapat dinyalakan/dijeda melalui tombol yang terlihat.
   Sentuhan manual atau meninggalkan halaman menjedakannya. Ikuti ayat audio
   tetap opsi terpisah di panel audio; keduanya bukan pengukur progres hafalan.
4. Gerak tetap mengikuti pengaturan animasi/penghemat daya HP. Pause, break dan
   sesi selesai tidak mengubah tugas menjadi selesai secara otomatis.
5. Adzan penuh belum tersedia. Channel pengingat memakai pengaturan notifikasi
   Android; izin, volume, DND dan kebijakan baterai HP mempengaruhi hasil aktual.
6. Pendamping Zikra masih panduan rutinitas offline, belum AI generatif. Masjid
   yang gagal memuat pada screenshot pengguna belum diperbaiki/dibuktikan live
   oleh ronde desain ini; jangan menyamakan perubahan tampilan dengan perbaikan API.

Tahap berikutnya: uji tampilan APK di HP terlebih dahulu, kemudian tangani
sorotan reader biasa, rekaman adzan dan kegagalan layanan masjid secara terarah.
