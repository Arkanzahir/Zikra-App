# Privasi dan batas lampiran — kandidat1.9.32

30 Oktober2026. [Laporan keamanan terarah](../../../security_best_practices_report.md)
memisahkan kontrol, satu perbaikan, dan gerbang publik yang masih terbuka.

Aturan backup legacy dan Android12+ ditambahkan pada manifest. Sembilan domain
penyimpanan dikecualikan pada cloud dan device-transfer; allowBackup tetapfalse.
Ini memperjelas niat tidak memindahkan data beta privat/credential instalasi
secara otomatis. Tidak mengubah DB, chat, izin lokasi atau berkas pengguna.
Tidak menambah layanan/paket/rotasi key atau memperbesar batas lampiran.

## Verifikasi

- [Build/lint](build.txt), [ringkasan](checks.json):assembleDebug berhasil,
  lint0 error/861 warning. Java runtime tidak diubah, unit/native lama tidak
  diulang atau dihitung sebagai hasil baru.
- [Audit source/APK](backup-policy-apk.json) melalui
  `tools/audit_backup_rules.py`:allowBackupfalse, dua referensi XML benar,
  sembilan pengecualian legacy/cloud/D2D benar-benar terkompilasi di APK.
  Helper awal mengharapkan format bool aapt2 lama; pembacaan diperbaiki agar
  menerima outputfalse dari tool saat ini. Bukan kegagalan aturan aplikasi.
- [30 backend terarah](backend-tests.txt),0skip/0gagal:konfigurasi/validasi
  pesan/tindakan/lampiran dan respons HTTP terkendali. Tidak mengirim data asli
  atau inference provider baru, bukan pentest produksi.
- [Audit APK](apk.json), [signer](signature.txt):45.420.244byte (<100MB),
  SHA256`d616f0e6c80e44388ce4fc5104dcea0c63194d74ee4ca069806107e9c6747a2c`,
  code59,sertifikat pembaruan sama,0 nilai credential privat dikenal dan dua
  audio cocok hash. Paket masih debug, bukan kandidat Play Store.
- [Preservasi](protected-state.json):WIP/stash/arsip/env/ikon/artwork tetap.
  HP tidak disentuh; versi terakhir terpasang tetap1.9.31. Backup/GPS/chat
  privat tidak masuk bukti publik.

APK lokal folder pembungkus:`Zikra-Privacy-v1.9.32-beta-debug.apk`.
Transfer OEM fisik dan mekanisme cross-platform baru belum diuji. Aturan sistem
bukan perlindungan terhadap root/komputer debug yang sudah diizinkan. Formal
kebijakan privasi/Data Safety/ketentuan usia/retensi seluruh layanan/G03 tetap
terbuka; tidak menyatakan seluruh APK kebal kebocoran.

Batas yang dikonfirmasi tetap satu file8MB,PDF3halaman,DOCX/TXT12.000karakter,
lampiran lokal25MB. Ini input lampiran; dokumen belum menjadi indeks RAG permanen.
C02/C07/B01/D02/D02a/E/F/G/publik tidak ditutup oleh checkpoint ini.

Source konfigurasi/helper `4f7aeaea9ad539da52a7d106fd60a8a9416c3504`; [receipt/hash source](receipt.json).
