# Kegunaan dan konsistensi fitur — 19 Oktober 2026 (WIB)

Riset dilakukan sebelum implementasi; lihat [sumber dan keputusan](../../../design/research/README.md). Aplikasi tetap Java/XML native. Sumber aplikasi yang diperiksa: `3bfe9737ef9cd88e595bbc885e1244fd22e32e2d` pada branch `recovery/study-hifz-refresh`. Commit laporan dapat menyusul tanpa mengubah kode aplikasi.

## Hasil

- Build APK debug dan APK instrumentasi lulus. JDK 17, AGP 8.7.3, Gradle 8.9, compile/target SDK 35, min SDK 26.
- **33 tes JVM lulus**, tanpa failure/error; [rincian](unit-results.json). Eksekusi unit berada pada log [unit dan lint awal](unit-run-and-initial-lint.txt). Lint awal menemukan alias header dinamis; perbaikan memakai getter komponen, bukan baseline/suppression. Lint akhir **0 error, 482 warning nonfatal**; [build akhir](scaled-controls-build.txt).
- **47 metode instrumentasi unik lulus** pada emulator AOSP API 28, software Swiftshader tanpa KVM. Rincian kelas/metode dan pemanggilan ulang ada di [summary.json](summary.json). Pemanggilan ulang tidak dihitung sebagai metode unik tambahan.
- [Putaran awal](device-main.txt) selesai 23 metode (22 lulus, satu race pada tes dismiss) lalu berhenti karena assertion tes lompat ayat di UI thread. Tes dikoreksi agar menunggu callback dismiss/OnShow; assertion diteruskan ke thread tes. Tidak ada perubahan validasi produksi untuk meloloskan tes. Kedua skenario kemudian lulus pada [putaran alur](device-journeys.txt) dan [sisa suite](device-remaining.txt).
- [10 skenario alur](device-journeys.txt), [13 skenario sisa](device-remaining.txt), [4 pemeriksaan akhir](device-final-layout.txt) dan [profil akhir](device-profile-final.txt) selesai dengan `OK`. Dua pemeriksaan Quran diulang setelah tombol dirapikan; profil diulang setelah judul progres lintas hari diperjelas. Tinjauan screenshot fontbesar menemukan label dock terpecah di tengah kata; setelah diperbaiki, asisten font besar lulus pada [putaran dock](device-nav-labels.txt). Home pada putaran itu gagal memperoleh fokus setelah resize WM dan query layanan window tidak merespons; Diagnosis mencatat SystemUI ANR dan RenderThread EGL yang macet ([bukti](emulator-focus-diagnosis.txt)). AVD uji dimulai ulang tanpa menghapus userdata; backend ANGLE/SwiftShader dengan Vulkan guest dinonaktifkan dipakai untuk pemulihan. Dialog ANR SystemUI saat boot ditangani dan fokus Launcher diperiksa sebelum tes. [Pengujian ulang Home](device-font-home-retry.txt) lulus dan screenshot diganti. Batas tunggu fokus pada helper uji diperpanjang untuk renderer software. Pengulangan tes tema mengandalkan nilai balik aksi aksesibilitas/`performClick`. RadioButton dapat berubah melalui `OnCheckedChangeListener` meski nilai balik klik false; tes kini menggulir pilihan, menekannya, lalu memeriksa checked state dan tema tersimpan, bukan nilai balik klik. [Pengujian ulang tema](device-theme-retry.txt) lulus tanpa perubahan logika tema produksi.

## Yang berubah dan diverifikasi

