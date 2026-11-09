# E02 — Pengiriman laporan gempa

Jalur ini terpisah dari AI: GitHub Actions → endpoint privat Vercel → tiga feed
BMKG + Redis → Firebase Cloud Messaging → Android. Tidak menjalankan inference
atau menerima lokasi/pesan pengguna. Ini laporan nasional, bukan prediksi atau
peringatan yang dijamin tiba sebelum guncangan.

## Konfigurasi

- Backend production membutuhkan `ZIKRA_FIREBASE_SERVICE_ACCOUNT_JSON` sebagai
  secret server, `ZIKRA_QUAKE_CRON_SECRET` berupa 64 karakter heksadesimal acak,
  dan koneksi Redis yang sudah tersedia. Jangan memakai key Featherless sebagai
  pemicu jadwal. Tidak ada kredensial di query URL.
- Service account khusus cukup diberi **Firebase Cloud Messaging API Admin**;
  jangan gunakan Owner/Editor atau kunci akun admin Firebase bawaan.
- Android membutuhkan konfigurasi **client** Firebase untuk `com.zikra.app`.
  Build membaca `.local/firebase-android.json` atau properti `zikraFirebaseConfig`,
  lalu menghasilkan resource SDK. Service-account JSON ditolak oleh task build.
  Tanpa file client, aplikasi tetap dibangun tetapi sakelar push tidak tersedia.
  Identitas client di APK bukan kredensial pengirim server; file sumbernya tetap
  tidak masuk Git. Build beta memakai topic `zikra-earthquake-beta-v1`.
- GitHub repository secret `ZIKRA_QUAKE_CRON_SECRET` harus sama dengan pemicu
  Vercel. Variable `ZIKRA_QUAKE_PUSH_ENABLED=true` mengaktifkan workflow.
  Tetapkan `false` untuk menghentikan penjadwalan tanpa mengubah key atau AI.
  Service-account private key **tidak** disimpan dalam GitHub Actions.

## Sumber, frekuensi dan pengendalian duplikat

Feed HTTPS tetap: `autogempa.json`, `gempaterkini.json`, dan
`gempadirasakan.json` pada `https://data.bmkg.go.id/DataMKG/TEWS/`.
Tiga request per pemeriksaan; tidak ada retry berulang di dalam satu pemeriksaan.
Response dibatasi 64 KiB per feed dan input waktu/koordinat/angka/teks divalidasi.
Redirect keluar ditolak. Atribusi BMKG dipertahankan.

Workflow dijadwalkan lima menit sekali, pada menit 2/7/12/…/57 dengan
ekspresi `2/5 * * * *` (bentuk start/step yang dicontohkan dokumentasi). Ini memakai
GitHub Actions karena cron Vercel Hobby dibatasi satu kali sehari. Jadwal GitHub
dapat terlambat atau dilewati. Redis juga memberlakukan interval minimal satu
menit dan lease 55 detik untuk mencegah pemeriksaan paralel.

Pemeriksaan lengkap pertama menyimpan baseline dan **tidak mengirim** kejadian
yang sudah ada. ID berupa SHA-256 waktu UTC kejadian dengan namespace BMKG;
revisi magnitudo/koordinat atau kemunculan pada feed lain tidak mengirim ulang.
Ledger Redis bertahan tujuh hari, melintasi proses/server baru. Hanya kejadian
berumur paling lama satu jam yang layak dikirim; maksimum tiga per pemeriksaan.

Sebelum request FCM dimulai, kejadian direservasi persisten. Respons penolakan
HTTP 4xx eksplisit melepaskan reservasi agar bisa dicoba pada pemeriksaan lain.
Timeout/hasil ambigu mempertahankan reservasi: mengutamakan tidak mengirim ulang,
dengan konsekuensi suatu laporan bisa tidak terkirim. Bukan jaminan exactly-once.
TTL FCM maksimal 15 menit, dibatasi sisa umur kejadian. Android memeriksa umur,
schema, ID dan ukuran lagi serta menyimpan 128 ID terakhir sebelum menampilkan.

## Android, izin dan privasi

Push default **nonaktif**. Sakelar berada di Protector → Notifikasi gempa.
Izin Android diminta sesudah pengguna mengaktifkannya. Pesan tetap ditolak lokal
jika fitur mati, izin ditolak, atau kanal/aplikasi diblokir. Mematikan fitur
membatalkan notifikasi gempa sendiri, termasuk yang masih antre, tanpa membatalkan
notifikasi fitur lain. Unsubscribe dapat menunggu jaringan; pengaman lokal segera
berlaku. Mematikan fitur tidak mengklaim menghapus semua identitas dari Firebase.

Firebase mengelola ID instalasi/token serta metadata teknis yang diperlukan
layanan push. Tidak menambahkan Google Analytics. Server Zikra menggunakan topic
dan tidak menyimpan daftar token perangkat, GPS, profil atau chat untuk fitur ini.
SDK dan penyedia tetap perlu dicakup pada kebijakan privasi/Data Safety sebelum
rilis publik; pekerjaan tersebut belum otomatis ditutup oleh implementasi E02.

Payload data-only memuat snapshot sumber terpilih. Notifikasi membuka snapshot
itu, bukan menggantinya diam-diam dengan laporan terbaru. Snapshot invalid/lebih
dari 24 jam menampilkan status tidak tersedia; hanya tindakan Muat ulang yang
berpindah ke laporan terbaru. Ikon notifikasi memakai simbol Zikra monokrom.
Tidak meminta full-screen intent, bypass DND, exact alarm atau lokasi background.

## Verifikasi dan pemulihan

`node --test backend/*.test.mjs` mencakup deduplikasi lintas feed/proses,
baseline, Lua Redis, pengiriman ambigu, input, otorisasi dan payload FCM.
Tes Lua memakai `ZIKRA_REDIS_TEST_PYTHON` dan fakeredis/Lupa di lingkungan uji.
`EarthquakeAlertPolicyTest` memeriksa batas usia/kontrak client.
`EarthquakeE02Test` hanya berjalan dengan marker emulator yang sudah dicadangkan.

Uji cloud harus memakai token emulator milik penguji dan label **UJI**, bukan
broadcast kejadian palsu ke topic pengguna. Simpan token hanya privat; unsubscribe
dan hapus token/instalasi uji setelah selesai. Pemulihan aplikasi memakai APK lama
dengan data tetap; mematikan variable jadwal menghentikan poll berikutnya.

Force-stop, HP mati, jaringan, Google Play services, pembatasan baterai OEM,
kuota layanan dan keterlambatan scheduler dapat menghambat penerimaan. Hasil
emulator tidak membuktikan semua HP. Status checklist mengikuti bukti terpisah.

## Rujukan primer

- [Data gempa BMKG, batas akses dan atribusi](https://data.bmkg.go.id/gempabumi/).
- [FCM Android: penerimaan pesan](https://firebase.google.com/docs/cloud-messaging/android/receive-messages).
- [FCM HTTP v1 dan autentikasi server](https://firebase.google.com/docs/cloud-messaging/send/v1-api).
- [Batas cron Vercel](https://vercel.com/docs/schedules/cron-jobs/usage-and-pricing).
- [Sintaks jadwal GitHub Actions](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax).
