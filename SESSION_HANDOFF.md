# Handoff aktif Zikra — 25 Oktober 2026 (WIB)

## Mulai di sini

- Root Git: `D:\Download\Zikra-App-main\Zikra-App-main`, bukan folder pembungkusnya. Remote: `https://github.com/Arkanzahir/Zikra-App.git`, branch `main`.
- Baca `AGENTS.md`, dokumen ini, `IMPLEMENTATION_PLAN.md`, lalu `RELEASE_SCOPE.md`. Rencana/checklist dan bukti terbaru mengalahkan catatan historis.
- Pengguna ingin pindah chat tanpa kehilangan arah. Gunakan skill **caveman**, bahasa Indonesia singkat. Kerjakan satu ID, catat bukti dan centang hanya bila kriterianya terpenuhi. Jangan mengulang seluruh desain/implementasi dari nol.
- Stack: Android native **Java + XML**, SQLite lokal; gateway AI **Node.js**. Tidak migrasi Flutter/React Native/WebView.
- `PROJECT_HANDOFF.md` memuat riwayat dan WIP lama yang belum di-commit. Jangan menimpa, membatalkan atau memasukkannya ke commit pekerjaan ini.

## Checkpoint yang dapat dipulihkan

- Source aplikasi terbaru: `b3f13f31e6c9ad77b9abe5ef765e7861b5f3619b`.
- Checkpoint dokumentasi sebelum handoff ini: `878a03e`, sudah dipush ke `main`.
- Aplikasi **1.7.6 beta / versionCode 24**. APK debug, belum paket produksi bertanda tangan.
- APK lokal: `D:\Download\Zikra-App-main\Zikra-Content-v1.7.6-beta-debug.apk`.
- Ukuran **31.319.928 byte**; SHA-256 `7dc808e6280ac4f4182f4bda89ceae43c88eb0329e25dd8baa3f2caaed0f5b57`.
- Bukti source/hash/tes/screenshot: [B04](android/validation/dua-completion-b04-2026-10-24/README.md), [receipt](android/validation/dua-completion-b04-2026-10-24/release.json).

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

## Satu pekerjaan selanjutnya: C01

**Audit dan siapkan konfigurasi backend produksi yang sudah ada**, sambil B01 menunggu dependensi. Belum dikerjakan oleh checkpoint handoff ini.

1. Baca `backend/server.mjs`, `backend/server.test.mjs`, `backend/README.md`, `backend/package.json`, serta kriteria C01. Gateway sudah tersedia; jangan membangunnya ulang.
2. Tinjau konfigurasi privat, validasi env, pemilihan model yang tersedia, health check dan cara menjalankan ulang. Jalankan tes backend terarah; gunakan skill keamanan untuk review JavaScript yang relevan.
3. Rekonsiliasi status lama README backend: tes simulasi tersedia; catatan lokal menyatakan gateway → Featherless pernah berhasil 22 Oktober. Itu **bukan** bukti APK → hosting permanen → provider. Jangan menyalin konfigurasi privat ke laporan.
4. Kunci yang pernah dibagikan harus dirotasi sebelum hosting publik; jangan klaim sudah dirotasi tanpa bukti. Kunci provider hanya server. Shared gateway token pribadi bukan autentikasi produksi.
5. Hosting/domain belum dipilih. Persiapan dapat dilanjutkan; deployment dan identitas/akses produksi adalah **C02**, memerlukan pilihan layanan nyata. Jangan mencentang C01 jika kriterianya masih belum terpenuhi; dokumentasikan dependensi.

Backend memakai Node 22+, tanpa dependensi npm tambahan; endpoint `/v1/assistant` dan `/health`. Mode offline Android default, online opt-in mengirim pesan yang dipilih. Jangan otomatis mengirim profil, lokasi, tugas atau berkas perangkat. HTTPS wajib, jangan bypass TLS atau redirect kredensial lintas host.

## Batas dan WIP yang harus dipertahankan

- Setiap versi unduhan awal harus **<100.000.000 byte**; audio/paket besar opsional. Cache setelah instalasi berbeda dari ukuran installer. Performa RAM/baterai/HP tetap perlu pengukuran.
- Pertahankan bookmark, posisi baca, tugas, lokasi dan preferensi. Screenshot konsep/build sukses tidak menggantikan pemeriksaan native.
- Referensi visual disetujui: `design/approved-2026-10-21/`; logo `design/branding/zikra-logo-primary.jpg`. Home/Quran/Study/Protector/AI/Hadis/Doa/Profil/onboarding sudah memiliki checkpoint native, tetapi F/G belum selesai. Hindari redesain total tanpa kebutuhan.
- Cek Git dahulu, stage file spesifik saja. Jangan stage `PROJECT_HANDOFF.md`, aset/prototipe lama yang tidak terkait, konfigurasi atau receipt privat.
- `.local/featherless.env` dan receipt `.local/` privat; jangan cetak/commit nilainya. Jangan menyalin API key dari riwayat chat.
- WIP audio A03 disimpan dalam stash `35686be384ac549e5a5bf85be87cfe56e3b667df` dan arsip `.local/pending-audio-a03-2026-10-24/source-wip.zip`. Jangan pop/drop/hapus/reset; tinjau terarah pada D03 setelah backup.
- Build hemat RAM, tes sesuai perubahan; tutup emulator setelah dipakai. Dokumen saja tidak memerlukan build/emulator. Petunjuk: `android/BUILDING.md`.
- Lingkungan lokal: JDK `C:\Users\Lenovo\.jdks\jbr-17.0.14`, SDK `E:\AndroidSDK`, Gradle cache `E:\.gradle`. Gunakan `git -c safe.directory=D:/Download/Zikra-App-main/Zikra-App-main` bila diperlukan.

Handoff ini hanya memperbarui dokumen. Tidak mengubah APK, memulai C01, memeriksa email ulang atau menyatakan B01 selesai.
