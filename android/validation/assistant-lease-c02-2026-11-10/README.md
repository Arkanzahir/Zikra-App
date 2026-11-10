# C02: penanda permintaan aktif setelah respons — 10 November 2026

Pengguna melaporkan APK1.9.37 menampilkan layanan penuh/connection error saat
laptop ditutup atau sleep. Internet HP berasal dari router dan layanan lain
masih berjalan menurut pengguna. Screenshot menunjukkan balasan Online lalu
pesan berikutnya layanan penuh. C02 laptop mati/sleep **belum lulus**.

Metadata CDN Vercel sekitar17.12WIB memperlihatkan satu request Android200
kemudian lima429; WAFallow. Metadata tidak memuat body galat, tidak memastikan
setiap request berasal dari HP ini dan tidak membuktikan hubungan sebab-akibat
sleep. Probe health dan satu chat sintetis melalui identitas terpisah200.
Tidak mengambil isi chat pribadi, token atau koordinat dari log.

## Temuan dan perbaikan

`reply()` sebelumnya mengakhiri respons sebelum `finally` menunggu Redis
release. Klien dapat menerima jawaban ketika lease masih hidup; runtime
serverless dapat menunda/menghentikan pekerjaan setelah respons. Sebelum deploy,
dua chat sintetis berturut-turut200; sesudah respons kedua masih teramati satu
lease hidup untuk instalasi uji. Ini bukti celah urutan, bukan reproduksi penuh
laptop sleep/429 HP atau bukti bahwa semua429 memiliki penyebab sama.

Sekarang reply menunggu pelepasan lease sebelum mengakhiri respons. Jalur
finally tetap membersihkan pembatalan/tanpa respons. Kegagalan Redis tetap
memakai expiry lease terbatas; tidak menghapus lease atau counter pengguna
secara manual. Batas100/hari,10/menit,satu aktif/instalasi dan dua global tetap.
Provider/model/token/konfigurasi hosting/paket tidak diubah. Error layanan
sibuk dari provider masih mungkin terjadi; tidak ada fallback berbayar atau
pengulangan otomatis.

87tes backend lulus/0skip termasuk Redis/Lua dan regresi deterministik:
release sengaja ditahan, HTTP200 maupun503 tidak boleh terlihat sebelum release
selesai, lalu request berikutnya diterima. Deployment allowlist22file diaudit
bebas nilai rahasia dikenal dan berhasil. Sesudah deploy, tiga chat sintetis
berurutan200/Cloudflare, masing-masing2,95–5,36detik dan lease hidup0ketika
respons diterima. Identitas/usage/lease uji dibersihkan; counter pengguna tidak
 direset. Bukti sebelum/sesudah dipisahkan, tanpa memperbaiki status percobaan lama.

APK tetap1.9.37/code64/hash e65767269d53d91fc126dde387dd7f57c805da7cd35dcfecb283676463e55e94,
49.377.416byte. Tidak mengubah Java/XML/aset/audio, build ulang, memasang APK
atau mengakses HP yang sudah dicabut. Uji Android tidak diulang karena perubahan
hanya backend. WIP PROJECT_HANDOFF/stash/arsip/env/logo/master tetap sesuai
checkpoint audit. Cron E02 tidak diubah.

**Berikutnya:** pengguna mencoba dua pesan berurutan di HP dengan internet
mandiri saat laptop sleep/mati, bukan hanya satu pesan. C02 tetap[ ] sampai
hasil itu diterima. B01,D02/D02a hak audio/sisa fisik/vendor,E03/E04,F/G/publik
masih mengikuti checklist; bukan seluruh A–D/E selesai.
