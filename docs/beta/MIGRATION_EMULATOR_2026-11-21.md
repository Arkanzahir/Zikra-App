# Uji pembaruan Android di emulator — 21 November 2026

`adb install -r` berhasil memperbarui `com.zikra.app` pada emulator Android 16/API 36 yang offline dari 1.9.55/code 82 ke 1.9.58/code 85. ID aplikasi di perangkat, direktori data, dan waktu instalasi awal cocok. Tidak ada uninstall di antara kedua pemasangan pada run ini.

| APK | Sumber | SHA-256 |
|---|---|---|
| 1.9.55 | Rebuild signed commit historis `83bc6db` | `7737da81b98dc404defb8373f7407f2b75c38c83c67511b9b9a810dba5dfbdd1` |
| 1.9.58 | Kandidat signed yang sudah diaudit | `dacd64ab3066de1d3062e018942e4e05f1e970b6795c60d19348763fb5355907` |

Kedua APK memiliki signer SHA-256 `c65c40ddc15dce2d4390496585e28a41bee95c2ca72f47791fed77a50431deeb`. APK 1.9.55 historis yang pernah terpasang di HP belum ditemukan di kandidat lokal. Rebuild memakai versi, source commit, package name, dan signer yang tercatat, dengan hash artefak berbeda.

| Data sintetis | Integritas SQLite setelah update | Isi fixture |
|---|---|---|
| Tugas dengan status selesai dan prioritas | Lulus | Cocok |
| Riwayat AI dengan draf | Lulus | Cocok |
| Bookmark hadis lama | Lulus | Preferensi utama dan indeks cocok |
| Jadwal kelas dengan pengingat dan SKS | Lulus | Cocok |

Splash, Study, AI, Hadis, dan layar akun 18+ menyelesaikan pembukaan pada kandidat. Emulator awal sempat offline. Boot dengan Wi-Fi virtual dinonaktifkan dan SwiftShader memulihkan uji layar. Pada percobaan awal, indeks bookmark sempat dibangun ulang dari preferensi kosong. Uji akhir mengisi sumber utama `hadith_bookmarks` pada 1.9.55 sebelum update dan mencocokkan preferensi serta indeks pada 1.9.58.

Bukti ini mencakup satu emulator dan empat fixture sintetis. Migrasi HP dari APK historis, SAF dan suara adzan fisik, serta alur akun dengan email terverifikasi masih terbuka. HP terakhir terverifikasi pada 1.9.55. Emulator dan server ADB khusus telah dihentikan.
