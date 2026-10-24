# Handoff aktif Zikra — 25 Oktober 2026 (WIB)

## Mulai di sini

- Root Git: `D:\Download\Zikra-App-main\Zikra-App-main`, bukan folder pembungkusnya. Remote: `https://github.com/Arkanzahir/Zikra-App.git`, branch `main`.
- Baca `AGENTS.md`, dokumen ini, `IMPLEMENTATION_PLAN.md`, lalu `RELEASE_SCOPE.md`. Rencana/checklist dan bukti terbaru mengalahkan catatan historis.
- Pengguna ingin pindah chat tanpa kehilangan arah. Gunakan skill **caveman**, bahasa Indonesia singkat. Kerjakan satu ID, catat bukti dan centang hanya bila kriterianya terpenuhi. Jangan mengulang seluruh desain/implementasi dari nol.
- Stack: Android native **Java + XML**, SQLite lokal; gateway AI **Node.js**. Tidak migrasi Flutter/React Native/WebView.
- `PROJECT_HANDOFF.md` memuat riwayat dan WIP lama yang belum di-commit. Jangan menimpa, membatalkan atau memasukkannya ke commit pekerjaan ini.

## Checkpoint yang dapat dipulihkan

- Parent pekerjaan C lanjutan: `20c21fc` (checkpoint lokal C01), handoff yang sudah dipush sebelumnya `83312da`. Source checkpoint kandidat lihat [receipt C](android/validation/assistant-c-2026-10-25/release.json); gunakan Git log aktual, jangan menganggap checkpoint lokal sudah dipush.
- Source kandidat 1.8.0: **`7982e10e0df98f9b2ee9ca01fa9d89c058363280`**, disimpan lokal; receipt dokumentasi adalah commit penerus, belum push.
- Aplikasi **1.8.0 beta / versionCode 25**, SQLite v4. APK debug, belum signed release publik.
- APK lokal: `D:\Download\Zikra-App-main\Zikra-AI-v1.8.0-beta-debug.apk`.
- Ukuran **31.362.014 byte**; SHA-256 `77ed491b65e2519758412bdbe44bc07d494a282ca8a3dda0d7ac5d55bd15a5c1`.
- [Bukti C/tes/screenshot/batas](android/validation/assistant-c-2026-10-25/README.md). Baseline konten **1.7.6 beta / code 24** (`b3f13f3`) dan APK lamanya dipertahankan; [receipt B04](android/validation/dua-completion-b04-2026-10-24/release.json).

## Status rencana: B hanya menyisakan B01

| ID | Status dan batas |
| --- | --- |
| A01–A04 | Selesai sesuai bukti pada rencana. Optimasi audio yang ditunda tetap WIP; bukan sudah digabung. |
| B01 | **Terbuka**, menunggu klarifikasi asal edisi/terjemahan, ketentuan konten dan kelengkapan/penomoran Hadis. |
| B02 | Selesai teknis: unduh koleksi kandidat beta, paging, cari nomor/kata, luring, batal/gagal/korupsi. Bergantung sumber B01 untuk distribusi publik. |
| B03 | Selesai teknis: reader, ukuran teks, salin/bagikan, ID sumber stabil, bookmark/resume lintas pembaruan. Bergantung sumber B01 untuk distribusi publik. |
| B04 | Selesai **kurasi editorial dan integrasi** Doa. Bukan sertifikasi ulama independen atas seluruh sanad/harakat. |
| C–G | Masih terbuka sesuai checklist; jangan menyebut seluruh aplikasi siap publik atau menghitung persentase dari jumlah centang. |

Hadis: 7 kutipan bawaan; paket kandidat opsional Bukhari **6.638 entri** (rentang nomor 1–7.008), Muslim **4.930 entri** (1–5.362). Nomor yang tidak muncul perlu penjelasan edisi/pemisahan; jangan menyimpulkan otomatis teks hilang. Pertahankan nomor sumber, jangan mengisi celah dengan teks buatan. Range API berbasis posisi array berbeda dari lookup nomor Hadis.

Doa: **14 pilihan bawaan + 225 bacaan tambahan setelah unduh** dari 227 kandidat EQuran; ini bukan jumlah bacaan unik karena dapat tumpang tindih. ID 42/144 dikecualikan karena field sumber tidak memadai. Ada koreksi/catatan untuk **33 entri**; raw dan ID tetap. Manifest fingerprint 225 entri menandai sumber baru/berubah **Belum ditinjau**, menonaktifkan koreksi lama. Pencarian mengenali teks asli dan hasil koreksi.

