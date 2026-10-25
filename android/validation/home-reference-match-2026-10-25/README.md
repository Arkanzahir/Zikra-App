# UI04 — Koreksi Home mengikuti referensi siang/malam

25 Oktober 2026. **Kandidat koreksi 1.9.1 beta/code 28**, source `6bdf360c0c6d6f12f06353e321e7d598777c28a7` (implementasi utama `51f7ac2`). Pengguna menolak perbedaan visual kandidat 1.9.0 dan meminta aset serta penempatan sangat mirip [acuan Home v5](../../../design/concepts/home-al-aqsa-day-night-review-2026-10-25-v5.png), dengan bagian gulir dari v4. Home telah diizinkan untuk diimplementasikan; tidak meminta persetujuan ulang. Kandidat ini untuk tinjauan hasil koreksi, bukan klaim penerimaan visual atau rilis publik.

## Hasil aplikasi

- [Siang](native/Reference-sky-day.png), [malam/sabit](native/Reference-sky-crescent.png), [bulan penuh](native/Reference-sky-full-moon.png): tangkapan emulator Android sebenarnya, 720 × 1392, bukan gambar generatif baru.
- [Home awal pada layar tinggi](native/Refresh-home-top-100.png), [bagian bawah kosong](native/Refresh-home-empty-bottom-100.png), [agenda berisi](native/Refresh-home-agenda-100.png).
- [Teks 200%](native/Refresh-home-top-200.png), [agenda 200%](native/Refresh-home-agenda-200.png), [layar sempit 320 dp/200%](native/FoundationHomeLargeText.png).

Header memakai simbol resmi putih dan wordmark serif, lokasi kanan dengan pin/chevron, sapaan/tanggal, countdown dan jam berikutnya, masjid Al-Qibli serta strip lima salat dengan titik emas dan pembatas. Delapan pintasan menjadi dua baris pada ukuran normal, latar datar ivory; Ruang harimu/bacaan terakhir di bawah, dock tetap di dasar. Font Roboto dan penyesuaian dock dibatasi pada Home. Tahan header untuk informasi sumber langit; ketuk tanggal tetap membuka kalender.

Lapisan transparan masjid/awan direkonstruksi dari konsep melalui image_gen, **bukan ekstraksi lapisan asli atau hasil identik per piksel**. Detail bangunan, awan dan bentuk beberapa ikon masih dapat berbeda dari gambar generatif acuan. [Asal dan hash aset](assets.json). Master logo tidak berubah. Matahari/bulan/bintang dan parallax dirender terpisah; warna berubah menurut waktu setempat. Ilustrasi fase memakai data yang tersedia/perkiraan, bukan sertifikasi astronomi atau rukyat.

Countdown, jadwal, lokasi, bacaan terakhir, progres dan agenda pada aplikasi memakai data lokal sebenarnya. Sesudah Isya, strip mengikuti jadwal besok seperti header. Agenda maksimal tiga tugas belum selesai, diurutkan tenggat; kondisi kosong tidak memalsukan tugas. Semua angka pada screenshot ini adalah fixture uji terisolasi: Surabaya, Al-Kahf ayat 12, tugas sintetis, Clock 12:30/20:00 dan SkyDay dengan fase 0,18/1,0. Status bar tetap waktu sistem emulator. Fase sabit pada screenshot bukan klaim fase nyata 25 Oktober.

## Pemeriksaan terarah

[Checks](checks.json), [log build](logs/build.txt), [log native](logs/) dan [snapshot source](source-files.json):

- Build debug + APK helper berhasil, satu worker/heap 768 MiB; emulator dimatikan saat build. Lint **0 error, 721 warning**, bukan nol temuan.
- **5 unit** SkyDay/SkyPalette lulus, 0 gagal/skip. **8 eksekusi native, 5 metode unik** lulus: dua metode HomeRefresh pada font 100% dan 200%, urutan countdown/jam, kalender, layar sempit/teks besar, serta perbandingan langit pada rasio acuan.
- Gulir, bacaan terakhir, agenda kosong/berisi, tujuan Study/fokus, brand di bawah status bar, label dock terjangkau dan jadwal besok diperiksa.
- Gerak awan dibuktikan dengan perbedaan piksel pada screenshot layar nyata selama 4,8 detik: [100%](motion-100.json), [200%](motion-200.json). Area tetap identik ketika animasi sistem dimatikan. Render juga berhenti saat offscreen dan dilanjutkan saat terlihat. Ini bukan pengukuran baterai HP.

Harness tambahan `4c64d80` menunggu perubahan terang/gelap pada piksel layar nyata sebelum menangkap frame siang/malam. Pemeriksaan visual menemukan satu capture rasio acuan masih menampilkan frame sebelumnya walau state model sudah malam; capture diulang dengan guard ini. Source runtime APK tetap `6bdf360`; perubahan hanya pada helper pengujian. Log publik dinormalisasi CR/LF/spasi akhir, hash salinan asli privat dicatat di checks.

[Preservasi](preservation.json): sepuluh file data internal asli emulator dipulihkan dan cocok SHA-256; marker sementara dihapus, font 1,0 dan ukuran layar sebelumnya dipulihkan, helper dihapus, emulator ditutup. Kandidat ini **tidak dipasang pada HP fisik**. Backup asli privat tetap di `.local/`; tidak masuk laporan publik.

## APK dan batas

`Zikra-Home-v1.9.1-beta-debug.apk`: **34.740.137 byte**, di bawah 100.000.000 byte, SHA-256 `bb6f86b1723ebb85e89209e6cfaf71bfd442ff9299da68770cfcf86b6942392f`. [Receipt versi/source/signature](release.json). Sertifikat debug sama dengan baseline 1.8.1; SQLite tetap versi 4. APK 1.7.6/1.8.1 dan kandidat lama tetap disimpan. Install sebagai pembaruan; jangan hapus aplikasi lama/data.

[Audit](audit.json) memeriksa nilai rahasia yang diketahui pada file Git/source/bukti dan seluruh entri APK yang didekompresi: 0 kecocokan, 0 entri privat/helper. PROJECT_HANDOFF WIP, env privat, stash/arsip audio dan APK baseline tetap utuh. Ini pemeriksaan terarah, bukan sertifikasi seluruh riwayat Git.

UI04 keseluruhan tetap **[ ]**: koreksi Home sudah tersedia untuk ditinjau, desain/fungsi AI baru belum diterapkan. Multi-chat, mode otomatis, foto/file dan kreasi gambar tetap C03/C07–C09. C02/C03/C04/C06, B01, adzan D02a, uji HP/performa F/G dan rilis publik belum ditutup oleh pekerjaan Home ini.
