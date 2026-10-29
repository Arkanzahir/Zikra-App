# C03/C04 — APK ke Vercel/Featherless nyata pada emulator

> **Tindak lanjut30 Oktober2026:** temuan ringkasan1:2 ditelaah pada [C04 beta1.9.28](../assistant-context-c04-2026-10-30/README.md); prompt membedakan konteks ayat sebelumnya dan dua sampel hosted baru diperiksa. Catatan di bawah adalah hasil25 Oktober, bukan status C04 terkini.

25 Oktober 2026 (WIB). HP tidak terdeteksi saat mulai, sehingga pengguna meminta
percobaan dilanjutkan di emulator. **Bukan bukti C05 dari HP fisik.** Backend tetap
`https://zikra-ai.vercel.app`, source `9438aa4`; tidak mengubah key/provider/hosting.
Kandidat baru **1.8.1 beta / code 26**, SQLite tetap v4, source **`196a8d2`** (fix TLS `0cf582d`, source set tes live opt-in).
[Receipt APK/source/uji](release.json).

## Temuan dan perbaikan

Percobaan nyata pertama memakai APK 1.8.0 berhasil menampilkan jawaban Featherless,
tetapi proses crash ketika menutup `AiActivity`. Stack menunjukkan
`NetworkOnMainThreadException` pada `ConnectionPool.evictAll()` dari
`OnlineAssistantClient.close()` di `onDestroy`: menutup TLS dapat menulis
`close_notify`. Respons simulasi sebelumnya tidak membuka socket TLS nyata.

Penutupan koneksi idle dipindahkan ke worker pembersihan singkat; pembatalan dan
penolakan callback lama tetap langsung berlaku. `close()` idempotent dan client
yang telah ditutup tidak menerima request baru. Tidak mematikan StrictMode,
TLS, autentikasi atau validasi respons untuk mengatasi crash. Source/UI/aset
lain tetap; versi dinaikkan agar APK perbaikan berbeda dari kandidat lama.

## Bukti terarah

| Pemeriksaan | Hasil / batas |
| --- | --- |
| Build APK dan APK tes | Berhasil, satu worker, heap 768 MiB, JDK 17; APK tes terpisah |
| Unit AI | 14 lulus, 0 gagal/skip, tiga suite; [ringkasan](checks.json) |
| Lint | 0 error / 716 warning; tidak diklaim bebas warning |
| Regresi native terkendali | [6 metode lulus](controlled-native.txt); jaringan respons buatan, dibedakan dari probe live |
| APK → HTTPS → Vercel → Featherless | [3 metode live lulus](hosted-native.txt), tanpa interceptor/pengganti jaringan |
| Consent dan chat nyata | Pilihan konteks default tidak dicentang; batal persetujuan tidak mengirim. Retry menghasilkan balasan `online` nyata; [receipt](native/conversation.json), [screenshot](native/reply-live.png) |
| Token salah | Token sintetis salah mendapat pesan akses tidak berlaku, tidak membuat jawaban lokal/sukses palsu; token benar + retry berhasil; [screenshot](native/unauthorized-live.png) |
| Pembatalan | Request nyata dibatalkan dari UI, tidak ada balasan terlambat pada jendela observasi 600 ms. Ini bukan bukti provider menghentikan seluruh komputasinya |
| Jaringan putus | Wi-Fi/data emulator dimatikan, pesan gagal jaringan muncul tanpa fallback; jaringan dipulihkan dan retry berhasil; [receipt](native/network.json), [screenshot](native/network-retry-live.png) |
| Usulan tugas nyata | Model menghasilkan proposal, belum tersimpan sampai ditinjau; judul uji dikoreksi pengguna tes, klik Simpan dua kali membuat satu task tanpa completion/reminder otomatis; [receipt](native/actions-source.json) |
| Tadabbur | Request ayat 1:2 menerima sumber EQuran URL/chapter/verse/teks yang cocok dan ringkasan terpisah; [screenshot](native/tadabbur-live.png). Ini verifikasi pengambilan/penyajian sumber, bukan pengesahan akurasi semua klaim ringkasan |
| Penutupan layar setelah TLS nyata | Beberapa activity ditutup selama tiga metode live; seluruh tes selesai dan buffer crash setelah fix kosong |
| Preservasi perangkat | Arsip app privat dibuat sebelum tes; delapan file asli dipulihkan dengan hash sama, token input sementara dihapus; [hasil](device-preservation.json). Tidak menghapus data HP atau reset emulator |
| APK/rahasia/WIP | [Audit](secret-preservation-apk.json): nol nilai secret yang dikenal pada source/399 entry APK; bukan audit seluruh secret historis. Tidak ada env/input privat/kelas tes pada APK aplikasi |

Kelas `HostedAssistantTest` berada di source set opt-in `src/hostedTest`:
build APK tes memakai `-PzikraHostedTests=true`, lalu kelas dipilih eksplisit.
Suite native standar tidak menyertakan kelas live dan tidak membutuhkan secret
atau menghubungi provider melalui kelas ini. Java kelas live sama dengan yang
menjalankan tiga metode di atas; pemisahan source set diperiksa lewat dua build
APK tes, bukan dengan mengulang request provider yang telah lulus.

## Temuan isi yang masih terbuka pada C04

Screenshot ringkasan ayat 1:2 memuat frasa “Ayat ini memulai dengan Basmalah”.
Kecocokan sumber/nomor tidak otomatis membuktikan semua kalimat ringkasan tepat;
frasa tersebut perlu ditinjau editorial/semantik terhadap ayat dan tafsir.
Jangan mencentang validasi isi Tadabbur berdasarkan tes transport/metadata ini.
Screenshot dialog proposal juga diambil saat transisi tampil, sehingga tidak
dipakai sebagai bukti penilaian visual final atau perubahan desain disetujui.

## Preservasi dan langkah berikutnya

Data jaringan/riwayat/nama yang dipakai sintetis, tanpa unggahan profil/tugas asli.
Gateway token dimasukkan melalui file privat sementara sesudah instalasi, tidak
dibundel ke APK/argumen tes/chat/Git; hanya token gateway, bukan provider key.
Task uji/receipt dibersihkan dan arsip data emulator dipulihkan saat proses berhenti.
Wi-Fi/data dipulihkan ke keadaan awal. Emulator sudah dimatikan setelah selesai;
image API 37 memakai sekitar 4,8 GB working set meskipun request RAM 1.536 MiB,
jadi tidak dijalankan bersamaan dengan Gradle build.

APK 1.8.0 lama tetap utuh. APK 1.8.1 berukuran **31.181.560 byte** (<100.000.000),
SHA-256 `8650b9cc12d2d7e061dd0730e4f6e03f3e1767924a5b60271a6c1cd0a900e87b`.
Kedua APK memiliki 399 nama entry yang sama; isi hanya berbeda pada manifest
versi dan `classes7.dex`. Aset/layout tidak berubah isinya. WIP handoff, env
privat, stash/arsip audio, desain dan data pengguna dipertahankan.

C02/C03/C05 tetap terbuka untuk HP, laptop mati dan pemeriksaan operasional/
kondisi gagal yang belum lengkap; C04 juga menunggu konteks kelas D04 dan tinjauan
isi di atas. Dikte mikrofon nyata C06, B01 dan rilis publik belum selesai. D02a
tetap tugas adzan/notifikasi yang belum diimplementasikan. Source disimpan lokal;
jangan menganggap checkpoint sudah dipush atau APK sudah menjadi signed release.
