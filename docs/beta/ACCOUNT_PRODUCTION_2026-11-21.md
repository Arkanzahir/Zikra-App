# Endpoint akun 18+ di produksi — 21 November 2026

Endpoint status, deklarasi, perpanjangan, dan pencabutan tersedia di [zikra-ai.vercel.app](https://zikra-ai.vercel.app/health). Konfigurasi produksi Zikra memakai `ZIKRA_ADULT_ACCOUNTS_ENABLED=true` dan `ZIKRA_ADULT_ACCOUNTS_REQUIRED=false`. APK signed 1.9.58 masih kandidat internal dan rilis berstatus **HOLD**.

## Deploy dan pemulihan

- Build bersumber dari `85ddb6966963935d24382b4b57b74a0cbbf1cd09`. Paket staging berisi 37 berkas terlacak (195.197 byte). Git, konfigurasi privat, APK, dan log tidak ikut paket. Seluruh 175 tes backend lulus tanpa skip. Audit npm mencatat 0 temuan.
- Staging pertama mengembalikan HTTP 500 saat Firebase Admin memuat `jose` melalui `jwks-rsa`. Staging itu tidak dipromosikan. Opsi resmi Vercel `NODE_OPTIONS=--experimental-require-module` diterapkan khusus ke produksi `zikra-ai`, lalu staging baru lolos. [Dokumentasi Vercel](https://vercel.com/docs/functions/runtimes/node-js/advanced-node-configuration#experimental-nodejs-require-of-es-module).
- Firebase Auth Reader dan subject key stabil tersimpan sebagai variabel Secret khusus produksi. Versi lokal subject key ada di `.local/account-subject.key` dengan ACL Windows terbatas. Gunakan key yang sama saat redeploy karena key menentukan pengenal akun Redis.
- Deployment aktif `dpl_AUeeUQF2r8JZxvjkmj3J8bYs1zTo`. Deployment awal untuk rollback `dpl_2eLJ9qtLLU5GBoBt3od5NQUj2iph`. Cek alias produksi dan ulangi probe health, akses instalasi, dan penolakan tanpa kredensial setelah rollback.

## Probe produksi

| Pemeriksaan | Hasil |
|---|---|
| Health | HTTP 200 |
| Status, deklarasi, perpanjangan, pencabutan tanpa token | HTTP 401 dengan `unauthorized` dan `Cache-Control: no-store` |
| Capabilities dengan token instalasi lama | HTTP 200 sebelum deploy, pada staging, dan setelah promosi |
| Capabilities, Quran, chat, gambar, laporan tanpa token | HTTP 401 |
| Pendaftaran sintetis di Firebase produksi | HTTP 200. Admin SDK memverifikasi token dan dapat membaca akun email belum terverifikasi. |
| Status dan deklarasi dengan token akun sintetis tersebut | HTTP 401 |
| Pembersihan akun sintetis | HTTP 200 saat penghapusan mandiri. Auth Reader mengonfirmasi UID sudah tidak ditemukan. |

Uji sintetis memakai domain `example.invalid` tanpa mengirim email. Alur pengiriman dan klik verifikasi, status/deklarasi berhasil, perpanjangan, dan pencabutan dengan akun terverifikasi masih perlu diuji. Uji rute tanpa kredensial tidak memakai inferensi. IAM mengembalikan HTTP 403 saat Auth Reader membaca daftar kunci. Status key lama perlu akses IAM pemilik. [Uji pembaruan emulator dari rebuild 1.9.55](MIGRATION_EMULATOR_2026-11-21.md) lulus dengan data sintetis.
