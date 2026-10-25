# C — AI native, konteks, tindakan dan paket deploy

**Lanjutan terbaru:** [backend Vercel sudah online](../backend-vercel-2026-10-25/README.md),
Hobby + Upstash Free; health/provider/kuota/revokasi/redeploy nyata lulus.
Catatan awal tanpa hosting di bawah bersifat historis. APK tetap 1.8.0 dari
`7982e10`; source backend yang dihosting `9438aa4`, bukan APK baru.

25 Oktober 2026 (WIB). Baseline `20c21fc`; kandidat **1.8.0 beta / versionCode 25**,
SQLite v4, unduhan awal **31.362.014 byte**. [Receipt source/APK/uji](release.json).
Source kandidat: **`7982e10`**; checkpoint lokal, belum dipush pada akhir sesi ini.
**Bagian C belum dinyatakan seluruhnya selesai.** Pengguna memilih siapkan paket
deploy dahulu karena belum ada hosting. [Keputusan terbaru pengguna](key-decision-2026-10-25.md)
membatalkan syarat rotasi: gunakan key yang sudah ada. C01 selesai persiapan
konfigurasi; key tidak diganti, receipt terdahulu tetap mencatat fakta saat diuji.

## Status per ID

| ID | Bagian yang sudah dibuktikan | Status / sisa pemeriksaan |
| --- | --- | --- |
| C01 | Validasi env, health/restart dan completion privat; mode private tetap kompatibel, mode installation tersedia | Selesai persiapan konfigurasi 25 Oktober; syarat rotasi dibatalkan pengguna, [bukti penutupan](key-decision-2026-10-25.md). Hosting nyata tetap C02 |
| C02 | Vercel Hobby + Upstash Free sudah online; HTTPS/provider 200, akses/revokasi 401, kuota 429, counter/revokasi bertahan redeploy | HP/laptop mati, pemantauan/backup/retensi dan failure/cancellation platform belum lengkap; [bukti terbaru](../backend-vercel-2026-10-25/README.md) |
| C03 | Native online/offline, preview izin per request, loading, batal, retry, label online dan sanitasi error terarah lulus | Alur native memakai respons jaringan terkendali; belum APK → HTTPS hosting permanen → provider |
| C04 | Nama/tugas/tenggat/perhitungan salat hanya saat dipilih; proposal ditinjau dan disimpan sekali; edit stale/selesai ditolak; Tadabbur mengambil sumber nyata | Jadwal kelas mingguan belum tersedia pada D04 dan tidak dikarang/dikirim sebagai data palsu; integrasi end-to-end setelah C02/C03 dan pemeriksaan cakupan konteks masih perlu |
| C05 | Probe gateway lokal dan HTTPS Vercel → provider nyata tersimpan terpisah | Belum diuji rantai APK/HP, putus koneksi dan laptop mati; probe komputer tidak menggantikan HP |
| C06 | Persetujuan layanan suara, hasil jadi draf, batal/kosong/tipe hasil salah serta batas 500 UTF-16 diperiksa; Vision tetap ditunda | Recognizer terdeteksi pada emulator, tetapi mikrofon/dikte layanan nyata belum diuji; hasil balik dalam tes disimulasikan, jangan disebut rekaman suara nyata |

## Perubahan aplikasi

- Setiap kiriman online menampilkan **Tinjau sebelum mengirim**. Default hanya
  pesan saat ini; nama, maksimal 10 tugas/tenggat, perhitungan salat/zona, riwayat
  chat online dan izin proposal semuanya tidak dipilih otomatis. Preview berasal
  dari snapshot yang benar-benar dikirim, dibatasi byte UTF-8 48 KiB. Riwayat
  offline dan metadata proposal/sumber tidak ikut diunggah sebagai konteks chat.
- Tombol kirim menjadi batal ketika menunggu; request dibatalkan saat berpindah
  layar. Akses salah/dicabut, kuota, timeout, jaringan, server, respons tidak valid
  dan sumber tafsir tidak tersedia punya pesan berbeda. Gagal online tidak
  menambahkan jawaban offline atau balasan sukses palsu.
- Model hanya mengusulkan **tambah/edit judul dan tenggat tugas Study**. Proposal
  membawa ID request lokal, tidak dapat menyelesaikan/menghapus tugas. Pengguna
  dapat mengubah judul/tenggat, membatalkan atau menekan Simpan ke Study. SQLite
  menyimpan receipt dan perubahan dalam satu transaksi: klik ulang/retry/restart
  tidak menggandakan tugas. Edit mensyaratkan judul/tenggat asli masih cocok dan
  tugas belum selesai. Reminder baru tidak otomatis dinyalakan.
