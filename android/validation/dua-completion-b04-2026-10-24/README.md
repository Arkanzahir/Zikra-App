# B04 — kurasi editorial dan integrasi Doa

Ditelaah 24 Oktober; penyelesaian/uji akhir **25 Oktober 2026 (WIB)**. **Beta 1.7.6 / code 24**. B04 ditutup untuk cakupan editorial dan integrasi yang tertulis pada rencana. B01 Hadis tetap menunggu kejelasan asal/ketentuan konten; penutupan B04 tidak mengesahkan dataset Hadis atau rilis publik seluruh aplikasi.

## Cakupan yang diperiksa

- **14 doa pilihan bawaan** tetap tersedia tanpa unduh. Resource/ID/urutan tidak diubah; rujukan dan pilihan kategori pada [review pilihan](../dua-selection-b04-2026-10-24/selection-review.json) tetap berlaku. Pagi/petang adalah pilihan bacaan, bukan seluruh rangkaian dzikir.
- **225 bacaan tambahan** ditelaah secara editorial untuk lafal/petikan Arab, arti Indonesia, judul dan konteks rujukan. [Catatan per entri](per-entry-review.json) mencakup seluruh 227 ID, termasuk alasan pengecualian lama ID 42 tanpa arti dan ID 144 tanpa rujukan. Tidak mengurangi koleksi untuk menutup checklist.
- Pembanding menggunakan ayat Quran, Hisnul Muslim, teks enam kitab Arab, kitab Ad-Dua dan halaman riwayat/artikel asal yang sesuai. [URL/commit/hash pembanding](comparison-source-receipts.json) dapat ditelusuri. Corpus serta halaman PDF disimpan privat untuk pemeriksaan, tidak dibundel ke Git atau APK. Paket Hadis aplikasi tidak diganti dengan corpus pembanding ini.
- Pencocokan normalisasi dipakai untuk menemukan petikan, lalu konteks diperiksa: frasa sama pada tasyahud tidak dipakai sebagai bukti doa rumah; bacaan pendek, gabungan, variasi riwayat dan penyesuaian kata ganti ditangani terpisah. Hasil 78 calon Hisn pada checkpoint lama tetap riwayat pencarian, bukan jumlah persetujuan otomatis.

Ini **review editorial berbantuan alat**, bukan terjemahan baru, audit seluruh sanad, penilaian sahih baru, pengesahan penelaah agama, atau sertifikasi semua harakat. Latin tetap bantuan dari penyedia; koreksi yang terbukti pada ID 111 diselaraskan dengan Arab. Perbedaan penilaian ulama tidak diputuskan dengan memilih salah satu tanpa atribusi. Batas ini tidak ditukar dengan klaim semua bacaan merupakan hadis sahih.

## Perbaikan yang diterapkan

Total **33 entri** mempunyai penanganan/catatan terikat snapshot, termasuk 14 checkpoint sebelumnya. Satu entri dapat memiliki lebih dari satu penanganan.

- Ejaan/format: petunjuk Indonesia pada ID 57/76/77/209 dipisahkan; kurung ID 225 dirapikan; lam awal Labbaik ID 186 dipulihkan; spasi al-Ahad ID 188 disatukan. Harakat akhir an-nusyur ID 6 dan pembukaan al-ghayba ID 111 diperbaiki. Fi al-ghaybi pada bagian berikutnya tetap kasrah; Latin awal ikut diselaraskan.
- Arti/judul: kata berulang ID 154 dihapus; kata ganti arti ID 123 diselaraskan menjadi aku. Cakupan judul ID 16/55/70 dan typo ID 66/103/121 diperjelas. Arab tidak diganti hanya agar mengikuti judul lama.
- Rujukan/konteks: ayat yang disesuaikan, doa untuk Anas/Jarir/Ibnu Abbas, ucapan kelahiran/hari raya, atsar Ibnu Masud dan susunan khutbah diberi catatan. Nomor pembanding ID 20/77/94/139/168/176 dibedakan dari nomor edisi sumber; raw tidak ditimpa. Perbedaan penilaian pada ID 3/55/96 dan kelemahan jalur kisah ID 159 dijelaskan.
- Pencarian mengenali nilai tampilan hasil koreksi serta nilai asal, termasuk لبيك dan kokohkanlah aku.
- Arti, Latin, judul atau lafal yang dirapikan tetap dapat dibandingkan dengan nilai sumber pada detail bacaan. Cache JSON asli serta ID tidak diubah oleh koreksi tampilan.

