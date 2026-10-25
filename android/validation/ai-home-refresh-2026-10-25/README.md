# AI dan kerapian Home — beta 1.9.5

25 Oktober 2026. Source runtime/harness `a5d070a`. ID aktif UI04; C08 ditutup
berdasarkan pemeriksaan lokal di bawah. C03/C07/C09/UI04 keseluruhan tetap terbuka
sesuai batasnya, bukan seluruh C atau rilis publik selesai.

## Hasil yang tersedia

- AI memakai permukaan krem/pine/mint, header riwayat/chat baru, saran ringkas,
  bubble percakapan, salin jawaban, +/dikte/kirim. Orb lama diganti. Dock hilang
  saat keyboard muncul. Pada font 200% area pengetikan dipadatkan dan tombol
  kirim/+ diperiksa **seluruh tingginya**, bukan sekadar sebagian terlihat.
- Default Otomatis memakai internet Android yang tervalidasi serta konfigurasi
  gateway. Manual Online/Offline tersedia; putus/pulih hanya memperbarui status,
  tidak mengirim draf. Error layanan diberi status dan retry; tidak disamarkan
  sebagai jawaban offline. Offline tetap panduan berbasis aturan.
- SQLite `assistant_chats.db` v1 memisahkan banyak chat dari SQLite utama v4.
  Migrasi history lama mempertahankan metadata dan preference asal; impor sekali.
  Cari judul, baru/pilih/ubah/hapus dengan konfirmasi, draf per chat dan pulih
  setelah buka ulang. Batas 40 chat, 200 pesan/128.000 karakter per chat;
  batas penuh meminta tindakan pengguna, tidak menghapus diam-diam. Pengiriman
  konteks hanya chat aktif/pilihan yang ditinjau, tidak seluruh daftar chat.
- + membuka Kamera/Galeri/Dokumen Android, satu lampiran sampai 8 MB. Foto
  JPG/PNG/WebP dinormalisasi JPEG tanpa EXIF; PDF sampai tiga halaman dirender
  lokal, DOCX/TXT sampai 12.000 karakter diurai lokal. Preview foto/halaman/teks
  dan hapus sebelum Kirim. Tidak mengunggah otomatis; per-request consent tetap
  default tanpa profil/tugas/salat/riwayat tambahan. Penyimpanan lampiran privat
  dibatasi 25 MB; hapus percakapan membersihkan paketnya. Tidak ada izin penyimpanan
  luas atau provider key di APK. Kamera memakai URI privat dengan grant sementara.
- Backend memilih model vision yang aksesnya diuji hanya untuk gambar/PDF,
  sedangkan teks tetap model lama. Body lampiran 2 MiB, pesan biasa tetap 48 KiB;
  MIME/base64/dimensi/nama/URL/ukuran divalidasi. Tidak menyimpan lampiran di gateway
  atau mencatat isi request pada log aplikasi.
- Home: judul/countdown/bacaan berbobot 600, simbol resmi disesuaikan proporsinya,
  warna teks bawah dan ukuran bacaan dirapikan. Aset/lampu siang-malam/ikon pilihan
  pengguna dari 1.9.4 dipertahankan. Bukan identik per piksel dengan mockup.

## Bukti terarah

| Pemeriksaan | Hasil dan lingkup |
| --- | --- |
| [Backend](backend-tests-final.txt) | 40 total: **36 lulus, 4 skip**, 0 gagal. Skip memerlukan Redis/Lua lokal terpisah; tidak disebut lulus. Lima tes lampiran dan konfigurasi vision diperiksa. |
| [Unit Android](unit-results.json) | 120 total: **116 lulus, 4 skip**, 0 gagal/error. Tidak mengulang unit sesudah perubahan khusus view/keyboard. |
| [Build/lint final](keyboard-fix-build.txt), [lint](lint-summary.txt) | Debug + helper terbangun; **0 error, 752 warning**. Warning tidak disembunyikan. |
| [AI native final](native-ai-100.txt) | 7 metode: migrasi/idempotensi/isolasi/batas; foto/PDF/DOCX/TXT dan berkas berbahaya; URI kamera/import/restore/hapus; riwayat/search/rename/batal-hapus/hapus/reopen; consent tanpa kebocoran; putus/pulih tanpa auto-send; keyboard. Jaringan uji dipulihkan. |
| [Regresi native](native-regression.txt) | 6 metode: HTTP sukses/auth/kuota/timeout/server/rusak terkendali, batal/late callback, review tugas atomik/migrasi, consent/draf dikte, loading/retry, sumber. Respons interceptor sintetis, bukan bukti provider nyata. |
| [Font 200%](native-ai-200.txt), [layar sempit](native-ai-narrow-200.txt) | Dua eksekusi tambahan metode keyboard; seluruh tombol kirim/+ terlihat, input terjangkau. Screenshot wide/narrow dibedakan. |
| [Home](native-home-100.txt), [Home 200%](native-home-200.txt) | 2 metode + 1 eksekusi font besar sebelum fix khusus keyboard AI; source Home sama. Countdown/data lokal/gulir/navigasi dan sky siang/malam/fase diperiksa. |
| [APK → Vercel → provider](native-live-vision.txt), [receipt](native/AI195-live.json) | Satu metode nyata membaca hitungan yang **hanya ada di gambar**: foto `17+26=43` dan PDF scan `28+34=62`, tanpa interceptor. Dilakukan tepat sebelum fix khusus keyboard; jalur jaringan/parser identik. Bukan foto kamera fisik. |
| [TXT/DOCX nyata](live-documents.json), [HTTPS](hosted-http.json) | Kode di isi dokumen terbaca melalui hosting, health200/tanpa token401/lampiran tidak valid400. Fixture instalasi sintetis kemudian dihapus dari Redis. |
| [Preservasi](preservation.json), [audit](audit.json) | 10 file asli emulator pulih cocok hash; font/ukuran/jaringan kembali, helper/marker dihapus, emulator ditutup. Tidak mengubah HP. WIP/env/stash/audio/baseline APK dipertahankan. Audit nilai rahasia saat ini bersih; bukan sertifikasi seluruh riwayat Git. |

