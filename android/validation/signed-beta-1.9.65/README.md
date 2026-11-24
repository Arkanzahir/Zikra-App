# Kandidat signed Zikra 1.9.65

24 November 2026. Kandidat standar code92 berstatus **HOLD**. Sumber APK
berasal dari arsip Git `24986c83f13a90ef9ee212ee8c65294a05232613` dan perubahan
versi di `android/app/build.gradle` pada checkpoint ini. WIP workspace dan
konfigurasi privat tidak disertakan dalam arsip. Build memakai akun 18+,
dengan Auth Emulator dan Hapus Akun nonaktif.

| Pemeriksaan | Hasil |
|---|---|
| Signing dan paket | Audit lulus. Signer sama dengan 1.9.64. Tidak debuggable, target Android 36, di bawah 100 MB, 0 berkas konfigurasi privat dan 0 kecocokan rahasia yang dipindai |
| Adzan bawaan | SHA-256 kedua MP3 cocok dengan [kredit APK](../../app/src/main/assets/bundled-adhan/CREDITS.txt). Rekaman adzan internal lama tidak ikut |
| JVM dan lint | 246 tes lulus tanpa skip. Lint 0 error dan 913 warning |
| Upgrade Android API 36 | Instal signed 1.9.64, simpan empat nilai baca sintetis, update `adb install -r`, periksa ulang nilai dan waktu pemasangan awal. Satu tes sebelum update dan satu sesudah update lulus |
| Murottal sesudah update | Dua tes native lulus dalam 23,020 detik. Putar mengulang ayat 2 setelah posisi macet. Tombol Berikutnya dan kontrol ikuti ayat berfungsi pada WAV sintetis |
| Saluran unduhan lokal | HEAD dan GET HTTPS loopback 200 dengan sertifikat uji terverifikasi, jenis paket APK, 41.995.490 byte dan SHA-256 sesuai manifest |
| Paket website | Pemeriksaan aset, caption, dan metadata 1.9.65 lulus. Paket Preview lokal 26 berkas, 5.846.908 byte, tanpa APK |

APK privat ada di `.local/releases/Zikra-v1.9.65-beta-candidate.apk`. Ukuran,
SHA-256 dan sertifikat tercatat di [manifest kandidat](../../../docs/beta/internal-artifact.json).
Audit menemukan audio berlisensi untuk dua rekaman adzan bawaan.
[Adzan umum CC0](https://commons.wikimedia.org/wiki/File:Beautiful_adhan.ogg).
[Adzan Subuh CC BY 4.0](https://github.com/nandxorandor/Athan/blob/ad2bf1799add51cca4aaf1f123b1c212378c7786/README.md#licence).

Uji tidak memakai HP, data pengguna, atau streaming qori nyata. Penyimpanan
Preview terpisah, uji hosted akun, persetujuan privasi, hak konten/promo, media
pemulihan signing dan URL publik masih terbuka. Lihat [persyaratan rilis](../../../docs/beta/PUBLIC_RELEASE_BLOCKERS.md).
