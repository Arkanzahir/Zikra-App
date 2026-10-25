# Paket deploy VPS/Docker Zikra — alternatif, belum dijalankan

**Jalur aktif:** pengguna kemudian memilih Vercel. Backend sudah online dengan
Upstash Free; [runbook Vercel](VERCEL.md) dan [bukti hosting](../../android/validation/backend-vercel-2026-10-25/README.md).
Bagian berikut adalah persiapan VPS/Docker sebelumnya, bukan status hosting terbaru.

Pengguna memilih **siapkan paket dahulu**, 25 Oktober 2026. Belum ada akun hosting,
VPS, domain atau biaya yang disetujui. Tidak ada resource cloud dibuat. C02/C05
tetap terbuka sampai layanan nyata dan HP dapat diperiksa.

## Isi dan batas

- Docker image Node 24 Alpine, user `node`, hanya source backend; tidak menyalin
  `.env`, `.local`, repository Android, tes atau key ke image. Node lokal 22.17.0
  sudah menjalankan tes source; **image/Compose/Caddy belum dijalankan** karena
  Docker tidak tersedia pada komputer ini. Pilih image LTS dengan patch/digest
  yang diperiksa saat deployment nyata, lalu lakukan smoke test pada image itu.
- Compose: satu backend, volume privat persisten `/data`, health check,
  restart otomatis, root filesystem baca saja, penghentian 45 detik. Port backend
  tidak dipublikasikan. Caddy menjadi pintu HTTPS pada domain operator.
- Mode `installation`: satu token acak unik untuk satu instalasi. Registry hanya
  menyimpan SHA-256 token + ID acak/status revoked. Registry dibaca ulang per
  request sehingga pencabutan berlaku tanpa restart. Tidak ada pendaftaran
  publik terbuka atau provider key di aplikasi.
- Kuota 10 request/menit dan default 100/hari UTC per instalasi, maksimal satu
  request aktif per instalasi dan dua global. Counter disimpan atomik pada volume
  sehingga restart tidak menghapus kuota. Kuota menghitung percobaan yang lolos
  akses, termasuk kegagalan provider; pengguna tidak dijanjikan jumlah completion.
- Implementasi file ini **satu proses/satu replika**. Jangan menjalankan beberapa
  backend/penulis kuota pada volume yang sama. Untuk skala lebih besar, pindahkan
  registrasi/kuota ke database transaksional bersama dan uji ulang. Jalankan
  administrasi penerbitan/pencabutan secara berurutan agar tidak kehilangan update.

## Langkah operator pada VPS yang nanti dipilih

1. Siapkan domain DNS ke VPS, Docker/Compose dan firewall yang hanya membuka
   port publik yang dibutuhkan proxy. Batasi akses SSH/operator. Periksa patch
   OS/runtime dan kebijakan privasi/retensi provider/hosting.
2. Gunakan key provider yang sudah ada sesuai keputusan pengguna; simpan pada
   berkas env di luar source atau secret store, akses hanya operator/service.
   Isi model yang telah dibuktikan tersedia. Gunakan
   [template kosong](../.env.production.example); jangan menyalin `.local` ke Git,
   image atau APK. Set `ZIKRA_ENV_FILE` ke path absolut berkas privat itu, serta
   `ZIKRA_DOMAIN` ke domain nyata. Jangan menaruh secret dalam command line.
3. Dari folder repo pada server, validasi konfigurasi Compose dan build image:

   ```sh
   docker compose -f backend/deploy/compose.yaml config --quiet
   docker compose -f backend/deploy/compose.yaml build backend
   ```

   Jangan menyimpan keluaran `docker compose config` tanpa `--quiet`: hasilnya
   bisa memuat nilai environment privat.
4. Inisialisasi satu instalasi sebelum menjalankan backend. Perintah ini menulis
   token mentah sekali pada berkas privat, **tidak** mencetak token:

   ```sh
   docker compose -f backend/deploy/compose.yaml run --rm --no-deps backend node client-admin.mjs issue /data/clients.json /data/first-installation.env
   docker compose -f backend/deploy/compose.yaml up -d
   ```

   ID instalasi yang dicetak bukan secret. Berikan token dari berkas tersebut
   secara privat hanya ke pemilik instalasi; masukkan di pengaturan koneksi AI
   Android dengan URL `https://DOMAIN/v1/assistant`. Jangan mengirim provider key
   ke perangkat. Setelah diserahkan, operator dapat menghapus salinan token mentah
   dengan prosedur server sendiri; registry tetap hanya berisi hash.
5. Pencabutan satu token, tanpa menyentuh instalasi lain:

   ```sh
   docker compose -f backend/deploy/compose.yaml exec backend node client-admin.mjs revoke /data/clients.json ID-INSTALASI
   ```

   Penerbitan berikutnya memakai nama berkas token baru, tidak menimpa berkas lama.
   Berkas registry/kuota korup atau tidak dapat ditulis membuat request gagal
   tertutup (503), tidak membuka akses atau menghapus counter diam-diam.

## Checklist sebelum C02/C05 dicentang

- [ ] Domain/TLS valid, HTTP backend tidak terekspos; redirect kredensial ditolak.
- [ ] Model nyata berhasil dari image produksi; kuota akun dan error disamarkan.
- [ ] Token salah/dicabut 401, kuota 429, dua instalasi tetap terisolasi di balik
  proxy, counter bertahan setelah restart/reboot/deploy.
- [ ] Registry/usage dipasang persisten dengan izin minimum; backup terenkripsi
  dan retensi metadata ditetapkan. File tidak menyimpan percakapan, nama, tugas
  atau koordinat; metadata tersisa berupa ID instalasi/hash/status dan counter.
- [ ] Health monitor dan restart otomatis diuji pada server nyata; periksa
  sintaks Caddy/Compose serta shutdown SIGTERM di platform itu.
- [ ] Tidak ada Authorization, payload chat atau env privat di access/error log,
  tracing, exception capture dan artefak CI; Caddy tidak menyalakan access log
  dalam template ini. Audit konfigurasi log/retensi platform yang dipilih.
- [ ] APK → HTTPS permanen → provider berhasil pada HP; token salah, kuota,
  jaringan putus/batal dan laptop pengembang mati benar-benar diperiksa.

Syarat rotasi dibatalkan oleh pengguna; gunakan key yang sudah ada. Lihat
[keputusan terbaru](../../android/validation/assistant-c-2026-10-25/key-decision-2026-10-25.md).

Rujukan: [Dockerfile](https://docs.docker.com/reference/dockerfile/),
[Caddy reverse proxy](https://caddyserver.com/docs/caddyfile/directives/reverse_proxy),
[HTTPS Caddy](https://caddyserver.com/docs/automatic-https),
[Node LTS](https://nodejs.org/en/about/previous-releases).
