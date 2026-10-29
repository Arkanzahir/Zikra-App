# C07 — lampiran dokumen, beta1.9.30

30 Oktober2026. C07 dikerjakan sebelum E sesuai permintaan pengguna. Checkpoint
ini menutup perbaikan parser dan regresi lokal yang dibuktikan di bawah, **bukan
seluruh C07/A–D atau kesiapan publik**. HP tidak tersambung dan tidak disentuh.

## Perubahan

Parser DOCX dipisahkan dari pengelola lampiran menjadi `AssistantDocx`.
Sebelumnya `<w:tab/>`, `<w:br/>`, dan `<w:cr/>` dilewati, sehingga misalnya
`matematika<TAB>Senin` menjadi `matematikaSenin`. Sekarang tab, baris, paragraf,
kolom dan baris tabel tetap terpisah. Ini ekstraksi teks, bukan pelestarian
seluruh tata letak atau semantik tabel Word.

XML dibaca langsung dari byte agar deklarasi/BOM UTF-8/UTF-16 terhormat.
Namespace Word transitional dan strict diterima; teks namespace asing di luar
run Word diabaikan. DOCTYPE/entity declaration ditolak melalui token parser,
termasuk UTF-16; tidak ada resolusi tautan atau pembukaan berkas tertanam.
Batas arsip tetap200 entri,2MB per entri,4MB total, kedalaman64, teks12.000
karakter. Anggaran diperiksa selama pembacaan, bukan setelah ekspansi penuh.
MIME/input8MB dan alur consent/pemutar/tema/font/UI tetap.

## Bukti

| Pemeriksaan | Hasil dan batas |
| --- | --- |
| [Build](build.txt), [helper akhir](harness-final.txt), [ringkasan](checks.json) | APK debug/helper terbangun;163 unit lulus/4 skip;lint0 error/862 warning. Build/lint aplikasi dilakukan setelah parser final; helper kemudian diperbaiki untuk urutan pemeriksaan jaringan, tanpa perubahan produksi. |
| [Dokumen Android](native-documents.txt) |4 metode API37.1 lulus: run/tab/break/paragraf/tabel/entity bawaan; UTF-16/namespace strict/teks asing;12 penolakan XML berbahaya/salah namespace/terlalu panjang/dalam + batas tepat12.000; draf tetap setelah impor gagal dan pembatalan antrean. File kamera sementara dibersihkan. |
| [Format/berkas berbahaya](native-parsers.txt) |1 metode lulus: normalisasi foto,3 halaman PDF,TXT,DOCX; MIME salah,PDF4halaman/rusak,UTF-8 salah,path traversal,DOCTYPE,arsip/teks berlebih ditolak. |
| [Provider kamera](native-camera.txt) |1 metode lulus: URI privat ditulis, diimpor, dipulihkan, dihapus. Ini provider lokal emulator, **bukan kamera/grant/picker fisik HP**. |
| [Consent](native-consent-final.txt), [screenshot](consent.jpg) |1 metode lulus: preview sebelum kirim,6 pilihan defaultfalse, hanya lampiran terpilih masuk request, tidak membocorkan profil fixture; draf dibersihkan hanya setelah respons sukses. HTTP interceptor sintetis, bukan provider nyata. |
| [Putus/pulih](native-network-final.txt), [screenshot](offline.jpg) |1 metode lulus: Wi-Fi/data benar-benar dinonaktifkan lalu dipulihkan; nol request otomatis; teks dan objek draf foto tetap. Mode Offline/Online terlihat. Jaringan dipulihkan. |
| [Backend lampiran](backend-attachments.txt) |5 tes lulus/0 skip untuk kontrak isolasi/validasi/batas gambar/teks/model vision. Backend tidak diubah atau dideploy ulang. |
| [Preservasi](preservation.json), [state terlindungi](protected-state.json) |32 berkas asli emulator pulih cocok hash; pengaturan/viewport kembali; helper/marker/screenshot harness dibersihkan dan emulator ditutup. WIP PROJECT_HANDOFF, stash/arsip audio, env, ikon/artwork tetap. Tidak mengunggah data asli ke provider. |
| [APK](apk.json), [sertifikat](signature.txt) |1.9.30/code57,45.465.323byte, <100MB;SHA256`5fa27e7faa466dc4d56d42e99890156c571cd1fba121f0089eb2d78a5cd46661`. Signer pembaruan sama,0 nilai credential privat dikenal di seluruh entri APK; dua audio tetap cocok hash. Bukan sertifikasi keamanan seluruh aplikasi. |

Sembilan eksekusi/delapan metode native unik lulus. Uji pembatalan terakhir
memakai **foto JPEG valid**, bukan sekadar berkas rusak: [hasil](native-cancel-valid-final.txt).
Dua sesi emulator masing-masing mencadangkan/memulihkan32 berkas cocok hash;
helper terakhir hanya memperkuat fixture pembatalan, tidak mengubah APK produksi.
Dua percobaan consent awal gagal pada
`Validated internet needed` sebelum Activity dibuka; [percobaan pertama](native-consent.txt)
dan [ulang](native-consent-retry.txt) tetap disimpan dalam [ledger](native-attempts.json).
Helper kini membuka halaman dan menunggu jaringan tervalidasi sebelum assertion.
Tidak mengubah klasifikasi jaringan, izin, TLS, atau memalsukan VALIDATED.
Percobaan gagal tidak dihitung lulus. Log publik dinormalisasi newline/whitespace;
log mentah tetap privat.

APK lokal folder pembungkus: `Zikra-Attachments-v1.9.30-beta-debug.apk`.
Belum dipasang di HP; versi terakhir yang dibuktikan di HP tetap1.9.29.
Backup emulator/log mentah privat disimpan di `.local/attachment-c07-20261030`.

## Yang tetap terbuka

- C07: picker/grant/izin ditolak pada HP dan variasi scan/OCR/model. Pengguna
  pernah mengonfirmasi kamera bekerja pada percakapan sebelumnya; konfirmasi
  itu tidak membuktikan seluruh alur grant/picker. Bukti provider nyata foto,
  PDF scan satu halaman,TXT/DOCX lama tetap [historis](../ai-home-refresh-2026-10-25/README.md).
  Tidak ada inference baru pada checkpoint ini; tidak menjamin semua scan akurat.
- C02: permintaan AI dari HP saat laptop mati. D02/D02a: ketentuan rekaman dan
  sisa pemeriksaan vendor/Doze/reboot/interaksi. B01 sumber Hadis tetap menunggu;
  tidak membaca atau mengirim email baru.
- E belum dimulai oleh checkpoint ini. Sorotan UI10 tetap ditunda pengguna.
  Seluruh A–D/E/F/G dan publik tidak otomatis selesai oleh build/test parser.

Preview lokal sintetis: [lampiran](attachment.jpg), [foto](preview.jpg),
[consent](consent.jpg), [offline](offline.jpg).
