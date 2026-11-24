# Retensi audio sorotan kata dan review situs tanpa promo

24 November 2026. Source setelah APK signed 1.9.65. Kandidat publik masih HOLD.

Cache rekaman sorotan kata sekarang kedaluwarsa setelah enam hari dari waktu
penulisan. Rekaman kedaluwarsa dihapus saat aplikasi mulai, ketika akan dipakai
lagi, atau pada pemeriksaan berkala selama layanan audio berjalan. Pembersihan
dibatasi ke MP3 sementara di `cache/qcf-timed-audio`. Audio offline tersimpan
terpisah di `files/quran-audio`. Jika aplikasi tidak berjalan, pembersihan
menunggu saat aplikasi kembali memiliki kesempatan berjalan.

- 26 tes JVM audio lulus tanpa skip. Tes baru memeriksa batas waktu, penghapusan
  sebelum pengambilan ulang, dan isi berkas offline tetap sama.
- Dua tes Android API 36 lulus dalam 21,234 detik dengan WAV sintetis. Tes baru
  menghapus cache kedaluwarsa lalu memutar rekaman offline melalui pemutar asli.
  Regresi pemulihan ayat yang macet juga lulus. Tidak memakai HP atau audio qori
  langsung, dan belum ada APK signed baru.
- [Draf Ketentuan Penggunaan](../../../website/templates/terms.html) tersedia
  bersama draf privasi di paket review terpisah. `prepare-public-review.cjs`
  menghasilkan 21 berkas (1.568.482 byte), tanpa promo, audio, APK atau tautan
  unduh. Situs berstatus noindex dan unduhan ditutup. Preview terlindungi dengan
  video tetap terpisah. Tidak ada deploy.
- Pemeriksaan situs asal lulus. Edge headless memuat beranda, privasi, dan
  Ketentuan Penggunaan pada 320, 390, dan 1.440 piksel tanpa overflow horizontal.

Rujukan batas cache dan status izin ada di [audit sumber](../../../docs/beta/CONTENT_LICENSE_TRACE.md).