| Area | Hasil implementasi dan pemeriksaan |
| --- | --- |
| Data rencana | Instalasi baru/hari baru kosong, tanpa 24 tugas contoh atau delapan penyelesaian palsu. ID, tanggal, status dan target lama tidak dihapus. Migrasi v1/v2, invalid input, template eksplisit dan progres per kategori diperiksa. |
| Study / Hifz / Ibadah | Akademik dan Hifz tetap dapat diakses lintas tanggal; selesai berada pada grup yang bisa dibuka. Ibadah tetap khusus hari ini. Editor basi mempertahankan draft dan menawarkan tindakan tambah ulang; bukan mengklaim simpan berhasil. Lima sholat hanya dibuat saat pengguna memilih template, tanpa duplikasi. |
| Home / navigasi / Profil | Home menghitung ibadah dan membuka lingkup ibadah; Profil menghitung akademik. Primary Study selalu akademik walau halaman Hifz masih ada. Header, dock, tindakan dan teks pendukung disatukan; nama default tidak mengarang identitas pribadi. |
| Quran | Katalog 114 surah, pencarian Arab tanpa harakat/tatweel, ukuran Arab 24–44sp dan arti 14–24sp tersimpan, preview, lompat ayat tervalidasi, pagination/live cache, tema dan recreation diperiksa. Teks ayat yang ditampilkan tidak diubah oleh normalisasi pencarian. |
| Onboarding | Tiga ilustrasi 2D statis 1536×1024 memenuhi lebar, dinding menyatu dengan header, judul dua baris, footer 01/03, wordmark tanpa titik. Master bulan+buku+spark tetap. Tiga cerita, swipe satu langkah, gambar diam dan preferensi lama/recreation diperiksa. |
| Font besar | Onboarding 320×560dp/font 200% melalui scoped layout dengan artwork nyata. Tombol Lewati tumbuh sesuai teks, tetap satu kata dan berada dalam viewport; [regresi](device-skip-scaled.txt) lulus. Home sekitar 320×720dp/font 200% dan asisten font 200% pada Activity aktual. Dock/tindakan tetap dapat dijangkau, ukuran WM/font dikembalikan. Label navigasi satu kata tidak terpecah; label compact memakai autosize10–12sp sementara isi halaman mengikuti font sistem sepenuhnya. Ini bukan bukti seluruh fitur/perangkat memenuhi semua standar aksesibilitas. |
| Doa / Hadis / Kalender / asisten | Pencarian/expand doa tanpa layanan suara Arab, kalender pergantian hari dan pilihan tanggal, percakapan lokal dan rute Hifz diperiksa. Asisten berlabel panduan offline; suara hanya ditawarkan ketika layanan Android tersedia. |
| Cuaca / gempa / sholat | Data valid dipertahankan saat refresh gagal, cuaca kedaluwarsa setelah enam jam, koordinat tak valid tidak membuka peta. Respons BMKG dibatasi 256 KiB, termasuk chunked. Lima alarm dan receiver/notifikasi nyata/duplikasi diuji di API 28. |

## Screenshot aktual

[**PDF lengkap**](preview.pdf) · [**Galeri HTML mandiri**](preview.html). Keduanya menanam **22 PNG aktual** dari APK, tanpa path relatif ke environment. Gambar dapat diperbesar pada HTML. Hash asli di [screenshot-hashes.json](screenshot-hashes.json); aset statis [diverifikasi lossless](asset-verification.json).

Seluruh rencana/progres pada galeri berasal dari **emulator uji**, termasuk catatan uji lama yang sengaja dipertahankan dan entri sementara `[Uji]`. Itu bukan aktivitas pengguna nyata. Screenshot tidak mengganti data jaringan dengan fixture. [Status pengambilan](network-screen-status.txt) mencatat Open-Meteo model/cache dan laporan terakhir BMKG yang benar-benar diterima saat pengambilan; ini bukan jaminan semua request berikutnya berhasil. Waktu sistem emulator dan zona acuan aplikasi dapat berbeda; jadwal/data cuaca memakai WIB untuk Sukolilo.

## Batas yang masih nyata

- Lokasi jadwal, cuaca dan Kiblat masih acuan **Sukolilo, Surabaya**, belum pilihan kota/GPS. Posisi bulan/awan/bintang ilustratif; sumber astronomi/fallback diberi label.
- Quran public v4, cache delapan bagian × 50 ayat, empat surah bawaan. Belum Mushaf edisi tertentu, murottal atau unduhan penuh. Pengguna belum punya production Content Sync.
- Asisten berbasis aturan offline, bukan AI generatif/analisis foto/evaluasi hafalan. Koleksi tetap empat doa dan tujuh kutipan hadis bersumber. Infaq berupa panduan kampus, tanpa pembayaran.
- Sensor Kiblat, suara perangkat, kelancaran/baterai, izin/alarm Android modern dan runtime API 35 masih perlu diuji pada **HP fisik**. Compile SDK 35 tidak membuktikan pengujian runtime API 35. Belum uji kegunaan dengan pengguna lintas usia atau sertifikasi aksesibilitas.

Main dan prototype lama tidak diubah. Ronde ini memperbaiki fungsi dan konsistensi yang terukur; tidak menyatakan semua fitur sudah optimal atau siap rilis produksi.
