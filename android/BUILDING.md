> **Lokasi/masjid:** versi terbaru memakai OpenStreetMap native. Tidak memerlukan API key atau billing Google. Buka `android/` dengan JDK 17/SDK 35; dependency osmdroid diunduh oleh Gradle. [Bukti terbaru](validation/location-mosques-2026-10-19/README.md).

> **Status 19 Oktober 2026:** ronde kegunaan memperbaiki onboarding 2D statis, header/navigasi, keterbacaan, rencana lintas tanggal dan pengaturan reader. Progres baru berasal dari tindakan pengguna, tanpa tugas atau penyelesaian demo otomatis. Build/lint, 33 tes JVM dan 47 metode emulator unik lulus; batas HP fisik/API 35 tetap dicatat. [Hasil dan screenshot terbaru](validation/quality-2026-10-19/README.md) · [RECOVERY.md](../RECOVERY.md). Hasil checkpoint lama di bawah bersifat historis.


# Build Android native

Folder `android` adalah project Android Studio mandiri. Semua source aplikasi memakai Java dan layout XML. Tidak ada plugin Kotlin, WebView, React Native, atau Flutter.

## Android Studio

Versi dasar sudah digabung ke **`main`**. Untuk perubahan baru, gunakan branch yang tercantum pada catatan validasi checkpoint tersebut:

```sh
git clone https://github.com/Arkanzahir/Zikra-App.git
```

Alternatif: buka branch tersebut di GitHub, pilih **Code → Download ZIP**, lalu ekstrak. Di Android Studio buka **`Zikra-App/android`** (folder yang berisi `settings.gradle`), bukan folder `prototype_zikra`. Saat memakai ZIP GitHub, nama folder luarnya dapat berbeda; tetap pilih subfolder `android`.

Versi ini memuat agenda Study/Hifz, logo master, onboarding ilustrasi 2D statis, Home siang/malam dengan astronomi harian, reader Quran native dan prakiraan Open-Meteo. Semua dibangun ulang sesuai referensi chat. Sumber kode asli versi lokal lama belum ditemukan; jangan menganggap implementasi baru identik piksel/kode dengan versi tersebut. Bukti APK dan batas uji ada di [validasi terbaru](validation/quality-2026-10-19/README.md).

1. Buka folder `android` dengan **Open**, lalu tunggu Gradle Sync selesai.
2. Gunakan **JDK 17** di **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK**. Pilih/unduh JDK 17 bila JDK bawaan Android Studio berbeda.
3. Instal **Android SDK Platform 35**, **Build Tools 35.0.0**, dan **Platform Tools** melalui SDK Manager. Android Studio membuat `local.properties` sesuai lokasi SDK lokal.
4. Pilih emulator/perangkat Android 8.0 (API 26) atau lebih baru, lalu tekan **Run**.

Jangan menyalin `/workspace/android-sdk`, `JAVA_HOME` cloud, konfigurasi proxy cloud atau `local.properties` dari environment ini ke Windows. Android Studio memakai SDK/JDK lokal dan membuat `local.properties` sendiri. Gradle Sync awal memerlukan Internet untuk mengambil dependensi; backend/API key tidak diperlukan untuk memasang debug APK. Input suara/sensor tetap bergantung pada layanan perangkat.

Versi build dipatok: Android Gradle Plugin **8.7.3**, Gradle **8.9**, `compileSdk`/`targetSdk` **35**, Java source/target **17**. Gradle Wrapper disertakan, termasuk SHA-256 distribusi pada `gradle-wrapper.properties`.

## Terminal

Linux/macOS:

```sh
cd android
./gradlew assembleDebug testDebugUnitTest lintDebug
```

Windows:

```bat
cd android
gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Uji instrumentasi terarah membutuhkan APK debug dan APK tes yang dipasang pada emulator/perangkat terhubung. Contoh suite reader (memerlukan jaringan untuk skenario live):

```sh
./gradlew assembleDebugAndroidTest
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e class com.zikra.app.RebuiltQuranTest com.zikra.app.test/android.test.InstrumentationTestRunner
```

Runner instrumentasi memakai `android.test.InstrumentationTestRunner` bawaan platform agar dependensi aplikasi tetap Java. Unit test memakai JUnit 4.13.2. OkHttp 3.14.9 dan Okio 1.17.2 merupakan dependensi runtime Java; Kotlin runtime tidak diperlukan.

## Lingkungan cloud ini

SDK terpasang di `/workspace/android-sdk`; lokasi tersebut hanya dicatat di `local.properties` yang diabaikan Git. Cache memakai direktori writable terpisah:

```sh
source /workspace/setup-zikra/env.sh
./gradlew assembleDebug testDebugUnitTest lintDebug
```

Proxy HTTPS cloud, bila diperlukan, dikonfigurasi pada `GRADLE_USER_HOME/gradle.properties` lokal, bukan pada repository. Jangan menyalin kredensial atau konfigurasi proxy cloud ke project yang dibagikan.

Gunakan JDK lengkap dengan `javac` dan `jlink`; JRE saja tidak cukup untuk transformasi SDK Android. Pada lingkungan ini emulator menyimpan konfigurasinya di `ANDROID_EMULATOR_HOME=/workspace/android-user` dan AVD di `ANDROID_AVD_HOME=/workspace/android-user/avd`. ADB memakai lokasi standar `~/.android`, yang pada instance ini diarahkan ke direktori writable `/workspace/android-user/adb`. Nilai `HOME` tidak diubah.

AVD yang berhasil dipakai untuk pembaruan BMKG adalah `Zikra_Design_API28` (Android 9), software rendering tanpa KVM, port 5556. Build aplikasi tetap menggunakan SDK 35; hasil uji perangkat terbaru dicatat terpisah pada `VALIDATION.md`.

Sensor rotasi, cahaya, akselerometer, getaran, aplikasi WhatsApp/Maps, pengenal suara, dan mesin TTS perlu diverifikasi juga pada perangkat fisik yang mendukungnya. Hasil build tidak menjamin ketersediaan sensor atau aplikasi pihak ketiga pada setiap perangkat.

## Pengingat sholat

Buka Jadwal Sholat melalui strip waktu atau tombol bel Home, lalu aktifkan **Pengingat sholat**. Android 13+ menampilkan permintaan izin notifikasi. Untuk Android 12+, tombol **Atur ketepatan pengingat** membuka akses alarm tepat waktu; jika tidak diizinkan, aplikasi tetap memakai alarm yang ketepatannya dapat dipengaruhi penghematan daya sistem. Pilihan per sholat diatur lewat ikon bel pada baris jadwal.

`PrayerRemindersTest` memakai layanan alarm/notifikasi perangkat sungguhan dan memberikan izin notifikasi melalui UiAutomation pada perangkat uji jika dibutuhkan. Tes memverifikasi jadwal aktif/nonaktif dan receiver notifikasi dengan broadcast uji, bukan menunggu waktu sholat sebenarnya.
