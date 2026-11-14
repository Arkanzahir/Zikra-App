# Cadangan lokal dan migrasi tanda tangan — 14 November 2026

Kandidat internal **1.9.50/code77**, **50.316.831 byte**, SHA256
`1e52d94689625726ed02e0d3e2b2742e0d0ac8a711e6412f6a236140fd70ba4d`.
APK lokal `D:/Download/Zikra-App-main/Zikra-Backup-v1.9.50-beta-debug.apk`.
**Debug/HOLD, bukan distribusi publik. HP tetap1.9.48 dan tidak diakses sesi ini.**

Pengguna memilih cadangan lokal sebelum perpindahan debug→release. Website
masih ditunda. Tidak membuat signer rilis, uninstall HP, mengubah server,
menyetujui hak/ketentuan, atau mengaktifkan backup cloud F06.

## Perubahan

- Profil → Cadangan lokal memakai pemilih dokumen Android, tanpa izin seluruh
  penyimpanan. Pengguna memilih tujuan; penyedia berkas dapat menyinkronkan
  berkas ke cloud. Tidak ada upload otomatis oleh modul ini.
- Format ZIKRAB01: PBKDF2-HMAC-SHA256600.000iterasi, salt16byte, AES256-GCM
  dengan nonce perchunk64KiB, AAD header/index/panjang, tag16byte.
  Password12–128karakter, tidak disimpan/dikirim/log; field tanpa autofill/
  personalized learning/save-state dan layar sensitif FLAG_SECURE.
- Snapshot logis lima DB/tabel pribadi, empat grup preferensi positif, draft
  dan media chat. Database sumber tidak ditimpa. Migrasi legacy chat memakai
  jalur idempoten existing dan tetap mempertahankan preferensi asal.
- Bukan dump SQL: skema dari helper aplikasi, kolom/versi harus cocok, nilai
  insert terikat, pemeriksaanintegritas dan urutan AUTOINCREMENT dipertahankan.
- Token/endpoint/consent AI, keystore, OSgrants, cache, paket Quran/Hadis dan
  audio unduhan tidak diekspor. Pilihan waktu adzan boleh dipindahkan, tetapi
  `reminder_enabled=false`; persetujuan dan izin harus ditinjau lagi.
- Arsip menolak path traversal/symlink/entri asing/duplikat/ukuran berlebih;
  snapshot8MiB/50.000baris, ekspansi total96MiB/12.000berkas. Decode media
  memeriksa ukuran, MIME, dimensi, metadata dan relasi chat. Orphan chat yang
  sudah dihapus dikecualikan dari ekspor, ditolak pada impor.
- Impor tervalidasi penuh pada staging privat; instalasi berdata hanya dapat
  memeriksa. Restore memerlukan instalasi kosong dan Android9/API28+.
  Android8 tetap dapat ekspor/periksa, belum dapat mengaktifkan restore ini.
- Permintaan restore menyegel hash staging dan fingerprint data tujuan.
  Aktivasi baru berjalan saat peluncuran berikutnya, dengan jurnal/rollback,
  cadangan file lama, guard DB/jobs/receiver, dan pengalihan entry aktivitas.
  Seluruh layar normal tetap terblokir sampai proses pemulihan ditutup.
  Buka sekali lagi setelah selesai agar cache SharedPreferences dimuat bersih.
- Stage gagal langsung dibersihkan; sisa akibat proses mati dibersihkan
  oportunistik setelah24jam saat startup berikutnya, hanya marker milik modul,
  umur yang cukup dan tanpa pending. Tidak menjanjikan secure erase flash.

## Bukti

| Pemeriksaan | Hasil | Lingkup |
|---|---|---|
| JVM seluruh proyek |224lulus,0gagal/0skip|17metode migration +207existing; fixture Quran/Hadis/Doa tersedia|
| Native snapshot |8metode lulus|Lima DB, pref, draf/urutanID, schema/type/JSON, wrongpassword, fresh/changed target, cleanup, media JPEG/PNG+metadata|
| Native lintas signer pada kandidat |2metode lulus|Prepare restore lalu verify setelah dua proses baru; ekspor fixture1metode dari pengembangan sebelumnya bukan tambahan hitungan kandidat|
| Jurnal JVM |Termasuk6metode dari224|Gangguan setelah rename, resume/rollback, file tujuan berubah, sidecar hilang dan ownership|
| Lint |0error/894warning|Warning lama dan advisori; tidak dianggap bersih seluruhnya|
| Audit APK |0entri credential dikenal/berkas privat|Signer debug tetap, kedua hash audio tetap, di bawah100.000.000byte|

