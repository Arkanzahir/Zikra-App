# Website Zikra — pratinjau lokal

Revisi produk16November2026. Situs statis di `dist`; tanpa framework, JavaScript, analytics, cookie, formulir atau aset pihak ketiga. Belum dihosting publik dan tidak menawarkan APK. Status aplikasi tetap HOLD.

## Desain sekarang

Logo memakai geometri vektor Android yang sama (`zikra_refined_pine.xml`), font Plus Jakarta Sans lokal dengan lisensi tersimpan. Pembuka di tengah, video promo52detik sebagai demonstrasi utama, lalu tiga bagian fitur dengan screenshot besar: Belajar, Ibadah dan Protector. Navigasi jangkar, privasi, status paket, pemasangan dan FAQ tetap tersedia. Tidak ada aksen huruf miring, kartu slogan melayang, orbit atau simbol privasi dekoratif.

Referensi ditelaah sebelum perubahan:
- https://culturedcode.com/things/ — identitas aplikasi dan pengenalan produk langsung.
- https://localsend.org/ — hierarki fungsi, navigasi dan unduhan yang jelas.
- https://ente.com/ — demonstrasi produk untuk menjelaskan fitur.

Referensi sebelumnya (Pillars/Beeper/Signal) merupakan arah iterasi pertama; desain sekarang mengikuti koreksi pengguna. Tidak menyalin aset, testimoni, statistik atau klaim produk lain.

## Promo lokal

Pengguna meminta memasukkan promo terakhir yang disetujui: `Zikra-Promo-Lengkap-52s-Tanpa-Awan.mp4`,52detik,1280×720,3.694.204byte. Sumber berada di parent repo. SHA256: `347d985b0ca9f2c7e12c33c148d2163661c32af0791c05cd631d24ffc4bb2407`. Poster dari frame lokal review-0267, naskah dari `design/promos/2026-11-12/script-approved.txt`.

`D:/node.exe website/prepare-preview.cjs` memeriksa hash sumber sebelum menyalin video/poster ke `dist/media`. Folder ini diabaikan Git. Video/voice tidak diunggah ke GitHub atau hosting; hak distribusi suara masih belum ditutup menurut catatan promo/lisensi. Persetujuan menyisipkan preview tidak dianggap menyelesaikan hak publikasi. Jangan deploy direktori ini sebelum persyaratan publikasi terpenuhi.

Pemutar HTML native: controls, playsinline, preload none, tanpa autoplay/mute. Suara mengikuti tindakan pengguna dan volume perangkat/browser. Naskah dapat dibuka tanpa menonton. Naskah bukan caption tersinkronisasi; caption dan pemeriksaan aksesibilitas video lengkap masih diperlukan sebelum publikasi.

## Aset lain

`study.png`, `quran.png`, `weather.png`: bukti emulator terdahulu pada `android/validation/beta-final-2026-11-14/screens`, berisi data contoh. Bukan rekaman ulang58 atau cuaca/jadwal langsung. Aset lama dipertahankan; bukan semuanya dipakai desain saat ini.

## Menjalankan dan memeriksa

1. Siapkan media lokal dengan `D:/node.exe website/prepare-preview.cjs` jika sumber tersedia.
2. Jalankan server HTTP dengan document root `website/dist` dan bind127.0.0.1. Preview sesi ini: http://127.0.0.1:64882/.
3. `D:/node.exe website/check.cjs` memeriksa seluruh referensi lokal termasuk media.
4. Pada checkout tanpa media privat: `D:/node.exe website/check.cjs --source-only`. Output ini bukan bukti pemutaran. Tidak mengunduh atau membuat ulang suara.

[Bukti terbaru dan batas](validation/2026-11-16-product/README.md). Bukti folder2026-11-16 terdahulu merupakan iterasi sebelum video.

## Gerbang publikasi

Tombol unduh tetap disabled, tanpa URL APK/auto-redirect. Metadata kandidat mengikuti `docs/beta/internal-artifact.json` (55); source58 belum disebut paket publik. AI18+ masih dijelaskan sebagai rencana. Sebelum membuka unduhan: sahkan paket/signer/migrasi, hak konten/promo, privasi/retensi/usia, uji akhir dan distribusi; cocokkan URL HTTPS, ukuran serta SHA256 dengan paket final. Perubahan website tidak menyelesaikan pekerjaan akun/backend/HP yang masih WIP.
