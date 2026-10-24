# B01/B04 — pemeriksaan penutupan

24 Oktober 2026. Belum selesai; tidak mengubah isi APK atau mencentang B01/B04.

## B01: bukti upstream tambahan

Halaman resmi [Sumber Konten TafsirQ](https://tafsirq.com/page/source) membedakan open data, scraping, partner dan kontribusi pengguna. Halaman tersebut tidak mengidentifikasi edisi/penerjemah Indonesia dataset Hadis ini. [Kebijakan TafsirQ](https://tafsirq.com/page/term) juga belum menjawab pemakaian ulang dataset Hadis secara khusus. Ini bukan kesimpulan bahwa pemakaian dilarang; buktinya belum cukup untuk menutup asal/kelengkapan/ketentuan konten. Tidak mengirim email atau menganggap MIT repositori mengesahkan seluruh konten upstream.

## B04: antrean pemeriksaan per entri

`per-entry-review-queue.json` memuat 227 ID/fingerprint, status layak tampil serta pemeriksaan awal dari inventaris lama. 225 layak tampil secara field, 2 tidak. Dua belas sampel awal tetap dibedakan dari penelaahan Arab, arti dan konteks rujukan secara penuh. Status pending tidak boleh diubah otomatis menjadi valid karena field lengkap atau nomor rujukan ditemukan.

[Ketentuan EQuran](https://equran.id/terms) diperiksa kembali: integrasi aplikasi dan berbagi dengan atribusi diperbolehkan, pemakaian wajar serta batas hak konten tetap berlaku. Ini tidak membuktikan ketepatan semua bacaan.

Lanjutan yang diperlukan: perbandingan teks/arti/konteks tiap entri terhadap sumber rujukannya, pencatatan perbedaan dan koreksi terikat fingerprint; kemudian uji hanya kode/konten yang berubah. APK 1.7.4 belum diperbarui oleh pekerjaan ini.

## Pemeriksaan lanjutan nyata

Tiga ID tambahan 2/3/5 dibandingkan terhadap Arab, arti dan konteks halaman pembanding; catatan per ID ada dalam JSON antrean. ID 3 memiliki variasi lafal dan perbedaan atribusi penilaian, ID 5 perbedaan nomor Bukhari. Ini pemeriksaan awal tiga entri, bukan tashih penuh. Tersisa 210 dari 213 entri yang sebelumnya belum dibandingkan; 15 perbandingan awal total. APK tidak diubah karena catatan baru belum diterapkan/diperiksa.

## Perapian tampilan sumber setelah pemeriksaan editorial

Lima penanganan diterapkan di source: ID 57/76/77/209 memisahkan petunjuk Indonesia dari lafal Arab dan mempertahankannya di catatan sumber; ID 225 menghapus kurung tidak berpasangan tanpa mengganti kata Arab. Cocok hanya pada fingerprint judul/Arab/arti/rujukan yang diperiksa. Raw JSON, ID dan terjemahan tetap; perubahan sumber menonaktifkan koreksi lama. Belum membuat APK distribusi/nomor versi baru atau menguji HP.

[Build/lint](content-build.txt) lulus dan [tiga tes DuaReview](dua-review-unit.xml) lulus tanpa skip, memakai snapshot EQuran asli, termasuk penolakan koreksi pada sumber yang berubah serta tidak ada teks Latin tersisa pada empat lafal tersebut. Tidak menyalakan emulator. Build sementara tidak menggantikan APK 1.7.4 yang dibagikan sebelumnya.

Pembacaan editorial judul/Arab/arti dilakukan pada katalog; hasilnya disimpan per entri. Ini bukan pemeriksaan seluruh rujukan/harakat. Temuan ID 16/26/55/66/70/103/121/154/186/188 masih memerlukan penanganan terverifikasi. Bukti pembanding ID 225: https://sunnah.com/hisn:119. Pembanding https://sunnah.com/bukhari:2834 dan https://sunnah.com/muslim:1805a menunjukkan bahwa keduanya tidak memuat keseluruhan redaksi Labbaik pada ID 186; jangan otomatis menyamakan semua redaksi atau menambahkan huruf berdasarkan dugaan saja.

B01: alternatif API Hadis Malaysia diperiksa ulang pada dokumentasi koleksi dan ketentuan resmi; butuh akun/key, dan belum memberi identitas penerjemah Indonesia yang cukup untuk migrasi. Tidak mengubah sumber atau bookmark secara diam-diam. https://developer.hadis.my/docs/collections/ · https://developer.hadis.my/legal/terma/

B01/B04 tetap terbuka. Tiga sampel lanjutan dan satu pembanding format ID 225 bukan penutupan validasi 225 bacaan. Review rujukan seluruh koleksi dan kejelasan sumber Hadis masih diperlukan.

## Checkpoint 1.7.5 dan permintaan klarifikasi terkirim

Pengguna memberi izin eksplisit untuk mengirim pertanyaan sumber; email berjudul **Klarifikasi sumber, penomoran, dan penggunaan data Hadith API untuk Zikra** berhasil dikirim ke contact@gading.dev melalui Gmail pada 24 Oktober 2026. Pengiriman dikonfirmasi label SENT, bukan hanya draf. Balasan belum diperiksa/diperoleh pada checkpoint ini. Bukti ID privat disimpan lokal, bukan di repo. Permintaan ini tidak dianggap persetujuan pemilik konten atau penutupan B01.

Empat judul tambahan dirapikan dengan fingerprint: ID 16 disesuaikan dengan isi petikannya (judul lama menyebut siksa kubur yang tidak terdapat pada petikan itu); typo ID 66/103/121 diperbaiki. Judul sumber tetap ditampilkan pada catatan. Tidak mengganti lafal/arti untuk empat kasus ini. Total penanganan kurasi sekarang 14 snapshot: 5 lama, 5 format dan 4 judul.

[v175-build.txt](v175-build.txt) dan [v175-dua-review-unit.xml](v175-dua-review-unit.xml) mencatat tiga tes snapshot asli lulus tanpa skip dan build/lint tanpa error. Empat tes alat perbandingan juga lulus. Tidak menyalakan emulator/HP; ini bukan bukti interaksi native baru. APK sebelumnya 1.7.4 tidak ditimpa.

## Pemetaan pembanding Hisnul Muslim

Satu halaman lengkap https://sunnah.com/hisn diperoleh melalui alat web setelah akses HTTP langsung mengembalikan 403; 3.890 baris bernomor lengkap tersimpan privat. `tools/audit_dua_reference.py` menolak halaman tidak lengkap dan mencari petikan Arab setelah normalisasi. Batas entri diputus sebelum rujukan/judul bab berikutnya. Hasil [hisn-comparison-candidates.json](hisn-comparison-candidates.json): **78 entri mempunyai calon pembanding lafal**. Tidak ada teks Arab/terjemahan seluruh buku pembanding yang dibundel ke Git atau APK.

Kecocokan tersebut mengabaikan harakat, spasi dan beberapa bentuk huruf; ini **calon pembanding, bukan 78 entri tersertifikasi benar**. Konteks pasar vs terminal pada ID 55, varian lafal, status atsar, arti dan penilaian tetap perlu dibaca terpisah. Tidak adanya pasangan di Hisnul Muslim juga bukan bukti bacaan salah. Seluruh status religious validation tetap tidak diklaim.

B01 menunggu kejelasan eksternal. B04 belum memenuhi pemeriksaan per entri menyeluruh; 78 hasil pencocokan otomatis tidak dipakai untuk menutup checklist. Tidak ada pengurangan koleksi atau perubahan cakupan rilis untuk memaksakan centang.