[Manifest 225 snapshot](../../app/src/main/java/com/zikra/app/discovery/DuaSourceSnapshots.java) mengikat **judul, Arab, arti dan rujukan** yang ditelaah. Entri baru atau perubahan salah satu empat field diberi label **Belum ditinjau**; koreksi lama tidak diterapkan. Ini pemeriksaan identitas data, bukan bukti agama dari hash. Grup/tag dan Latin umum bukan empat field yang disahkan manifest; jangan mengklaim seluruh metadata/Latin telah tersertifikasi.

## Bukti aplikasi

[Metadata APK/hasil uji](release.json), [build/lint](build.txt), [native](native.txt), dan tiga laporan unit berada di folder ini.

- **10 tes unit Doa**, 0 gagal/0 skip: parser, pilihan bawaan dan kurasi; snapshot asli 225 bacaan, 33 penanganan, empat jenis perubahan field pada setiap entri, ID baru, nilai sumber tetap serta pengecualian koreksi lama. Bukan suite seluruh aplikasi.
- **2 metode native**, API 37, 720×1600, density 320, font 100%. Metode pertama memeriksa **21 entri terdampak**: cari/buka, judul, Arab, Latin, arti, catatan dan nilai sumber. Mengubah satu entri cache di fixture membuktikan label Belum ditinjau dan penolakan koreksi lama. Metode kedua mengunduh respons EQuran asli, membuka cache, paging, pencarian, kategori, hasil kosong, penolakan pembaruan rusak dan mempertahankan cache valid.
- Fixture cache dikembalikan pada akhir tes. APK produksi tidak memuat data contoh pengujian. Native diulang pada source akhir, bukan mengandalkan tes sebelum penambahan catatan terakhir.
- Label tombol tetap **Dengarkan · TTS Android** dan data ke TTS berasal dari lafal tampilan yang dikoreksi. Keluaran suara/mesin bahasa Arab nyata tidak direkam atau diklaim terverifikasi; teks luring tidak menjamin semua mesin TTS luring. Pemeriksaan perangkat/audio lanjutan tetap G02/F03.
- Lint: 0 error, 709 warning. Emulator meminta 1.536 MB tetapi guest melaporkan sekitar 4 GB; hasil memori aktual dicatat, bukan mengklaim emulator ringan. Emulator ditutup setelah pemeriksaan. Tidak memakai HP pengguna.

Screenshot APK asli tanpa pengolahan gambar: [perlindungan anak](Doa-Reviewed-77.png), [Labbaik](Doa-Reviewed-186.png), [pencarian hujan](Doa-Library-Rain.png). Screenshot memperlihatkan bagian layar pada posisi scroll, bukan seluruh detail panjang. Pemeriksaan teks catatan dilakukan oleh tes native; gambar bukan pengganti pengujian.

APK **31.319.928 byte**, di bawah batas 100.000.000 byte. SHA/source pada release.json. Distribusi masih debug beta; pengujian HP fisik, seluruh ukuran layar, signing rilis dan gerbang publik G belum selesai.

## B01 tetap terpisah

Thread klarifikasi sumber Hadis dicek kembali pada 24 Oktober 2026: masih satu pesan terkirim, belum ada pesan masuk pada thread tersebut. Tidak mengirim ulang email. Kejelasan sumber/ketentuan konten Hadis tetap menunggu jawaban; izin mengirim email bukan persetujuan konten.
