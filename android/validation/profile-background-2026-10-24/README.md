# Latar Profil — 1.6.1, 24 Oktober 2026

Latar sebelumnya memakai garis dengan alpha sekitar 6–13%, sehingga terlihat hampir polos di screenshot. Revisi menggantinya dengan ilustrasi vector berwarna: buku pada rak, lampu kuningan, permukaan meja dan catatan. Area tengah tetap lapang untuk nama, caption dan tombol. Tinggi header, data Profil dan jalur navigasi tidak diubah.

Screenshot native: [Profil](Profile.png), [nama diperbarui](ProfileNamed.png). Ilustrasi adalah resource vector Android, bukan bitmap layar atau gambar konsep. Tidak ada animasi latar baru.

`assembleDebug`, `assembleDebugAndroidTest` dan `lintDebug` berhasil; 0 error lint. Satu pemeriksaan terarah `DailyDesignReleaseTest#testProfileNameValidationPersistenceAndRealProgress` lulus dalam 8,907 detik, memeriksa penolakan nama kosong, penyimpanan nama setelah membuka ulang halaman, dan caption progres Study. Pemeriksaan visual memastikan ilustrasi lebih terlihat dan tidak menimpa nama/tombol.

Log: [build](build.txt), [instrumentasi](instrumentation.txt). Emulator menggunakan gambar Android 37.1 Play Store yang sudah tersedia. Percobaan membatasi guest ke 1536 dan 2560 MB gagal karena low-memory killer sistem mematikan proses; bukan exception pada kode Profil. Pemeriksaan berhasil setelah kembali ke kapasitas normal gambar tersebut, 4096 MB. Aplikasi Google tambahan sempat dinonaktifkan pada satu percobaan dalam instance read-only sementara, bukan pada HP atau data AVD permanen. Instance final dimulai ulang dalam kondisi asal. Emulator sudah ditutup sesudah screenshot; belum diuji HP.

APK: `D:\Download\Zikra-App-main\Zikra-Daily-v1.6.1-debug.apk`, versi 1.6.1 (15). SHA-256: `d95263efaa418fa6ffc5e16d9815fc6c7a1e0a6051f67789ea553512ba8400e1`.

APK berasal dari snapshot 1.6.0 yang sudah diuji, ditambah dua file: `app/build.gradle` dan `res/drawable/daily_profile_scene.xml`. Keduanya dibandingkan byte dengan checkout sebelum pengemasan. Pekerjaan audio Quran yang belum selesai tetap tidak disertakan.
