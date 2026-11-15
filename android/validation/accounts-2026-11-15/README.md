# Akun, Firebase dan laporan AI — 15 November 2026

HOLD. Source/internal debug1.9.56/code83; HP tetap release bertanda tangan1.9.55. Belum ada APK release56 atau distribusi publik. Ini implementasi bertahap, bukan penutupan A–D/E/F/G atau sertifikasi privasi.

## Bukti terarah

-158tes backend lengkap lulus,0skip/0gagal, termasuk HTTP akun/Redis abusegate/renewal. Bukti usia yang terbit sebelum pembaruan akun tidak dapat dipakai ulang untuk renew; grant wali lama ikut invalid.
-235JVM lulus,0skip/0gagal. Build akhir berhasil; lint0error/909warning. SDK Auth25 sempat memunculkan ketidakcocokan metadata Kotlin pada lint; diganti Auth24.0.1 yang kompatibel. Tidak menonaktifkan lint atau validasi TLS.
-Empat native pada APK akhir/API36: consent/input/password state dan signup→verifikasi email→keluar→masuk serta reset password memakai SDK Android dan Firebase Auth Emulator resmi15.36.0, proyek demo-zikra-accounts. Email contoh hanya example.test; outbox lokal, tanpa email/akun produksi. Akun sintetis dihapus. [Transkrip](account-native-final.txt).
-Sembilan kasus BackupSnapshot termasuk schema Hadith lama lulus pada build56 pendahulu; logika snapshot sama. Guard tes awal salah membedakan SDK hurufbesar; diubah case-insensitive, marker/emulator guard tetap wajib. [Transkrip](backup-snapshot.txt). Ini menuntaskan regresi isolated yang masih tertunda pada receipt55, bukan mengulang migrasi HP.
-Satu kasus UI laporan pada build56 pendahulu: tombol Laporkan→Alasan lain→tinjauan→Kirim laporan menghasilkan record produksi dengan tepatdua field dan TTL7hari. Server diperiksa memakai identitas sintetis, lalu report/rate/registry fixture dihapus. Tidak memakai modelAI atau chat pribadi. [Receipt](report-ui-receipt.json), [native](report-ui-native.txt). Dua percobaan harness awal gagal mengakses kontrol; setelah layanan aksesibilitas disiapkan sebelum Activity dibuka, lulus. UI produk tidak diubah untuk meloloskan tes. Toast balasan tidak dinilai tersendiri; record server terkonfirmasi.

Jangan menjumlah seluruh14metode native seolah dijalankan pada hash APK akhir yang sama. Empat kasus akun adalah run final; sembilan snapshot dan satu hosted report berasal dari build56 pendahulu dengan perubahan auth/UI berikutnya, tanpa perubahan source snapshot/report produk.

## Artefak internal dan preservasi

APK akhir44.860.138byte, SHA25629401005610db407966c33c8510699cdd319dad516a7c0472cdb11fa6347108e. [Audit](debug-apk.json): nol berkas privat/nilai credential dikenal, tanpa MP3, signature debug valid. artifactChecksPassed=false dan distributionApproved=false memang benar: ini DEBUG, bukan release siap sebar. Flag signin/emulator aktif khusus build uji; defaultdebugfalse dan release dipaksa false. Aturan HTTP debug hanya10.0.2.2 untuk emulator; jangan menyamakan manifest cleartextDisabled dengan tidak adanya pengecualian debug. Aturan ini tidak menjadi sourceSet release.

HP/USB tidak dipakai. Emulator proyek RAM1536MB, Firebase localhost, penanda dan paket tes dibersihkan/ditutup. Pref/gateway emulator dipulihkan setelah uji laporan; hanya chat uji milik kasus dihapus. WIP PROJECT_HANDOFF, stash audio, data HP, source audio dan konfigurasi privat dipertahankan.

## Aktivasi nyata yang disetujui

Firebase Authentication Email/Password aktif pada proyekZikra, paket Spark/No-cost tetap. Passwordless/Google/Phone/Anonymous tidak ditambahkan. Pengguna menyetujui aktivasi; tidak ada upgrade billing atau akun server/key/role baru. Kebijakan password Require/min12 disimpan setelah persetujuan spesifik berikutnya. Upaya Save awal ditolak auto-review karena persetujuan login tidak mencakup policy; tidak dibypass, kemudian dicoba ulang setelah izin eksplisit. Force upgrade akun lama tetapoff. Bukti UI disimpan privat, hash pada [receipt](firebase-activation.json). Ini bukti konfigurasi, bukan tes email produksi atau audit seluruh akun/retensi.

## Cakupan dan batas berikutnya

Handler akun kini tersedia lokal dengan body16KB/8detik, respons no-store/error minimal, bearer/re-auth dan pre-auth Redis guard. Belum dimasukkan ke route/deployment Vercel. Layar masuk/reset/verifikasi email internal tersedia, menampilkan email akun, tidak menyimpan password view state, FLAG_SECURE dan pembersihan password saat keluar layar. Tidak mengirim riwayat/tugas lokal otomatis. Lihat [kontrak integrasi](../../../backend/deploy/SELF_SERVICE_ACCOUNTS.md).

Usia/wali nyata belum selesai: adapter assurance yang sesuai, portal wali, penghubung endpoint/Android/protectedAI, pencabutan lintas request, penghapusan akun dan kebijakan remaja/lokasi/retensi masih perlu integrasi dan bukti. Emailverified tidak membuktikan usia atau hubungan wali. Adapter sintetis tidak boleh dipasang sebagai production. F05 baru sebagian untuk akun; Google login/cloudsync bukan selesai. B01/rekaman asal tetap ditunda sesuai scope beta yang disetujui, bukan dihidupkan kembali.

PA04 teknis beta memiliki bukti UI→produksi/TTL/triase terarah; bukan jaminan approval Play atau evaluasi keselamatan model menyeluruh. API36 lintas versi lama dan sisa perangkat, salinan signer media terpisah, retensi, keputusan rilis/J14 tetap mengikuti checklist. HOLD tidak dicabut.

Tambahan final: batas password input disamakan4096dengan server; reset ke password lebih dari128karakter lalu login melalui UI lulus. Run boundary awal salah mengharapkan status email selalu belumverified dan mengalami penantian Activity antarkasus; harness mengisolasi task serta memeriksa pengguna/login menurut status SDK. Trace main thread idle/tidak adaANR. Finalempatkasus lulus6,437detik.

Arahan terbaru pengguna: tidak ingin review usia/wali oleh pengelola. Pilihan manual beta yang sempat diterima dibatalkan sebelum ada implementasi/akun review produksi. Arah berikutnya perhitungan usia otomatis dan persetujuan wali digital, dengan batas bahwa deklarasi usia/verifikasi email bukan bukti identitas. Handler/adapter assurance masih belum terhubung; jangan memasang adapter sintetis. Pengguna juga meminta perapian layar Pengingat harian dan menjelaskan adzan sesudah update; keduanya pekerjaan berikutnya, belum termasuk APK56ini.
