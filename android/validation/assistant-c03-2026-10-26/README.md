# C03 — koneksi AI dan alur kegagalan

26 Oktober 2026, beta1.9.7/code34, source `249b523`. Penutupan C03 mengikuti receipt dan log final
di folder ini; bukan penutupan C02/C04/C06/C07, seluruh C atau rilis publik.
Backend Vercel/Redis tidak diubah atau dipasang ulang.

## Perubahan

- `AssistantNetwork` memisahkan offline, jaringan belum tervalidasi, portal login
  dan online. Callback memakai snapshot kemampuan dari Android pada main handler;
  perubahan metadata dengan status sama tidak menghapus error layanan. Registrasi
  dilepas saat layar ditinggalkan. Tidak menambah polling atau antrean upload.
- `OnlineAssistantClient` membedakan timeout/putus aliran ketika membaca body dari
  JSON rusak. Error HTTP tetap dipertahankan ketika halaman error bukan JSON.
  Deadline45 detik, HTTPS, verifikasi sertifikat, larangan redirect dan tanpa retry
  otomatis tetap berlaku. Tes deadline memakai deadline150ms pada client uji,
  bukan menurunkan deadline produksi atau mengubah TLS.
- Menyimpan koneksi baru membatalkan pekerjaan aktif/consent lama dan menyegarkan
  ketersediaan gambar. Balasan koneksi lama tidak boleh masuk ke chat. Kode akses
  tetap terenkripsi dengan Android Keystore; key penyedia tetap hanya backend.
- Baris status tidak boleh menekan tombol mode hingga nol lebar. Teks besar
  memperoleh lebar baris yang tersedia; pesan gagal/batal dan tombol coba lagi
  digulirkan ke area terlihat. Desain/aset Home dan data aplikasi dipertahankan.

## Bukti final

| Pemeriksaan | Hasil dan batas |
| --- | --- |
| [Build final](build-final-runtime.txt), [build/unit](build-delivery.txt), [unit](unit-results.json), [lint](lint-summary.txt) |116 unit lulus/4 skip,0 gagal; lint0 error/753 warning. Unit tidak diulang untuk perubahan view/gulir terakhir; native mencakup APK final. Tidak menyebut warning/skip selesai. Build hemat RAM, satu worker, emulator mati saat build. |
| [C03 native](native-c03-contracts.txt) |7 metode: kemampuan jaringan kosong, timeout/putus saat membaca body, deadline aktual pada OkHttp dengan interceptor, retry dengan consent baru, mode manual/persistensi/draf lokal, validasi serta pergantian koneksi dengan balasan tertunda, pembatalan background dan pergantian mode saat menunggu. Beberapa skenario digabung dalam satu metode; respons gateway sintetis, bukan provider nyata. |
| [Regresi client/layar](native-assistant-regression.txt) |6 metode; termasuk auth/kuota/timeout/server/sumber/JSON rusak dan tambahan halaman HTML pada401/502, pembatalan, review tugas/consent/draf suara. Callback suara tetap simulasi pada suite ini. |
| [Putus/pulih](native-auto-network.txt), [gambar](native-image-cancel.txt) |2 metode regresi, jaringan emulator diputus/pulih sungguhan; draf tidak dikirim sendiri, pembatalan gambar/chat baru tetap menolak hasil tertunda. Tidak mengulang inference Cloudflare. |
| [Portal](native-network-captive_portal.txt), [tanpa internet](native-network-unvalidated.txt), [probe](network-probes.json) |2 eksekusi metode yang sama. Wi-Fi Android nyata tetap tersambung dengan VALIDATED hilang; CAPTIVE_PORTAL dibuktikan pada skenario portal. Android NetworkMonitor memakai server probe sintetis localhost, mengikuti fasilitas AOSP CTS. Mode manual online mempertahankan draf, pemulihan tidak melakukan upload otomatis. Bukan pengujian jaringan hotel/operator tertentu. |
| [Teks200%/layar sempit](native-timeout-narrow-200.txt), [keyboard](native-keyboard-narrow-200.txt) |2 eksekusi: pesan timeout/retry dan target mode terlihat penuh; target kirim/+ utuh saat keyboard benar-benar terbuka. Override640×1280/density asli emulator. |
| [Total native](native-results.json) |19 eksekusi/17 metode unik final. Tidak menyamakan hitungan eksekusi dengan metode unik atau mengganti bukti provider/HP sebelumnya. |
| [Preservasi](preservation.json), [audit](audit.json) |10 berkas internal asli emulator cocok hash. Probe DeviceConfig/global, Wi-Fi/data, font, ukuran layar, stay-awake dan keyboard dipulihkan; helper dihapus, emulator ditutup. Audit nilai rahasia yang diketahui bersih pada source/evidence/APK; bukan sertifikasi seluruh riwayat Git. |

Sumber metode jaringan: [Android membaca status jaringan](https://developer.android.com/develop/connectivity/network-ops/reading-network-state),
[fasilitas URL probe localhost AOSP](https://android.googlesource.com/platform/packages/modules/Connectivity/+/8f265c9b436b730aadf8636f1bd7a2bb2a9de930/staticlibs/framework/com/android/net/module/util/NetworkStackConstants.java),
[tes portal AOSP](https://android.googlesource.com/platform/packages/modules/Connectivity/+/79a504b227bbc776e95d268a5275c713fdba05a2/tests/cts/net/src/android/net/cts/CaptivePortalTest.kt).
URL probe hanya dipakai pada emulator dengan kedaluwarsa kurang dari10 menit;
tidak dimasukkan ke konfigurasi aplikasi atau perangkat pengguna.

Percobaan awal tidak dihitung lulus: fixture memakai API Android yang tidak publik,
host salah memperlakukan marker belum ada/forward belum dibuat sebagai kegagalan,
dan assertion lebar mode terlalu besar untuk label Offline. Kegagalan cleanup host
dipulihkan manual dan10 hash berkas asli diperiksa sebelum pengulangan. Screenshot
juga menemukan baris mode tertekan dan pesan error teks besar di luar viewport;
layout/gulir serta assertion tampak penuh kemudian diperbaiki dan diuji ulang.

## APK dan lanjutan

[Receipt APK/source hashes](apk-receipt.json), [signer](signature.txt).
`Zikra-AI-v1.9.7-beta-debug.apk` di folder pembungkus,37.218.400 byte, installer kurang dari100MB,
signer debug sama; SQLite utama4/chat1. Paket lama dan WIP tetap ada. Belum signed
release publik. Semua source/runtime pada receipt harus cocok dengan checkpoint
Git yang ditambahkan saat penyimpanan hasil.

Contoh layar: [portal login](native/AssistantC03-captive_portal-100.png),
[tanpa internet](native/AssistantC03-unvalidated-100.png),
[timeout teks200%](native/AssistantC03-timeout-200.png),
[keyboard sempit](native/AssistantC03-keyboard-narrow-200.png).

HP sudah dideteksi dan12 berkas aslinya dicadangkan privat untuk pekerjaan C06;
belum dipakai sebagai bukti C03 baru. Bukti provider/HP C05 tetap pada laporan
sebelumnya. Berikutnya C06 input suara nyata, lalu C07 kamera/picker/izin dan
variasi OCR. C02 operasional/laptop mati serta C04/jadwal D04 masih terbuka.
B01 belum mendapat balasan pada pemeriksaan26 Oktober; tidak menganggap diam
sebagai izin sumber. Memori permanen, D02a, F/G dan rilis publik belum selesai.
