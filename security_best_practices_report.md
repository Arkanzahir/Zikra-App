# Tinjauan keamanan dan privasi Zikra — 30 Oktober2026

## Ringkasan

Peninjauan terarah pada alur AI, lampiran, penyimpanan dan manifest APK.
Satu kekurangan konfigurasi backup diperbaiki pada kandidat **1.9.32/code59**.
Aturan sumber dan hasil kompilasi APK diperiksa;30 tes backend terarah lulus,
lint0 error/861 warning. **Bukan pentest menyeluruh atau penutupan G03/publik.**
HP tidak diubah; versi terakhir yang dipasang tetap1.9.31.
[Bukti dan APK](android/validation/privacy-review-2026-10-30/README.md).

Skill security-best-practices digunakan untuk backend JavaScript. Proyek memakai
`node:http`, bukan Express/Next yang mempunyai referensi khusus dalam skill;
review juga mengacu [panduan Node.js](https://nodejs.org/learn/getting-started/security-best-practices).
Java/Android berada di luar panduan bahasa skill, sehingga pemeriksaan manifest
mengacu [panduan resmi Android](https://developer.android.com/privacy-and-security/security-best-practices).

## Tinggi — gerbang distribusi publik, belum ditutup

**SEC-01 — APK yang dibangun masih debug, bukan paket Play Store.**
APK terverifikasi memiliki `debuggable=true` dan sertifikat debug yang sama
dengan pembaruan sebelumnya. Dampak: akses debugger/run-as dari komputer yang
diizinkan masih tersedia pada beta; paket ini belum boleh dinyatakan sebagai
release publik yang dikeraskan. G04 tetap perlu build release, penandatanganan
dan audit paket final. Ini sifat distribusi beta yang diketahui, bukan temuan
bahwa aplikasi jarak jauh dapat membaca seluruh HP.

## Sedang — diperbaiki secara konfigurasi

**SEC-02 — Aturan perpindahan data antarperangkat belum eksplisit.**
Sebelum perubahan, manifest hanya menetapkan `allowBackup=false`. Android
menjelaskan bahwa sebagian vendor tetap mengizinkan D2D walaupun flag itufalse.
Kini [manifest:28](D:/Download/Zikra-App-main/Zikra-App-main/android/app/src/main/AndroidManifest.xml:28)
merujuk aturan legacy dan Android12+, mengecualikan sembilan domain penyimpanan
pada cloud dan D2D, termasuk DB/preferences/berkas/penyimpanan device-protected.
[Aturan legacy](D:/Download/Zikra-App-main/Zikra-App-main/android/app/src/main/res/xml/backup_rules.xml:3),
[aturan Android12+](D:/Download/Zikra-App-main/Zikra-App-main/android/app/src/main/res/xml/data_extraction_rules.xml:3).

[Pemeriksaan APK](android/validation/privacy-review-2026-10-30/backup-policy-apk.json)
membuktikan resource dan referensi manifest benar-benar dibundel. Belum melakukan
transfer fisik antarvendor; tidak menjamin OEM mematuhi seluruh aturan. File yang
secara eksplisit diekspor pengguna ke luar area aplikasi tidak menjadi milik
aplikasi dan tidak otomatis dihapus oleh kebijakan ini. Data pada HP yang ada
tidak dihapus; migrasi/backup pengguna yang dikelola aplikasi belum dibuat.
[Dasar dan batas Android](https://developer.android.com/identity/data/autobackup).

## Sedang — peninjauan privasi publik masih terbuka

**SEC-03 — Disclosure seluruh layanan/retensi belum diaudit menyeluruh.**
Alur izin AI sudah menjelaskan pemrosesan online dan pilihan data tambahan,
tetapi itu belum menyelesaikan kebijakan privasi publik, deklarasi Data Safety,
ketentuan usia, serta verifikasi seluruh metadata/log platform dan layanan lain.
Featherless [menyatakan tidak mencatat prompt/completion API](https://featherless.ai/docs/privacy-and-logging);
ini pernyataan penyedia, bukan hasil audit independen Zikra atau jaminan semua
lapisan infrastruktur tidak mencatat metadata. Gambar generatif memakai layanan
berbeda; lokasi/cuaca/peta juga punya jalur data sendiri. G03 tetap terbuka.

## Informasional — batas penyimpanan dan kepercayaan

**SEC-04 — Sandbox bukan enkripsi semua data di tingkat aplikasi.**
[Chat SQLite:29](D:/Download/Zikra-App-main/Zikra-App-main/android/app/src/main/java/com/zikra/app/assistant/ConversationStore.java:29)
dan [teks/gambar lampiran:153](D:/Download/Zikra-App-main/Zikra-App-main/android/app/src/main/java/com/zikra/app/assistant/AssistantAttachments.java:153)
disimpan di area privat aplikasi, tanpa lapisan enkripsi khusus kontennya.
Jangan menyamakan ini dengan akses publik, tetapi perangkat root, proses debug
yang diizinkan atau perangkat yang telah dikuasai berada di luar perlindungan
sandbox biasa. Token gateway berbeda: [AES-GCM/AndroidKeyStore:78](D:/Download/Zikra-App-main/Zikra-App-main/android/app/src/main/java/com/zikra/app/assistant/AssistantConnection.java:78).
Kebutuhan kunci aplikasi/enkripsi DB tambahan perlu ditentukan menurut ancaman
yang ingin dilindungi, bukan diklaim sudah tersedia.

## Kontrol yang ditemukan dan diuji terarah

- Provider key tetap backend; pemindaian seluruh entri APK tidak menemukan nilai
  credential privat yang dikenal. Kredensial aktif tidak diubah; ini bukan audit
  riwayat paparan key atau sertifikasi semua dependency.
- HTTPS wajib; redirect kredensial dimatikan; provider lampiran tidak exported.
  Kamera menggunakan grant URI sementara, pemilih Android tidak meminta akses
  penyimpanan luas. Izin dasar sekali tidak menyalakan pilihan profil/tugas/GPS.
- Gateway mengautentikasi instalasi dan menolak input/badan berlebih sebelum
  mengirim ke model. Request tidak disimpan oleh kode gateway yang ditinjau;
  registry/kuota berbeda dari isi percakapan. Respons `no-store` bukan jaminan
  metadata infrastruktur nihil.
- MIME/konten/arsip/dimensi divalidasi; DOCX menolak DOCTYPE/entity; foto
  dinormalisasi sehingga metadata EXIF asli tidak dikirim. Isi visual atau teks
  yang memang terlihat tetap dapat mengandung data pribadi; tidak ada redaksi
  otomatis. Lampiran diperlakukan sebagai data tidak tepercaya, tidak memberi
  izin menjalankan perintah atau menulis tugas tanpa review tindakan.

## Batas lampiran, bukan kapasitas RAG permanen

| Batas | Implementasi saat diperiksa |
| --- | --- |
| Jumlah/input |1 file per pesan, maksimum8.000.000byte |
| Foto |JPG/PNG/WebP; normalisasi JPEG, batas awal40MP |
| PDF |1–3halaman; seluruh dokumen >3halaman ditolak, tidak dipotong diam-diam |
| DOCX/TXT |Maksimum12.000 unit karakter Java;TXT UTF-8, maksimum64.000byte teks mentah |
| DOCX terurai |200entri,2MB per entri/4MB total, kedalaman64 |
| Payload gateway |2MiB;JPEG/PNG per gambar360KB, total gambar1,08MB, maksimum3 |
| Penyimpanan lampiran lokal |25MB keseluruhan; sampai dihapus dari draf/chat |
| RAG dokumen permanen |Belum ada indeks embedding/vector atau koleksi dokumen permanen |

[Batas Android:21](D:/Download/Zikra-App-main/Zikra-App-main/android/app/src/main/java/com/zikra/app/assistant/AssistantAttachments.java:21),
[batas server:25](D:/Download/Zikra-App-main/Zikra-App-main/backend/attachments.mjs:25).
Ukuran input8MB berbeda dari payload jaringan: foto/PDF diubah menjadi gambar,
DOCX/TXT menjadi teks. Tidak ada janji semua file dalam batas itu selalu terbaca
model secara akurat. Hapus chat membersihkan lampiran lokalnya; tidak berarti
menarik kembali data yang sudah pernah diproses pihak lain.
