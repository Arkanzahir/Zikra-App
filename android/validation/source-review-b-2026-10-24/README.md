# B01/B04 — lanjutan pemeriksaan sumber dan koreksi terbatas

24 Oktober 2026. Source **`3ef14dc`**, APK beta **31,433,745 byte / 31.4 MB**, **107 tes unit lulus / 3 dilewati**, lint **0 error / 698 peringatan**. **B01/B04 tetap terbuka.** Pemeriksaan ini menambah bukti dan koreksi nyata; bukan sertifikasi seluruh kitab atau 225 doa. Tidak ada email/komentar ke pengelola yang dikirim. Kontak `contact@gading.dev` dicantumkan oleh pengelola pada [profil GitHub-nya](https://github.com/gadingnst), bukan layanan AI atau backend baru Zikra.

## Hadis: delapan sampel dan konsekuensi penomoran

[Metadata/hash sampel dan URL pembanding](source-review.json). Teks kandidat dibaca kembali dari commit Gading yang sama. Pembanding adalah teks Arab yang terlihat pada Sunnah.com, bukan API lain yang hanya menyalin dataset Gading. Penilaian sampel dilakukan pada sanad/matan; tidak mengklaim kesamaan byte, semua tanda baca/harakat, edisi cetak ataupun mutu setiap terjemahan Indonesia.

| Nomor Tafsirq/Gading | Pembanding yang sejalan |
| --- | --- |
| Bukhari 1 | [Bukhari 1](https://sunnah.com/bukhari:1) |
| Bukhari 2977, sebelum celah | [Bukhari 3216](https://sunnah.com/bukhari:3216) |
| Bukhari 3001, sesudah celah | [Bukhari 3240](https://sunnah.com/bukhari:3240) |
| Bukhari 7008, ujung dataset | [Bukhari 7563](https://sunnah.com/bukhari:7563) |
| Muslim 1, awal dataset | [Muqaddimah Muslim](https://sunnah.com/muslim/introduction), bagian awal tentang kehati-hatian meriwayatkan |
| Muslim 47, sebelum celah 48 | [Muslim 32](https://sunnah.com/muslim:32) |
| Muslim 49, sesudah celah 48 | [Muslim 34](https://sunnah.com/muslim:34) |
| Muslim 5362, ujung dataset | [Muslim 3033b](https://sunnah.com/muslim:3033b) |

Ini **sampel pembanding**, bukan tabel konversi seluruh nomor. Nama kitab tidak memberi nilai sahih otomatis untuk setiap riwayat muqaddimah; [pengantar pembanding](https://sunnah.com/muslim/introduction) membedakan materi pengantar dari koleksi utama. Catatan ini ditambahkan pada informasi sumber di APK. Nomor asli/ID/bookmark tidak diubah.

[Issue pengelola #11](https://github.com/gadingnst/hadith-api/issues/11) melaporkan nomor Muslim 48 tidak tersedia. Laporan tersebut menguatkan bahwa celah perlu ditangani sebagai keadaan data nyata; tidak menjelaskan seluruh penyebab celah atau memberi izin mengisi kekosongan dengan teks dari edisi lain. Bukhari 3001 di Sunnah.com membahas perjalanan, sehingga tautan otomatis memakai integer Tafsirq yang sama akan menuju bacaan berbeda.

**B01 yang belum dapat ditutup:** identitas penerjemah/penerbit/edisi Indonesia, ketentuan pemakaian konten upstream dan penjelasan kelengkapan belum ditemukan. MIT repo tidak diartikan sebagai bukti hak semua terjemahan. Draf klarifikasi telah disiapkan lokal; belum memperoleh jawaban pemilik sumber.

Alternatif [API Hadis Malaysia](https://developer.hadis.my/docs/collections/) mendokumentasikan Arab/Indonesia dan cakupan lebih besar, tetapi memerlukan akun/key. [Ketentuannya](https://developer.hadis.my/legal/terma/) mengizinkan integrasi edukasi dengan pembatasan; identitas penerjemah Indonesia dan pemakaian cache/distribusi tetap perlu jelas. Tidak mendaftar akun, membeli paket, mengganti dataset atau memindahkan bookmark otomatis. HadeethEnc adalah opsi kurasi riwayat tertentu, bukan bukti kitab Bukhari/Muslim lengkap.

## Doa: ketentuan penggunaan ditemukan, kurasi masih per entri

[Ketentuan resmi EQuran](https://equran.id/terms) mengizinkan penggunaan API untuk pengembangan aplikasi dan membagikan konten dengan atribusi. Penggunaan harus wajar, tidak mengganggu layanan, serta data yang didapat tidak diperjualbelikan. Atribusi EQuran dan URL sumber tetap ditampilkan; koleksi diunduh setelah tindakan pengguna dan dibaca dari cache, bukan pengambilan otomatis berlebihan. Ketentuan tersebut bukan jaminan ketepatan teks dan tidak dipakai untuk menyimpulkan terjemahan bebas hak cipta.

[Dua belas sampel yang ditelusuri](doa-review.json) mencakup tidur, bangun, angin/hujan, kendaraan/perjalanan, ilmu, rumah dan makan. **213 entri tampil lainnya belum mendapat pemeriksaan sampel per entri**, dan detail harakat/terjemahan/sanad dua belas sampel belum dianggap tashih penuh. Duplikasi redaksi pada konteks berbeda tidak otomatis dihapus: misalnya Bismillah dipakai dalam beberapa aktivitas.

Lima penanganan dalam beta 1.7.1:

- **ID 6:** harakat akhir an-nusyūr diselaraskan dengan [Bukhari 6312](https://sunnah.com/bukhari:6312). Raw Arab sumber tetap disimpan; koreksi tampilan juga dipakai untuk TTS.
- **ID 18:** judul tampilan diperjelas menjadi *Ketika angin bertiup*. Konteks didukung [Abu Dawud 5097](https://sunnah.com/abudawud:5097); bacaan tersebut tidak diberi kategori doa khusus petir. Judul asli tetap ada dalam cache, dan konteks perbedaannya dijelaskan.
- **ID 20:** pembanding [Bukhari 1032](https://sunnah.com/bukhari:1032) dicantumkan; nomor 985 pada catatan EQuran tidak dianggap identik tanpa keterangan edisi.
- **ID 96:** sumber mencantumkan penilaian sahih Al-Albani, sedangkan [Ibn Majah 251](https://sunnah.com/ibnmajah:251) pada pembanding mencantumkan daif Darussalam. Kedua atribusi dijelaskan, tanpa menetapkan penilaian baru atau meratakan sanad/redaksi yang berbeda.
- **ID 139:** ditambahkan pembanding [Tirmidzi 3458](https://sunnah.com/tirmidhi:3458) dan [Abu Dawud 4023](https://sunnah.com/abudawud:4023); nomor Abu Daud 4043 dalam sumber tidak diam-diam dipetakan sebagai nomor edisi pembanding.

Koreksi/catatan memakai **fingerprint SHA-256 judul + Arab + arti + rujukan** yang dibatasi pemisah NUL (NUL pada field ditolak). Jika provider mengubah salah satu field tersebut, koreksi lama tidak dipaksakan: tampil catatan bahwa data perlu diperiksa ulang. Raw JSON tetap utuh, ID tetap `equran-doa:N`. Pencarian angin memakai konteks yang diperbaiki; judul/tag lama tidak menjadikannya hasil doa petir. Tidak ada Arab/terjemahan baru yang dikarang untuk mengisi kekurangan.

**Pagi/petang dan petir belum ditambahkan.** Ada pembanding [HadeethEnc 5490](https://hadeethenc.com/id/browse/hadith/5490) untuk pagi/sore dan [Al-Adab Al-Mufrad 723](https://sunnah.com/adab:723) untuk atsar tentang guruh. Atsar sahabat tidak akan diberi label sabda Nabi. Endpoint API HadeethEnc belum dapat diakses dari lingkungan pemeriksaan; halaman pembanding bisa dibaca. Ini bukan klaim semua perangkat gagal mengakses API.

## Bukti build dan batas

[release.json](release.json) dan [log build](build.txt) mencatat paket 1.7.1/code19 serta hasil unit/lint. Tes khusus memakai respons EQuran asli yang tersimpan privat: memastikan lima fingerprint dikenali, satu koreksi Arab tepat sasaran, konteks pencarian angin, data tidak dikenal/telah berubah tidak dikoreksi, dan pemisah snapshot tidak dapat disisipkan. Seluruh corpus tidak ditambahkan ke Git/APK. Tidak menyalakan emulator atau mengulang unduhan kitab pada putaran ini. Bukti interaksi native koleksi tetap checkpoint 1.7.0; belum ada screenshot/uji HP baru untuk catatan kurasi 1.7.1.
