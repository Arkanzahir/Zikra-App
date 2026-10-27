# Workflow proyek Zikra

- Sebelum melanjutkan fitur, baca `IMPLEMENTATION_PLAN.md`; gunakan sebagai checklist aktif dan baca laporan bukti untuk modul terkait. Catatan lama tidak mengalahkan status terbaru yang sudah dibuktikan.
- Kerjakan satu ID aktif sampai syarat selesainya terpenuhi. Ketika selesai, centang item, tambahkan tanggal/tautan bukti, dan perbarui jurnal rencana bersama checkpoint kode.
- Kode tersedia, build berhasil, mockup atau respons simulasi tidak otomatis berarti fitur selesai. Biarkan item belum dicentang bila masih membutuhkan pemeriksaan HP, sumber data, atau server produksi.
- Periksa status Git dan pertahankan WIP/rahasia privat. Jangan stage seluruh working tree. Bedakan fitur lokal, koneksi API langsung, dan backend yang benar-benar sudah dihosting.
- Instruksi pengguna 27 Oktober 2026: setiap checkpoint perubahan proyek yang selesai dan diverifikasi harus di-commit dan dipush ke `origin/main`, bukan dibiarkan hanya lokal. Audit isi commit sebelum push, gunakan fast-forward tanpa force, dan verifikasi hash remote sesudahnya. WIP yang diminta dipertahankan, stash audio, konfigurasi privat dan data pengguna tetap di luar commit; jika push gagal, laporkan status sebenarnya.
- Gunakan build/tes terarah yang hemat RAM; tutup emulator selesai dipakai. Perubahan dokumen tidak memerlukan build aplikasi atau emulator.
- Urutan yang diperjelas pengguna 24 Oktober: tuntaskan A03 lalu A04 sebelum melanjutkan B01. Penelitian B01 terdahulu tidak berarti A03/A04 boleh dilewati.
- Terapkan keamanan selama pengerjaan: provider key hanya di backend, konfigurasi privat tidak masuk APK/Git/log, tidak bypass verifikasi TLS, validasi input/URL/berkas dari luar dan jangan mengunggah data pribadi tanpa alur izin yang jelas. Kode gateway pribadi bukan autentikasi produksi; pemeriksaan keamanan publik tetap C01/C02/G03.