Validasi B04 terakhir: **10 unit + 2 metode native lulus**, termasuk 21 entri terdampak, paging/pencarian, unduh nyata/luring, perubahan sumber dan penolakan pembaruan cache rusak. Lint 0 error, 709 warning. Emulator ditutup; **HP fisik dan keluaran suara TTS nyata belum diuji**. Jangan memperluas hasil terarah menjadi klaim semua fitur teruji.

File utama B04: `DuaReview.java`, `DuaSourceSnapshots.java`, `DuaCatalog.java` dalam `android/app/src/main/java/com/zikra/app/discovery/`; `DuaLibraryActivity.java`; resource `dua_review.xml`/`dua_library.xml`; `OptionalContentTest.java` dan unit Doa.

## B01: email Gading dan alasan menunggu

- Kandidat dataset: repositori Gading pada commit `8e15b4f9e7585822426a0d470e8dfec27e2a1707`; dokumentasi mengarah ke TafsirQ. Identitas edisi/penerjemah dan ketentuan konten upstream belum cukup jelas pada bukti yang diperiksa.
- **Bukan aturan bahwa semua API harus minta izin lewat email.** Pertanyaan diperlukan di sini karena dokumentasi belum menjawab asal terjemahan/edisi, penggunaan offline/distribusi/atribusi, serta celah nomor dan pemetaan edisi. Lisensi MIT repositori belum cukup menjadi bukti seluruh hak konten upstream telah terjawab. Ini bukan kesimpulan bahwa pemakaian dilarang.
- Pengguna **sudah mengizinkan** pengiriman pertanyaan. Email ke `contact@gading.dev` terkirim **24 Oktober 2026**, subjek “Klarifikasi sumber, penomoran, dan penggunaan data Hadith API untuk Zikra”.
- **Pemeriksaan terakhir 24 Oktober:** belum ada balasan masuk pada thread klarifikasi. Ini bukan klaim sudah memeriksa inbox pada 25 Oktober.
- Balasan perlu dinilai isinya, bukan otomatis dianggap izin semua pemilik konten atau jaminan akurasi. Jika belum menjawab, B01 tetap terbuka atau pilih sumber yang bukti asal/ketentuannya lebih jelas.
- Jangan mengirim email duplikat atau menunggu secara aktif tanpa pekerjaan. Modul independen bisa lanjut. Jangan memasukkan identitas Gmail pengguna, ID thread atau receipt privat ke Git.
- Bukti: [audit sumber](android/validation/hadith-source-2026-10-24/README.md), [review sumber](android/validation/source-review-b-2026-10-24/README.md), [riwayat klarifikasi terkirim](android/validation/b-final-review-2026-10-24/README.md). Bagian awal laporan lama bersifat historis; B04 terbaru ada pada laporan penyelesaiannya.

## Pekerjaan aktif: C — kode lokal siap, hosting/HP dan dependensi belum ditutup

Pengguna meminta melanjutkan seluruh C dan menambahkan tugas pengingat adzan.
Keputusan 25 Oktober: **belum ada hosting; siapkan paket deploy dahulu**.
Keputusan terbaru pengguna: **pakai key yang sudah ada; syarat rotasi dibatalkan**,
menggantikan keputusan menunda sebelumnya. Jangan meminta rotasi ulang sebagai
prasyarat. Key/env privat tidak diubah; provider key tetap hanya di server.

[Bukti/source/checklist C](android/validation/assistant-c-2026-10-25/README.md):
- C02 source: token unik per instalasi, revoke, kuota persisten; satu proses/replika.
  [Paket Docker/Compose/Caddy dan runbook](backend/deploy/README.md) tersedia,
  **belum dijalankan/deploy**. Tidak ada akun/VPS/domain/biaya dibuat. C02/C05 [ ].
- C03 native: preview per request, default hanya pesan saat ini; opt-in nama,
  10 tugas/tenggat, perhitungan salat/zona, riwayat online dan izin proposal.
  Loading/batal/retry/error spesifik dan label online teruji dengan respons
  terkendali. Belum APK → HTTPS permanen → provider dari HP, C03 [ ].
