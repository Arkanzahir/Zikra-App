# Persyaratan aktif rilis publik

22 November 2026. Status **HOLD** untuk kandidat signed 1.9.64/code91. Bukti terbaru tercatat di [status beta](CURRENT_STATUS.md).

| Persyaratan | Bukti saat ini dan pekerjaan tersisa |
|---|---|
| Chat mandiri | APK signed sudah menerima jawaban dengan akun terverifikasi dan token instalasi sementara. Uji APK tanpa token instalasi masih diperlukan pada konfigurasi self-service yang terlindungi. |
| Hapus akun | Uji Firebase Auth Emulator lulus. Staging memerlukan akun layanan terpisah dengan izin `firebaseauth.users.delete` dan konfigurasi khusus staging, serta APK dengan fitur hapus akun aktif. |
| Konten dan privasi | Cakupan beta menunda dua kitab Gading dan alur wali. Konfirmasi hak suara promo dan sumber lain yang ikut distribusi. Finalkan peninjauan retensi penyedia dan draf privasi 18+. |
| Signing dan unduhan | APK signed lolos pemeriksaan unduhan HTTPS lokal dan pemasangan ulang emulator. Cadangan pemulihan kunci di media terpisah dan unduhan dari URL publik final masih perlu diverifikasi. |

Pakai akun sintetis untuk tes hapus akun di staging. Konfirmasi sesi lama ditolak, akun tidak dapat login kembali, dan akses chat akun berhenti. Lihat [kontrak aktivasi](../../backend/deploy/SELF_SERVICE_ACCOUNTS.md).

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
