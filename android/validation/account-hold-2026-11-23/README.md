# Pencabutan akses saat penyimpanan gagal — 23 November 2026

Source tetap 1.9.64/code91, **HOLD**. Perubahan ini belum dipaketkan menjadi APK
signed baru. Kandidat signed 1.9.64 dan HP 1.9.63-internal tetap bukti sebelumnya.

## Perubahan dan alasan

`AccountSession` sebelumnya mengabaikan hasil `SharedPreferences.commit()`.
Saat commit pengaktifan ulang gagal, nilai dalam memori preferences dapat menjadi
false meskipun penanda pencabutan di disk masih true. Akses dalam proses tersebut
bisa terbuka tanpa penyimpanan yang berhasil.

`AccountPrivacyHold` mempertahankan blokir dalam memori untuk key akun/proyek
yang gagal ditulis, termasuk saat pembacaan atau penulisan melempar exception.
Pengaktifan ulang melepaskan blokir hanya setelah commit berhasil. Interceptor
memeriksa blokir kembali setelah mengambil token. Layar akun tidak menampilkan
konfirmasi aktif ketika commit pengaktifan ulang gagal.

Pencabutan dan penghapusan akun tetap mencoba menghubungi server meskipun
penyimpanan lokal gagal. Jika permintaan juga gagal, UI menjelaskan bahwa blokir
hanya terjamin selama proses aplikasi masih berjalan dan meminta percobaan ulang.
Tidak menyimpan DOB, email atau token baru; key tetap hash proyek dan UID.

## Bukti

- Enam tes JVM `AccountPrivacyHoldTest` lulus, tanpa error, kegagalan atau skip.
- Regresi JVM release lengkap: 243 tes dalam 54 suite, tanpa error, kegagalan
  atau skip. Jumlah ini sudah mencakup enam tes baru, bukan tambahan di luar 243.
- Lint release: 0 error, 913 warning. Tidak ada temuan pada enam berkas source
  dan resource yang berubah untuk perbaikan ini. [Ringkasan](proof.json).
- Kompilasi aplikasi debug dan seluruh source Android instrumentation lulus.
  Konfigurasi debug memakai akun 18+ dan Firebase Auth Emulator; tidak menjalankan
  emulator Android atau Firebase pada sesi ini.
- Kasus fault injection: commit pencabutan gagal, commit pengaktifan ulang gagal
  meski preferences dalam memori berubah, exception tulis, exception baca,
  pemisahan akun/proyek, serta pemulihan melalui commit sukses dan proses baru.
- Tes proses baru menggunakan penyimpanan sintetis terpisah dari memori. Ini
  bukan process-kill Android atau kegagalan disk fisik.

Perintah terarah dari `android` dengan JDK 17 dan cache Gradle lokal:

```powershell
./gradlew.bat :app:testDebugUnitTest --tests com.zikra.app.account.AccountPrivacyHoldTest :app:compileDebugAndroidTestJavaWithJavac -PzikraPublicBetaScope=true -PzikraAccountSignIn=true -PzikraAccountEmulator=true -PzikraAdultAccounts=true --offline --no-daemon --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=512m'

./gradlew.bat :app:testReleaseUnitTest :app:lintRelease -PzikraReleaseAdultAccounts=true -PzikraUnsignedCandidate=true --offline --no-daemon --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=512m'
```

Regresi lengkap memakai fixture lokal HadeethEnc, Mushaf dan doa yang tercantum
di [petunjuk akun](../adult-accounts-2026-11-15/README.md). Tidak menyalin korpus
fixture ke Git atau mengambil ulang konten dari layanan.

Percobaan awal sandbox ditolak saat membuka lock cache `E:/.gradle`; eksekusi
dengan akses cache yang disetujui berhasil. Percobaan awal tes release berhenti
pada validasi signing karena tidak memasok kunci. Pemeriksaan release selanjutnya
menggunakan opsi inspeksi `zikraUnsignedCandidate=true`, tanpa membuat APK dan
tanpa membaca kredensial signing.

## Batas yang tetap terbuka

Jika commit pencabutan gagal, disk belum mempunyai penanda baru. Proses baru
tidak dapat memulihkan blokir yang hanya ada dalam memori; tes mencatat batas ini
secara eksplisit. Pencabutan server harus dikonfirmasi atau penulisan lokal
diulang sampai berhasil. Jangan mengklaim ketahanan penuh terhadap disk rusak,
ENOSPC, power-loss atau proses mati sebelum RPC selesai.

UI pesan kegagalan belum diuji melalui fault injection native. Tidak ada uji HP,
restart perangkat, inferensi hosted, perubahan IAM, deploy atau publikasi.
Staging hapus akun dan chat mandiri signed, retensi/hak, cadangan signing dan
keputusan rilis tetap mengikuti [status aktif](../../../docs/beta/CURRENT_STATUS.md).