- C04: proposal tambah/edit Study perlu review; SQLite v4 receipt + mutasi atomik
  mencegah duplikasi, menolak edit stale/tugas selesai. Tidak otomatis mengubah
  progres/reminder. Menu ayat Quran → Tanya makna mengambil petikan tafsir EQuran
  (sumber Kemenag menurut dokumentasinya), ringkasan dan teks sumber dibedakan;
  sumber gagal tidak diganti tebakan. Provider privat nyata proposal/Tadabbur 200.
  Jadwal kelas belum tersedia sebelum D04; integrasi end-to-end masih perlu, C04 [ ].
- C06: penjelasan privasi dikte, hasil tetap draf, batal/hasil salah/batas teks
  teruji. Recognizer emulator ada, **mikrofon/dikte layanan nyata belum diuji**;
  callback tes disimulasikan. C06 [ ], Vision tetap setelah rilis pertama.
- 26 tes Node lulus; 118 unit Android = 114 lulus/4 skip, tidak disamakan semua
  lulus. Enam native AI + 11 migrasi + dua metode font 200% lulus; lint 0 error /
  716 warning. Build satu worker/heap 1.024 MiB. Emulator dimatikan; image
  menaikkan RAM aktual sekitar 4 GB walau request 1.536 MiB.

C01 selesai persiapan konfigurasi, dengan [keputusan terbaru dan bukti penutupan](android/validation/assistant-c-2026-10-25/key-decision-2026-10-25.md).
C05 butuh server permanen dan HP; jangan menganggap probe localhost
atau screenshot interceptor sebagai bukti C05. Lanjutkan dari source ini, bukan
membuat ulang gateway/UI. Pilih layanan/domain ketika pengguna siap, uji paket
secara nyata, lalu lakukan rantai APK/HP dan dikte. Konteks jadwal kelas mengikuti
D04; jangan mengarang data jadwal untuk mencentang C04.

**D02a ditambahkan, belum diimplementasikan:** sakelar utama + pilihan per salat
**adzan penuh / notifikasi biasa tanpa adzan / getar / mati** saat masuk waktu.
Pilihan persisten, audio berhenti/izin/senyap/alarm/reboot perlu dibuktikan pada
D02. Ini pengingat lokal, tidak memerlukan backend AI. Baseline notifikasi biasa
sudah ada; jangan menyebut seluruh adzan tersedia pada APK ini.

## Batas dan WIP yang harus dipertahankan

- Setiap versi unduhan awal harus **<100.000.000 byte**; audio/paket besar opsional. Cache setelah instalasi berbeda dari ukuran installer. Performa RAM/baterai/HP tetap perlu pengukuran.
- Pertahankan bookmark, posisi baca, tugas, lokasi dan preferensi. Screenshot konsep/build sukses tidak menggantikan pemeriksaan native.
- Referensi visual disetujui: `design/approved-2026-10-21/`; logo `design/branding/zikra-logo-primary.jpg`. Home/Quran/Study/Protector/AI/Hadis/Doa/Profil/onboarding sudah memiliki checkpoint native, tetapi F/G belum selesai. Hindari redesain total tanpa kebutuhan.
- Cek Git dahulu, stage file spesifik saja. Jangan stage `PROJECT_HANDOFF.md`, aset/prototipe lama yang tidak terkait, konfigurasi atau receipt privat.
- `.local/featherless.env` dan receipt `.local/` privat; jangan cetak/commit nilainya. Jangan menyalin API key dari riwayat chat.
- WIP audio A03 disimpan dalam stash `35686be384ac549e5a5bf85be87cfe56e3b667df` dan arsip `.local/pending-audio-a03-2026-10-24/source-wip.zip`. Jangan pop/drop/hapus/reset; tinjau terarah pada D03 setelah backup.
- Build hemat RAM, tes sesuai perubahan; tutup emulator setelah dipakai. Dokumen saja tidak memerlukan build/emulator. Petunjuk: `android/BUILDING.md`.
- Lingkungan lokal: JDK `C:\Users\Lenovo\.jdks\jbr-17.0.14`, SDK `E:\AndroidSDK`, Gradle cache `E:\.gradle`. Gunakan `git -c safe.directory=D:/Download/Zikra-App-main/Zikra-App-main` bila diperlukan.

Handoff `83312da` adalah baseline dokumentasi lama. Checkpoint C berikutnya berisi kode/backend/APK dan bukti di atas; C, B01 serta rilis publik tetap belum ditutup. Tidak memeriksa email ulang atau memasang APK pada HP fisik.
