# Daftar Doa — UI01, 1.6.2

24 Oktober 2026. Koreksi pengguna: kartu Doa terasa terlalu generik/membulat dan ikon hati berulang.

## Hasil

- Satu panel bacaan dengan divider; jarak antarkartu besar dihilangkan. Judul 15sp, kategori beraksen perunggu dan rujukan singkat menggantikan deskripsi generik pada daftar.
- Ikon buku, hujan, tangan berdoa dan perisai mengikuti isi. Judul dan rujukan tidak diubah menjadi teks dalam gambar.
- Bacaan terbuka mendapat latar sage; teks Arab rata kanan, arti, sumber dan tombol suara tetap tersedia.
- Status mesin suara hanya ditampilkan saat ada bacaan terbuka yang terlihat. Pelafalan/TTS tidak diubah menjadi rekaman Quran atau audio dari sumber API.

Screenshot native final: [daftar](DoaList.png), [bacaan dibuka](DoaExpanded.png), [halaman awal](DoaOverview.png). Screenshot daftar menampilkan posisi setelah digulir; hero pada bagian atas tetap tersedia.

## Pemeriksaan

`assembleDebug`, `assembleDebugAndroidTest`, `lintDebug` berhasil, satu worker dan heap 512 MiB. Lint seluruh aplikasi: **0 error, 684 warning**, bukan klaim seluruh warning selesai.

Satu metode unik `DailyDesignReleaseTest#testDuaFilterAndReadWithoutVoice` lulus pada APK final: membuka isi, sumber Taha 20:114, hasil pencarian kosong dan pemulihannya. Metode yang sama diulang setelah perapian akhir/masalah capture; bukan beberapa tes unik. Run bukti terakhir **7,168 detik**. Satu capture awal menangkap transisi putih dan tidak digunakan; bukti final diambil ulang dengan animasi transisi sistem emulator dimatikan.

Log [build](build.txt), [instrumentasi](instrumentation.txt), [hash sumber/APK](release.json). Buffer crash final kosong. Emulator Android 37.1, 720 × 1600/density 320, 4096 MB/dua core; ditutup sesudah gambar diperiksa. Tidak memakai HP pengguna. Suara TTS, teks besar, Android versi lain dan baterai tidak diuji pada perubahan ini.

APK lokal: `D:\Download\Zikra-App-main\Zikra-Daily-v1.6.2-debug.apk`, versi 1.6.2 (16), 31.216.429 byte.

SHA-256: `e8eba21c2411531fe26fab9374012368ed4231f4645b7d5755a2bcf863123f05`.

Delapan file lingkup source dibandingkan byte dengan snapshot build, dan 465 file produksi terlacak dibandingkan dengan normalisasi LF/CRLF; dua file WIP audio tetap dikecualikan seperti checkpoint sebelumnya. Tidak ada perubahan pada data Hadis, service audio Quran, backend AI atau peta.

## Isi dan rencana penyimpanan

Doa masih **empat entri lokal**, Hadis masih tujuh kutipan. **B01–B04 tetap belum selesai.** [Usulan pengiriman konten](../../../design/CONTENT_DELIVERY_PLAN.md): validasi sumber/ketentuan → server menyimpan versi konten → cache HP untuk baca luring. Server konten belum dibuat/di-hosting; akun/sinkronisasi data pribadi tidak otomatis termasuk.
