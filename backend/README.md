# Jalur Featherless untuk Pendamping Zikra

Status: gateway dan klien Android tersedia di sumber. Belum dihosting dan belum
mengirim permintaan live ke Featherless; tes memakai respons provider simulasi.
Mode offline tetap menjadi default. Tidak ada API key yang dibundel ke APK.

Alur: aplikasi Android → backend HTTPS milik pengguna → Featherless. Aplikasi
mengirim pesan percakapan yang dipilih (maksimal 24 pesan/16.000 karakter), bukan
profil, lokasi, isi tugas atau file perangkat. Provider key hanya dibaca dari
environment server. Jawaban online diberi label online; kegagalan tidak diganti
diam-diam dengan jawaban lokal.

## Menyiapkan server pribadi

Memerlukan Node.js 22+ dan layanan hosting dengan HTTPS/reverse proxy. Tidak ada
paket npm tambahan. Jangan membuka port HTTP 8787 ke internet.

1. Salin `.env.example` menjadi `.env` **hanya pada server**.
2. Isi `FEATHERLESS_API_KEY` dari akun sendiri dan `FEATHERLESS_MODEL` dengan ID
   model yang tersedia untuk akun itu. Jangan mengirim key melalui chat atau Git.
3. Buat `ZIKRA_CLIENT_TOKEN` acak, minimal 32 karakter. Ini kode akses gateway
   pribadi, berbeda dari provider key. Contoh pembuat kode lokal:

   ```sh
   node -e "process.stdout.write(require('node:crypto').randomBytes(32).toString('hex'))"
   ```

4. Jalankan `node --env-file=.env server.mjs`. Letakkan reverse proxy HTTPS di
   depannya. Jalur publik: `https://DOMAIN-SENDIRI/v1/assistant`. `/health` hanya
   mengembalikan status, tanpa konfigurasi atau data pengguna.
5. Di Pendamping Zikra, ketuk label mode → **Online — melalui backend**. Baca
   keterangan pengiriman pesan, masukkan URL lengkap di atas dan kode akses
   gateway. Jangan memasukkan provider key. Kode gateway di perangkat dienkripsi
   AES-GCM dengan kunci Android Keystore, dan aplikasi tidak mengizinkan HTTP
   atau redirect kredensial ke host lain.

Pengaturan ini untuk pengujian **pribadi**. Kode akses bersama bukan identitas
pengguna atau mekanisme otorisasi distribusi publik. Sebelum rilis umum, ganti
dengan autentikasi/token per instalasi yang dapat dicabut serta kuota per
pengguna. Kode gateway tidak di-hardcode di APK, tetapi perangkat pemilik tetap
bukan tempat menyimpan provider key.

Gateway membatasi dua permintaan aktif, sepuluh permintaan per menit per alamat
socket, ukuran request 48 KiB, konteks 16.000 karakter, jawaban 8.000 karakter dan
waktu provider 35 detik. Jika dipasang di belakang satu proxy, alamat socket
proxy berbagi kuota; jangan mempercayai `X-Forwarded-For` dari sembarang klien.
Tambahkan pembatasan ukuran request/timeout/kuota di reverse proxy sesuai hosting.
Server tidak mencatat isi percakapan. Log dan kebijakan retensi hosting/provider
tetap perlu diatur oleh pemilik layanan.

## Pemeriksaan

`node --test server.test.mjs` menjalankan tes lokal tanpa API key sungguhan dan
tanpa permintaan internet. Tes tersebut tidak membuktikan model atau akun live.
Sebelum mengaktifkan online, periksa konfigurasi hosting, HTTPS, akses model,
batas akun dan jalankan satu percakapan asli dari HP.

API yang digunakan: [Featherless Quickstart](https://featherless.ai/docs/quickstart-guide),
[Chat completions](https://featherless.ai/docs/completions),
[Daftar model](https://featherless.ai/docs/api-reference-models). Endpoint provider
tetap `https://api.featherless.ai/v1/chat/completions`; model hanya diatur server.