Ada kegagalan yang **tidak dihitung lulus**: percobaan live awal tidak menampilkan
jawaban; percobaan berikutnya menampilkan `43`, tetapi mengabaikan permintaan
tambahan membaca kode referensi. Tes live terakhir sengaja memeriksa **hitungan
visual saja**, bukan mengklaim seluruh instruksi/OCR dipenuhi. Probe kode terpisah
langsung ke provider berhasil, tetapi akurasi umum belum terbukti. Preview
menjelaskan keterbatasan foto/scan/tabel/tulisan kecil. Build lint pertama sempat
gagal pada lookup ID header; telah diperbaiki. Pemeriksaan screenshot juga
menemukan tombol terpotong walau assertion lama lulus sebagian; fix dan assertion
tinggi penuh diuji ulang pada APK final.

## APK dan deployment

APK di folder luar: `Zikra-AI-v1.9.5-beta-debug.apk`, **37.175.496 byte**, code32.
SHA-256 `a25f0db2ac004721c4a531aab044920a6a28add5f16f2fa2cdb34828f55ab13f`.
[Receipt/source hashes](apk-receipt.json), [signer](signature.txt). Sertifikat
debug sama baseline, installer <100.000.000 byte; kandidat 1.9.4 dan lebih lama
tetap ada. Bukan signed release publik.

Deployment existing [zikra-ai](https://zikra-ai.vercel.app/health), ID
`GXHBqWcUXxyy5SZFSLFzkpcUpYpZ`, alias immutable
`https://zikra-3k3xtmtcm-arkanzahir86-3257s-projects.vercel.app`.
Konfigurasi tambahan server-only `FEATHERLESS_VISION_MODEL`, tanpa mengganti key
asli, token pengguna, database/proyek/paket hosting atau auto-upgrade berbayar.

## Belum selesai

- C03: Wi-Fi tersambung tanpa akses internet/captive portal dan seluruh timeout/
  konfigurasi UI belum dibuktikan. C06: mikrofon/recognizer nyata belum diuji.
- C07: kamera fisik, grant/picker/izin ditolak di HP dan variasi dokumen/OCR
  memerlukan pemeriksaan. Tiga halaman teruji parser lokal; live scan satu halaman.
  Format lain belum didukung. Bukan jaminan jawaban/scan selalu benar.
- C09: layanan generasi gambar belum dikonfigurasi atau dipasang. Gemini API/
  AI Studio memungkinkan image generation, tetapi model gambar API yang ditinjau
  tidak menyediakan Free Tier; belum ada persetujuan biaya/key Gemini. Tidak
  mengaktifkan billing atau tombol yang berpura-pura menghasilkan gambar.
  [Dokumentasi](https://ai.google.dev/gemini-api/docs/image-generation),
  [harga](https://ai.google.dev/gemini-api/docs/pricing).
- C02 operasional/laptop mati/monitoring, C04 jadwal kelas/akurasi Tadabbur,
  B01 sumber Hadis, D02a adzan, F/G serta publik tetap terbuka.

Preview native: [Home](native/Refresh-home-top-100.png),
[siang](native/Refresh-sky-day-100.png), [malam](native/Refresh-sky-crescent-100.png),
[AI awal](native/AI195-welcome-100.png), [riwayat](native/AI195-history-renamed-100.png),
[foto nyata via layanan](native/AI195-live-photo.png),
[PDF nyata via layanan](native/AI195-live-scan.png),
[font 200% sempit](native/AI195-keyboard-narrow-200.png).
