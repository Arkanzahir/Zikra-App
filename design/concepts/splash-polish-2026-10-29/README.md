# UI07 — Polesan splash, 29 Oktober 2026

**Preview untuk ditinjau pengguna; belum masuk Android/APK.** Versi aplikasi
tetap1.9.24/code51 dengan splash400ms pada source`f42859d`. Permintaan aktif:
“coba poles splashnya terlebih dahulu”. Referensi ini memakai caveman dan
frontend-design; mempertahankan bulan, bintang, lembar Quran dan font asli.

- [Animasi GIF](splash-polished.gif), [MP4](splash-polished.mp4).
- [Storyboard](storyboard.png), [pose akhir](final.png).
- [Preview interaktif](preview/index.html), [screenshot browser](browser-desktop.png),
  [tampilan320px](browser-mobile.png), [bukti pemeriksaan](validation.json).
- [Kontrak gerak](motion.json), [sumber SVG/gerak bersama](preview/motion-core.mjs).

## Gerak yang diusulkan

Durasi800ms, dua tahap. Logo utuh sudah terlihat pada bingkai pertama agar
handoff dari splash sistem kelak dapat tetap mulus. Lembar Quran menekuk ringan
lalu kembali terbuka; bulan berputar maksimal4derajat dan mendapat sapuan garis
tipis. Bintang mengecil lalu berpendar sekali, tanpa menambah simbol baru.
Mulai420ms logo bergeser ke kiri, nama Zikra muncul mulai500ms; selesai800ms.
Pose akhir mengembalikan semua bagian ke geometri asli. Pine`#15332F`, putih,
dan pendar hangat kecil; Plus Jakarta Sans600 dari aset proyek yang sama.

Media menahan pose akhir400ms dan mengulang **hanya untuk tinjauan**.
Rencana native: sekali saat cold launch, tanpa hold/replay, tanpa menunggu
jaringan. Warm launch/recreation/pengaturan animasi mati/hemat daya mengikuti
guard native yang sudah ada, bukan janji hasil pengujian versi baru.

## Cara melihat

Sajikan hanya folder`preview/` melalui server statis pada127.0.0.1, lalu buka
alamatnya. Modul dan font lokal tidak memerlukan layanan luar. Tersedia Putar
ulang, Lihat perlahan(0,5×), empat fase, slider dengan keyboard dan Kurangi gerak.
Pembanding400ms adalah studi gerak splash saat ini, bukan rekaman pixel-identik
Android atau waktu launch sistem. Pilihan Kurangi gerak langsung memakai pose
akhir; animasi berhenti saat selesai/halaman tersembunyi.

## Pemeriksaan dan batas

Browser1100×900 dan320×900: font termuat, tidak melebar horizontal, tombol
utama48px. Fase160ms mengubah lembar Quran; pembanding400ms tidak mengubah
lembar; mode Kurangi gerak menunjukkan pose800ms dengan wordmark penuh.
Keyboard Home→ArrowRight menghasilkan10ms; putar lambat menunjukkan fase gerak.
Viewport sementara dipulihkan. Screenshot tersimpan dari browser nyata.

Pemeriksaan deterministik memastikan empat path sama dengan referensi asli,
semua nilai gerak finite, pose akhir pulih dan hold tidak mengubah pose.
GIF/MP4/storyboard berasal dari`svgAt()` yang sama dengan browser:36frame30fps,
resvg-py0.5.0, Pillow dan ffmpeg lokal. FontTools4.60.1 membuat instans600
privat untuk renderer; font sumber di preview tetap identik dengan aset asli.

Tidak menjalankan build/emulator atau mengubah HP untuk referensi ini. Tidak
ada uji handoff sistem Android baru atau penerimaan visual pengguna yang
diklaim. Ini bukan ekspor/canvas Figma. Seluruh status F04/A–D/HP/publik tetap
mengikuti checklist. WIP, stash audio, artwork, ikon dan konfigurasi privat
dipertahankan; video pribadi pengguna/MP3/APK tidak disertakan.
