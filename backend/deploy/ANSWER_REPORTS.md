# Pelaporan jawaban AI

Lokal siap diuji, produksi belum aktif. POST /v1/answer-reports memakai bearer instalasi. JSON reason unsafe/inaccurate/other dan excerpt1–500karakter, maksimum4096byte.201 received=true hanya sesudah write/deduplikasi Redis. Client memerlukan receipt valid.
Sebelum aktivasi ZIKRA_REPORTS_ENABLED=true dan deployment, tetapkan pemilik triase, akses Redis terbatas, retensi akun/log/backup, informasi privasi serta uji end-to-end.

Operator meninjau key answer-report:<installation>:<digest> di namespace privat melalui SCAN bertahap. Hindari ekspor massal atau log isi. Nilai hanya reason/excerpt, identitas instalasi berada pada key. Kategorikan dan tindak lanjuti perbaikan/pembatasan konten lalu hapus key setelah ditangani. Jangan kirim kutipan ke penyedia AI. TTL7hari di Redis aktif; backup/log mengikuti kebijakan akun yang harus diverifikasi. Tidak ada dashboard publik atau respons otomatis.
Batas5laporan berbeda per24jam terpisah dari kuota chat. Retry sama idempoten selama key masih ada. Revoked credential ditolak termasuk race sebelum writeLua. Penghapusan instalasi mencakup laporan/rate. Riwayat, foto, profil dan koordinat tidak dilampirkan client. Pengguna bisa menyunting kutipan sebelum kirim.
