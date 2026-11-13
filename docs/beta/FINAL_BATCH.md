## Update14November2026

Kandidat internal1.9.47,debug/HOLD. [Status terkini](STATUS.json), [handoff](../../ACCOUNT_HANDOFF.md), [bukti27native](../../android/validation/beta-final-2026-11-14/README.md). Website lokal desktop/mobile teruji. Rincian dan gerbang lama hanya berlaku sejauh belum dikoreksi checkpoint ini; tidak menutup HP/signing/hak/putusan beta.

## Update verifikasi dan pindah akun

Lihat [handoff akun](../../ACCOUNT_HANDOFF.md) dan [bukti terbaru](../../android/validation/final-batch-2026-11-13/README.md). Uji terarah sudah dijalankan; matriks lengkap/HP dan gerbang rilis tetap terbuka. Website pratinjau lokal tersedia, belum publish.

# Satu batch penutupan beta

Status13November2026: implementasi terbaru di APK internal1.9.46/code73;
49.511.880byte. Tidak membuat APK baru hanya karena dokumen diperbarui.
Uji runtime ditunda pengguna. Manifest artefak [internal](internal-artifact.json)
selalu HOLD dan tidak membawa tautan unduhan. Website publik menunggu J14.

## Yang telah tersedia

- J01: Content Sync hosted reader/Mushaf/locate200, akses401, kuota429,
  pencabutan401 serta cleanup identitas sintetis pada receipt yang tercatat.
- J02/J03: reader/download/generasi atomik, font tervalidasi sebelum penggantian,
  retensi file lama, pemetaan604halaman, pengaman storage/ukuran/pembatalan.
- J08: pengingat cuaca/harian opt-in, ringkasan lokal, kategori Doa dan deduplikasi.
- J09: bantuan/dialer/draf tanpa kirim otomatis, navigasi web/geo/salin manual.
- J10/F01: panduan visual; guard viewport/foreground/power/screen/sensor.

Ini membedakan kode yang sudah dipaketkan dari bukti runtime. Daftar ini tidak
menutup seluruh kriteria J02/J03/J08/J09/J10 atau parent A–G.

## Langkah tanpa HP berikutnya

1. Tinjau bersama cacat source/konsistensi F02–F03 menurut APP_GUIDE; hindari
   perombakan desain diterima. Perbaikan fungsional baru dikumpulkan sebelum
   membangun kandidat berikut, bukan versi baru untuk tiap perubahan dokumen.
2. Tutup matriks ketentuan/atribusi layanan J04 dan prosedur operator J07/J11.
   Retensi penyedia, hak konten dan identitas penerbit memerlukan bukti,
   bukan disimpulkan dari kode atau paket gratis.
3. Ketika kode stabil: satu build, audit paket dan uji otomatis/emulator gabungan
   dengan skenario pada tiga receipt implementasi terbaru. Jangan menyebut
   langkah ini lulus sebelum dijalankan. Tidak mengaktifkan pengingat penguji.

## Yang tidak dapat diselesaikan dengan kode saja

| Gerbang | Kebutuhan konkret |
|---|---|
| J05/B01 | Klarifikasi edisi/hak/kelengkapan Bukhari–Muslim atau keputusan perubahan cakupan; HadeethEnc opsional bukan pengganti diam-diam |
| J06 | Bukti kewenangan distribusi dua rekaman adzan dan penelaahan lafaz |
| J11 | Identitas penerbit final dan retensi/proses penghapusan penyedia yang disahkan; email dukungan telah diberikan |
| J12 | Signer release tetap/cadangan privat dan migrasi data dari debug; jangan uninstall pengguna |
| J13 | Satu sesi HP terkoordinasi: update terisi, C02/D02 dan smoke lintas fitur pada hash kandidat yang sama |
| J14/J15 | Putusan beta setelah bukti lengkap; baru tautan unduhan/instal kanal nyata dan publikasi promo berizin |

Tidak mengirim email, menerima ketentuan, membuat signer, menambah biaya atau
menjalankan tindakan darurat atas nama pengguna untuk menutup daftar ini.

## Satu sesi verifikasi gabungan

Gabungkan: Quran online→download→offline→refresh, batal/korupsi/storage dan
preservasi bookmark; Mushaf/font/ayat/audio; cuaca/pengingat opt-in/opt-out,
pergantian lokasi/hari/zona dan duplikat; darurat/draf tanpa submit; peta kosong/
gagal/cache; seluruh layar terang/gelap/teksbesar/keyboard; motion0/hematdaya/
screenoff/scroll/restore. Pengukuran baterai/sensor serta sisa alarm fisik
ditangani J13. Tidak mengulang reboot yang sudah terbukti tanpa alasan baru.

Jika gagal, perbaiki penyebab dan ulangi bagian terdampak; jangan mengulang
seluruh proyek atau menganggap bukti versi lama berlaku otomatis untuk APK baru.
