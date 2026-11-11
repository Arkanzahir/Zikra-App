# C02: waktu tunggu aplikasi dan diagnostik antrean — 11 November 2026

Pengguna memprioritaskan gangguan AI sebelum migrasi Google Maps. Ini perbaikan
terarah, **bukan penutupan C02** atau bukti bahwa semua penolakan antrean telah
hilang. HP tidak diakses; APK baru belum dipasang ke HP.

## Temuan dan perubahan

`OnlineAssistantClient` sebelumnya menetapkan call timeout45detik tetapi tidak
menetapkan read timeout. OkHttp3.14.9 memakai read timeout10detik, sedangkan
gateway dapat menunggu provider35detik. Uji HTTP lokal nyata dengan header
respons ditunda12detik membuktikan konfigurasi lama mengalami
SocketTimeoutException dan konfigurasi baru menerima200/body utuh.

Factory `AssistantHttp` kini membatasi connect10/read45/write20/call60detik.
Tidak ada retry otomatis, redirect atau pengiriman ulang pesan. Perubahan ini
mencegah putus prematur untuk respons yang melewati10detik, tetapi bukan bukti
bahwa kejadian pengguna sebelumnya pasti disebabkan timeout tersebut.

Lua kini membedakan antrean `installation` dan `global` secara atomik. Respons
publik tetap429/try_later, kompatibel dengan APK lama. Log backend hanya
menambahkan enum `queue` tersebut pada event penolakan yang sesuai; tidak ada
isi chat, token, koordinat atau identitas instalasi di log. Batas tetap1aktif per
instalasi/2global/10permenit/100perhari dan lease55detik; counter pengguna tidak
direset. Jika dua batas penuh sekaligus, alasan instalasi diperiksa dahulu.

## Verifikasi

- [Hasil tes dan kegagalan awal](checks.json):91backend/0skip dan181unit Android
  lulus/4skip; lint0error/878warning. Uji Lua nyata via fixture lokal memeriksa
  kedua alasan antrean, kuota tidak dikonsumsi saat ditolak dan slot pulih.
- Uji transport12detik menggunakan loopback tanpa inference eksternal. Tidak
  ada emulator atau uji suara/sensor/UI native baru untuk checkpoint ini.
- [Deployment](deployment.json):22berkas allowlist diperiksa terhadap nilai
  rahasia yang dikenal, deployment produksi berhasil tanpa perubahan env,
  model, provider, paket atau cron.
- [Probe hosted](hosted.json):3pesan contoh berurutan200/Cloudflare, masing-masing
  nol lease milik instalasi uji setelah balasan. Satu fixture hanya pada key
  instalasi uji menghasilkan429 tanpa memakai kapasitas global tambahan;
  [runtime](diagnostics.json) membuktikan queue=installation tercatat. Key uji
  dibersihkan. Ini validasi diagnostik, bukan reproduksi identitas/kejadian HP.
- Percobaan awal gagal karena akses Python/cache Gradle; sesudah akses yang
  sesuai, dua tes backend melewati deadline saat build berjalan bersamaan.
  Penyebab penjadwalan tepatnya tidak ditetapkan. Putaran final berurutan91/91
  lulus; kegagalan tidak dihapus dari catatan privat.

## APK dan preservasi

[Receipt APK](apk.json):1.9.39/code66,49.443.800byte, SHA256
`ac0e5dff4e5f8bdcee0a283e6677e227d18a799faa179e51198be9667610de43`.
File lokal berada di folder induk repo:
`Zikra-AI-Connection-v1.9.39-beta-debug.apk` dan tidak masuk Git.
Signer debug/dua audio tetap; nol nilai credential server dikenal ditemukan
pada APK terurai, bukan audit seluruh riwayat rahasia. Identitas client Firebase
sengaja dibundel. PROJECT_HANDOFF WIP/stash/arsip audio/Featherless env/logo/master
Home cocok hash terlindungi. Tidak mengubah data, izin atau setting HP.

## Yang masih terbuka

Pasang pembaruan tanpa uninstall, uji pesan berurutan/response lambat dan ulangi
dengan laptop sleep/mati saat HP memakai internet mandiri. Jika gagal lagi,
cocokkan waktu dengan kode antrean baru; jangan menganggap provider berganti
otomatis menyelesaikan masalah. C02 tetap[ ]; B01, D02/D02a, E03/E04, F/G dan
rilis publik tetap mengikuti gerbang masing-masing. Migrasi Google Maps belum
dikerjakan; penundaan UI10 dan mode orang tua tetap.
