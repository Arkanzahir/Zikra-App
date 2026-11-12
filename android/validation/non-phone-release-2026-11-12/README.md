# Audit lanjutan tanpa HP — 12 November 2026

Bukti source ada pada source-checks.json. Tidak mengubah APK/runtime/backend,
keystore, TTL produksi atau data pengguna. Tidak mengakses HP/emulator, membuat
credential, menerima ketentuan, mengirim email atau menerbitkan website.

## Kontak dan privasi

Pengguna mengizinkan arkanzahir46@gmail.com tampil sebagai email dukungan. Nama
aplikasi tetap Zikra; identitas penerbit legal tidak ditebak. Draf privasi dan
catatan beta diperbarui. Tabel retensi kini dibedakan antara TTL kode dan retensi
penyedia yang belum terverifikasi. Registry tidak otomatis kedaluwarsa; revoke
bukan delete. Semua angka merujuk source, bukan inspeksi data Redis pengguna.

## Quran Foundation: hasil sumber primer dan batas kesimpulan

[Developer Terms](https://api-docs.quran.com/legal/developer-terms/), diperbarui
11 Oktober 2026, membolehkan font/gambar Mushaf terintegrasi dengan syarat akun
developer aktif dan atribusi. Izin itu berbeda dari aturan offline konten yang
tersedia melalui Content Sync. Akun/atribusi dan sumber pihak ketiga tetap perlu
verifikasi. [FAQ](https://api-docs.quran.foundation/docs/tutorials/faq/) memperjelas
bahwa metadata audio dan berkas rekaman bukan hal yang sama.

Temuan kode: QuranRepository membaca unduhan offline/JSON cache tanpa umur
kedaluwarsa, memakai verses/by_chapter dan translation33; akses cache memperbarui
mtime. MushafRepository memakai metadata API biasa dan font CDN QCF/nama surah.
Maka kesimpulan audit: jalur offline perlu migrasi, bukan sekadar label sumber
atau menghapus cache setiap tujuh hari. Ini bukan klaim penerbit mencabut izin.
Tidak menghapus atau mengubah isi bacaan/terjemahan/unduhan lama.

[Tanzil](https://tanzil.net/docs/text_license) memiliki izin teks bersyarat, tetapi
izin itu tidak otomatis berlaku untuk semua data Quran.com, terjemahan33, font
QCF, timing atau rekaman. Tidak mengganti sumber diam-diam. Gading B01 dan dua
rekaman adzan masih memerlukan bukti terpisah sesuai laporan sebelumnya.

## Implementasi berikutnya: migrasi luring yang harus dibuktikan

Rujukan teknis [Content Sync](https://api-docs.quran.com/docs/tutorials/content-sync/getting-started/).
Rencana implementasi, belum dijalankan:

1. Periksa akses produksi/resource yang tersedia memakai konfigurasi privat;
   pisahkan lingkungan prelive/production. Tidak membundel client secret di APK.
2. Identifikasi resource Quran core, terjemahan33, layout dan qari yang benar;
   jangan menganggap ID ayah audio sama dengan ID chapter recitation.
3. Unduh snapshot ke staging; validasi schema, hitungan/identitas, ukuran dan
   teks sumber tanpa normalisasi yang mengubah bacaan. Impor satu transaksi.
4. Simpan token hanya setelah semua halaman/snapshot berhasil diterapkan;
   bookmark/progres tetap memakai identitas surah/ayat dan tidak dihapus.
5. Terapkan perubahan dan refresh sumber sesuai protokol; jangan menganggap
   membaca cache berarti memperbarui isi dari penerbit.
6. Uji putus koneksi, rollback, token/cursor salah, resource dihapus, snapshot
   gagal dan migrasi data lama. Sediakan preservasi unduhan serta rollback.
7. Bedakan font/CDN dari konten JSON dan berkas audio. Atribusi tampil jelas;
   jangan mencentang gerbang hanya karena download mengembalikan HTTP200.

## Persiapan paket rilis

Guard signing sudah ada, tetapi sertifikat distribusi belum dipilih/dibuat.
Tidak memakai debug key sebagai release atau membuat identitas baru diam-diam.
Release pertama dengan signer baru tidak dapat menimpa debug di HP ber-ID sama.
Jangan uninstall. Pilihan aman: perangkat penguji baru untuk release pertama,
atau migrasi/export-import data yang sudah diuji sebelum perpindahan identitas.
Audio privat hanya source set debug; release belum setara fitur dan belum berizin.

## Status nyata

- [x] Kontak dukungan dicatat sesuai izin pengguna.
- [x] Draf privasi/retensi berbasis source dan catatan beta diperbarui.
- [x] Audit syarat font bundel dan jalur QF, temuan serta rencana migrasi tersimpan.
- [ ] Implementasi migrasi Content Sync dan verifikasi atribusi/akun.
- [ ] Hak konten/audio yang belum memiliki bukti.
- [ ] Identitas penerbit, kebijakan publik final, signing dan migrasi distribusi.
- [ ] Sisa C02/D02/HP serta gerbang E/F/G.

Audit selesai pada lingkup ini; pekerjaan tanpa HP belum seluruhnya selesai.
Tidak ada klaim beta/publik siap. Jangan menutup G03/G04 atau A–D dari laporan ini.
