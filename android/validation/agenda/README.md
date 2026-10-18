# Study/Hifz sebagai agenda — 18 Oktober 2026

Tahap ini melanjutkan pemulihan `f08c42d`, khusus desain Study Hub dan Diary Hifz. Header kembali/judul, font Plus Jakarta Sans, warna Pine dan dock bersama dipertahankan. Quran tetap reader; pola agenda tidak diterapkan ke halaman bacaan atau semua fitur secara otomatis.

## Perubahan yang dapat dicoba

- Latar ivory dan judul lebih tegas; progres ringkas tanpa panel besar. Seluruh isi agenda ikut digulir di bawah header tetap; tombol tambah dan dock berada di luar area scroll.
- Panel **Lanjutkan hari ini** berisi kegiatan belum selesai pertama dalam kategori terpilih. Urutan tetap urutan pengguna; target Hifz terstruktur mendahului catatan lama. Ini tidak mengklaim prioritas/deadline otomatis. Kegiatan panel tidak diulang dalam daftar di bawahnya.
- Baris lainnya memakai pemisah dan penanda kategori; bagian **Selesai** dilipat secara default dan dapat dibuka/dicentang kembali. Filter dan status lipatan bertahan saat Activity dibuat ulang. Progres mengikuti kategori terpilih, termasuk target panel.
- Hifz memakai struktur XML header yang sama, menampilkan surah/rentang dan tindakan bacaan sesuai sumber. Empat surah lokal tetap native; lainnya berlabel Quran.com dan membuka browser. Catatan lama tetap “Lengkapi target”, tanpa ayat rekaan.
- Tidak ada perubahan schema SQLite, API/provider atau sumber branding. Editor/dikte tetap memerlukan Simpan; daftar target dan status pengguna tidak dihapus untuk mengganti desain.

## Verifikasi

JDK17, Gradle8.9/AGP8.7.3, compile/target35, min26. Emulator AOSP API28 software rendering, bukan HP fisik. Log build/lint final: [final-build.txt](final-build.txt). Hasil UI mencakup **lima skenario unik yang akhirnya lulus**: tiga lulus dalam [putaran utama](final-device-tests.txt), dua alur sentuh lulus setelah [perbaikan runner](touch-regression-tests.txt). [Regresi catatan Hifz](note-regression-tests.txt) mengulang satu skenario setelah catatan ditampilkan di panel utama. Log putaran utama tetap mencatat satu failure dan satu error dari runner sentuh awal, bukan disembunyikan sebagai suite yang langsung bersih.

Pemeriksaan kode memperbaiki enabled state tombol Selesai; runner kemudian memakai input touchscreen nyata. Dua kegagalan putaran utama berasal dari event tanpa `SOURCE_TOUCHSCREEN`, yang diperbaiki sebelum kedua alur diulang. Opacity seluruh baris selesai dihapus agar teks tetap memiliki kontras yang cukup. Putaran sebelumnya yang masih menggunakan `performClick` ada di `device-tests.txt`; itu tidak dijadikan bukti tap fisik.

Perintah dari root:

```bash
source /workspace/setup-zikra/env.sh
./android/gradlew -p android assembleDebug assembleDebugAndroidTest lintDebug --console=plain
adb -s emulator-5558 install -r android/app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5558 install -r android/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5558 shell am instrument -w -r -e class com.zikra.app.StudyRecoveryTest com.zikra.app.test/android.test.InstrumentationTestRunner
```

Suite UI mencakup perpindahan kegiatan yang dicentang, hitungan per kategori, bagian selesai memakai input sentuh emulator, pemulihan state setelah recreate, editor/validasi/dikte simulasi, metadata katalog live, target dan rute reader lokal. Tes menggunakan contoh sementara dan mengembalikan completion kegiatan yang disentuh serta bookmark semula. Suite data/migrasi yang tidak berubah tidak diulang; hasil historis 12 tes ada di folder recovery.

Screenshot menggunakan PNG fisik `screencap` dari APK; tanpa edit piksel. Contoh surah/kegiatan tambahan adalah data uji yang dibersihkan setelah tes. Preview: [PDF](preview.pdf) dan [HTML mandiri](preview.html), dengan gambar tertanam sehingga tidak membutuhkan path relatif. Screenshot standar berada di `screenshots/`; layar kecil di `small-screenshots/`. API35 runtime, keyboard/pengenal suara fisik, TalkBack, FPS/baterai dan semua fitur aplikasi belum diuji pada tahap desain ini.


## Layar kecil dan batas hasil

Pada 540×1170 dengan density 240 (sekitar **360×780dp**) dan font scale **1,3**, tampilan agenda dan bagian Selesai ditangkap sebelum kegagalan. Tap membuka bagian Selesai berhasil, tetapi `testAgendaCompletionFilterAndRecreation` **tidak lulus seluruhnya**: setelah Activity dibuat ulang, fokus diambil dialog ANR **com.android.systemui**. Log ActivityManager mencatat broadcast `TIME_TICK` System UI dan beban software emulator sekitar 95%, tanpa exception AndroidRuntime aplikasi. Ini tidak diklaim sebagai keberhasilan recreate layar kecil atau bukti performa HP.

Percobaan awal dengan override 480×1040/density213 juga tertahan System UI dan dicatat di `small-system-ui-initial.txt`; gambar dialog ada di `diagnostics/`. Percobaan kedua memakai density saja setelah restart System UI; hasil mentah tetap di [small-large-text-tests.txt](small-large-text-tests.txt), dan diagnosis di [small-runtime-errors.txt](diagnostics/small-runtime-errors.txt). Tidak ada assertion yang dinonaktifkan. Regresi recreate pada konfigurasi standar sudah lulus; batas konfigurasi kecil tetap perlu diuji pada emulator yang stabil atau HP fisik.

Sesudah pemeriksaan, ukuran/density dikembalikan ke fisik540×1170/density210, font scale1,0, animator_duration_scale1. System UI dimulai ulang pada konfigurasi standar. SQLite read-only `quick_check=ok`, 24 kegiatan contoh dengan 8 selesai, nol baris sementara; data tugas yang disentuh dikembalikan. Screenshot standar dan contoh test tidak mewakili data pribadi pengguna yang hilang dari instance sebelumnya.
