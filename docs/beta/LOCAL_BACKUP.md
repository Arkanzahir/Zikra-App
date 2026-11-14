# Cadangan lokal Zikra beta

Implementasi internal1.9.50, belum izin merilis APK. [Bukti dan batas](../../android/validation/local-migration-2026-11-14/README.md).

## Sebelum mengganti instalasi

1. Gunakan Profil → Cadangan lokal → Buat cadangan pada versi yang mendukungnya.
   Pilih folder Download atau tujuan lain yang kamu pahami; penyedia dokumen
   dapat menyinkronkan ke cloud. Zikra tidak mengirim cadangan otomatis.
2. Buat kata sandi12–128karakter yang kuat dan simpan terpisah. Kata sandi tidak
   bisa dipulihkan oleh pengembang. Jangan kirim kata sandi atau cadangan ke chat.
3. Tunggu konfirmasi tersimpan dan berhasil dibuka ulang. Periksa berkas yang
   sama melalui Periksa atau pulihkan cadangan. Instalasi berdata hanya akan
   memvalidasi, tanpa menimpanya. Jangan mengubah data saat ekspor berlangsung.
4. Simpan salinan berkas dan kata sandi secara aman di luar direktori privat
   aplikasi. Jangan menghapus aplikasi lama hanya karena file sudah muncul.
5. Migrasi HP harus dikoordinasikan sesudah APK bertanda tangan final tersedia
   dan prosedur pemulihan dipastikan. Saat ini jangan uninstall HP: kandidat
   masih debug dan belum diuji lewat pemilih dokumen pada HP.

## Pada instalasi tujuan kosong

1. Android9atau lebih baru, versi tujuan tidak lebih lama dari pembuat cadangan,
   dan skema data harus masih kompatibel. Sediakan minimal sekitar400MiB ruang
   sementara; data besar dapat memerlukan lebih banyak ruang.
2. Pilih cadangan, masukkan kata sandi, lalu baca jumlah catatan/media.
3. Siapkan pemulihan hanya bila instalasi belum berisi data pribadi. Tidak ada
   merge otomatis atau overwrite atas data yang sudah dibuat di instalasi baru.
4. Tutup Zikra memakai tombol yang tersedia, buka kembali, tunggu pemulihan.
   Tutup sekali lagi setelah berhasil, lalu buka agar seluruh preferensi dimuat
   dalam proses baru. Jangan membuka layar lain selama pemulihan.
5. Periksa data sebelum membuang salinan lama. Tinjau ulang izin, akses/consent
   AI dan pengingat salat. Pengingat tidak otomatis diaktifkan oleh cadangan.

Jika salah password, file rusak, tujuan berubah atau ruang tidak cukup, jangan
hapus data aplikasi. Pemulihan ditolak atau mencoba rollback tanpa menimpa data
baru. Bila layar pemulihan meminta coba lagi, pertahankan instalasi dan staging
sampai masalah diperiksa; jangan mengirim cadangan pribadi ke dukungan sembarang.

## Isi dan pengecualian

Termasuk profil/preferensi yang disetujui, tugas, jadwal kelas, Hifz/jurnal,
penanda bacaan/Hadis, chat dan draf, preview/teks lampiran serta gambar tersimpan.
Metadata legacy chat disertakan agar migrasi lama tidak diulang.

Tidak termasuk password/token/endpoint AI/keystore/consent, izin Android,
langganan push, cache, paket Quran/Hadis atau audio unduhan. PDF/DOCX asli yang
berada di folder lain tidak disalin; hanya hasil yang tersimpan di chat.
Penanda Hadis dapat memerlukan paket asal diunduh kembali. Tidak ada login,
cloudbackup/sinkronisasi otomatis atau pemindahan kunci Android Keystore.

Format dienkripsi dengan PBKDF2-HMAC-SHA256600.000iterasi dan AES256-GCM perchunk.
Snapshot8MiB,50.000baris dan ekspansi total96MiB/12.000file dibatasi. Password
terlupa atau cadangan terpotong tidak dapat dilewati. Bukan janji perlindungan
terhadap perangkat yang sudah dikuasai penyerang; database chat aktif tetap
menggunakan penyimpanan privat Android, bukan enkripsi DB tambahan.