- Menu ayat Quran mempunyai **Tanya makna ayat**; membuka draf tanpa langsung
  mengirim. Online mengambil petikan tafsir melalui endpoint EQuran yang tetap,
  lalu menyertakan sumber dan ringkasan secara terpisah. Klaim sumber Kemenag
  berasal dari [dokumentasi EQuran](https://equran.id/apidev/v2), bukan sertifikasi
  independen seluruh teks. Jika sumber gagal/rusak/tidak cocok, provider tidak
  dipanggil untuk menebak tafsir. Petikan maksimum 6.000 karakter diberi penanda
  bila dipotong; link sumber hanya dibuka melalui tindakan pengguna.
- Dikte memakai layanan Android eksternal setelah penjelasan privasinya. Teks
  hasil tidak otomatis terkirim; batal atau hasil salah tidak menimpa draf.
  Tidak menambah kemampuan Vision atau klaim mikrofon sudah diuji nyata.

SQLite **v3 → v4** hanya menambah tabel `assistant_actions`; task/bookmark/Hifz/
preferensi tidak direset. Jangan memaksa downgrade ke APK lama setelah database
ditingkatkan. Desain utama/ilustrasi disetujui dipertahankan.

## Backend dan deploy

[Runbook deploy](../../../backend/deploy/README.md) memuat template env kosong,
Docker non-root tanpa secret build, proxy HTTPS, volume registry/kuota persisten,
health check dan restart. Mode installation tidak menerima shared token sebagai
fallback. Operator menerbitkan token ke berkas privat, registry hanya menyimpan
hash/ID acak/status revoked; admin tidak mencetak token. Pencabutan dibaca ulang
tanpa restart. Default batas 10 percobaan/menit dan 100/hari UTC per instalasi,
satu request aktif/instalasi dan dua global. Korupsi storage gagal tertutup.

File store ini **satu proses/satu replika**, belum database terdistribusi. Tidak
menyimpan percakapan atau profil; tetap perlu retensi metadata/backup dan audit
log platform saat hosting dipilih. Compose/Caddy/systemd yang belum dijalankan
tidak menjadi bukti C02 sudah selesai. Key provider tetap hanya di env server;
key yang sudah ada tetap dipakai sesuai keputusan pengguna. Tidak ada resource cloud/biaya dibuat.

## Bukti dan batas uji

| Pemeriksaan | Hasil |
| --- | --- |
| Node terarah | [26 tes lulus](backend-tests.tap), tanpa key nyata/internet untuk suite; termasuk token/revokasi, kuota restart, input/konteks, peran, body, status provider, timeout/batal dan sumber gagal |
| Provider nyata privat | [Receipt live](live-local.json): usulan add_task dengan judul/tenggat sintetis yang sesuai 200; Tadabbur dengan petikan sumber nyata 200. Bukan APK/hosting/HP |
| Iterasi probe pertama | HTTP 200 tetapi proposal belum terbentuk; kontrak JSON/prompt diperbaiki. Receipt akhir hanya menyatakan hasil setelah perbaikan; bukan bukti model selalu patuh atau bisa eksekusi |
| Android unit | 118 metode: **114 lulus, 4 skip**, 0 gagal/error; seluruh 14 tes assistant lulus. Skip berada pada Doa/Mushaf, bukan disamakan lulus |
| Android build/lint | Build debug + test APK lulus; lint **0 error / 716 warning**. [Ringkasan](checks.json); warning lama/baru belum disertifikasi bersih seluruh proyek |
| Native AI | [6 metode lulus](native-tests.txt): callback/status terkendali, cancel terlambat, default izin, hasil dikte draf, UI loading/retry/label, review/simpan/double-tap serta SQLite upgrade/once/stale/completed |
| Migrasi yang terdampak | [11 metode lulus](migration-tests.txt): DB/Hifz dan migrasi akademik lama; memakai database fixture khusus, tidak menghapus data pengguna |
| Teks besar | [2 metode pada font 200% lulus](font200-tests.txt), consent/review dapat digulir dan tombol terjangkau; bukan penutupan seluruh F03 |
| Secret/installer/preservasi | [Audit nilai key/token yang diketahui dan fixture](secret-and-preservation.json); pemindaian source saat ini + setiap entri APK yang didekompresi. Bukan audit semua secret/riwayat Git |

Native memakai emulator API 37.1; request RAM 1.536 MiB tetapi image melaporkan
sekitar 4.008.372 KiB MemTotal. Build satu worker/heap 1.024 MiB. Emulator
ditutup setelah pemeriksaan, bukan dibiarkan berjalan. Tidak mengubah preferensi
atau memasang APK pada HP fisik. Snapshot preferensi emulator dikembalikan dan
task/receipt fixture dibersihkan oleh tes. Screenshot tidak berisi data pengguna.

Screenshot asli: [izin default](native/consent.png), [loading](native/loading.png),
[balasan terkendali](native/online-reply.png), [review tugas](native/task-review.png),
[draf suara](native/voice-draft.png), [izin font 200%](font200/consent.png),
[review font 200%](font200/task-review.png). Screenshot balasan online memakai
interceptor tes; jangan menyebutnya bukti sambungan provider melalui hosting.

## Pengingat adzan — tambahan tugas pengguna

**D02a tercatat, belum diimplementasikan pada checkpoint C ini:** saat masuk
waktu salat pengguna memilih **adzan penuh / notifikasi biasa tanpa adzan /
getar saja / mati**, dengan sakelar utama dan preferensi per salat. Pilihan harus
bertahan saat tutup/reboot/pembaruan, tidak memutar adzan pada mode biasa, punya
tombol berhenti dan menghormati izin/audio focus/senyap. Ini pengingat lokal,
tidak bergantung backend AI. D02 tetap mencakup sumber audio/penjadwalan dan HP.

## Langkah berikutnya

1. Pilih akun layanan/VPS/domain ketika pengguna siap; jalankan paket, uji TLS,
   volume/restart/kuota/pencabutan dan laptop mati.
2. Hubungkan APK ke endpoint permanen dan token instalasinya, uji rantai nyata
   serta gagal/batal/kuota dari HP. Jangan isi provider key di APK.
3. Uji dikte/mikrofon layanan nyata, lanjutkan konteks jadwal kelas setelah D04,
   lalu nilai checkbox C03/C04/C06 dari bukti tersebut. B01 tetap menunggu sumber.

Rujukan teknis: [Featherless tool-calling/workaround JSON](https://featherless.ai/docs/tool-calling),
[EQuran API/sumber](https://equran.id/apidev/v2),
[RecognizerIntent Android](https://developer.android.com/reference/android/speech/RecognizerIntent),
[alarm Android](https://developer.android.com/develop/background-work/services/alarms),
[notification channels](https://developer.android.com/develop/ui/views/notifications/channels).
