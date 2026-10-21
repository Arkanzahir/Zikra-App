# Akses Production Foundation — 21 Oktober 2026 (WIB)

Pengguna membuat app API-access-only / Server di Developer Console dan mengisi
kredensial Production ke `.local/quran-foundation.env`. Folder tersebut diabaikan
Git root dan bukan bagian Android app. Secret/token tidak dicetak, disimpan di
laporan, dimasukkan ke command line, di-stage atau dibundel ke APK.

## Pengujian aktual

Pada 06:59 WIB, menggunakan Client Credentials dengan scope content sesuai
[quickstart resmi](https://api-docs.quran.foundation/docs/quickstart/):

| Probe | HTTP | Ringkasan respons |
| --- | --- | --- |
| Token Production | 200 | Token diterima; hanya di memori proses |
| chapters | 200 | 114 surah |
| verses/by_chapter/67 | 200 | 30 ayat, 67:1–67:30, Utsmani dan resource 33 |
| verses/by_page/1, mushaf=1 | 200 | 7 ayat; 36 elemen kata/glyph dengan metadata baris |
| recitations/7/by_ayah/1:1, segments | 200 | 1 berkas audio, 4 segmen |

[Bukti tersanitasi](access.json). Ini uji akses dari komputer, **bukan verifikasi
rendering Mushaf, decoder Android atau kesesuaian timing seluruh rekaman**.
Elemen words dapat mencakup penanda akhir ayat; jangan menyamakan jumlah glyph
dengan jumlah kata bersuara pada timing.

## Perubahan status dan langkah berikutnya

Hambatan autentikasi Production sebelumnya sudah teratasi; tidak perlu meminta
kredensial lagi di chat. Akun developer kini tersedia. Data/font/image berikutnya
tetap mengikuti atribusi, sumber CDN terdokumentasi dan ketentuan penyimpanan QF.
Token API content biasa bukan bukti akses Content Sync; izin/endpoint itu belum diuji.

Berikutnya: periksa page-layout API, pemetaan font/glyph/nomor ayat untuk mushaf 1,
uji halaman awal/tengah/akhir/Al-Mulk dan unduh font dari URL resmi. Setelah itu
hubungkan loader/renderer Android dan perpindahan mode/posisi. Buat backend sebelum
menggunakan kredensial dari aplikasi; secret tidak boleh dipindahkan ke Java/XML.
Tes HP qari asli masih tertunda sesuai pilihan pengguna.

## Mengulang probe

Jalankan `tools/Test-QuranFoundation.ps1 -OutputPath <laporan.json>` dari root.
Script memakai konfigurasi lokal ignored, host production/prelive yang dipatok,
tanpa redirect, dan hanya menyimpan status/jumlah/indikator metadata. Error mentah
HTTP tidak dicetak karena dapat mengandung detail sensitif.
