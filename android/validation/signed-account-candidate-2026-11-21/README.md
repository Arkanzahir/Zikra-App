# Kandidat akun Zikra 1.9.58

Kandidat internal 21 November 2026. Berkas lokal `.local/releases/Zikra-v1.9.58-account-candidate.apk`. Versi 1.9.58, kode 85, ukuran 38.929.647 byte.

- Gradle `:app:assembleRelease :app:testReleaseUnitTest` selesai. Seluruh 237 tes JVM release lulus tanpa gagal atau dilewati.
- `-PzikraReleaseAdultAccounts=true` mengaktifkan akun dan login, dengan emulator mati. Build menolak konfigurasi klien Firebase yang hilang. Opsi tetap mati pada build release biasa.
- Audit memverifikasi tanda tangan yang sama dengan APK sebelumnya, identitas aplikasi, target API 36, dan ukuran di bawah 100 MB. Tidak ditemukan berkas konfigurasi privat, kecocokan rahasia yang dipindai, atau bundel MP3 adzan. [Receipt](audit.json).

Kandidat belum dipasang ke HP. Backend akun produksi belum aktif dan uji pemasangan, pembaruan data, serta alur akun produksi masih terbuka. Akses AI saat ini masih memerlukan token instalasi yang diterbitkan terpisah. Status rilis HOLD.
