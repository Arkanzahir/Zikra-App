# UI07 — Splash800ms disetujui dan diterapkan

29 Oktober2026. **Beta1.9.25/code52**, setelah pengguna menyetujui polesan
800ms: “gas implementasikan dah bagus itu”. [Referensi asli](../../../design/concepts/splash-polish-2026-10-29/README.md).
Selesai teknis beta terarah pada HP API36, bukan penutupan seluruh A–D/F04/publik.

## Hasil

- Empat bagian vektor asli: lembar Quran bergerak ringan, sapuan tipis pada
  bulan, bintang berpendar sekali lalu logo bergeser dan nama Zikra muncul.
  Waktu fase/easing mengikuti referensi800ms, tanpa400ms hold media preview.
- Ikon pertama tetap utuh untuk handoff splash sistem Android12+. Gerak bagian
  tetap berjalan sesudah cover sistem dilepas. Font asli600 dan pine dipertahankan.
- Tetap satu animator/satu navigasi: warm launch/recreation melewati intro;
  kebijakan animasi sistem/penghemat daya dan pembatalan saat pause/fokus hilang
  dipertahankan. Path, Paint, PathMeasure dan shader disimpan; tidak menambah
  bitmap/video/model atau jaringan ke APK. Durasi aktual launch juga dipengaruhi
  splash/animasi sistem;800ms adalah durasi nominal animator aplikasi.

## Bukti nyata

[Rekaman HP MP4](phone-splash.mp4), [GIF](phone-splash.gif),
[fase Canvas Android](native-phases.png), [hasil native](native-results.json),
[unit/lint](checks.json), [APK/audit](apk-audit.json), [preservasi](preservation.json).

Empat metode native lulus pada HP pengguna/API36: Canvas enam fase0/90/160/
340/520/800ms dengan perbedaan piksel dan pose awal/akhir, cold/warm menuju
satu Home, pause tidak menavigasi di background lalu resume sekali, serta
animator sistem0 melewati intro. Helper hanya membaca tampilan/lifecycle;
tidak menanam fixture atau mengubah profil, chat, jadwal dan lokasi.

Video berasal dari cold launch HP asli; bagian launcher/aplikasi lain, status
bar dan Home yang dapat berisi data pribadi dikeluarkan. GIF mengulang untuk
tinjauan, bukan perilaku produksi. Gambar fase terpisah adalah render View
Android pada Canvas dalam uji native, bukan screenshot seluruh Activity.
Full launch ditinjau privat: tidak ada emblem besar bayangan bersama logo kecil.

Build APK/helper berhasil, **155 unit lulus/4 skip/0 gagal** dan **lint0 error/
862 warning**. Tidak mengklaim lint bersih. Penghemat daya/recreation/API26–30
tidak diuji ulang pada HP sesi ini; guard sama dengan UI06 dan gerbang F04/G02
tetap berlaku. Tidak menyalakan emulator atau mengubah font/jaringan/izin HP.

## Pembaruan dan preservasi

HP dari1.9.22 aktual diperbarui langsung ke1.9.25 tanpa uninstall utama.
APK sebelumnya dan32 berkas internal dicadangkan privat sebelum pemasangan;
seluruh berkas asli tetap ada, isi17 tabel dan nilai preferensi lama terjaga,
berkas lain cocok byte. Izin, pengaturan sistem dan kebijakan baterai sama.
Pengaturan animasi dipulihkan persis: pemeriksaan awal membedakan string1 dan
1.0 walaupun nilai sama; final cocok. Marker/helper dan gambar uji di HP sudah
dibersihkan. Zikra dibuka kembali; pengguna kemudian boleh mencabut USB.
Tidak mengembalikan backup lama di atas data pengguna yang sedang dipakai.

WIP PROJECT_HANDOFF, stash/arsip audio, konfigurasi Featherless privat dan
artwork cocok hash awal; vektor ikon/logo tidak diubah. Rekaman biasa dan Subuh
tetap sama dengan pilihan terakhir. APK seluruh entry didekompresi untuk mencari
nilai kredensial privat yang diketahui:0temuan. APK/MP3/DB/backup/chat/GPS/raw
video pribadi tetap di luar Git.

## Paket dan batas

APK lokal folder pembungkus: `Zikra-Splash-v1.9.25-beta-debug.apk`.
**43.638.534byte**, SHA256`a7f54e6233ef42ba8b12266099de47e8532bafa0d2d3e1b31924468cbadcf583`.
Signer debug sama: `c7888fe2ca40e14698726ed20b2f93947033cef0bdee9cfd127c429d0ea0ca1b`.
Unduhan APK langsung<100.000.000byte. Ini bukan signed release publik.
Source checkpoint diikat oleh receipt sesudah commit kode; dokumentasi tidak
mengubah APK.

Pengguna menanyakan lampu saat sesi ini. [Crop fasad dari HP](phone-mosque-lights.png)
menunjukkan cahaya pilar/pantulan UI06 sudah ada pada versi baru. **Interior
lengkungan tetap gelap**, belum dibuat bercahaya seperti referensi; jangan
menyebut interior selesai. Pengguna juga menanyakan font Arab: Mushaf memakai
QCFv2 per halaman, mode ayat/terjemahan belum menetapkan font Arab khusus
(mengandalkan fallback Android). Penyelarasan font/interior tidak diterapkan
diam-diam dalam checkpoint splash ini.

B01 menunggu sumber Hadis; C02 laptop-mati/C04/C07, ketentuan dan sisa HP
D02/D02a serta gerbang E/F/G tetap terbuka. USB sesi ini bukan bukti semua
uji fisik sudah selesai. Referensi Figma lama juga tidak dinyatakan selesai.

## Source/push

Source`231a6a089689440a8d20b41969a642b0e16e92a9` sudah dipush dan hash
origin/main cocok. [Receipt](receipt.json) mengikat APK/source/uji. Audit22blob
baru/1.261.636byte terhadap nilai privat yang diketahui dan pola kredensial:
0temuan. Commit receipt berikutnya hanya dokumentasi; APK tidak berubah.
