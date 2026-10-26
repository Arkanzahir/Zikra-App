# AI Seimbang — implementasi native, 26 Oktober 2026

Pengguna menyetujui varian **Seimbang** dan meminta footer peringatan di bawah
composer dihapus. [Acuan/persetujuan](../../../design/concepts/ai-seimbang-approved-2026-10-26.md).
Implementasi Android Java/XML tetap memakai penyimpanan dan gateway yang sudah ada.

## Perubahan

- Header riwayat/judul-mode/chat baru, sapaan dan empat pintasan lebih ringkas;
  ikon tipis dan warna putih hangat/mint/hijau, dengan pasangan tema malam.
- Jawaban asisten tanpa kartu berulang; pesan pengguna mint, salin jawaban asli.
- Composer multiline dengan lampiran, tambah/Alat, dikte dan kirim/batal menyatu.
  Tidak menampilkan footer yang ditolak pengguna atau dock aplikasi di area chat.
- Sheet lampiran Kamera/Galeri/Dokumen dengan rincian format yang dapat dilipat.
  Pilihan tetap draf. Konfirmasi kirim dan pilihan konteks tetap tersedia.
- Riwayat dengan pencarian berisi, baris ringkas, chat aktif, rename/hapus dengan
  konfirmasi dan penyimpanan lokal. Buka/tutup panel tidak mengirim/menghapus draf.
- Gambar dinonaktifkan ketika capability online belum tersedia; tidak meniru
  kemampuan Gemini/ChatGPT yang tidak tersedia pada backend Zikra.

`AssistantUi.java` memisahkan pembentukan sheet/ikon/surface dari pengambilan
berkas dan pengiriman. Tidak ada perubahan API, kredensial, kuota server,
schema SQLite (utama4/chat1), sumber Hadis, Home, atau aset audio.

## Pemeriksaan

Build debug + helper native + lint berhasil, satu worker/heap768MiB.
Unit Android:120 tercatat, **116 lulus/4 skip**,0 failure/error.
Lint:0 error/764 warning; tidak mengklaim seluruh warning proyek sudah selesai.

Putaran pertama pada source5283647:
3 metode chat/riwayat/lampiran lulus,6 regresi asisten lulus,1 pengulangan panel
tema malam lulus,1 pemeriksaan visibilitas composer font200% lulus.
Respons online/regresi memakai interceptor/sintetis, bukan panggilan provider baru.
Pemeriksaan composer awal tidak mensyaratkan IME benar-benar terbuka; bukti keyboard
harus mengikuti uji ImageGenerationTest pada putaran final.

Inspeksi screenshot menemukan tema malam mewarisi tema Android langsung dan
melewati pengaturan system bar aplikasi. Source5c46f1a memperbaiki pewarisan
Theme.Zikra, mempertahankan dialog malam, serta menambah assertion header/composer
terhadap batas status/navigation bar. Screenshot yang keliru disisihkan privat.

Putaran final, receipt/hash dan hasil aktual dicatat pada `release.json` dan
`native-results.json`. Pemeriksaan meliputi panel terang/malam dan composer pada
layar sempit640×1280/font200% dengan **IME wajib terlihat**, memakai metode
`ImageGenerationTest#testImageComposerAtLargeTextHasWholeSendAndAttachmentTargets`.
Capability gambar diatur sebagai fixture tampilan; tidak menjalankan generasi gambar.

Penyempurnaan berikutnya menyembunyikan sapaan saat IME aktif dan memakai controller
system bar agar kontras tetap benar setelah dialog/sidebar. Assertion kontras
terang/malam lulus. Pada satu percobaan terakhir proses instrumentasi keyboard
crash sebelum menghasilkan hasil metode. Data dipulihkan; metode keyboard diulang
pada **APK yang sama** dan lulus dengan IME terlihat/target utuh. Tidak menyimpulkan
akar crash dari ketiadaan stack trace. Percobaan gagal dan pengulangan dicatat di
`native-retry.json`; hasil final menggabungkan dua panel lulus dan pengulangan ini.
Tidak menyebut seluruh percobaan berjalan tanpa hambatan.

## Preservasi dan batas

- Pengujian hanya emulator. HP fisik tidak dipasang/dibuka/diganti konfigurasi.
- Data non-cache emulator dicadangkan privat dan dipindahkan sementara di perangkat
  sebelum fixture dibuat; dipulihkan dan diverifikasi hash. Pengaturan font, keyboard,
  wake, ukuran dan tema dikembalikan; jaringan tidak diubah. Lihat `preservation.json`.
- Satu sesi uji terhenti sebelum hasil native. Saat dilanjutkan, cadangan asli
  dipulihkan dan diverifikasi lebih dahulu; tidak menimpa arsip asli dengan fixture.
- WIP PROJECT_HANDOFF.md, stash/arsip audio, env privat, logo/aset Home dan APK1.9.7
  dipertahankan; audit hanya membuktikan nilai rahasia yang diketahui tidak ada pada
  source/evidence/APK yang dipindai. Bukan audit semua kemungkinan secret historis.
- Belum menguji desain1.9.8 pada HP pengguna, kamera/picker/OCR fisik C07,
  mikrofon nyata ulang, atau AI saat laptop mati C02. C04/D04 tetap mengikuti dependensi.
- Persetujuan mockup dan hasil terarah ini tidak mencentang seluruh UI04/F/G,
  B01, seluruh C atau rilis publik. APK bertanda tangan debug untuk beta.
