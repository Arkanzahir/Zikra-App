# B04 — pilihan doa luring dan inventaris koleksi tambahan

24 Oktober 2026. **Beta 1.7.2 / code 20**. [Metadata APK dan hasil pemeriksaan](release.json). B02/B03 tetap selesai teknis; **B01/B04 belum dicentang**. Putaran ini menyelesaikan penambahan bacaan wajib yang sebelumnya belum tersedia, bukan menyatakan seluruh kitab/225 bacaan telah ditashih.

## Hasil yang sudah diterapkan

- Halaman Doa Harian sekarang memiliki **14 bacaan bawaan**, sebelumnya 4. Teks Arab, Latin bantu, arti dan rujukan tersedia sejak instalasi tanpa mengunduh koleksi. Pencarian menoleransi harakat Arab, dapat mencari nomor rujukan, dan dapat dipersempit berdasarkan kategori.
- Kategori belajar, makan, tidur, rumah, perjalanan, hujan/guruh, musibah dan pagi/petang sudah diwakili. Pagi/petang masing-masing satu bacaan pilihan, bukan klaim seluruh rangkaian dzikir. Latin merupakan bantuan baca; teks Arab tetap acuan.
- Bacaan guruh ditandai **atsar Abdullah bin az-Zubair**, bukan sabda Nabi. Bacaan angin pada koleksi tambahan tetap terpisah. Variasi lafal pagi/sore tidak disamakan dengan penomoran/varian riwayat lain.
- Data disimpan dalam [resource pilihan](../../app/src/main/res/raw/dua_selected.json) dengan ID `daily:*`, terpisah dari ID katalog `equran-doa:*`. Empat urutan lama dipertahankan. Keadaan bacaan terbuka menyimpan ID, sehingga tidak bergantung semata pada indeks daftar baru.
- Koleksi EQuran tambahan tetap 225 bacaan setelah unduh; tidak dihapus atau diklaim semuanya sudah diperiksa. Teks pengantar/status sekarang menjelaskan batas pemeriksaan. Menghapus cache tambahan tidak menyentuh pilihan bawaan.
- Pelafalan jelas dilabeli **TTS Android**, bukan rekaman qari. Suara memerlukan mesin/data bahasa yang mendukung; teks luring tidak berarti semua mesin TTS dapat berbicara tanpa jaringan.

## Daftar pilihan dan rujukan pembanding

