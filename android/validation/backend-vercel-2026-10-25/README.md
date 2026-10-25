# C02 — backend Vercel + Upstash

25 Oktober 2026 WIB. Pengguna memilih Vercel setelah menolak verifikasi kartu
Oracle. Proyek `zikra-ai` dibuat di akun Hobby; Upstash for Redis
`zikra-ai-access` tersedia dan terhubung pada environment production. Permintaan
provisioning menggunakan `plan=free`, `autoUpgrade=false`, `eviction=false`,
`prodPack=false`, region `sin1`. Persetujuan ketentuan diselesaikan pengguna di
browser; keberadaan resource dikonfirmasi ulang lewat CLI dan REST Redis nyata.

**Backend sudah online** di `https://zikra-ai.vercel.app`, source **`9438aa4`**;
[receipt deployment](deployment.json). Dua deployment production READY; HTTPS
publik diperiksa tanpa login Vercel/bypass. C02/C05 dan seluruh C tetap terbuka
untuk HP/laptop mati serta pemeriksaan operasional yang tersisa.
Android tidak diubah; APK 1.8.0 beta / code 25 tetap 31.362.014 byte, source APK
`7982e10`, SHA-256 `77ed491b65e2519758412bdbe44bc07d494a282ca8a3dda0d7ac5d55bd15a5c1`.

## Bukti terarah

- [34 tes backend](backend-tests.tap) lulus, 0 skip/0 gagal pada Node 24.22.1;
  pemeriksaan sebelumnya pada Node 22.17.0 juga lulus. Empat tes Redis memakai
  fakeredis/Lupa yang mengeksekusi Lua produksi, bukan server Redis produksi.
  [Dependency uji](test-requirements.txt) hanya dalam venv privat, tidak runtime.
- [Redis REST nyata](redis-live.json): PING, kuota lintas instance, revokasi,
  dua slot global dan expiry key sintetis lulus. Probe memakai namespace UUID
  tersendiri, tanpa data pengguna; key probe diberi expiry 60 detik.
- [HTTPS → provider nyata](https-live.json): health minimal 200, tanpa token 401,
  satu completion Featherless sintetis 200 dengan reply valid, tanpa nilai key
  atau jawaban disimpan. Endpoint aplikasi `/v1/assistant`.
- [Akses cloud](https-access.json): sepuluh body JSON salah menghasilkan 400,
  request berikutnya quota 429; tidak memakai data pengguna. Token probe dicabut
  lalu 401. [Deploy ulang](redeploy.json): health 200, counter 10 dan status revoked
  bertahan, registry sintetis korup menghasilkan 503; tidak mengubah registry pengguna.
- [Paket gratis aktual](free-plan.json): status available, plan Free, kartu tidak
  diperlukan, preauthorization 0, autoUpgrade/eviction/prodPack false menurut API.
- [Query sampel log](runtime-log-sample.json) berhasil untuk 10 menit terakhir,
  **0 entri runtime** dikembalikan; nol kecocokan secret pada hasil query kosong
  bukan sertifikasi retensi/sanitasi semua log platform/provider. Query awal 1 jam
  ditolak karena di luar retensi Hobby, lalu dipersempit; log mentah tetap privat.
- Build Vercel berhasil. Dua fungsi Node 24.x, maksimum 60 detik, cancellation
  aktif; handler tidak membuka port/listen. [Audit paket dan preservasi](secret-preservation-build.json)
  memeriksa file Git/source dan paket fungsi aktual dengan secret yang diketahui.
- Key Featherless yang sudah ada disimpan sebagai Secret production, model,
  prefix dan limit ditambahkan tanpa nilai ke log/Git/APK. Environment lokal asli
  tidak berubah. Variabel Redis `KV_REST_API_URL`/`KV_REST_API_TOKEN` diterima
  langsung dari Marketplace; tidak perlu menyalin credential ke source.
- CLI privat lama dapat dimulai dua kali: health minimal 200, tanpa token 401,
  proses berhenti, tanpa bocoran log. Tidak mengulang completion provider untuk
  pemeriksaan tersebut. Windows process termination bukan bukti SIGTERM Linux.

## Perubahan dan batas

[Runbook Vercel](../../../backend/deploy/VERCEL.md) menjelaskan deployment,
administrasi token, uji sintetis dan gerbang C02/C05. Handler HTTP bersama
mempertahankan kontrak Android `/v1/assistant`, context consent, proposal yang
ditinjau, sumber tafsir tetap serta sanitasi respons. Request body lambat ikut
timeout 35 detik, lebih pendek dari lease 55 detik; request Vercel maksimum
60 detik. Redis memakai jam server, kuota atomik dan lease bersama lintas fungsi.
Saat Redis gagal, layanan mengembalikan 503 tanpa fallback shared token/memori.

Belum lengkap pemantauan, backup/pemulihan serta audit retensi platform, kondisi
cancellation/timeout cloud, HP/laptop mati atau dikte nyata.
C03/C04/C05/C06, B01 dan rilis publik belum ditutup. Jadwal kelas mengikuti D04;
D02a tetap tugas notifikasi/adzan yang belum diimplementasikan. WIP handoff,
env privat, stash/arsip audio, desain dan data pengguna dipertahankan.

Source checkpoint backend: **`9438aa4`**, commit yang menambahkan laporan ini; bukti source
APK sebelumnya tidak diubah menjadi commit backend. Tidak ada push atau rilis
publik yang disimpulkan dari checkpoint lokal.
