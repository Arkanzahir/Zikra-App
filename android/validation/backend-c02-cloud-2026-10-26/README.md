# C02 — monitor GitHub dan batas waktu di runtime cloud

26 Oktober 2026. **C02 tetap [ ]: uji HP saat laptop mati belum dilakukan.**
APK tetap 1.9.7/code34, 37.218.400 byte. Tidak membangun APK/emulator, mengubah
data HP, menjalankan inference atau memperbarui deployment production.

## Hasil terverifikasi

- [Monitor GitHub berhasil](monitor-run.json),
  [run 40273711756](https://github.com/Arkanzahir/Zikra-App/actions/runs/40273711756),
  job health 121411875130, source `922d36a`, seluruh langkah success.
  [JSON probe dari log job](monitor-probe.json): health200 dan lookup Redis menolak
  credential acak401, tanpa credential aktif/kuota/inference. Pemicu manual melalui
  halaman Actions; empat jadwal harian sudah dikonfigurasi, **eksekusi terjadwal
  belum diamati**. Ini bukan jaminan SLA atau notifikasi kegagalan selalu diterima.
- [Sebelum header upstream](timeout-headers.json): handler HTTP asli memberi504
  setelah **35.037 ms**, abort diterima fetch dan socket upstream ditutup.
- [Saat body upstream macet](timeout-body.json): header200/JSON parsial sudah
  diterima, handler tetap memberi504 setelah **35.006 ms**. Error hanya
  `provider_unavailable`, tanpa isi upstream atau credential.
- Kedua kasus memakai **batas default asli 35 detik**, bukan deadline tes yang
  dipendekkan. Satu panggilan sebelum retry, satu pelepasan kapasitas. Setelah
  pemulihan upstream terkontrol, retry eksplisit memberi200; total dua panggilan/
  dua pelepasan dan kapasitas kosong. Tidak ada retry otomatis atau penulisan
  upstream terlambat setelah abort.
- [60 tes backend](backend-tests.txt):60 lulus,0 gagal/skip. Dua metode tambahan
  memeriksa transport HTTP fixture pada deadline pendek serta gate akses/expiry
  preview. Integrasi Lua tetap melalui fixture Redis lokal yang sudah ada.
- [Pembersihan preview](preview-cleanup.json): deployment sementara dihapus,
  URL404. `vercel curl` membuat satu automation bypass proyek saat mengakses
  preview terlindungi; akses baru itu dicabut melalui API setelah tes. Bypass
  lama tidak diubah. Deployment production tetap `dpl_Du6X24ysdLE3Zc6Ua6A6Q5Nt6CLR`,
  env/node/retensi/proteksi tetap. [Probe production setelahnya](production-health-after.json)
  kembali health200/Redis401. Tidak menambah proyek/database/paket berbayar.

## Cakupan fixture dan batas klaim

Preview Node24.x/maxDuration60 menjalankan salinan source handler yang hash-nya
ada pada [manifest](source-files.json). API fixture membuat dua server HTTP
loopback di dalam invocation: gateway sebenarnya dan upstream lambat terkontrol.
`fetch` asli, sinyal abort, pembacaan stream dan socket HTTP benar-benar digunakan;
hasil bukan callback palsu atau timeout klien yang dianggap timeout server.

**Upstream bukan Featherless dan access store fixture berada di memori.**
Tidak memasukkan provider key, credential Redis, profil/chat atau token instalasi
production ke preview. Tidak mengklaim outage/biaya provider sebenarnya, transport
HTTPS Featherless yang macet atau pelepasan lease Redis nyata dari kasus preview.
Jalur HTTPS/provider dan Redis nyata memakai [bukti sebelumnya](../backend-vercel-2026-10-25/README.md),
termasuk [abort/backup/restore cloud terpisah](../backend-operations-c02-2026-10-26/README.md).
Fixture ini menutup bukti mekanisme deadline penuh di runtime Vercel; gangguan
provider alamiah tidak sengaja dipaksakan dengan menghabiskan kuota inference.

Source fixture berada di `backend/test-support/`, dikecualikan allowlist
production `.vercelignore`. API preview memerlukan token acak privat dan expiry
30 menit, menolak token salah/stage selain headers/body. Tidak menerima URL,
model, deadline atau isi chat dari pemanggil. Tidak menambahkan route tes ke
production atau APK. Token/header, output CLI, metadata akun dan paket preview
tetap privat; setelah pencabutan/penghapusan, tidak ada endpoint uji aktif.

## Menjalankan ulang tanpa mengubah production

1. Salin hanya modul runtime pada manifest dan dua modul fixture ke folder privat
   tersendiri. Entry `api/timeout.js` mengekspor API fixture. Gunakan Node24.x,
   framework null, satu fungsi maxDuration60/supportsCancellation:true.
2. Deploy **preview**, bukan production; jangan memasang credential provider/Redis
   atau prefix production. Token fixture acak64 hex dan expiry30 menit disuplai
   sebagai env deployment, bukan source/file Git atau argumen chat.
3. Snapshot daftar bypass proyek secara privat sebelum `vercel curl`. Jalankan
   POST kosong dengan header stage headers, lalu body; Authorization dari file
   header privat. Response report harus passed, deadline35000/defaulttrue.
4. Cabut **hanya** automation bypass baru milik tes, tanpa regenerate. Hapus
   **hanya ID deployment preview yang baru dibuat**, setelah memeriksa target dan
   bahwa ID tersebut berbeda dari production. Verifikasi URL404 serta production
   dan konfigurasi aslinya tetap. Jangan menghapus proyek atau deployment lama.

Rujukan operator: [durasi fungsi](https://vercel.com/docs/functions/configuring-functions/duration),
[CLI curl](https://vercel.com/docs/cli/curl),
[pencabutan automation bypass](https://vercel.com/docs/rest-api/projects/update-protection-bypass-for-automation).

## Gerbang berikutnya

[Prosedur HP saat laptop mati](PHONE_LAPTOP_OFF.md) sudah disiapkan, belum dijalankan.
HP pada jaringan nyata dengan laptop pengembang mati masih wajib untuk C02;
GitHub/cloud fixture tidak menggantikannya. Pengguna sebelumnya memilih lanjut
tanpa HP. C06 mikrofon dan C07 kamera/picker fisik tetap menunggu perangkat;
C04 mengikuti D04. B01, adzan D02a, UI04/F/G dan rilis publik belum selesai.

WIP PROJECT_HANDOFF.md, stash/arsip audio, konfigurasi lama, desain disetujui,
data pengguna dan APK terdahulu dipertahankan; [audit terarah](audit.json).

## Pemeliharaan monitor setelah pemicu manual

Checkout lama memberi peringatan runtime Node20 pada log runner. Workflow sekarang
memakai checkout v7.0.1/Node24 yang dipin pada SHA resmi, tetap contents:read dan
persist-credentials:false. Sparse checkout mengambil hanya berkas probe; push
pada main yang mengubah workflow/probe juga memicu health check, selain jadwal
empat kali sehari dan pemicu manual. Tidak ada pemicu pull_request_target atau
credential produksi/GitHub secret baru. [Metadata versi/hash](monitor-maintenance.json).
[Job otomatis setelah push](monitor-push-run.json), run40277773046 pada8ae9f00,
**success**: checkout pin baru/sparse checkout/persist-credentials:false teramati;
probe kembali200/401, tanpa peringatan Node20 pada log job. Run manual pada922d36a
tetap bukti historis yang terpisah. Invocation schedule belum diamati.
