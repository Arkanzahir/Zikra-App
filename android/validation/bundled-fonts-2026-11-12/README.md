# Audit font yang dibundel — 12 November 2026

Pemeriksaan terbatas pada tiga font lokal dan notice-nya, bukan persetujuan
seluruh konten atau distribusi beta. APK 1.9.41 tetap, tidak dibangun ulang,
tidak memakai HP/emulator dan tidak mengganti desain/font/pengaturan pengguna.

## Bukti

[Receipt APK](apk-fonts.json) memeriksa hash ketiga font dan notice pada source
serta ZIP APK. Inventaris seluruh res/font cocok, tidak ada tambahan di luar
[manifest](../../../docs/beta/bundled-fonts.json). Semua lulus pada SHA APK
29ad8fb40f6449efe1678df949665075a4a54394a06bb44187b21e807e261321.

[Lima tes](tests.txt) lulus: paket cocok tidak menyetujui distribusi, notice
hilang, font berubah, font tambahan tanpa inventaris dan source drift ditolak.
Tidak ada build aplikasi atau tes Android baru untuk perubahan tool ini.

Pemeriksaan ulang dari root repo (output harus baru):

```powershell
python tools/audit_bundled_fonts.py --apk <paket.apk> --output .local/font-audit-baru.json
python -m unittest discover -s tools -p test_audit_bundled_fonts.py -v
```

## Peninjauan sumber primer

- [Amiri](https://github.com/aliftype/amiri) menyatakan OFL; notice lokal memuat
  identitas Amiri Quran dan copyright. Bukti asal/version sebelumnya tetap di
  ../quran-font-ui09-2026-10-30/font-source.json.
- [Plus Jakarta Sans](https://github.com/google/fonts/tree/main/ofl/plusjakartasans)
  dan [Roboto](https://github.com/google/fonts/tree/main/ofl/roboto) memiliki OFL.txt.
  Notice lokal mempertahankan nama proyek/copyright masing-masing. Ini bukan
  klaim binary lokal sama dengan versi terbaru upstream.
- [OFL resmi](https://openfontlicense.org/open-font-license-official-text/)
  mengizinkan bundling bersyarat dan meminta copyright serta lisensi disertakan;
  ketentuan reserved name/versi modifikasi tetap berlaku. Pemeriksaan hash hanya
  membuktikan kesetaraan dengan pin yang ditinjau, bukan audit hukum otomatis.

## Sisa gerbang

Font halaman QCF v2 dan nama surah diunduh oleh MushafRepository dari layanan
Quran Foundation, bukan tiga TTF yang dibundel. Hak font tersebut harus diperiksa
terpisah; OFL Amiri tidak dapat dipakai untuk mengesahkannya. Hak teks/terjemahan,
timing dan rekaman Quran juga terpisah dari font. B01 dan hak/lafaz adzan tetap
terbuka. Jangan menyatakan seluruh G03, A–D atau beta selesai dari laporan ini.

Privasi/kontak/signing/migrasi tetap mengikuti docs/beta/README.md; tidak membuat
kontak atau sertifikat fiktif, menerima ketentuan atau mengirim email. Tidak
mengubah runtime/backend/APK/konfigurasi privat/WIP pengguna.
