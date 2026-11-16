# Website Zikra — pratinjau lokal

Revisi16November2026. Sumber statis di `dist`, tanpa build dependency, JavaScript, analytics, cookie, formulir, atau aset jaringan pihak ketiga. Jalankan server HTTP lokal dengan document root `website/dist`.

## Arah desain

Hijau pinus dan mint, Plus Jakarta Sans lokal, tampilan aplikasi asli sebagai fokus. Logo SVG menyalin tiga path dari `android/app/src/main/res/drawable/zikra_refined_pine.xml`; tidak digambar ulang. Wordmark mengikuti font aplikasi. Atas koreksi pengguna, simbol privasi lingkaran, orbit, dan kartu slogan melayang dihapus. Bagian privasi memakai teks serta tautan yang menjelaskan fungsi.

Susunan: hero, navigasi fitur, belajar/ibadah/Protector, galeri horizontal, privasi, status beta, langkah pemasangan, FAQ, dukungan. FAQ native HTML; galeri dapat digeser dengan sentuhan/keyboard. Fokus keyboard terlihat dan reduced motion dihormati.

## Referensi yang ditelaah

- https://thepillarsapp.com/ — hierarki aplikasi ibadah dan penjelasan fitur.
- https://www.beeper.com/ — pengenalan produk dan galeri aplikasi.
- https://signal.org/android/apk/ — informasi distribusi APK/signer.

Referensi dipakai untuk arah, bukan menyalin aset, testimoni, angka pengguna, atau klaim produk mereka.

## Asal aset

- `assets/zikra-symbol.svg`: geometri logo Android yang sama.
- `assets/plus-jakarta-sans.ttf`: font aplikasi; lisensi `assets/FONT-LICENSE.txt`.
- `home-day.png`, `quran.png`, `study.png`, `weather.png`: cuplikan emulator yang sudah tersedia di `android/validation/beta-final-2026-11-14/screens`. Data contoh; bukan rekaman ulang versi58, jadwal pengguna, atau bukti akurasi waktu salat.
- PNG lama dipertahankan untuk menjaga riwayat; brand website kini memakai SVG.

## Status distribusi

Tetap HOLD, belum dihosting publik, tanpa URL APK/auto-download. Tombol unduh disabled. Metadata kandidat mengikuti `docs/beta/internal-artifact.json` (55), bukan versi source58 yang belum menjadi paket publik. Situs tidak mengaktifkan akun atau mengubah backend/HP. AI18+ dijelaskan sebagai rencana yang belum diluncurkan.

Sebelum publikasi: finalkan paket/signer/migrasi, hak aset/konten, privasi/retensi/usia, uji akhir dan persetujuan distribusi. Setelah itu cocokkan URL HTTPS, ukuran dan SHA256 dengan paket final, lalu uji unduhan/pembaruan nyata.

## Validasi

Jalankan `D:/node.exe website/check.cjs` dari root repo. Bukti dan batas pemeriksaan browser di `validation/2026-11-16/README.md`. Pratinjau lokal sesi ini: http://127.0.0.1:64882/ (memerlukan server lokal tetap berjalan).