Fixture sintetis dienkripsi oleh tanda tangan asal
`c7888fe2ca40e14698726ed20b2f93947033cef0bdee9cfd127c429d0ea0ca1b`.
Instalasi emulator saja dihapus, lalu kandidat diberi sertifikat uji sekali pakai
`56786df615929492abe6175b0fdb96812de9d9fc65a96eb848b0da726eb5ff5c`.
Varian itu identik isi ZIP dengan APK kandidat, kecuali entri tanda tangan.
Prepare menulis pending; force-stop→Splash menjalankan recovery dengan hasil
`restored`; force-stop kedua→verifikasi membaca profil, tugas, chat+draf dan
reminderfalse, tanpa token/consent lama. Hash fixture/varian di receipt.json.
Kunci uji bukan kunci rilis; sertifikat publik bukan rahasia.

Emulator ZikraMigrationQA API37.1 memakai ADB terpisah5042/loopback5583,
bukan HP. Image emulator memaksa4GB walau permintaan konfigurasi1536MB;
jangan menyebut aktual1536MB. [Screenshot](backup-screen.png) berisi halaman
contoh; FLAG_SECURE hanya dilepas melalui instrumentation emulator untuk capture,
bukan pada source produksi.

Kegagalan awal disimpan privat: marker fixture belum dibuat menyebabkan6gagal
setup (bukan kelulusan produk); setelah diperbaiki5/6lulus karena test mengharapkan
GeneralSecurityException sedangkan cipher sengaja membungkus IOException.
Ekspektasi test diperbaiki; kemudian6/6lulus. Dua lint failure konstanta orientasi
numerik dan akses kelas AndroidX restricted diperbaiki memakai konstanta/API
publik, bukan disuppress. Akhir8/8 +2portability kandidat lulus.

## Batas dan berikutnya

- Belum uji HP, OEM/provider dokumen riil, jalur picker/password/cancel/grant
  end-to-end, UI dark/200% khusus halaman ini, atau final release non-debug.
  Screenshot dan core instrumentation bukan pengganti bukti tersebut.
- Uji gangguan rename JVM bukan actual kill/powerloss/ENOSPC saat restore
  native. Tidak menjamin semua vendor/future DBschema; mismatch ditolak.
- Minimal ruang sementara sekitar400MiB; kebutuhan aktual dapat lebih besar
  untuk data mendekati batas. Kegagalan ruang tidak mengizinkan overwrite live.
- Tidak transfer file PDF/DOCX asli di luar aplikasi; hanya preview/teks/media
  yang memang tersimpan dalam chat. Penanda Hadis paket perlu sumber yang sama
  diunduh lagi agar entri dapat ditampilkan.
- [Panduan](../../../docs/beta/LOCAL_BACKUP.md); signer final, praktik restore HP,
  B01/hak/audio, privasi remaja13–17/retensi, J13/J14 dan beta tetap terbuka.
  Hasil Quran1.9.49/HP1.9.48 tetap historis sesuai versi, tidak dijumlahkan.
- Bukti mentah privat `.local/local-migration-20261114`; tidak commit arsip,
  password, private key atau dokumen pengguna. WIP/stash/audio/privat dipertahankan.

## Penutupan lingkungan

Emulator/dataE:/Zikra-MigrationQA-20261114 dan dua privatekey uji sekali pakai
sudah dihapus. Penutupan console awal tersangkut; proses headless milik tugas
dihentikan setelah pemeriksaan PID/executable/waktu mulai. SDK/cache umum tidak dihapus; ADB
utama, HP, aplikasi pengguna dan konfigurasi privat tidak dihentikan/diubah.
Tujuh hash konfigurasi privat, WIP PROJECT_HANDOFF, arsip stash dan kedua aset
logo/masjid cocok baseline; stash35686be tetap.
