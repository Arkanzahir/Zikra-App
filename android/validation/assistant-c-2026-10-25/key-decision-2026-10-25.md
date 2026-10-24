# C01 — keputusan key dan penutupan persiapan konfigurasi

25 Oktober 2026 (WIB). Pengguna menyatakan: “gausah rotasi lah kan sudah ada key nya gimana dah”.
Keputusan aktif: gunakan key yang sudah ada; batalkan kewajiban rotasi sebagai
prasyarat C01/deploy. Ini menggantikan keputusan menunda rotasi sampai sebelum
publik. Jangan meminta rotasi ulang sebagai gerbang pekerjaan berikutnya.
Key tidak dirotasi dan konfigurasi privat tidak diubah.

C01 selesai **persiapan konfigurasi**, berdasarkan bukti yang terpenuhi:

| Kriteria | Bukti |
| --- | --- |
| Env privat valid, server dapat dijalankan ulang | Pemeriksaan ulang `backend/check-private.mjs`, exit 0; [receipt](key-decision-restart.json) |
| Dua start pada port loopback; health minimal 200 dan tanpa token 401 | Kedua proses berhenti; log tidak mengandung nilai secret, tanpa panggilan provider ulang |
| Model yang dikonfigurasi dapat digunakan | [Completion nyata awal](../backend-c01-2026-10-25/private-live-check.json), [proposal dan Tadabbur nyata lanjutan](live-local.json), HTTP 200; data sintetis |
| Key/token saat ini tidak ditemukan pada source/APK yang diperiksa | [Pemindaian 1.8.0 dan preservasi](secret-and-preservation.json): 1.506 file/399 entri APK, nol kecocokan secret yang dikenal; bukan audit semua secret historis |
| Unduhan awal di bawah 100.000.000 byte | APK 1.8.0 beta tetap 31.362.014 byte; [receipt](release.json), source `7982e10` |

Pemeriksaan ulang menggunakan Node v22.17.0, loopback privat; penghentian proses
Windows tidak menjadi bukti SIGTERM/restart otomatis Linux. Tidak membangun APK
atau menjalankan emulator untuk perubahan dokumentasi ini. Receipt terdahulu
tetap menyimpan `providerKeyRotated: false` sesuai fakta, bukan gerbang aktif.

C02–C06 tetap terbuka: hosting permanen belum ada, paket Docker/proxy belum
dijalankan, rantai APK/HP/provider dan dikte nyata belum diuji, konteks jadwal
kelas menunggu D04. Seluruh C, B01 dan rilis publik belum dinyatakan selesai.
D02a tetap tugas pengingat adzan yang belum diimplementasikan. WIP
`PROJECT_HANDOFF.md`, env privat, stash audio dan desain dipertahankan.
