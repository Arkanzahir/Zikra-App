# Emulator: proporsi layar dan gerak fokus — 1.3.1

Emulator berhasil dijalankan setelah pengguna meminta mencoba kembali: satu AVD
Medium_Phone, Android 37.1 x86_64, GPU host, Vulkan mati, tanpa jendela/audio,
2 core dan guest RAM 1536 MiB. Layar uji 720×1600, density 320 (360 dp lebar).
Alokasi guest bukan total RAM host: proses emulator tetap memakai beberapa GiB.
Emulator ditutup setelah pengambilan bukti; adb kosong dan RAM bebas 8,92 GiB.
HP pengguna tidak dipakai.

## Koreksi dari screenshot aktual

Home: header/countdown lebih ringkas; sumber langit dan pengingat tetap melalui
ikon informasi, kalender melalui tanggal. Study/AI: heading, kartu dan jarak
lebih padat agar isi terlihat. Fokus: ring dan ilustrasi lebih kecil; tombol
mulai/jeda di luar area gulir sehingga tetap terlihat, termasuk teks 200%.
Tes piksel menemukan karakter masih bergerak ketika timer dijeda. Kondisi
animasi kini memerlukan timer running; break aktif tetap bergerak, pose paused
berhenti. Tidak mengubah waktu timer, SQLite, data tugas atau audio.

## Bukti aplikasi asli

- [Home](home.png), [Study](study.png), [AI](ai.png), [katalog Quran](quran.png).
- [Fokus siap](focus.png), [break aktif](break.png), [paused](focus-paused.png),
  [font 200%](focus-large-text.png).
- [Video delapan detik break aktif](break-active.mp4): diambil setelah fase
  break terkonfirmasi; [dua frame karakter fokus](character-motion-a.png) dan
  [frame berikutnya](character-motion-b.png) membuktikan perubahan piksel.
- Label `[DATA UJI]` berasal dari tugas sementara instrumentasi, dihapus teardown.
  Screenshot di atas bukan mockup; ilustrasi Canvas lebih sederhana dari konsep
  generated dan tidak diklaim identik piksel.

## Hasil dan kegagalan yang ditindaklanjuti

[Build](build.txt): assembleDebug, assembleDebugAndroidTest, lintDebug lulus
27 detik, satu worker, heap 512 MiB, offline. Tidak mengulang 96 JVM atau 9 tes
backend tahap 1.3.0 karena bagian tersebut tidak berubah.

[Empat tes Android final](android-tests.txt) lulus dalam 18,855 detik:
1. Break hanya dimulai lewat aksi, karakter berubah piksel, countdown berjalan.
2. Piksel karakter fokus bergerak lalu berhenti saat pause; waktu tetap.
3. Teks 200%: label terpisah dari ilustrasi, seluruh tinggi tombol terlihat.
4. Pause tidak memulai break, recreate mempertahankan sisa waktu, resume fokus.

[Tes rekaman fokus](focus-preview-test.txt) sebelumnya lulus pada kandidat 1.3.1
sebelum koreksi pause. Percobaan awal [terhalang izin notifikasi](initial-permission-failure.txt):
GrantPermissionsActivity berada di depan dan render Activity berhenti. Izin
POST_NOTIFICATIONS diberikan hanya di emulator sebelum pengujian lanjutan.
[Tes piksel awal](initial-pause-motion-failure.txt) menemukan bug gerak saat pause,
lalu kode diperbaiki dan empat tes final diulang. Tidak menyembunyikan kegagalan
awal sebagai hasil lulus. [Buffer crash](crash-log.txt) tidak memuat crash.

## Batas

Belum tes ulang audio qari/sorotan/gulir pada versi ini, belum validasi performa,
baterai atau notifikasi/adzan di HP Xiaomi. Emulator tanpa audio tidak membuktikan
suara terdengar. Home/langit dan orb terlihat pada screenshot; pergerakannya belum
diaudit piksel di sesi ini. Backend Featherless belum di-host/configure/live.
Eksperimen prefetch audio lokal tetap tidak disertakan pada build terisolasi.

APK luar repo: `Zikra-Concept-Refresh-v1.3.1-debug.apk`, versionCode 6.
SHA256: `ede22a74561b855935e85f183070aac7006bc2dd1291e6489bf5431ed43dee64`.
