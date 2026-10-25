# Gateway Featherless untuk Pendamping Zikra

Status 25 Oktober 2026: [persiapan C01](../android/validation/backend-c01-2026-10-25/README.md)
dilanjutkan dengan [alur C03/C04/C06 dan bukti](../android/validation/assistant-c-2026-10-25/README.md).
Usulan tugas terstruktur dan Tadabbur bersumber telah menerima completion nyata
melalui gateway privat lokal. Mode akses per instalasi, pencabutan dan kuota
persisten tersedia di source, dengan [paket deploy](deploy/README.md).
**Sudah dihosting di Vercel** pada `https://zikra-ai.vercel.app` dengan Upstash Free;
[bukti HTTPS/provider/akses/redeploy](../android/validation/backend-vercel-2026-10-25/README.md).
Endpoint Android `/v1/assistant`, health `/health`. Rantai APK dari HP belum dibuktikan.
[Runbook Vercel](deploy/VERCEL.md) menjadi jalur aktif; paket Docker/VPS tetap alternatif.
Pengguna memilih Vercel dan memakai key yang sudah ada. Syarat
rotasi dibatalkan sesuai [keputusan terbaru](../android/validation/assistant-c-2026-10-25/key-decision-2026-10-25.md).

Alur sasaran: Android → backend HTTPS → Featherless. Mode offline Android tetap
default; online mengirim pesan saat ini, atau riwayat online bila dipilih
(maksimal 24 pesan / 16.000 karakter). Nama, maksimal 10 tugas/tenggat dan
perhitungan salat/zona hanya dikirim lewat pilihan eksplisit per request dengan
preview. Tidak mengirim koordinat GPS, profil, tugas atau berkas otomatis.
Provider key hanya dibaca di server. Jawaban gagal tidak diganti diam-diam dengan
jawaban lokal. Model AI tidak dibundel ke APK.

## Konfigurasi privat

Tanpa dependensi npm runtime tambahan; hosting Vercel dipin Node 24.x, uji Node
24.22.1 lulus. CLI privat lama juga diperiksa pada Node 22.17.0; hasil tersebut
bukan sertifikasi patch runtime produksi. Konfigurasi file di bawah khusus
gateway privat/VPS; Vercel memakai env Redis dan handler tanpa `listen()`.

| Variabel | Validasi / fungsi |
| --- | --- |
| `FEATHERLESS_API_KEY` | Wajib, 16–512 karakter ASCII terlihat tanpa spasi/control; bukan dikirim ke APK |
| `FEATHERLESS_MODEL` | Wajib, ID persis dari akun; maksimal 200 karakter huruf/angka/titik/garis miring/underscore/hyphen |
| `ZIKRA_CLIENT_TOKEN` | Wajib, token acak terpisah 32–256 karakter huruf/angka dan `._~+/=-`, tidak berawalan `sk-`, berbeda dari key provider; sesuai validasi Android, hanya uji pribadi |
| `HOST` | Default `127.0.0.1`; IP literal atau `localhost`. Bind selain loopback hanya setelah isolasi jaringan hosting ditinjau |
| `PORT` | Default `8787`, bilangan bulat 1–65535 |
| `ZIKRA_ACCESS_MODE` | Default `private`; pilih `installation` untuk token instalasi yang dapat dicabut |
| `ZIKRA_CLIENTS_FILE` / `ZIKRA_USAGE_FILE` | Mode installation: dua path absolut terpisah pada volume privat persisten; registry dan kuota, tanpa percakapan |
| `ZIKRA_DAILY_LIMIT` | Mode installation: 1–1000 percobaan/hari UTC per instalasi, default 100 |

`ZIKRA_CLIENT_TOKEN` hanya wajib pada mode private. Mode installation tidak
menerima shared token sebagai fallback; provider key tetap wajib di server.
Lihat administrasi penerbitan/revokasi dan batas satu replika pada runbook deploy.

