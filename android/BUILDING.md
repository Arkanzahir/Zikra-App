# Build Android native

Folder `android` adalah project Android Studio mandiri. Semua source aplikasi memakai Java dan layout XML. Tidak ada plugin Kotlin, WebView, React Native, atau Flutter.

## Android Studio

1. Buka folder `android` dengan **Open**, lalu tunggu Gradle Sync selesai.
2. Gunakan JDK 17 atau 21 di **Settings → Build Tools → Gradle → Gradle JDK**. Android Studio menyediakan JDK yang kompatibel.
3. Instal **Android SDK Platform 35**, **Build Tools 35.0.0**, dan **Platform Tools** melalui SDK Manager. Android Studio membuat `local.properties` sesuai lokasi SDK lokal.
4. Pilih emulator/perangkat Android 8.0 (API 26) atau lebih baru, lalu tekan **Run**.

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

Uji instrumentasi membutuhkan emulator/perangkat yang terhubung:

```sh
./gradlew connectedDebugAndroidTest
```

Runner instrumentasi memakai `android.test.InstrumentationTestRunner` bawaan platform agar dependensi aplikasi tetap Java. Unit test memakai JUnit 4.13.2. OkHttp 3.14.9 dan Okio 1.17.2 merupakan dependensi runtime Java; Kotlin runtime tidak diperlukan.

## Lingkungan cloud ini

SDK terpasang di `/workspace/android-sdk`; lokasi tersebut hanya dicatat di `local.properties` yang diabaikan Git. Cache memakai direktori writable terpisah:

```sh
export GRADLE_USER_HOME=/workspace/gradle-cache
export ANDROID_USER_HOME=/workspace/android-user
export ANDROID_HOME=/workspace/android-sdk
export JAVA_HOME=/workspace/java/jdk-17.0.20.1+1
./gradlew assembleDebug testDebugUnitTest lintDebug
```

Proxy HTTPS cloud, bila diperlukan, dikonfigurasi pada `GRADLE_USER_HOME/gradle.properties` lokal, bukan pada repository. Jangan menyalin kredensial atau konfigurasi proxy cloud ke project yang dibagikan.

Sensor rotasi, cahaya, akselerometer, getaran, aplikasi WhatsApp/Maps, pengenal suara, dan mesin TTS perlu diverifikasi juga pada perangkat fisik yang mendukungnya. Hasil build tidak menjamin ketersediaan sensor atau aplikasi pihak ketiga pada setiap perangkat.
