# Fondasi Mushaf dan timing — 21 Oktober 2026

Baseline `4ec1cbe060ca2b0ddda404586b7fa9384d53c2ba`. Gambar Mushaf tambahan dari
pengguna disimpan di `design/references/mushaf-user-reference-2026-10-21.png`.
Ini acuan UI, bukan sumber ayat, baris atau timestamp.

## Implementasi aktual

- `MushafPage`: model immutable edisi/revisi/font/halaman/baris/kata; memeriksa
  nomor ayat, urutan baris, word-id/posisi hilang atau duplikat. Mendukung ayat
  lintas baris dan beberapa ayat dalam satu baris; tidak melakukan wrapping teks.
- `MushafIndex`: indeks inventori halaman satu edisi/revisi; menolak paket halaman
  tidak lengkap, campuran edisi dan gap kata antarhalaman. Pemetaan ayat ke halaman
  mempertahankan halaman pilihan bila ayat tersebut membentang dua halaman.
  Pemeriksaan ini bukan pembuktian seluruh isi korpus benar/lengkap secara ilmiah.
- `WordTimeline`: lookup timestamp sumber berdasarkan posisi player yang diberikan
  pemanggil, nomor ayat dan SHA-256 recording. Menolak segmen tumpang tindih,
  durasi di luar rekaman dan cakupan kata tidak lengkap. Tidak menghasilkan durasi
  dari panjang teks. Recording/ayat berbeda, buffering, gap atau opt-out menghasilkan
  tanpa sorotan. Jeda/seek/repeat mengikuti posisi input, tanpa jam animasi tersendiri.

**Belum ada renderer, parser/loader paket sumber, adapter MediaPlayer, mode Mushaf,
penyimpanan posisi baru atau kontrol sorotan di UI.** Modul belum terhubung dengan
layar produksi. Fungsi pemutar/reader dan data lama tetap utuh. Belum ada aset Quran
eksternal yang dibundel. Nama SHA-256 pada model bukan bukti bahwa berkas streaming
sudah diverifikasi; adapter berikutnya harus membuktikannya sebelum memanggil lookup.

## Sumber konkret yang diperiksa

- [QUL layout 10](https://qul.tarteel.ai/resources/mushaf-layout/10): KFGQPC V2,
  cetakan 1421H, 604 halaman/15 baris. Kandidat yang cocok, belum diimpor atau final.
- [Font 249](https://qul.tarteel.ai/resources/font/249): QPC V2, 604 font per halaman;
  halaman mendokumentasikan bundling font native dan TTF. Panduan penggunaan ini
  dicatat, tetapi belum ditemukan teks lisensi eksplisit dari pemegang hak yang
  mencakup paket font+layout+script yang akan didistribusikan.
- [Script 61](https://qul.tarteel.ai/resources/quran-script/61): QPC V2 glyph per kata,
  pasangan yang ditautkan oleh layout 10. Jangan menganggap JSON contoh di halaman
  dokumentasi sebagai dump produksi yang konsisten; file lengkap belum diaudit.
- [Isu izin QUL 770](https://github.com/TarteelAI/quranic-universal-library/issues/770):
  endpoint comments GitHub HTTP 200, daftar komentar kosong saat pemeriksaan.
  Ketiadaan jawaban bukan izin. Tidak mengirim pesan/permintaan atas nama pengguna.
- Pengguna mengonfirmasi **belum mempunyai akun Developer Console Foundation**.
  Jalur bundle font/image Foundation yang mensyaratkan akun aktif belum tersedia.
  [Syarat dan audit sebelumnya](../local-qari-2026-10-21/SOURCE_AUDIT.md).

## Pengujian sesi ini

`assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest`, `lintDebug` lulus:
**69 JVM (17 baru), nol failure/error/skip, lint nol error/577 warning**.
[Log akhir](build-final.txt), [ringkasan](summary.json).

Fixture memakai identitas/token/timing sintetis; bukan Quran, edisi mushaf atau
rekaman qari. Tidak mengulang emulator bermasalah; pengguna menunda tes HP.
Tidak ada screenshot/video baru atau klaim UI Android sudah menyerupai referensi.

## Langkah berikutnya

Selesaikan izin/akses paket satu edisi, simpan versi+hash dan audit korpus nyata
termasuk nomor ayat inline, halaman awal/tengah/akhir/Al-Mulk, basmalah/waqaf dan RTL.
Lalu buat loader/renderer native, indeks korpus penuh, persistensi posisi/mode dan
adapter player. Verifikasi pasangan recording/timing per qari sebelum sorotan emas.
Tes HP audio asli tetap pekerjaan terbuka tersendiri. Jangan menambah tombol kosong.
