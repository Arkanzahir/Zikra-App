# Operasional laporan jawaban AI

Aktif pada produksi15November2026 setelah persetujuan eksplisit pengguna. PengelolaZikra dikonfirmasi memakai kontak dukungan arkanzahir46@gmail.com. Tidak mengirim email otomatis. Laporan hanya memuat reason/excerpt maksimal500karakter, authenticated per instalasi, batas5laporan berbeda/24jam, retryidempoten dan TTL7hari di Redis aktif. Salinan log/backup penyedia tetap perlu verifikasi akun.

Alat lokal backend/answer-report-admin.mjs tidak ikut deployment. KonfigurasiRedis dimuat dari lingkungan privat. Contoh dari rootrepo berikut memakai path absolut milik pengguna, direktori .local harus sudah ada.

    node backend/answer-report-admin.mjs inspect D:/Download/Zikra-App-main/Zikra-App-main/.local/reports-review.json

File output harus baru dan privat. Terminal hanya menampilkan jumlah/truncated. Buka file secara privat, tinjau reason/excerpt dan catat tindakan yang diperlukan. SCAN maksimum20batch/50laporan, truncated berarti belum seluruh antrean. Jangan menyalin isi ke Git/log atau meneruskannya ke penyediaAI.
Setelah ditangani, gunakan reference persis dari file untuk resolve dengan konfirmasi sama.

    node backend/answer-report-admin.mjs resolve REFERENCE --confirm-report REFERENCE

Resolve hanya menghapus satu keylaporan. Tidak menghapus data pengguna/registry/kuota lain. TTL tetap berlaku bila belumditangani; periksa antrean secara rutin agar laporan tidak terlewat sebelum kedaluwarsa. Ini prosedur operasional manual, bukan jadwal otomatis atau janjiSLA.
Uji3unitoperator dan laporan sintetis produksi lulus. Uji kirim nyata melalui UIHP masih terbuka. Untuk menonaktifkan penerimaan, ubah ZIKRA_REPORTS_ENABLED=false dan deploy sesuai prosedur.
