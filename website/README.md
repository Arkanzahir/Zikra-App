> **Preview online16November2026:** https://zikra-jyr79p4ka-arkanzahir86-3257s-projects.vercel.app/ — memerlukan loginVercel pemilik. Video lengkap bersuara; unduhanAPK tetapdisabled. [Bukti/deploy](DEPLOYMENT.md). Ini menggantikan status lokal-saja di iterasi sebelumnya; tidak mengubah HOLD aplikasi.

# Website Zikra — pratinjau lokal

Revisi produk16November2026. Situs statis di `dist`; tanpa framework, JavaScript, analytics, cookie, formulir atau aset pihak ketiga. Belum dihosting publik dan tidak menawarkan APK. Status aplikasi tetap HOLD.

## Desain sekarang — arah Ente

Logo dan delapan ikon berasal dari vektor APK. Font Plus Jakarta Sans lokal. Pembuka memakai judul dua warna dan tiga layar aplikasi. Enam panel visual dan12topik pelengkap menjelaskan fitur Zikra; lihat [pemetaan fitur](FEATURE_MAP.md). Video52detik tetap tersedia, tanpa autoplay. Tidak ada testimoni/angka pengguna palsu, aksen italic, atau simbol privasi dekoratif.

Rujukan utama saat ini: https://ente.com/ — susunan hero dan panel demonstrasi fitur. Visual diperiksa langsung sebelum implementasi. Warna/logo/aset Zikra dipertahankan; aset dan klaim Ente tidak disalin. Things/LocalSend/Pillars/Beeper/Signal adalah referensi iterasi sebelumnya.

## Promo lokal

Pengguna meminta memasukkan promo terakhir yang disetujui: `Zikra-Promo-Lengkap-52s-Tanpa-Awan.mp4`,52detik,1280×720,3.694.204byte. Sumber berada di parent repo. SHA256: `347d985b0ca9f2c7e12c33c148d2163661c32af0791c05cd631d24ffc4bb2407`. Poster dari frame lokal review-0267, cuplikan AI dari review-0393, naskah dari `design/promos/2026-11-12/script-approved.txt`.

`D:/node.exe website/prepare-preview.cjs` memeriksa hash sumber sebelum menyalin video/poster ke `dist/media`. Folder ini diabaikan Git. Promo dipakai di preview Vercel terlindungi. Hak distribusi suara untuk situs publik masih terbuka. [Audit sumber](../docs/beta/CONTENT_LICENSE_TRACE.md).

Pemutar HTML native: controls, playsinline, preload none, tanpa autoplay/mute. Suara mengikuti tindakan pengguna dan volume perangkat/browser. Naskah dapat dibuka tanpa menonton. Caption Bahasa Indonesia dapat dipilih di pemutar. Waktunya mengikuti jeda audio video 52 detik. Edge lokal menampilkan seluruh 17 cue pada posisi yang diuji. Batas cue dicocokkan dengan jeda audio MP4 final. Pemeriksaan aksesibilitas keseluruhan masih diperlukan sebelum publikasi.

## Aset lain

`study.png`, `quran.png`, `weather.png`: bukti emulator terdahulu pada `android/validation/beta-final-2026-11-14/screens`, berisi data contoh. Bukan rekaman ulang58 atau cuaca/jadwal langsung. Aset lama dipertahankan; bukan semuanya dipakai desain saat ini.

## Menjalankan dan memeriksa

1. Siapkan media lokal dengan `D:/node.exe website/prepare-preview.cjs` jika sumber tersedia.
2. Jalankan server HTTP dengan document root `website/dist` dan bind127.0.0.1. Preview sesi ini: http://127.0.0.1:64882/.
3. `D:/node.exe website/check.cjs` memeriksa seluruh referensi lokal termasuk media.
4. Pada checkout tanpa media privat: `D:/node.exe website/check.cjs --source-only`. Output ini bukan bukti pemutaran. Tidak mengunduh atau membuat ulang suara.

[Bukti terbaru dan batas](validation/2026-11-16-ente/README.md). Bukti pemutaran video ada di validation/2026-11-16-product; folder2026-11-16 merupakan iterasi sebelum video.

## Gerbang publikasi

Tombol unduh tetap disabled, tanpa URL APK/auto-redirect. Metadata kandidat signed 1.9.65 mengikuti `docs/beta/internal-artifact.json`. AI18+ masih dijelaskan sebagai rencana. Sebelum membuka unduhan: sahkan paket/signer/migrasi, hak konten/promo, privasi/retensi/usia, uji akhir dan distribusi; cocokkan URL HTTPS, ukuran serta SHA256 dengan paket final. Perubahan website tidak menyelesaikan pekerjaan akun/backend/HP yang masih WIP.
