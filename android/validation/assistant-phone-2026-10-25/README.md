# C05 — percakapan AI nyata dari HP fisik

**Selesai 25 Oktober 2026 (WIB), dalam lingkup C05.** Pengguna mengizinkan agen
menguji HP yang terhubung dan menyelesaikan persetujuan pemasangan paket tes di
layar HP. Perangkat **POCO F7, Android 16/API 36**, menjalankan APK **1.8.1 / code
26**, SQLite v4. Pengujian memakai APK aplikasi yang sama dengan kandidat
`196a8d2`, bukan aplikasi dengan respons jaringan pengganti. Backend Vercel tetap
`9438aa4`; [receipt](receipt.json) membedakan source APK dan source harness tes.

## Hasil nyata

[Satu metode instrumentasi lulus dalam 26,384 detik](native-result.txt), berisi
alur berikut pada HP fisik:

| Langkah | Bukti |
| --- | --- |
| Consent | Lima pilihan konteks/riwayat/proposal default tidak dicentang. Payload diperiksa hanya berisi pesan sintetis saat ini dan context kosong; [tampilan dialog](native/consent.png) |
| Batal consent → retry | Tidak ada jawaban/sukses palsu setelah pembatalan; retry dengan izin kirim menghasilkan balasan nyata |
| APK → HTTPS Vercel → Featherless → layar HP | Balasan `online` nonkosong tampil, tanpa interceptor/mock jaringan; [tampilan balasan](native/reply.png) |
| Token salah → koreksi/retry | Token sintetis salah menghasilkan pesan akses gagal; tidak menyisipkan jawaban offline. Token benar dan retry berhasil; [tampilan](native/wrong-token.png) |
| Kuota server | Token fixture terpisah dengan counter harian diposisikan pada batas di Redis nyata; server mengembalikan 429 dan HP menampilkan batas pemakaian, tanpa jawaban palsu; [tampilan](native/quota.png) |
| Pembatalan request | Tombol kirim membatalkan request aktif; tidak ada balasan terlambat pada jendela observasi satu detik; [tampilan](native/cancel.png). Ini tidak membuktikan provider menghentikan semua komputasinya |
| Jaringan putus | Wi-Fi HP dimatikan sementara; data seluler memang sudah mati dan tidak dinyalakan. App menampilkan gagal jaringan tanpa fallback; [tampilan](native/network-off.png) |
| Jaringan pulih → retry | Wi-Fi dipulihkan; retry menghasilkan balasan AI online nyata; [tampilan](native/network-restored.png) |

[Receipt native](native/phone-live.json) hanya memuat boolean/status; bukan teks
profil, tugas atau riwayat asli. Kasus kuota menggunakan fixture sintetis yang
diposisikan di batas, **bukan menghabiskan 100 request model atau kuota pribadi**.
Registry/counter fixture diberi TTL pendek dan tidak mengubah token produksi.

## Preservasi dan kebersihan

- Sebelum tes, app dihentikan lalu `shared_prefs` dan database dicadangkan secara
  privat di laptop. Nama/profil/tugas/lokasi tidak diganti dan tidak dipilih
  sebagai konteks yang dikirim. Riwayat AI diisolasi sementara untuk pesan uji,
  kemudian dipulihkan, bukan dihapus permanen.
- [Tujuh file asli cocok hash setelah pemulihan](preservation.json). Gateway AI
  awalnya belum dikonfigurasi; endpoint/token gateway yang telah tersedia kini
  disimpan melalui `AssistantConnection`/Android Keystore agar online dapat
  digunakan. Ini perubahan konfigurasi yang disengaja; nilai token tidak ada
  pada laporan/chat/Git/APK dan provider key tetap hanya di server.
- Flag Wi-Fi/data dikembalikan persis ke keadaan awal. File input token sementara
  di app dihapus, paket `com.zikra.app.test` dicopot setelah selesai; aplikasi
  utama tidak dihapus. Arsip/XML/identifier unik perangkat tetap di `.local`.
- Gambar berasal dari view aplikasi/dialog yang memuat data sintetis, tanpa
  menangkap notification shade atau aplikasi lain. [Audit secret/preservasi](audit.json)
  menjelaskan batas pemindaian terhadap nilai secret yang diketahui.
- Kelas `PhoneAssistantTest` hanya tersedia dengan `-PzikraPhoneTests=true`,
  membutuhkan file izin/koneksi privat yang dipasang sesudah instalasi, memeriksa
  HP fisik/layar terbuka/versi APK, dan dipilih eksplisit. [Pemeriksaan dua APK tes](test-opt-in.json)
  membuktikan kelas tersebut tidak terdapat pada suite standar. Java kelas
  emulator tidak diubah untuk melewati guard perangkatnya.

Pemasangan helper pertama dibatalkan sistem (`INSTALL_FAILED_USER_RESTRICTED`);
pengguna meminta diulang dan mengizinkannya. Tidak menonaktifkan proteksi HP.
Pengambilan bukti memakai `adb pull`; pembacaan external storage melalui
`run-as` ditolak pada perangkat ini. Upaya membuka activity AI internal dari shell
juga ditolak karena tidak exported; aplikasi dibuka kembali melalui launcher
resminya, tanpa memperlemah manifest/izin. Kendala alat tersebut tidak mengubah
hasil satu metode native yang sudah lulus.

## Batas penutupan

**C05 ditutup untuk percakapan dari HP, token salah, putus koneksi dan kuota**
sesuai kriterianya. Laptop masih hidup selama pengujian; verifikasi saat laptop
mati tetap **C02**, tidak diklaim oleh laporan ini. Mode/konfigurasi manual dan
timeout UI C03 belum ditutup penuh. Kualitas ringkasan Tadabbur/konteks kelas C04,
mikrofon/dikte nyata C06, B01, adzan D02a dan rilis publik tetap belum selesai.
Tidak menjalankan pemeriksaan kontak, pesan pribadi, mikrofon atau aplikasi lain.

Tidak ada perubahan runtime/desain atau APK aplikasi baru. APK tetap
31.181.560 byte (<100.000.000), SHA-256
`8650b9cc12d2d7e061dd0730e4f6e03f3e1767924a5b60271a6c1cd0a900e87b`.
Build opt-in dan build standar setelahnya berhasil; APK aplikasi sama persis.
Tidak menjalankan emulator atau mengulang suite luas yang tidak terkait.
