# Adzan dari audio lokal — 15 November 2026

Kandidat1.9.57/code84, tetap HOLD. HP belum diperbarui pada tahap ini.

## Perubahan

- Pengguna dapat memilih MP3 sendiri melalui pemilih dokumen Android, terpisah antara Subuh dan adzan biasa. File disalin ke penyimpanan privat aplikasi; tidak diunggah, tidak dibundel di Git/APK, dan tidak memerlukan izin membaca seluruh penyimpanan. File sumber tetap utuh.
- MP3 satu track, 10detik–5menit, maksimal10MB. File harus sudah diunduh lokal dan dapat dicari posisinya; pipe/stream provider harus diunduh dahulu. Impor dikerjakan di worker dengan cancellation/timeout30detik, staging privat dan penggantian atomik setelah validasi. Input rusak/gagal/batal menjaga rekaman lama.
- Pilihan Adzan penuh kembali tersedia untuk waktu yang mempunyai audio. Pilihan ADHAN lama tetap disimpan dan dapat berlaku kembali setelah audio dipilih; izin OS dan pengingat utama tidak otomatis dinyalakan. Tanpa audio atau setelah salinan dihapus, cakupan publik kembali ke notifikasi. Subuh dan rekaman biasa independen.
- Preview, notifikasi pemutar dan Hentikan adzan memakai layanan lama. Batas pemutaran dihitung dari audio mulai, sehingga waktu persiapan tidak memotong rekaman mendekati5menit. Pembatasan mode senyap/DND/volume alarm/kelas/fokus tetap berlaku.
- Hapus salinan audio tersedia; tidak menghapus file asli atau pilihan mode. Cadangan lokal tidak membawa MP3: pilih ulang setelah reinstall/restore. allowBackup tetapfalse.
- Lint menemukan headingAPI28 pada perbaikan pengingat sebelumnya; atribut dipindah ke values-v28, tanpa menaikkan minSDK26 atau menonaktifkan lint.

## Penggunaan

Profil → Pengingat salat → Pilih audio adzan dari HP. Tentukan rekaman biasa/Subuh dan pilih MP3 lokal. Kemudian aktifkan pengingat utama, izinkan notifikasi/Alarm & pengingat Android, dan pilih Adzan penuh pada waktu yang dikehendaki. Tombol Hentikan adzan ada di notifikasi selama pemutaran. Android/OEM tetap dapat membatasi alarm; banner mengikuti izin perangkat.

## Bukti dan batas

Build debug/release,235JVM/0skip dan lint0error/911warning lulus. Sepuluh kasus native berbeda lulus pada APK release bertanda tangan yang sama:9kasus impor/validasi/fallback/preview+Stop (1,701detik), kemudian1alarm nyata melalui AlarmManager dan PrayerAlarmReceiver→audio lokal→notifikasi kanal adzan→Stop (6,513detik). Seluruhnya pada emulatorAOSP API36; helper uji ditandatangani privat, tidak dibagikan.

Uji awal provider fixture kehilangan authority sehingga7kasus gagal sebelum kode impor; fixture diperbaiki lalu9kasus lulus. APK release tidak diubah untuk mengatasi kegagalan fixture. Kontrol impor dan penjelasan file lokal dibuka melalui UI dan screenshot ditinjau. Pemilihan berkas langsung melalui SAF di HP belum diuji; native import memakai ContentResolver/provider fixture dengan decoder Android asli.

APK38.917.467byte, SHA256`fd3cdc9ebe6cb2b624d8a735f35b4a555c23056401f719547b1af45970e31903`; signer sama dengan55, backupdisabled, cleartextdisabled,0konfigurasi privat/0knowncredentialentry,0MP3bundel. Audit artefak lulus, distributionApprovedfalse. Data fixture/pengaturan uji dipulihkan; HP tidak dipakai. Tidak mengklaim uji suara fisik, reboot, hemat baterai, seluruh API26–36, atau rilis publik selesai. Tidak mengubah backend, hak distribusi rekaman bawaan, koleksi hadis tertunda atau gate usia/wali.