| ID pilihan | Konteks | Rujukan |
| --- | --- | --- |
| `study` | Belajar | [Taha 20:114](https://quran.com/20/114) |
| `rain` | Hujan | [Bukhari 1032](https://sunnah.com/bukhari:1032) |
| `goodness` | Kebaikan dunia/akhirat | [Al-Baqarah 2:201](https://quran.com/2/201) |
| `calamity` | Musibah | [Muslim 918a](https://sunnah.com/muslim:918a) |
| `sleep` | Sebelum tidur | [Bukhari 6320](https://sunnah.com/bukhari:6320) |
| `wake` | Bangun tidur | [Bukhari 6312](https://sunnah.com/bukhari:6312) |
| `before-meal` | Sebelum makan | [Bukhari 5376](https://sunnah.com/bukhari:5376) |
| `after-meal` | Setelah makan | [Tirmidzi 3458](https://sunnah.com/tirmidhi:3458) |
| `leave-home` | Keluar rumah | [Abu Dawud 5095](https://sunnah.com/abudawud:5095) |
| `enter-home` | Masuk rumah | [Muslim 2018a](https://sunnah.com/muslim:2018a) |
| `travel` | Berangkat bepergian | [Muslim 1342](https://sunnah.com/muslim:1342) |
| `morning` | Pagi | [HadeethEnc 5490 / an-Nasai al-Kubra 10323](https://hadeethenc.com/id/browse/hadith/5490) |
| `evening` | Petang | [HadeethEnc 5490 / an-Nasai al-Kubra 10323](https://hadeethenc.com/id/browse/hadith/5490) |
| `thunder` | Mendengar guruh | [Al-Adab al-Mufrad 723, atsar sahabat](https://sunnah.com/adab:723) |

Pemeriksaan membandingkan lafal/konteks dan arti pada rujukan yang disebutkan; **bukan pengesahan penelaah agama atau jaminan setiap harakat seluruh katalog**. Tujuh tambahan dari EQuran memakai snapshot terdahulu dengan nomor asal pada `origin`; koreksi akhir an-nusyur diterapkan pada pilihan bangun tidur, dan satu salah ketik arti dibenahi. Rujukan terpilih memakai nomor pembanding yang dinyatakan; nomor sumber berbeda tidak dipetakan otomatis. Bismillah sebelum makan/masuk rumah adalah bacaan singkat sesuai konteks, bukan matan hadis lengkap.

Untuk pagi/petang, lafal dipilih berdasarkan penjelasan [HadeethEnc](https://hadeethenc.com/ar/browse/hadith/5490), yang memuat variasi penutup. [Abu Dawud 5068](https://sunnah.com/abudawud:5068) dan [Tirmidzi 3391](https://sunnah.com/tirmidhi:3391) memiliki variasi redaksi; nomor tersebut tidak dianggap satu teks identik. Arti ringkas Indonesia pagi/petang/guruh disusun untuk Zikra, bukan diklaim sebagai terjemahan penerbit tertentu.

[Atribusi EQuran](https://equran.id/terms) dipertahankan: penggunaan API dan berbagi dengan sumber diperbolehkan menurut ketentuan yang dibaca, penggunaan wajar dan tidak memperjualbelikan data tetap berlaku. [Tentang HadeethEnc](https://hadeethenc.com/id/home/about) menyediakan ensiklopedia secara gratis melalui berbagai sarana. Tidak menyalin uraian panjang, audio atau keseluruhan terjemahan modern dari halaman pembanding.

## Seluruh katalog sekarang memiliki inventaris per entri

[doa-inventory.json](doa-inventory.json) memuat **227 ID**, fingerprint judul/Arab/arti/rujukan, kategori, kelengkapan field, URL sumber dan status pemeriksaan masing-masing. **225 dapat ditampilkan; ID 42 tanpa arti dan 144 tanpa rujukan tetap dikecualikan.** Dua belas sampel lama hanya dikenali sebagai sudah diperiksa bila fingerprint cocok; **213 bacaan tampil lainnya masih perlu penelaahan per entri**. Kelengkapan field bukan penilaian sahih atau bukti ketepatan terjemahan.

Jalankan ulang [alat inventaris](../../../tools/audit_dua_inventory.py) terhadap respons JSON tersimpan bila sumber diperbarui. Alat tidak menghubungi provider, tidak membaca kredensial dan tidak memasukkan seluruh corpus ke repo. Duplikasi lafal di konteks berbeda dicatat, tidak dibuang otomatis.

## Pemeriksaan dan batas putaran ini

Build Android, lint dan unit test lulus; jumlah/hash/commit pada `release.json`. Tiga tes pilihan membaca resource yang benar-benar dibundel: cakupan kategori/urutan lama, pencarian Arab/nomor, perbedaan lafal pagi-petang, atribusi atsar dan penolakan identitas ganda/rujukan kosong. Tes kurasi lama dijalankan terhadap snapshot EQuran asli. Inventaris dijalankan terhadap seluruh 227 entri.

**Tidak menyalakan emulator atau memakai HP pada putaran ini**, untuk menjaga penggunaan RAM. Alur buka/cari/cache sebelumnya memiliki bukti native 1.7.0; interaksi tampilan 14 pilihan dan suara di perangkat belum diperiksa ulang pada 1.7.2. Tidak ada screenshot native baru yang diklaim.

## Yang belum dapat ditutup

- **B01:** sumber teknis sudah dipatok/diuji; identitas edisi/penerjemah Indonesia dan kejelasan penggunaan konten upstream Hadis belum ditemukan. Halaman TafsirQ v2 masih berisi placeholder; izin proyek API lain yang menyalin sumber sama tidak membuktikan asal terjemahan. Tidak ada email dikirim. Klarifikasi tidak harus lewat email bila bukti publik/alternatif yang memadai tersedia.
- **B04:** kategori yang kurang sudah diisi dan semua entri memiliki inventaris, tetapi penelaahan 213 bacaan tambahan, detail teks/arti serta pemeriksaan perangkat yang tersisa belum dinyatakan selesai. Pilihan 14 tidak digunakan untuk diam-diam menganggap seluruh koleksi 225 selesai.

Cakupan rilis tetap [A02](../../../RELEASE_SCOPE.md). Jumlah pilihan boleh bertambah setelah review; daftar tambahan yang belum ditelaah tidak diubah menjadi konten terverifikasi hanya karena mempunyai nomor rujukan.
