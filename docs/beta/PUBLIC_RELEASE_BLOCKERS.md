# Persyaratan aktif rilis publik

24 November 2026. Status **HOLD** untuk kandidat signed 1.9.67/code94. Bukti terbaru tercatat di [status beta](CURRENT_STATUS.md).

| Persyaratan | Bukti saat ini dan pekerjaan tersisa |
|---|---|
| Chat mandiri | Lulus dari APK signed 1.9.66 di emulator API 36 pada Vercel Preview dengan Firebase Spark dan Redis Upstash terpisah. Klien menerima jawaban sintetis tanpa token instalasi. Pencabutan memblokir request lokal dan backend menolak token yang sama. Aktivasi self-service produksi belum dilakukan. [Bukti staging](../../android/validation/signed-self-service-staging/README.md). |
| Hapus akun | Aktif di produksi. Uji sintetis di alamat produksi menghapus akun Firebase dan menolak login ulang serta token lama. Tombol pada APK signed 1.9.67 juga telah lulus uji di emulator dengan Preview. [Bukti produksi](ACCOUNT_PRODUCTION_2026-11-21.md). |
| Konten dan privasi | Draf privasi dan Ketentuan Penggunaan disetujui penerbit dan tersedia di Preview tanpa promo. Streaming dan unduhan privat tiga qori untuk belajar didasarkan pada ketentuan publik Al Quran Cloud. Islamic Network mengarahkan pertanyaan khusus ke kanal bantuan. Cakupan syarat retensi dan persetujuan data sensitif Quran Foundation, serta publikasi dokumen, masih terbuka. [Audit konten](CONTENT_LICENSE_TRACE.md). |
| Signing dan unduhan | Cadangan signing terenkripsi dan pemulihan signer lulus. APK signed 1.9.67 berhasil diunduh melalui Preview Vercel terlindungi dengan ukuran dan SHA-256 yang sesuai manifest. Beranda, privasi, ketentuan, dan APK mengarahkan pengunjung anonim ke login. Unduhan publik masih belum dibuka. |
| Uji perangkat | Upgrade signed 1.9.63-internal → 1.9.67 lulus di HP lewat USB. Posisi baca dan pilihan qori cocok. Al-Fatihah ayat 1–2 berlanjut otomatis untuk ketiga qori. Ayat dari laporan awal belum diketahui. HP tidak memiliki file unduhan offline untuk dibandingkan. [Bukti emulator dan HP](../../android/validation/signed-beta-1.9.67/README.md). |

Pakai akun sintetis untuk tes hapus akun di staging. Konfirmasi sesi lama ditolak, akun tidak dapat login kembali, dan akses chat akun berhenti. Lihat [kontrak aktivasi](../../backend/deploy/SELF_SERVICE_ACCOUNTS.md).

Untuk Preview, integrasi Upstash pada akun ini hanya menawarkan paket berbayar
untuk resource tambahan. Database yang tersedia terhubung ke proyek produksi.
Paket sumber Preview backend lokal memuat 38 berkas terlacak dari allowlist Vercel
dan tidak memuat kredensial. [Upstash sementara](https://upstash.com/docs/redis/overall/getstarted)
menyediakan REST yang cocok untuk pengujian sintetis dan kedaluwarsa setelah
72 jam. Uji hosted memakai akun buatan dengan alamat `example.test` dan
jawaban tetap. Akses deployment dicabut dan Redis kembali kosong.
Staging yang menyimpan data pengguna memerlukan database yang sudah diklaim
dan terpisah.

## Catatan historis 15 November 2026

Status HOLD, kandidat1.9.53/code80. Pengguna meminta publikasi sesudah seluruh persyaratan terpenuhi. Pengguna mengonfirmasi belum memiliki bukti tertulis hak distribusi. Tidak ada perubahan cakupan13+, penghapusan fitur atau penggantian audio diam-diam.

| Persyaratan | Keadaan | Bukti yang diperlukan |
|---|---|---|
| Hadis B01 | Menunggu sumber/hak | Pemegang hak, asal edisi/terjemahan, izin cakupan distribusi APK dan penyimpanan, atribusi |
| Adzan normal/Subuh | Rekaman pilihan teridentifikasi, hak belum tersedia | Izin pemilik/kewenangan lisensi untuk kedua hash rekaman dan ketentuan atribusi |
| Quran/terjemahan/audio/timing | Hak layanan dan cache belum seluruhnya tuntas | Ketentuan tiap resource dan bukti akun/izin yang cocok dengan pemakaian |
| Privasi13–17 | Mekanisme usia/wali dan kelayakan layanan belum final | Proses yang telah ditinjau, persetujuan/penarikan dan batas lokasi, dokumentasi orangtua |
| PelaporanAI | Kode lokal/135tesbackend/UIbatal lulus | Operator/akses/triase, retensi akun/log/backup, aktivasi produksi dan E2E |
| Kompatibilitas | API36 lima metode unik/tujuh eksekusi lulus | Versi Androidlama/OEM/landscape/izin/background dan data final |
| Signer/pemindahan data | Backup terenkripsi ada, APKdebug | Key rilis privat/cadangan/pin, paketrelease dan migrasi HP nyata |
| Kanal publik | Website ditunda sebelumnya | Setelah gate, halamanprivasi/dukungan/unduhan HTTPS, uji instal/update dan penarikan |

Permintaan bukti lisensi dapat menyebut aplikasiZikra, penerbitZikra, kanalAPK/website/kemudianPlay, konten/rekaman tertentu, penggunaan offline/cache, wilayah dan jangka waktu, serta atribusi. Jangan menganggap tidak dibalas sebagai persetujuan. Draft korespondensi boleh disiapkan; pengiriman kepada pihak lain memerlukan instruksi pengguna.
Peluncuran publik dilakukan setelah kriteria tersebut terpenuhi. Tidak membuat tanggal rilis dari jumlah tes yang lulus.

Penelusuran15November tersedia pada [sumber/lisensi](CONTENT_LICENSE_TRACE.md). Bukti lisensi publik diterima sebagai dasar jika cocok; surat khusus bukan kewajiban semua sumber. AsalduaMP3 sudah teridentifikasi, yang terbuka cakupan izin rekaman.