Konfigurasi salah menghentikan startup tanpa mencetak nilai/stack trace.
`NODE_TLS_REJECT_UNAUTHORIZED=0` ditolak. Provider URL tetap, redirect ditolak;
klien tidak boleh memilih provider/model melalui request.

1. Gunakan secret store hosting atau berkas env **di luar source**. `.env.example`
   dan `.env.production.example` hanya template kosong; env privat serta `.local/`
   diabaikan Git. Batasi akses
   berkas ke operator/service. Jangan paste key ke chat, perintah shell, log,
   ticket, laporan atau konfigurasi Android.
2. Gunakan key provider yang sudah tersedia pada env privat sesuai keputusan
   pengguna. Tidak ada kewajiban rotasi sebagai prasyarat C01 atau deploy.
   Pemeriksaan C01 tidak mengubah atau mencabut key.
3. Tentukan ID model yang tersedia untuk akun/kuotanya. `GET /v1/models` menurut
   dokumentasi adalah katalog; hasil katalog saja bukan bukti akses completion.
   Catatan lokal lama mendapat 404 pada katalog tetapi completion berhasil.
   Jangan mengganti model diam-diam. Gunakan satu pesan sintetis lewat gateway
   untuk membuktikan akses model yang benar-benar dikonfigurasi.
4. Buat token gateway acak di lingkungan privat, misalnya:

   ```sh
   node -e "process.stdout.write(require('node:crypto').randomBytes(32).toString('hex'))"
   ```

   Simpan keluarannya langsung di secret store/env privat; jangan simpan output
   terminal ini sebagai bukti publik.

Perintah dari root repo dengan konfigurasi privat yang sudah tersedia:

```sh
node --env-file=.local/featherless.env backend/server.mjs --check-config
node --env-file=.local/featherless.env backend/server.mjs
```

`--check-config` hanya validasi bentuk env, tanpa membuka port atau menghubungi
provider. `npm start` dari `backend/` memakai env yang **sudah diinjeksi** oleh
service manager; tidak membaca `.env` otomatis. Alternatif dari `backend/`:
`node --env-file=.env server.mjs` dengan berkas privat yang di-ignore.

## Pemeriksaan dan cara menjalankan ulang

```sh
cd backend
npm test
```

Tes ini tanpa key nyata/internet. Dari root repo:

```sh
node --env-file=.local/featherless.env backend/check-private.mjs
node --env-file=.local/featherless.env backend/check-private.mjs --live
```

Pemeriksaan memulai dua proses CLI pada port loopback sementara yang sama,
memeriksa `/health`, menolak akses tanpa token, menghentikan tiap proses, serta
mencetak status/boolean saja. `--live` menambahkan **satu permintaan provider**
dengan teks sintetis tetap; dapat menggunakan kuota akun. Tidak mencetak ID model,
pesan balasan, key atau token. Proses pemeriksaan tidak mengedit env privat.
Di Windows penghentian child process adalah terminasi proses; hasil tersebut
bukan bukti drain SIGTERM Linux.

`GET /health` mengembalikan `{"status":"ok"}` tanpa konfigurasi. Ini liveness
proses, bukan readiness model/kuota atau status hosting publik.

Startup ulang: hentikan proses milik service ini, injeksikan env privat yang
terkonfigurasi, jalankan lagi lalu periksa health dan satu completion sintetis
bila perlu. Pada Unix SIGTERM/SIGINT menghentikan penerimaan koneksi, memberi
kesempatan request selesai, lalu menutup koneksi tersisa paling lambat 40 detik.

[Template systemd](deploy/zikra-private.service) menyiapkan user service terpisah,
env `/etc/zikra/backend.env`, restart saat gagal dan batas shutdown 45 detik.
Path/user harus disesuaikan di hosting; contoh env memakai `HOST=127.0.0.1`.
Template belum dipasang/diverifikasi pada Linux, tidak mengatur sertifikat/TLS
dan **tidak boleh langsung dipublikasikan** dengan shared token. Penanganan boot,
restart otomatis, health monitor dan drain pada platform nyata harus diuji C02.

