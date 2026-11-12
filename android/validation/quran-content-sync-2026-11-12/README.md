# Fondasi protokol Content Sync Quran — 12 November 2026

**Tahap mesin server selesai terarah; migrasi APK belum selesai/diaktifkan.**
Modul baru belum diimpor handler produksi, belum ada route/relay publik, env
Vercel baru atau deploy. Tidak mengganti reader, unduhan, bookmark/progres,
Mushaf, sumber audio atau cache lama. APK tetap1.9.41; HP tidak diakses.

## Implementasi dan bukti

QuranContentSync menyiapkan generasi terpisah, memvalidasi snapshot dan urutan
incremental, lalu menerbitkan data/token akhir bersama di memori. Gagal/abort
meninggalkan generasi sebelumnya. Ini **bukan transaksi database durable**;
cold start masih perlu bootstrap. Filter hanya quran_core:1 dan translations:33.
Validasi kanonis memeriksa114surah/6236ayat/6236terjemahan dan pemetaan kunci.
Text asli tidak ditrim/ditulis ulang pada penyimpanan. Mutasi resource/row,
paging, batas halaman/record, duplikasi dan URL source diuji; state yang keluar
merupakan salinan agar caller tidak mengubah generasi yang telah diterbitkan.

Transport server-only memisahkan host OAuth/content tetap, menolak redirect
serta URL asing, mempunyai deadline45detik per request dan batas body, menjaga
credential/token hanya di memori. Snapshot maksimal64MiB, sync2MiB/auth16KiB.
Env produksi lokal dibaca tanpa dicetak, diubah atau dimasukkan ke Git/APK.

[17tes](tests.txt) lulus tanpa skip: rollback, pembatalan/pemulihan, paging,
checkpoint akhir, urutan, filter/path asing, boot tak lengkap, token dan batas
transport. [Akses awal](initial-access.json) auth200/bootstrap200.
[Uji akhir](live-engine.json): bootstrap kanonis memakai snapshot asli privat,
lalu satu incremental HTTPS produksi berhasil. Core90345record (termasuk114
surah/6236ayat/83665word), terjemahan33sebanyak6236record. Ini tidak memvalidasi
seluruh isi agama secara independen atau menguji mutasi penerbit di produksi.

## Temuan yang diperbaiki

Probe snapshot awal membatasi2MB dan berhenti; snapshot terjemahan lebih besar.
Batas probe privat ditingkatkan terukur, core asli35.246.389byte. File/korpus
utuh tidak masuk Git/APK. Semua URL uji ulang memakai URLSearchParams untuk
filter kanonis. Checkpoint probe awal dengan filter nonkanonis menghasilkan422
pada incremental ([catatan](initial-incremental-failure.json)); tidak dipakai lagi.

Bootstrap produksi mengurutkan enumerasi menurut resource, sehingga historical
sequence dapat turun (core548765 lalu translation548578). Validator awal salah
menerapkan urutan incremental ke bootstrap. Diperbaiki: boot hanya CREATE tiap
resource unik dan lengkap; incremental tetap harus meningkat. Regresi memakai
contoh struktur sintetis, tanpa memasukkan naskah Quran atau token asli.

## Batas dan lanjutan wajib

- Integrasi backend harus menambahkan akses instalasi/rate limit khusus konten,
  penanganan cold start/penyimpanan, dan respons ringkas sebelum deploy. Snapshot
  core35MB tidak boleh dikirim utuh melalui respons fungsi serverless.
- Hubungkan Android melalui jalur yang tidak membundel secret. Terapkan storage
  durable/generasi staging, refresh sumber, rollback/migrasi file lama dan uji
  native sebelum mengaktifkan jalur baru. Jangan menyalin seluruh snapshot ke
  memori HP sekaligus atau menjadikan mtime pembacaan sebagai waktu refresh.
- Endpoint pembaca, QuranDownloadService dan layout Mushaf masih jalur lama;
  audio binary dan font CDN terpisah. Jangan menyatakan migrasi selesai.
- Tombstone harus menyembunyikan resource yang dicabut; kebijakan validasi penuh
  dan availability aplikasi perlu diuji pada integrasi, bukan melayani generasi
  lama tanpa batas setelah resource dihapus.
- B01/hak audio/privasi/signing/migrasi distribusi/C02/D02/E/F/G tetap terbuka.

Kontrak primer: [Getting Started](https://api-docs.quran.com/docs/tutorials/content-sync/getting-started/)
dan [Developer Terms](https://api-docs.quran.com/legal/developer-terms/).
Bukti incremental memakai konten sumber umum, bukan chat/GPS/data pengguna.
Tidak menerima ketentuan, mengirim email, mengaktifkan biaya atau mengubah key.
