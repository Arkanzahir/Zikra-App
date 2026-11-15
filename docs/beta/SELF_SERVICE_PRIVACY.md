## Arahan terbaru: otomatis, tanpa review pengelola

Pengguna membatalkan pilihan review manual beta yang sempat disetujui. Jangan melanjutkan role/queue peninjau pengelola. Arah yang diminta: perhitungan usia otomatis dan persetujuan wali digital. Bedakan deklarasi usia, kontrol email dan pembuktian identitas/usia/relasi; jangan mengklaim hal yang tidak diverifikasi. Belum diintegrasikan, HOLD.

## Pembaruan1.9.56

Signin/reset/verifikasi email SDK tersedia dan teruji pada emulator resmi. Firebase Email/Password Spark dan Require/min12 telah diaktifkan dengan izin pengguna. Handler lokal/renewal/pre-auth abusegate juga tersedia;158backend/235JVM/4native final lulus. [Bukti/batas](../../android/validation/accounts-2026-11-15/README.md). Usia/wali/provider assurance/portal dan wiringAI belum tuntas; akun bukan izinAI. Tidak ada email/akun produksi uji atau deploy endpoint akun. Detail fondasi di bawah historis dan digantikan untuk subset yang sudah diverifikasi.

# Pendaftaran mandiri, usia dan wali

15November2026. Pengguna menyetujui pengembangan; fondasi server teruji lokal, belum tersedia sebagai alur pengguna di1.9.55. TetapHOLD.

Tujuan alur ialah aksesAI bagi pengguna13+ tanpa kode undangan manual. Pendaftaran akun dan bukti email, penetapan usia, persetujuan wali serta izin berbagi pesan memiliki bukti dan pencabutan masing-masing. Riwayat lokal tidak otomatis menjadi data akun/cloud.
AkunFirebase dapat menjadi dasar autentikasi. IDtoken harus diverifikasi server, termasuk issuer/audience/signature/expiry serta revokasi, sebelum menerbitkan akses instalasi. Emailverified mengonfirmasi penguasaan email. Hindari menyebutnya usia atau hubungan keluarga terverifikasi.
Rancangan keadaan yang perlu diterapkan: belumterdaftar, emailbelumterverifikasi, usiabelumdapatditentukan, di bawah13ditolak, dewasaeligible,13–17menungguwali, izinwaliaktif, ditolak/dicabut. Keadaan belumterverifikasi tidak mendapat aksesAI. Parent dan child harus terikat pada permintaan tertentu, versi kebijakan, masa berlaku, perlindungan replay dan kanal pencabutan. Tidak mengunggah KTP/wajah atau mengirim email atas nama pengguna dari alatagent tanpa persetujuan.
Pemilihan metode assurance harus cocok dengan risiko dan kanalAPKwebsite. GooglePlayAgeSignals hanya berlaku pada cakupan tertentu dan aplikasi diperbaruiPlay, jadi belummenjadi jalurwebsite. Modeunknown/under13/pencabutan/restore tidak boleh membukaakses lewatfallback.
Sebelum aktivasi publik, selesaikan penilaianrisiko sesuai sumber resmi/ketentuanlayanan, metode usia/wali yang dipilih, retensi bukti minimum, email/pemberitahuan, abusebudget dan uji endtoend. Jangan menyatakan sekadar checkbox menutup seluruh kewajiban. Dokumen ini bukan implementasi atau keputusan kepatuhan.

Rujukan primer
- https://firebase.google.com/docs/auth/android/email-link-auth
- https://firebase.google.com/docs/auth/admin/verify-id-tokens
- https://firebase.google.com/docs/auth/admin/manage-sessions
- https://developer.android.com/google/play/age-signals/overview
- https://developer.android.com/google/play/age-signals/test-age-signals-api
- https://jdih.komdigi.go.id/index.php/produk_hukum/view/id/1011/t/keputusan%2Bmenteri%2Bkomunikasi%2Bdan%2Bdigital%2Bnomor%2B219%2Btahun%2B2026

Metadata aturan219/2026 menunjukkan perubahan142/2026 tentang indikator/penilaianrisiko. Teksoperatif dan penerapan akhir belum tuntas. Tidak memakai angka usia/klasifikasi yang ditebak dari metadata.

## Checkpoint fondasi server

Aturan eligibility/permintaanwali/keputusan/revoke dan adapter Firebase Admin tersedia lokal.12tes baru lulus, total150backend/0skip/0gagal, termasuk Lua concurrent registration dan konsumsi challenge satu kali. [Kontrak/batas](../../backend/deploy/SELF_SERVICE_ACCOUNTS.md). Belum ada provider assurance nyata, endpoint pendaftaran, layar Android, email, akun produksi atau deploy baru. Tidak menutup PA01/J11/F05 atau HOLD. Bukti email bukan bukti usia/hubungan keluarga. APK/HP tetap signed1.9.55.
