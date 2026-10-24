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
