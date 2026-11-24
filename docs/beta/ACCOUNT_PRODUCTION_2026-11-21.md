# Endpoint akun 18+ di produksi — 21 November 2026

## Hapus Akun aktif — 24 November 2026

- Akun layanan produksi terpisah mendapat peran kustom dengan satu izin
  `firebaseauth.users.delete`. Pembaca Firebase tetap terpisah. Kunci penghapus
  disimpan sebagai Secret khusus produksi dan flag penghapusan diaktifkan.
- Deployment `dpl_wmpdgrUgNqhwEA8A2mDbjZwkh3pY` diuji sebelum dipromosikan dan
  diuji ulang di domain produksi. Deployment sebelumnya
  `dpl_AUeeUQF2r8JZxvjkmj3J8bYs1zTo` tercatat untuk rollback.
- Kedua uji memakai akun berbeda beralamat `example.test`, tanpa email
  verifikasi atau permintaan inferensi. Health dan login awal HTTP 200.
  Permintaan hapus tanpa token HTTP 401, konfirmasi salah HTTP 400,
  dan konfirmasi benar HTTP 200. Firebase mengonfirmasi akun hilang,
  login ulang HTTP 400, dan API menolak token lama HTTP 401.

## Uji privat chat mandiri — 24 November 2026

Kandidat `dpl_Grd4kY6ntu19Hyu15ZgPgejV1SSi` memakai konfigurasi produksi
dengan tambahan `ZIKRA_ADULT_ACCOUNTS_SELF_SERVICE=true` khusus deployment.
Domain produksi tetap menunjuk ke deployment Hapus Akun yang sudah lulus.
Pengunjung tanpa token mendapat 401. Akun uji mendapat 403 sebelum deklarasi
usia, lalu status dan chat tanpa token instalasi mengembalikan 200.
Pencabutan berhasil dan akses berikutnya ditolak 403. Hapus Akun juga
berhasil dan token lama ditolak. Chat memakai satu pesan sintetis.
Percobaan sebelum header Cloudflare ditambahkan mendapat 409. Akunnya
dihapus langsung dari Firebase, sehingga deklarasi sintetis percobaan itu
mengikuti masa simpan Redis bawaan.

## Riwayat 21 November 2026

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

Uji sintetis memakai domain `example.invalid` tanpa mengirim email. Uji dengan email terverifikasi dibuktikan pada run berikutnya. Uji rute tanpa kredensial tidak memakai inferensi. IAM mengembalikan HTTP 403 saat Auth Reader membaca daftar kunci. Status key lama perlu akses IAM pemilik. [Uji pembaruan emulator dari rebuild 1.9.55](MIGRATION_EMULATOR_2026-11-21.md) lulus dengan data sintetis.

## Verifikasi email dan akses AI mandiri

- Firebase mengirim tautan ke alamat uji yang disetujui. Pemilik membuka tautan. Token login baru tercatat memiliki email terverifikasi.
- Endpoint produksi mengembalikan HTTP 200 untuk status awal, deklarasi 18+, perpanjangan dengan revisi baru, dan pencabutan. Status akhir `withdrawn`. Usia tetap deklarasi sendiri.
- Staging terlindungi `dpl_68Cae68VFhkZHE7jUexdwKDHY8pS` dari source `aecaa62` memakai self-service aktif dan kewajiban akun mati. Tanpa token instalasi, capabilities 200, chat 200 pada percobaan ulang, gambar 200, dan laporan 201. Capabilities dengan token instalasi lama 200.
- Satu permintaan chat akun dan satu chat token lama mendapat HTTP 504. Penyebab timeout belum terisolasi. Uji chat memakai prompt sintetis dan tidak menilai keandalan layanan.
- Setelah pencabutan, empat rute staging mengembalikan 403. Akun Firebase dan data Redis yang khusus milik fixture sudah dihapus. Token lama dari akun uji ditolak 401. Kuota global tetap mengikuti TTL normal.
- Alias publik tetap pada deployment sebelumnya. Akses mandiri produksi, APK final, dan pengujian HP tetap terbuka. Status rilis HOLD.
