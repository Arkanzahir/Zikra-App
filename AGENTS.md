# Workflow proyek Zikra

- Sebelum melanjutkan fitur, baca `IMPLEMENTATION_PLAN.md`; gunakan sebagai checklist aktif dan baca laporan bukti untuk modul terkait. Catatan lama tidak mengalahkan status terbaru yang sudah dibuktikan.
- Kerjakan satu ID aktif sampai syarat selesainya terpenuhi. Ketika selesai, centang item, tambahkan tanggal/tautan bukti, dan perbarui jurnal rencana bersama checkpoint kode.
- Kode tersedia, build berhasil, mockup atau respons simulasi tidak otomatis berarti fitur selesai. Biarkan item belum dicentang bila masih membutuhkan pemeriksaan HP, sumber data, atau server produksi.
- Periksa status Git dan pertahankan WIP/rahasia privat. Jangan stage seluruh working tree. Bedakan fitur lokal, koneksi API langsung, dan backend yang benar-benar sudah dihosting.
- Instruksi pengguna 27 Oktober 2026: setiap checkpoint perubahan proyek yang selesai dan diverifikasi harus di-commit dan dipush ke `origin/main`, bukan dibiarkan hanya lokal. Audit isi commit sebelum push, gunakan fast-forward tanpa force, dan verifikasi hash remote sesudahnya. WIP yang diminta dipertahankan, stash audio, konfigurasi privat dan data pengguna tetap di luar commit; jika push gagal, laporkan status sebenarnya.
- Gunakan build/tes terarah yang hemat RAM; tutup emulator selesai dipakai. Perubahan dokumen tidak memerlukan build aplikasi atau emulator.
- Urutan yang diperjelas pengguna 24 Oktober: tuntaskan A03 lalu A04 sebelum melanjutkan B01. Penelitian B01 terdahulu tidak berarti A03/A04 boleh dilewati.
- Terapkan keamanan selama pengerjaan: provider key hanya di backend, konfigurasi privat tidak masuk APK/Git/log, tidak bypass verifikasi TLS, validasi input/URL/berkas dari luar dan jangan mengunggah data pribadi tanpa alur izin yang jelas. Kode gateway pribadi bukan autentikasi produksi; pemeriksaan keamanan publik tetap C01/C02/G03.

## Prioritas pengguna27 Oktober2026: tuntaskan D dahulu

D06 selesai teknis beta dengan pembaruan APK berisi data; [bukti](android/validation/storage-d06-2026-10-27/README.md). D03/D04/D05 juga selesai teknis beta. D01/D02/D02a tetap terbuka untuk pemeriksaan fisik dan sumber/ketentuan audio. Jangan pindah ke E atau menyebut seluruh D/publik selesai; kerjakan bukti yang bisa dilakukan tanpa HP dahulu. Pengguna menunda HP: jangan berulang meminta sambungan. Semua checkpoint terverifikasi tetap commit/push origin/main; PROJECT_HANDOFF.md WIP tetap di luar commit.

## Status28 Oktober2026 — sesi HP D01

D01 selesai teknis beta terarah dengan [bukti fisik/manual/native/preservasi](android/validation/location-phone-d01-2026-10-28/README.md). Ini menggantikan status D01 terbuka pada catatan27 Oktober. D02/D02a tetap terbuka untuk pengujian yang tersisa dan ketentuan audio. HP disambungkan/diizinkan untuk sesi28 Oktober; jangan menganggap izin ini berarti otomatis tersedia pada sesi lain. Runtime tetap1.9.15; B01/C/E/F/G/publik tidak otomatis selesai.

## Status HP28 Oktober2026 — beta1.9.22

Prioritas terbaru pengguna tetap A–D dahulu. [Bukti HP/adzan terbaru](android/validation/prayer-phone-2026-10-28/README.md) menghitung uji pagi, mode/epoch, pilihan audio terakhir dan preservasi; D02/D02a tetap terbuka untuk ketentuan/sisa HP. C02 monitor terjadwal sudah teramati, laptop-mati tetap perlu. Masukan tema global/dock AI/lampu malam/tanggal strip belum diperbaiki oleh checkpoint audio; ikuti handoff. Jangan menimpa refresh lokasi/izin yang dikonfirmasi pengguna atau mengunggah chat/GPS/backup.

## Status29 Oktober2026 — tema global beta1.9.23

Permintaan terbaru pengguna untuk tema konsisten lintas fitur selesai pada F03a dengan [bukti](android/validation/global-theme-2026-10-29/README.md). Home/Profil/Quran/Mushaf memakai satu pilihan; otomatis Quran mengikuti tema perangkat, bukan lux. Palet/dialog/dock/input dan restore sakelar reader diperbaiki; draf AI terpelihara.153 unit/4 skip dan9 native lulus; lint0 error/860 warning. HP tidak diubah; gerbang A–D/F03/publik, dock AI, lampu malam dan splash native tetap mengikuti handoff. Ini menggantikan status tema-belum-diperbaiki untuk checkpoint audio, tanpa menutup pekerjaan lainnya.

## Status29 Oktober2026 — UI06 beta1.9.24

Pengguna memprioritaskan dock AI, lampu masjid malam dan splash sebelum C04; ketiganya selesai teknis beta terarah dengan [bukti native/video/batas](android/validation/ui-followup-2026-10-29/README.md).155 unit/4 skip,11 native lulus; lint0 error/862 warning. HP belum diubah/diuji untuk versi ini; Figma canvas lama tetap belum selesai. C04 berikutnya; A–D/F04/G/publik tidak otomatis selesai. Tema global1.9.23 tetap dan regresi back stack/draf lulus. Pertahankan WIP, stash, data, konfigurasi privat dan desain/ikon asli; checkpoint tetap audit/commit/push.

## Status29 Oktober2026 — UI07 beta1.9.25

Polesan splash800ms disetujui lalu diterapkan. [Bukti HP/preservasi/batas](android/validation/splash-polish-2026-10-29/README.md):4 native API36,155 unit/4 skip lulus;lint0 error/862 warning. HP aktual1.9.22 diperbarui ke1.9.25;32 berkas/17 tabel/preferensi/izin/pengaturan terjaga, helper dibersihkan. USB kemudian dilepas; jangan menganggap tersedia. Lampu fasad sudah terlihat, interior lengkungan belum bercahaya. Font Arab terjemahan masih fallback HP, berbeda dari QCF Mushaf; keduanya bukan perubahan checkpoint splash. C04/A–D/F04/G/publik tetap terbuka, WIP/stash/env/ikon/artwork tetap.