## Batas dan gerbang publik

Gateway membatasi dua request aktif, sepuluh request per menit per alamat socket,
16 koneksi, 64 header, body 48 KiB, konteks 16.000 karakter, jawaban 8.000 karakter
dan body provider 128 KiB. Request body timeout 15 detik, header 10 detik, timer
provider 35 detik (dimulai saat request diterima). Respons client body terlalu
besar 413; JSON/isi client salah 400; tipe salah 415; token salah 401; provider
429 tetap 429; body provider rusak/terlalu besar menjadi 502; timeout 504.
Pembatalan klien membatalkan fetch provider.

Lampiran terpilih memakai header `X-Zikra-Attachments: 1` dan batas body 2 MiB;
pesan tanpa lampiran tetap 48 KiB. Satu foto atau PDF maksimal tiga halaman
dikirim sebagai gambar JPEG/PNG tervalidasi (360.000 byte per gambar, dimensi
maksimal 1.600 piksel). DOCX/TXT yang sudah diurai perangkat dikirim sebagai teks
maksimal 12.000 karakter. Gateway menolak URL gambar eksternal, nama/path tidak
aman, MIME/base64/dimensi salah; isi lampiran diperlakukan sebagai data pengguna.
Set `FEATHERLESS_VISION_MODEL` hanya pada backend setelah akses model nyata diuji;
tanpa konfigurasi itu foto/PDF ditolak `vision_unavailable`, tanpa balasan palsu.
Model teks tetap memakai `FEATHERLESS_MODEL`. Gateway tidak menyimpan lampiran;
kebijakan penyimpanan provider/platform tetap perlu diperiksa sebelum publik.

Mode private memakai kuota socket di memori yang hilang saat restart. Mode
installation dengan file memakai identitas token yang dapat dicabut dan kuota persisten
per instalasi, satu proses/satu replika. Vercel memakai Redis bersama dengan
counter/lease Lua atomik lintas fungsi; lihat runbook Vercel. Semua mode mengabaikan
`X-Forwarded-For`. Jangan menambah replika pada volume file yang sama tanpa
database/koordinasi kuota transaksional bersama yang diperiksa ulang.

Jangan membuka HTTP port ini ke internet. C02 wajib menyediakan HTTPS permanen,
isolasi port internal, reverse proxy dengan batas body/timeout yang sesuai,
identitas/token per instalasi yang dapat dicabut, kuota dan monitoring minimum.
Shared gateway token hanya untuk uji pribadi. Log aplikasi hanya pesan lifecycle
generik; proxy/hosting/provider perlu audit retensi dan sanitasi sendiri. Jangan
mencatat Authorization atau isi percakapan pada log/tracing platform.

Di Android: pilih online secara sadar, baca keterangan pengiriman, masukkan URL
HTTPS `/v1/assistant` dan token **gateway**, bukan key provider. Token gateway
disimpan lewat Android Keystore; TLS/redirect tetap diverifikasi. Penggunaan APK
dari HP, laptop mati, kuota nyata dan kondisi jaringan tetap C02/C03/C05/G02.

## Rujukan audit

- [Featherless Quickstart](https://featherless.ai/docs/quickstart-guide),
  [chat completions](https://featherless.ai/docs/completions),
  [katalog model](https://featherless.ai/docs/api-reference-models).
- [Node.js security](https://nodejs.org/en/learn/getting-started/security-best-practices),
  [HTTP server](https://nodejs.org/api/http.html),
  [stream iterator](https://nodejs.org/docs/latest-v22.x/api/stream.html#readableiteratoroptions),
  [rilis LTS](https://nodejs.org/en/about/previous-releases).
