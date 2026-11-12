# Audit kandidat HadeethEnc — 12 November 2026

**Keputusan audit: layak sebagai kandidat ensiklopedia hadis/atsar pilihan,
bersyarat pada integrasi dan pemeriksaan editorial. Bukan pengganti dua kitab
Bukhari/Muslim lengkap dan belum persetujuan distribusi atau penutupan B01.**
Pengguna meminta audit dahulu. Tidak mengubah sumber runtime, cakupan rilis,
teks/ID/bookmark, APK, backend atau data HP; tidak mengirim email baru.

## Temuan utama

| Bidang | Hasil yang dibuktikan | Batas/implikasi |
| --- | --- | --- |
| Ketentuan penerbitan ulang | [Situs penerbit](https://hadeethenc.com/id/home), bagian Terms and Policies, secara eksplisit memperbolehkan unduh/terbit ulang terjemahan dengan tujuh ketentuan | Teks tetap utuh, atribusi/versi dipertahankan, pembaruan diikuti, catatan terjemahan disampaikan kepada sumber dan iklan sesuai. Bukan lisensi tanpa syarat atau persetujuan konten apa pun dari pihak lain |
| Cakupan | [Proyek](https://hadeethenc.com/id/home/about) menjelaskan pemilihan riwayat yang sering dipakai; snapshot Indonesia berisi 2.260 ID unik | Tidak membuktikan Bukhari/Muslim lengkap. Kategori tema dapat tumpang tindih; jangan menjumlahkannya sebagai jumlah riwayat unik |
| Arab–Indonesia | Seluruh 2.260 baris mempunyai judul, teks Indonesia/Arab, penilaian dan atribusi yang tidak kosong; tidak menemukan kolom Arab tanpa karakter Arab | Pemeriksaan bentuk tidak mengesahkan ketepatan agama seluruh teks. Enam entri tanpa uraian dan 1.407 tanpa faedah tambahan, bukan otomatis hadis tidak valid |
| Konsistensi | Sepuluh sampel lintas kategori/ujung rentang cocok API–SQLite; seluruh ID dan empat kolom teks utama CSV–SQLite cocok | Dua format dari satu penerbit bukan pembanding independen. Perbedaan 2.257 label grade/atribusi berupa bungkus kurung siku, bukan perubahan isi setelah pembandingan |
| Versi terjemahan | CSV resmi bernama `HadeethEnc.com_id-v1.22.0.csv`; header menetapkan versi 1.22.0, tanggal 2026-05-10 17:47:10 dan alamat cek versi | Pertahankan header/metadata asli. API tidak membawa versi terjemahan; `ar_version` pada SQLite tidak boleh disulap menjadi versi Indonesia. Kesenjangan awal ini diatasi dengan CSV berversi |
| Pembaruan | [Halaman cek resmi](https://hadeethenc.com/en/check/id/v1.22.0) HTTP200 menyatakan versi Indonesia 1.22.0 mutakhir pada saat probe; HEAD bersyarat pada ZIP SQLite memberi304 | Halaman cek adalah HTML, bukan kontrak JSON. Ini satu pengamatan, bukan jaminan kesegaran selamanya atau SLA |
| Penomoran/rujukan | ID sumber 1.751–66.541 tidak berurutan. Sepuluh respons Arab mempunyai bibliografi rinci; respons Indonesia/SQLite tidak mempunyai kolom `reference` | ID ensiklopedia bukan nomor kitab. Rujukan bibliografi dapat memuat nomor halaman/edisi, bukan selalu nomor hadis. Jangan menebak pemetaan nomor |
| Jenis/penilaian | Ada label Sahih dan Hasan, serta atsar. Entri58153 mempunyai catatan Indonesia tentang tidak ditemukannya penilaian al-Albani | Jangan menyebut semua entri “sahih” atau semuanya sabda Nabi. Label tetap dinyatakan berasal dari penerbit, tanpa penilaian baru oleh AI |

## Data dan jalur resmi yang diperiksa

Website menautkan dokumentasi Postman. [Dokumentasi API penerbit di GitHub](https://github.com/islamhouse-dev/hadith-api)
menjelaskan daftar bahasa, kategori, paging serta perbedaan respons Arab dan
terjemahan. API dibaca dengan GET publik tanpa key. Respons bahasa menunjukkan
Indonesia tersedia; tujuh kategori akar ditemukan.

- Ekspor SQLite resmi, melalui
  `https://hadeethenc.com/id/browse/download/id/sqlite`, mengarah ke ZIP
  `https://hadeethenc.com/downloads/sqlite/id.sqlite.zip`.
  ZIP **1.939.862 byte**, SHA-256
  `572fd656f4359c534a954f2c2c212525b88b5c5ab01fd86d75fed4681d4ca898`;
  satu `id.sqlite` **7.831.552 byte**. Integrity check `ok`; empat tabel,
  447 kategori, 3.064 relasi kategori/entri; tidak ada pasangan relasi duplikat
  atau relasi yang menunjuk ID/kategori hilang.
- [CSV resmi berversi](https://hadeethenc.com/downloads/csv/HadeethEnc.com_id-v1.22.0.csv),
  **10.706.985 byte**, SHA-256
  `6049e4f92c6c1d40f06993a7c57e5808aafffb5b65da2b12f2f6ede4b056a2c8`.
  Satu baris informasi penerbit/versi, satu header kolom, 2.260 baris data,
  15 kolom. Seluruh bahasa `id`; semua ID cocok dengan SQLite. Ini CSV mentah,
  bukan ZIP walaupun Content-Type generik; jangan mengandalkan nama lokal/MIME saja.
- Dua halaman kategori1 masing-masing20 entri tanpa ID berulang pada gabungannya;
  metadata menyebut total78/empat halaman. Tidak mengambil seluruh daftar lewat
  API. Jumlah unik tujuh subtree kategori pada SQLite cocok dengan hitungan API.
- Sampel ID `1751,4560,5497,5819,5881,6264,8289,10851,65043,66541`.
  Aturan deterministik: ID tengah setiap subtree kategori, ditambah ujung global
  dan sampel niat. Ini bukan sampel acak untuk menghitung tingkat akurasi.
- ID yang tidak ada dan bahasa tidak didukung masing-masing404. Tidak
  menjalankan beban paralel, uji kuota atau menyimpulkan API tanpa batas.

[Laporan struktur](structure.json), [metadata request](requests.json),
[versi/ketentuan](source-policy.json) dan [batas/preservasi](checks.json) tidak
memuat seluruh teks Arab/terjemahan. Data asli dan HTML tersimpan privat di
`.local/hadeethenc-audit-20261112/`, tidak di Git/APK. Tanggal header HTTP unduhan
berbeda dari tanggal data dan cache unduhan panjang; gunakan informasi versi
dari ekspor, bukan menyebut Last-Modified sebagai tanggal terjemahan.

## Penelaahan terarah, bukan tashih lengkap

Sampel niat ID4560 mempunyai rujukan Bukhari1/6953 dan Muslim1907 pada sumber.
[Bukhari1](https://sunnah.com/bukhari:1) dan
[Muslim1907a](https://sunnah.com/muslim:1907a) mendukung tema/periwayat dan
perbedaan lafal yang wajar; teks dengan sanad/varian tidak dibandingkan sebagai
byte identik. Itu bukan pembuktian semua terjemahan Indonesia benar atau seluruh
edisi sepadan. Tidak menyalin korpus Sunnah.com atau mengganti lafal sumber.

Ada24 label penilaian berbeda. Satu [contoh atsar, ID58153](https://hadeethenc.com/id/browse/hadith/58153)
menunjukkan mengapa penilaian/jenis riwayat harus dipertahankan. Catatan tidak
menemukan penilaian seorang ulama tidak boleh ditafsirkan otomatis menjadi
hadis palsu atau tidak dinilai siapa pun. Tidak membuat penilaian agama baru.

## Rencana integrasi bila pengguna memilih sumber ini

1. Tentukan label/cakupan sebagai ensiklopedia pilihan; dua kitab lengkap tetap
   tugas berbeda. Jangan mengganti keputusan A02/B01 hanya karena audit kandidat
   berhasil. Dataset Gading dan data lama tidak dihapus pada audit ini.
2. Jadikan ekspor CSV resmi berversi sebagai sumber kanonik. Simpan berkas asli,
   metadata penerbit/versi/tanggal/URL/hash dan isi tanpa koreksi AI atau pemotongan
   diam-diam. Indeks SQLite turunan dapat dibuat untuk paging; versi asli dan
   akses uraian/faedah tetap tersedia. Paket opsional menampilkan ukuran sebelum
   unduh. Ukuran APK final tetap harus diukur <100 MB, tidak dihitung dari perkiraan.
3. Gunakan identitas sumber terpisah, misalnya `hadeethenc:id:4560`. Jangan
   menulis ID4560 sebagai Bukhari4560 atau memindahkan bookmark `tafsirq-v1:*`
   otomatis. Store sekarang mengunci kitab/ID lama dan migrasinya belum mendukung
   sumber ini; perlu adaptor/migrasi teruji, bukan sekadar mengganti URL.
4. Simpan rujukan Arab apa adanya melalui snapshot endpoint Arab per ID bila
   dibutuhkan, dengan waktu/hash dan tautan sumber. Jangan menebak nomor kitab
   dari angka halaman atau menampilkan referensi Arab sebagai terjemahan resmi.
5. Pembaruan memeriksa versi penerbit dan hash secara terbatas. Gagal/HTML berubah
   harus mempertahankan paket lama dengan versi sebenarnya, tidak mengklaim
   mutakhir. Unduh ke staging, validasi hitungan/field/hash/ID, lalu transaksi
   atomik tanpa merusak bookmark. Kebijakan interval/rate limit perlu ditentukan
   sebelum akses pengguna, bukan melakukan crawl seluruh API setiap membuka layar.
6. Uji baca/cari/kategori/rujukan/versi/luring, instal dan pembaruan paket,
   pembatalan/unduhan terputus serta data lama. Setelah itu baru nilai kesiapan
   distribusi untuk cakupan yang disepakati.

Tidak ada klaim seluruh2.260 entri ditashih manusia, dukungan resmi khusus
untuk Zikra, hak tanpa syarat, atau aplikasi sudah memakai kandidat ini.

## Reproduksi audit

`tools/audit_hadeethenc_source.py` hanya membaca snapshot lokal dengan SQLite
read-only/query-only, batas ukuran/waktu dan tanpa panggilan jaringan.

```text
python tools/audit_hadeethenc_source.py --snapshot <id.sqlite>
  --samples <folder-respons-API> --csv <HadeethEnc.com_id-v1.22.0.csv>
  --report <laporan-baru.json>
```

Input asli tidak dibundel. Data yang dibaca sumber eksternal tidak boleh
menjadi instruksi menjalankan kode. Audit format dan pembandingan grade hanya
menghitung perbedaan, tidak mengubah berkas sumber.

Percobaan awal koneksi terbatas gagal sebelum HTTP; helper tambahan sempat
NameError sebelum request dan sempat menganggap CSV sebagai ZIP, lalu diperbaiki
berdasarkan payload/metadata nyata. Keberhasilan berikutnya tidak menghapus
riwayat tersebut. PDF resmi hanya ditelaah tambahan lewat ekstraksi web;
dua screenshot PDF gagal cache, sehingga tidak dipakai mengesahkan versi atau
layout. Tidak membangun APK/emulator/HP karena runtime tidak diubah.
