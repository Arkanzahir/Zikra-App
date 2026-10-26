# Vercel + Upstash — C02

Keputusan pengguna 25 Oktober 2026: gunakan Vercel. Proyek `zikra-ai` telah dibuat
di akun Hobby; backend kini memakai empat entry function Node 24.x untuk health,
assistant, capabilities dan images. [Runbook operasional/backup](OPERATIONS.md)
memuat lanjutan26 Oktober; keberadaan workflow lokal bukan monitor yang sudah aktif.
Status deployment dan bukti terbaru: [laporan C02](../../android/validation/backend-vercel-2026-10-25/README.md).
Jangan menganggap proyek dibuat atau build berhasil sebagai hosting AI yang bekerja.

## Arsitektur dan batas

Android → HTTPS Vercel `/v1/assistant` → Featherless. Provider key yang sudah ada
tetap hanya dalam environment server; tidak dimasukkan ke Android/Git/image.
`GET /health` hanya liveness minimal, bukan kesiapan Redis/model/kuota.
Alias `/api/health` dan `/api/assistant` menuju entry masing-masing; metode tetap
dibatasi. Capabilities dan images mempunyai entry terpisah yang mematok path.
Tidak membutuhkan domain berbayar, memakai subdomain `.vercel.app`.

Vercel tidak memakai file registry/usage lokal. Redis menyimpan hash token,
UUID instalasi/status dicabut, pemetaan UUID→hash, counter dan lease acak.
Tidak menyimpan nama, tugas, koordinat atau percakapan. Setiap request membaca
status akses ulang; Lua memeriksa pencabutan lagi saat mengambil kuota.
Kuota 10/menit kalender dan default 100/hari UTC per instalasi memakai jam Redis.
Percobaan berizin yang sudah mengambil kuota tetap dihitung saat input/provider
gagal. Lua atomik membatasi satu request aktif per instalasi, dua secara global
lintas fungsi/proses. Lease 55 detik pulih setelah crash; request dibatasi
35 detik termasuk pembacaan body. Pelepasan lease gagal tidak membuka akses;
pengguna mungkin harus menunggu lease habis. Counter kedaluwarsa setelah dua
hari tanpa aktivitas; registry/pencabutan tetap tersimpan tanpa TTL.

Tidak ada fallback shared token atau storage memori bila Redis gagal. Akses
ditolak 503 tanpa meneruskan provider. URL Redis wajib HTTPS `*.upstash.io`,
credential hanya header, redirect/TLS bypass ditolak. Body request 48 KiB,
provider 128 KiB dan respons Redis 64 KiB; respons error tidak mencetak rahasia.
Paket VPS/Docker lama tetap tersedia untuk alternatif; batas satu prosesnya
berlaku pada mode file, bukan Redis.

## Akun dan paket gratis

Vercel Hobby untuk proyek pribadi nonkomersial dengan batas pemakaian. Upstash
Redis dipilih **Free**, `autoUpgrade=false`, `eviction=false`, `prodPack=false`;
jangan memilih pay-as-you-go, trial berbayar atau menambahkan kartu atas nama
pengguna. Batas gratis dapat berubah; periksa dashboard saat provisioning.
Tidak menjanjikan semua skala/fitur selamanya gratis. Kuota Featherless terpisah.
Jika penyimpanan penuh/kuota Redis habis, layanan gagal tertutup sampai pulih;
jangan menghapus registry untuk memaksa layanan kembali bekerja.

## Pemasangan

1. Dari `backend/`, login dan hubungkan proyek Vercel milik pengguna. Root proyek
   harus folder `backend`, framework Other/null. Gunakan konfigurasi `vercel.json`.
   Jangan mengunggah root repo atau seluruh `.local`.
2. Pasang Upstash melalui dashboard Storage/Marketplace atau CLI berikut. Pemilik
   akun menyelesaikan persetujuan ketentuan di browser bila diminta. Periksa
   resource benar-benar muncul setelahnya; pesan pengguna sudah menyetujui
   belum menggantikan hasil server bahwa integrasi belum terpasang.

   ```sh
   vercel integration add upstash/upstash-kv --name zikra-ai-access --plan free --metadata primaryRegion=sin1 --metadata autoUpgrade=false --metadata eviction=false --metadata prodPack=false --environment production --no-env-pull
   ```

3. Simpan variabel dalam [template kosong](../.env.vercel.example) ke environment
   **production**. `FEATHERLESS_API_KEY` bertipe Secret/sensitive, diisi dari env
   privat lokal tanpa menampilkan nilainya. Redis REST URL dan token tulis
   berasal dari integrasi; nama `KV_REST_API_URL` dan `KV_REST_API_TOKEN` dari
   Vercel Marketplace juga diterima langsung tanpa menyalin nilai credential.
   Jangan memakai token read-only.
   Jangan memberikan prefix/database production pada preview/development.
4. Build dan periksa paket fungsi. `.vercelignore` memakai allowlist source;
   env, `.vercel`, admin CLI, tes, Android dan laporan tidak diunggah. Jangan
   commit `.vercel/project.json`, env yang ditarik atau nilai keluaran CLI privat.

   ```sh
   vercel pull --yes --environment production
   vercel build --prod
   vercel deploy --prod
   ```

