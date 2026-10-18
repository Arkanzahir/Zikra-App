# Pembangunan ulang dari referensi chat

## Checkpoint 1: branding dan taman onboarding

Implementasi baru berdasarkan arahan dan screenshot pengguna; sumber asli versi taman belum ditemukan. Tetap Java/XML native.

- Master `design/branding/zikra-logo-primary.jpg` tidak berubah. Varian Pine/putih memakai alpha, kanvas dan geometri identik melalui skrip deterministik yang diizinkan pengguna. Spark dipertahankan.
- Komponen XML logo bersama dipakai splash/onboarding, header Home dan Tentang. Adaptive launcher memakai simbol dari master.
- Tiga cerita menggantikan jenjang/minat. Status langkah bertahan saat recreation, preferensi lama tidak dihapus.
- Ilustrasi taman baru dibuat dari brief referensi: pohon/meja belajar, masjid dan kolam. Tidak diklaim sebagai aset asli video. Canvas menambahkan awan bertingkat, float, parallax sentuh/tilt, dibatasi 30fps; berhenti di background, animator scale 0 atau hemat daya.
- Header Home baru hanya branding/lokasi; langit Home masih tahap berikutnya.

## Pemeriksaan satu putaran

JDK 17, SDK 35, emulator API 28 software rendering. `assembleDebug assembleDebugAndroidTest lintDebug` lulus; tiga tes perangkat lulus: gerak vs pause, skip/Home/ikon aktual, tiga cerita/recreation/preservasi preferensi. Log [build](branding-onboarding-final-build.txt) dan [tes](branding-onboarding-tests.txt). Sensor tilt, kelancaran dan baterai HP fisik belum diuji.

Screenshot berasal dari APK yang dijalankan, bukan mockup:

| Onboarding | Home branding | Ikon aktual |
| --- | --- | --- |
| ![Cerita 1](screens/RebuiltOnboarding1.png) | ![Home](screens/RebuiltHomeBrand.png) | ![Ikon](screens/RebuiltAppIcon.png) |

[Cerita 2](screens/RebuiltOnboarding2.png) · [Cerita 3](screens/RebuiltOnboarding3.png).
