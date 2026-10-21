# Gerak orb Pendamping Zikra — v1.2.1

Pengguna mengoreksi bahwa orb pada referensi bergerak. Orb v1.2 masih statis;
revisi ini menambahkan gerak di Canvas native yang sudah ada.

- Pita cyan/lavender mengalir, kilau bergerak, putaran halus dan denyut kecil.
  Warna tetap mengikuti acuan; bukan pemutar video, GIF atau rangkaian bitmap.
- Satu callback dengan jeda 50 ms (target batas sekitar 20 fps). Shader dibuat
  saat ukuran berubah; warna, Paint, Rect dan Path digunakan ulang. Pengaturan
  animasi sistem dan penghemat daya mengikuti AmbientMotionPolicy yang ada.
- AiActivity menyalakan animasi hanya saat foreground. Callback dihentikan saat
  pause, hilang fokus, keluar viewport, jendela tidak terlihat atau detach.
  Listener scroll/layout mengaktifkannya kembali bila orb terlihat lagi.
- Tidak menambah izin mikrofon, backend atau kemampuan AI generatif. Gerak ini
  dekoratif; bukan visualisasi amplitudo suara atau tanda model AI sedang bekerja.

APK `Zikra-Visual-Refresh-v1.2.1-debug.apk`, versionCode 4 / versionName 1.2.1.
SHA256: `F476B849C0B1EC6AC3D3DB80DC9B2E7C9AEEDC5DB014CF0ABDF5C312A6D9C1C4`.
Sertifikat debug sama dengan APK 1.2 dan WordSync sebelumnya.

[Build](build.txt) · [hasil terstruktur/hash sumber](result.json).
assembleDebug dan lintDebug lulus dalam 34 detik, satu worker, heap 768 MiB,
tanpa emulator. Lint 0 error / 608 warning; bukan proyek bebas warning.
Tes JVM tidak diulang untuk perubahan visual ini; 87 tes lulus pada ronde v1.2.

Sumber APK terpisah dari eksperimen prefetch audio lama yang tetap dipertahankan
di working tree. Belum diuji gerakan, offscreen/lifecycle, penghemat daya atau
kinerja/baterai pada HP. Belum ada rekaman/screenshot native baru. Pengguna memilih
APK dahulu; build dan inspeksi kode bukan bukti gerak di perangkat atau persetujuan
visual pengguna. Semua batas fitur dari laporan v1.2 tetap berlaku.