5. Terbitkan token instalasi hanya dari mesin operator dengan env Redis privat.
   Perintah dari root repo; ganti path absolut berkas tujuan yang belum ada:

   ```sh
   node --env-file=.local/vercel-production.env backend/redis-client-admin.mjs issue PATH_ABSOLUT_TOKEN_BARU
   node --env-file=.local/vercel-production.env backend/redis-client-admin.mjs revoke UUID_INSTALASI
   ```

   Token acak mentah ditulis sekali ke berkas privat, tidak ke stdout/registry.
   Kegagalan issue bisa menyisakan berkas token yang tidak terdaftar; jangan
   menganggapnya token aktif. Gunakan nama baru, jangan menimpa file lama.
   Serahkan token gateway hanya ke instalasinya secara privat. Jangan mengisi
   provider key pada pengaturan Android.
6. Jalankan probe sintetis dengan endpoint sebenarnya. `--live` menggunakan
   satu request Featherless; output hanya status/boolean, tanpa jawaban/token:

   ```sh
   node --env-file=.local/vercel-client.env backend/check-vercel.mjs https://NAMA_PROYEK.vercel.app PATH_RECEIPT --live
   ```

## Bukti sebelum menutup C02/C05

- [x] Resource Redis benar-benar Free; auto upgrade/eviction mati, konfigurasi
  production terpisah dari preview. Tentukan backup/retensi dan prosedur pemulihan
  tanpa menyalin raw token ke laporan. Diperiksa26 Oktober: Free/false dan secret
  production-only melalui API; backup lokal AES-GCM serta drill Upstash nyata
  lulus. Cadence/retensi30 hari adalah prosedur operator manual, bukan scheduler
  backup aktif. [Bukti operasi](../../android/validation/backend-operations-c02-2026-10-26/README.md).
- [x] Build/paket fungsi bersih; HTTPS endpoint nyata health 200, tanpa token 401,
  model nyata 200, token salah/dicabut 401 dan quota 429 tanpa bocoran respons.
  Terpenuhi 25 Oktober; [receipt HTTPS/akses](../../android/validation/backend-vercel-2026-10-25/README.md).
- [x] Quota/revokasi/lease lintas dua fungsi dan redeploy benar-benar bekerja
  pada Redis/Vercel nyata; fixture Lua lokal saja belum cukup.
  Bukti25 Oktober Redis/HTTPS/redeploy tetap berlaku; kode akses bersama tidak
  berubah. Gambar C09 memiliki bukti dan batas tersendiri, bukan klaim seluruh
  inferensi gambar sudah diuji paralel pada cloud.
- [ ] Cancellation/timeout/retry dan pemulihan Redis/kuota diperiksa pada platform.
  Tinjau log/observability/retensi Vercel dan Upstash; jangan menyalakan pencatatan
  header Authorization, environment atau isi chat dalam tracing/log drain.
  Cancel nyata26 Oktober: lease teramati, client abort dan pelepasan325ms sebelum
  expiry55 detik, retry malformed400 tanpa inference kedua. Bukan jaminan biaya
  provider dibatalkan; timeout provider/cloud panjang masih belum dibuktikan.
- [ ] Pantau endpoint dan kegagalan layanan tanpa mencatat payload. Uji dari HP
  dengan laptop pengembang mati, lalu rantai APK → HTTPS → provider. C05 dan
  seluruh C tidak otomatis selesai ketika endpoint sudah tersedia.
  Probe tokenless HTTPS+lookup Redis lulus; workflow GitHub disiapkan lokal,
  belum aktif sebelum default branch menerima perubahan. HP/laptop mati belum.

## Uji lokal

Runtime tidak membutuhkan dependency npm baru. Integrasi Lua lokal memakai
`fakeredis[lua]==2.32.1` (Lupa) dalam venv privat, bukan server Upstash nyata.
Tetapkan `ZIKRA_REDIS_TEST_PYTHON` ke Python venv itu sebelum `node --test
backend/*.test.mjs` dari root repo. Empat tes integrasi diberi skip bila env
tersebut tidak tersedia; jangan melaporkannya lulus.

Rujukan diperiksa 25 Oktober 2026:
[Vercel Functions Node](https://vercel.com/docs/functions/runtimes/node-js),
[cancellation](https://vercel.com/docs/functions/functions-api-reference),
[Hobby](https://vercel.com/docs/plans/hobby),
[integrasi CLI](https://vercel.com/docs/cli/integration),
[Upstash REST](https://upstash.com/docs/redis/features/restapi),
[Upstash Free](https://upstash.com/pricing/redis).

Pembaruan26 Oktober setelah checkpoint ee2b751: [push dan workflow](../../android/validation/checkpoint-push-2026-10-26/README.md)
sudah diverifikasi di GitHub (state active); hasil job belum diverifikasi.
Status workflow lokal pada catatan sebelumnya bersifat historis. C02 tetap[ ].
