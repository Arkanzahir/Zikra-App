# Persyaratan aktif rilis publik

24 November 2026. Status **HOLD** untuk kandidat signed 1.9.65/code92. Bukti terbaru tercatat di [status beta](CURRENT_STATUS.md).

| Persyaratan | Bukti saat ini dan pekerjaan tersisa |
|---|---|
| Chat mandiri | APK signed sudah menerima jawaban dengan akun terverifikasi dan token instalasi sementara. [Instrumen staging](../../android/validation/signed-self-service-staging/README.md) sudah dibangun dan cocok signer. CLI Vercel menemukan proyek, tetapi belum ada deployment atau variabel Preview. Deployment yang sebelumnya dipakai untuk uji berlabel Production. Uji hosted tanpa token instalasi masih memerlukan Preview terlindungi, Redis terpisah dan akun sintetis terverifikasi. |
| Hapus akun | Uji Firebase Auth Emulator lulus. Staging memerlukan akun layanan terpisah dengan izin `firebaseauth.users.delete` dan konfigurasi khusus staging, serta APK dengan fitur hapus akun aktif. |
| Konten dan privasi | APK hanya membawa dua audio adzan berlisensi. [Audit 1.9.65](CONTENT_LICENSE_TRACE.md) membedakan Murottal unduhan dari cache untuk sorotan kata. Cakupan izin audio, batas usia cache, dan hak promo publik masih terbuka. Draf privasi memuat sumber Murottal dan retensi penyedia. Ketentuan Penggunaan publik, konfigurasi akun, cadangan, dan persetujuan draf tetap terbuka. |
| Signing dan unduhan | APK signed 1.9.65 lulus audit, unduhan HTTPS lokal, dan update emulator dengan data baca tetap utuh. Cadangan pemulihan kunci di media terpisah dan unduhan dari URL publik final masih perlu diverifikasi. |
| Uji perangkat | Emulator API 36 lulus update dan percobaan ulang ayat yang macet. Kandidat belum dipasang di HP. Uji padanan gejala asli dan data pengguna masih terbuka. |

Pakai akun sintetis untuk tes hapus akun di staging. Konfirmasi sesi lama ditolak, akun tidak dapat login kembali, dan akses chat akun berhenti. Lihat [kontrak aktivasi](../../backend/deploy/SELF_SERVICE_ACCOUNTS.md).

Untuk Preview, integrasi Upstash pada akun ini hanya menawarkan paket berbayar
untuk resource tambahan. Database yang tersedia terhubung ke proyek produksi.
Paket sumber Preview lokal memuat 38 berkas terlacak dari allowlist Vercel dan
tidak memuat kredensial. Belum ada resource atau deployment baru.

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
