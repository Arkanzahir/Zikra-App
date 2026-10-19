# Rancangan lokasi dan Quran

Keputusan mengikuti kebutuhan pengguna: Java/XML native, ivory/Pine, logo dan Home dipertahankan. Pekerjaan dimulai dari halaman lokasi/masjid, kemudian pengguna memilih murottal → gulir Quran → Hifz dan meminta dukungan background/offline diselesaikan bersama.

## Referensi yang diperiksa

- [Pillars](https://thepillarsapp.com/prayer-times-app): referensi produk untuk fokus pada rutinitas ibadah dan hierarki sederhana; tidak menyalin aset atau menambah fitur yang tidak tersedia.
- [NN/g — Maps and Location Finders on Mobile Devices](https://www.nngroup.com/articles/mobile-maps-locations/): daftar menjadi tampilan awal; peta pilihan terpisah, jarak dan arah tersedia tanpa harus memilih pin. Artikel terbit 2014; digunakan untuk pola interaksi, bukan bukti performa Zikra.
- [Android — Touch target size](https://support.google.com/accessibility/android/answer/7101858?hl=en): area sentuh minimal 48dp; diuji pada layout 320dp/font 200%.
- [Material segmented buttons](https://m3.material.io/components/segmented-buttons/guidelines): URL resmi diperiksa, tetapi konten memerlukan JavaScript. Detail keputusan di sini berasal dari riset NN/g dan pengujian aplikasi, bukan klaim telah membaca seluruh pedoman Material.
- Referensi pengguna yang sudah ada: `design/references/ibadah-isha-manzoor.png`, serta screenshot APK lokasi/masjid sebelum perubahan. Gambar pengguna dibuka dan diperiksa langsung.

## Penerapan

Header dan halaman lokasi/masjid memakai satu permukaan ivory. Pengantar besar diganti informasi lokasi/radius. Daftar/Peta menjadi RadioGroup dengan status pilihan yang bisa dibaca layanan aksesibilitas. Kartu XML bersama mengutamakan nama, jarak dan tombol arah 48dp; alamat lengkap tersedia lewat rincian. Marker peta memilih kartu yang sama, bukan memaksa membuka aplikasi eksternal. Hasil lama dan waktu pengambilannya tetap ada jika refresh gagal. Pilihan kota lama tidak tertinggal sebagai hasil kota baru.

Reader memakai pemutar bawah yang tetap terlihat, kontrol ikon berlabel, serta pengaturan qari/rentang/pengulangan/gulir dalam dialog yang bisa digulir. Mode malam mempertahankan kontras. Gulir baca berhenti saat pengguna menyentuh bacaan. Mengikuti murottal memakai identitas ayat, bukan perkiraan panjang rekaman. Mengubah tema tidak mengganti sumber teks atau menandai hafalan selesai.

## Sumber audio dan Android

- [Al Quran Cloud CDN](https://alquran.cloud/cdn): pemetaan global 1–6236, rekaman per ayat, 128kbps; pilihan Alafasy, Al-Husary dan Al-Minshawi sesuai daftar yang dipublikasikan.
- [Ketentuan Al Quran Cloud](https://alquran.cloud/terms-and-conditions), diperiksa 19 Oktober 2026: sumber mengizinkan streaming/unduhan personal dan pendidikan pada bitrate yang dipublikasikan; hak rekaman tetap pada qari/pemegang hak. Atribusi dan tautan ketentuan tersedia pada pengaturan audio. Rekaman tidak ditambahkan ke repository atau APK.
- [Android MediaPlayer dalam service](https://developer.android.com/media/platform/mediaplayer/background), [audio focus](https://developer.android.com/media/optimize/audio-focus): satu layanan playback dengan MediaSession, foreground mediaPlayback, audio focus dan pelepasan resource. Unduhan memakai service dataSync terpisah; tidak memakai tipe playback untuk mengunduh.

Koneksi ke `cdn.islamic.network` ditolak proxy cloud (HTTP CONNECT 403). Dokumentasi sumber dibaca melalui pencarian web. Keberhasilan decoder/antrean/notifikasi menggunakan fixture WAV sintetis pada tes perangkat tidak dinyatakan sebagai keberhasilan rekaman qari atau server live.
