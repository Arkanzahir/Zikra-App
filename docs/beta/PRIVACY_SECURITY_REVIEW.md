# Pemeriksaan privasi/keamanan terarah —14November2026

Scope: persetujuan konten AI, antrean transport dan batas privasi beta13+.
Bukan penetration test, audit seluruh aplikasi/library, atau pengesahan kepatuhan.
NativeAndroid Java dan gateway Node HTTP biasa; tidak ada referensi framework
khusus yang cocok pada panduan keamanan lokal. Peninjauan memakai alur kode,
prinsip pembatasan data, dokumentasi primer dan tes terarah.

## Ringkasan

Satu kelemahan pengikatan review/izin diperbaiki di1.9.51.230JVM/14native
emulator lulus. Tiga area rilis tetap terbuka; tidak menaikkan status HOLD.
Perbaikan dijalankan dalam instruksi pengguna melanjutkan privasi/keamanan,
tanpa memakai HP, mengubah produksi atau membuat aturan usia palsu.

## PR01 — P2: review/izin belum terikat sampai transport (diperbaiki)

**Dampak potensial:** payload yang ditinjau sebelumnya dapat melewati perubahan
izin/layanan sebelum pengiriman antre berjalan. Tidak ditemukan bukti kejadian
terhadap pengguna; kondisi semula dari pemeriksaan source, lalu guard diuji.

Izin kini memakai revisi dan receipt immutable yang terikat endpoint/credential.
Snapshot review membawa receipt yang sama; client menolak sebelum enqueue dan
interceptor memeriksa sebelum transport. Revokasi→accept ulang tidak menghidupkan
receipt lama. Konfirmasi gambar memakai binding perprompt terpisah dari basicchat.
Latechatreply tidak diteruskan setelah revoke. Validlegacygrant tidak dipaksa ulang.

Lokasi: `AiActivity.java:782` (review), `AiActivity.java:826` (send boundary),
`assistant/OnlineAssistantClient.java:79` dan`:88` (client/antrean),
`assistant/ImageGenerationClient.java:27` dan`:31` (gambar),
`assistant/AssistantConsent.java:19` (revisi/receipt).
Path Java seluruhnya di `android/app/src/main/java/com/zikra/app/`.
[Bukti9permission+5regresi](../../android/validation/privacy-consent-2026-11-14/README.md).
Tidak ada janji recall untuk request yang telah melewati guard/diterima penyedia.

## PR02 — P1 gerbang rilis: mekanisme remaja belum tersedia (terbuka)

Target13+ bukan pemeriksaan usia atau bukti wali. Role/tingkat pendidikan dan
basicconsent tidak boleh dipakai sebagai penggantinya. Belum ada otorisasi
wali/usia pada registry backend. Persiapan publik tetap HOLD sampai proses,
ketentuan model dan skenario penolakan/pencabutan ditinjau.
[Tinjauan dan keputusan implementasi](TEEN_PRIVACY_REVIEW.md).
Ini gap kesiapan, bukan klaim telah terjadi pelanggaran atau eksploitasi.

## PR03 — P1 gerbang remaja: lokasi presisi (terbuka)

`android/app/src/main/java/com/zikra/app/location/LocationStore.java:45`–`:51`
menyimpan koordinat/akurasi pilihan, dan fitur terkait memakai lokasi tersebut.
Jalur manual tersedia, tetapi belum ada pemisahan perilaku untuk remaja.
Izin lokasi Android tidak menutup peninjauan kebijakan remaja. Tidak membulatkan
atau menimpa posisi/jadwal pengguna secara diam-diam pada sesi ini.
Perlu review penerapan aturan dan rancangan kontrol sebelum mengundang remaja.

## PR04 — P2 kesiapan: retensi/penghapusan lintas layanan (terbuka)

Revokasi izin lokal, penghapusan chat HP, erasure registry dan penghapusan
log/backup penyedia berbeda. Alat erasure satu instalasi telah diuji sebelumnya,
bukan penghapusan produksi. Loggingrejection gateway pada `backend/server.mjs:99`
berisi kategori/status/error/queue, bukan payload; jangan menyamakannya dengan
nol log pada Vercel/Cloudflare/Redis/FCM. Dashboard/fitur/backup belum diverifikasi
menyeluruh. Retensi temp kamera/PDF setelah actualprocesskill juga belum diaudit
lengkap. [Retensi](RETENTION_REVIEW.md), [prosedur](INSTALLATION_ERASURE.md).

## Urutan berikutnya

1. Putuskan dan tinjau mekanisme usia/wali, lingkup layanan serta lokasi remaja;
   baru implementasi UI/backend dan uji kasus negatif. Tidak cukup checkbox.
2. Verifikasi retensi akun dan prosedur permintaan data end-to-end tanpa
   mempublikasikan/mereset credential atau menghapus pengguna percobaan nyata.
3. Uji satu sesi HP saat siap untuk picker/restore dan sisa kasus penting.
4. Tutup hak/B01/audio/signer/J14 sesuai checklist; tidak mengulang seluruh tes
   audio tanpa perubahan atau alasan baru.
