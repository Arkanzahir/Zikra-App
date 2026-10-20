# Audit lokal Quran — 21 Oktober 2026 (WIB)

Baseline `9f97bd44d473df8f4686f129a7be5212de09cdda`, main sama dengan origin/main
setelah fetch; checkout awal bersih. **Validasi audio asli Android belum selesai.**

## Hasil aktual

- Build APK aplikasi/tes, lint dan **52 tes JVM** lulus memakai JDK 17/SDK build 35.
  [Log](build-unit-lint.txt), [lint akhir](lint-final.txt), [ringkasan](summary.json).
- Dua metode `LiveQariPlaybackTest` baru dikompilasi, **nol metode live lulus**.
  Suite menguji streaming tiga qari/kontrol/background/layar mati/pergantian surah,
  serta unduhan tujuh ayat Al-Fatihah, repair file rusak/parsial, offline, repeat
  dan rentang. Preferensi/file disimpan-dipulihkan; tanpa pm clear/uninstall.
- API Al-Mulk HTTP 200, 30 ayat 67:1–67:30, teks Utsmani dan terjemahan resource 33.
  Sampel MP3 1:1 Alafasy/Husary/Minshawi HTTP 200, audio/mpeg dan signature valid.
  [Ukuran/hash](providers.json). Belum membuktikan suara didengarkan.
- Timing public recitation 7 / 1:1 HTTP 200 dengan empat segmen. MP3 rujukannya
  dan MP3 Alafasy aplikasi sama panjang 146830 byte, tetapi SHA-256 berbeda.
  Metadata/encoding bisa menjelaskan perbedaan; **belum membuktikan beda bacaan
  atau kecocokan timeline**. PCM/durasi/offset dan seluruh korpus belum dibandingkan.

## Kegagalan dan hambatan

Emulator tersedia menggunakan image 37.1; runtime SDK dilaporkan 37, bukan 35.
Tidak ada HP terhubung atau image runtime API 35 yang ditemukan.

1. Boot read-only awal menghasilkan WHPX `GuestCannotBeFaulted` berulang; dihentikan.
2. Tanpa snapshot/Vulkan boot sempat selesai, APK aplikasi terpasang; ADB lalu offline.
3. Disk C terukur **0 byte kosong**. TEMP/TMP pengujian dialihkan ke D. Boot dan
   pemasangan dua APK berhasil, tetapi instrumentasi menghasilkan `Process crashed.`
   sebelum hasil metode: [log](device-live-d-temp-api37.txt).
4. ADB kembali hilang. Pembacaan crash buffer hanya menunjukkan daemon restart /
   menunggu perangkat: [diagnostik](crash-api37.txt). Stack trace tidak diperoleh;
   akar crash aplikasi/kompatibilitas belum ditentukan.
5. Setelah emulator berakhir C kembali sekitar 9 GB bebas. Tidak menghapus berkas
   pengguna, wipe AVD, reset aplikasi atau mengubah source produksi.

[Percobaan sebelum relokasi](device-live-initial-api37.txt) gagal saat ADB offline.
Tidak ada screenshot/video baru. Playback/seek/download penuh/offline/layar mati
rekaman asli, interupsi jaringan saat transfer, headset dan telepon belum terbukti.
Bukti historis tidak dihitung sebagai tes ulang sesi ini.

## Melanjutkan

Gunakan emulator stabil (utamakan API 35) dengan ruang disk cukup, ambil crash log
segera. Jalankan suite live **hanya pada emulator uji**, bukan HP pengguna: radio
dimatikan sementara lalu dipulihkan di finally. Suite menolak hardware selain
ranchu/goldfish. Crash/host mati bisa mencegah cleanup; gunakan AVD terpisah.

```text
cd android
gradlew.bat assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e class com.zikra.app.LiveQariPlaybackTest com.zikra.app.test/android.test.InstrumentationTestRunner
```

Audit host: jalankan `tools/Test-QuranProviders.ps1 -OutputPath <json>` dari root.
`-TemporaryRoot` opsional untuk memilih disk yang cukup. MP3/raw response dibersihkan
dari folder sementara unik; hanya ringkasan disimpan di Git.
[Audit sumber/izin](SOURCE_AUDIT.md). Mushaf/sorotan kata belum ditambahkan.
