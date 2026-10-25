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
- Kandidat terbaru **1.8.1 beta / code 26** memperbaiki crash TLS saat keluar AI. [Receipt source/uji emulator nyata](android/validation/assistant-hosted-emulator-2026-10-25/release.json); gunakan source commit pada receipt ini untuk APK terbaru, bukan backend atau APK 1.8.0 lama.
- Source backend Vercel: **`9438aa4`**, checkpoint lokal; dua deployment production berhasil, [receipt hosting/uji](android/validation/backend-vercel-2026-10-25/deployment.json). APK 1.8.0 pada receipt hosting memakai `7982e10`; APK terbaru mengikuti receipt 1.8.1. Jangan menyamakan commit backend dengan APK.
- Aplikasi **1.8.1 beta / versionCode 26**, SQLite v4. APK debug, belum signed release publik.
- APK lokal: `D:\Download\Zikra-App-main\Zikra-AI-v1.8.1-beta-debug.apk`; 1.8.0 tetap dipertahankan.
- Ukuran **31.181.560 byte**; SHA-256 `8650b9cc12d2d7e061dd0730e4f6e03f3e1767924a5b60271a6c1cd0a900e87b`.
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

## Pekerjaan aktif: C — backend Vercel online, APK/HP dan operasional belum ditutup

Pengguna meminta melanjutkan seluruh C dan menambahkan tugas pengingat adzan.
Keputusan awal 25 Oktober adalah paket deploy dahulu; keputusan terbaru:
**gunakan Vercel**, setelah pengguna memilih tidak melanjutkan verifikasi kartu Oracle.
Keputusan terbaru pengguna: **pakai key yang sudah ada; syarat rotasi dibatalkan**,
menggantikan keputusan menunda sebelumnya. Jangan meminta rotasi ulang sebagai
prasyarat. Key/env privat tidak diubah; provider key tetap hanya di server.

[Bukti/source/checklist C](android/validation/assistant-c-2026-10-25/README.md):
- C02: **backend sudah online** `https://zikra-ai.vercel.app`; endpoint Android
  `https://zikra-ai.vercel.app/v1/assistant`, health `/health`. Vercel Hobby,
  Upstash Redis Free di `sin1`, tidak perlu kartu; autoUpgrade/eviction/prodPack
  terkonfirmasi false. Pengguna sudah menyelesaikan login CLI/ketentuan; jangan
  meminta persetujuan yang sama lagi. [Bukti hosting](android/validation/backend-vercel-2026-10-25/README.md)
  dan [runbook Vercel](backend/deploy/VERCEL.md): HTTPS publik tanpa bypass/login
  Vercel health 200, tanpa token/dicabut 401, Featherless nyata 200, kuota 429,
  revokasi/counter bertahan deploy ulang, storage korup 503 tanpa provider.
  Redis menyimpan hash/UUID/counter/lease, tanpa percakapan. Mode file/VPS tetap
  satu proses/replika; mode Vercel memakai kuota/lease atomik bersama.
  C02 [ ] untuk laptop mati/operasional; C05 [x] untuk alur HP nyata.
  Jangan menyamakan penutupan C05 dengan seluruh C02/C.
- C03 native: preview per request, default hanya pesan saat ini; opt-in nama,
  10 tugas/tenggat, perhitungan salat/zona, riwayat online dan izin proposal.
  Loading/batal/retry/error spesifik dan label online teruji dengan respons
  terkendali; inti alur HP kini lulus pada C05. C03 [ ] untuk cakupan mode/
  konfigurasi manual dan timeout UI yang belum ditutup penuh.
  **Lanjutan terbaru:** [1.8.1 pada emulator ke hosting nyata lulus](android/validation/assistant-hosted-emulator-2026-10-25/README.md),
  3 metode live + 6 regresi terkendali, 14 unit AI dan lint 0 error/716 warning.
  Token salah, jaringan putus/retry, batal, proposal ditinjau/disimpan sekali serta
  sumber Tadabbur nyata diperiksa. 1.8.0 sempat crash saat menutup TLS di UI thread;
  1.8.1 memindahkan pembersihan socket ke worker tanpa bypass StrictMode/TLS.
  Data emulator dipulihkan (8 file asli cocok hash), input token sementara
  dihapus, Wi-Fi/data dipulihkan, emulator ditutup. Bukan HP fisik/C05.
- C04: proposal tambah/edit Study perlu review; SQLite v4 receipt + mutasi atomik
  mencegah duplikasi, menolak edit stale/tugas selesai. Tidak otomatis mengubah
  progres/reminder. Menu ayat Quran → Tanya makna mengambil petikan tafsir EQuran
  (sumber Kemenag menurut dokumentasinya), ringkasan dan teks sumber dibedakan;
  sumber gagal tidak diganti tebakan. Provider privat nyata proposal/Tadabbur 200.
  Jadwal kelas belum tersedia sebelum D04; integrasi end-to-end masih perlu, C04 [ ].
  Pada uji emulator terbaru proposal/sumber telah melewati hosting nyata, tetapi
  frasa Basmalah pada ringkasan ayat 1:2 perlu tinjauan isi. Jangan mencentang
  akurasi ringkasan berdasarkan kecocokan metadata sumber saja; lihat laporan.
- C06: penjelasan privasi dikte, hasil tetap draf, batal/hasil salah/batas teks
  teruji. Recognizer emulator ada, **mikrofon/dikte layanan nyata belum diuji**;
  callback tes disimulasikan. C06 [ ], Vision tetap setelah rilis pertama.
- **C05 [x], 25 Oktober:** pengguna mengizinkan uji HP POCO F7/API 36, APK 1.8.1.
  [Satu metode HP nyata lulus](android/validation/assistant-phone-2026-10-25/README.md):
  chat online, token salah/retry, kuota server sintetis, batal, Wi-Fi putus/pulih.
  Tidak ada profil/tugas/riwayat asli dikirim, tidak ada interceptor/mock jaringan.
  Tujuh file asli cocok hash setelah dipulihkan; helper/input sementara dihapus,
  Wi-Fi/data dipulihkan. Koneksi AI baru ditinggalkan terenkripsi/siap online;
  sebelumnya belum dikonfigurasi. Source APK tetap `196a8d2`, harness `41c7b14`.
  Identifier HP dan arsip/XML privat hanya di `.local/phone-c05`, jangan dipush.
  HP saat itu terhubung lewat USB; jangan menganggap koneksi masih ada tanpa
  memeriksa ADB. Tidak menjalankan emulator atau menguji mikrofon nyata.
- 26 tes Node lulus; 118 unit Android = 114 lulus/4 skip, tidak disamakan semua
  lulus. Enam native AI + 11 migrasi + dua metode font 200% lulus; lint 0 error /
  716 warning. Build satu worker/heap 1.024 MiB. Emulator dimatikan; image
  menaikkan RAM aktual sekitar 4 GB walau request 1.536 MiB.

C01 selesai persiapan konfigurasi, dengan [keputusan terbaru dan bukti penutupan](android/validation/assistant-c-2026-10-25/key-decision-2026-10-25.md).
Untuk pengujian lanjutan HP, pakai endpoint yang sudah ada; jangan menganggap probe
komputer atau screenshot interceptor sebagai bukti C05. Token instalasi aktif
tersimpan privat di `.local/vercel-client.env`, tanpa nilai di Git/chat; konfigurasi
Redis yang ditarik ada di `.local/vercel-production.env` (key provider sensitive
berisi placeholder `[SENSITIVE]`, jangan menimpa key asli dari file ini).
`.local/featherless.env` asli tetap dipertahankan. Jangan menerbitkan ulang token
atau membuat database/proyek duplikat. Lanjutkan dari source ini, lalu lakukan
rantai APK/HP dan dikte, pemantauan/backup/retensi platform. Konteks jadwal kelas mengikuti
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

Handoff `83312da` adalah baseline dokumentasi lama. Backend Vercel `9438aa4` sudah terpasang dengan key yang ada; APK terbaru 1.8.1 sudah dipasang pada HP sebagai pembaruan dari 1.1, dan C05 selesai. C secara keseluruhan, B01 serta rilis publik belum ditutup. Tidak memeriksa email ulang. Checkpoint tetap lokal sampai push dibuktikan.

## Keputusan terbaru — preview Home/AI dahulu, 25 Oktober

Pengguna meminta melihat desain referensi sebelum eksekusi. [Konsep v1](design/concepts/home-ai-review-2026-10-25-v1.md) sudah dibuat, **belum disetujui**. Jangan melanjutkan implementasi tanpa pilihan pengguna. Draf source/test/build sebelumnya disimpan privat di `.local/home-ai-review-draft/` dan file terkait dikembalikan byte-for-byte ke HEAD; tidak ada perubahan APK di HP. Emulator dimatikan sebelum fixtures/tes baru dijalankan; data emulator tidak direset. Kandidat tetap 1.8.1. UI04 belum dicentang.

Preview terbaru untuk tinjauan pengguna: [Home Al-Aqsa v4 — posisi awal dan setelah digulir](design/concepts/home-al-aqsa-layout-review-2026-10-25-v4.md). Menggunakan referensi Al-Qibli berkubah abu-abu/perak dan awan lebih alami, logo asli sebagai acuan, sapaan kecil sebagai opsi. Belum dipilih/disetujui; semua data contoh dan belum ada implementasi APK baru. Tidak melanjutkan draf kode hanya karena mockup selesai.

Preview tinjauan terbaru menggantikan acuan visual v4 sebagai usulan: [Home Al-Aqsa siang/malam v5 dan AI final-proposal v2](design/concepts/home-ai-final-review-2026-10-25.md). Home tetap memakai bagian gulir v4. Permintaan final AI adalah pematangan desain, bukan persetujuan implementasi. Belum ada pilihan/persetujuan pengguna; semua gambar statis dan data contoh. Tidak mengubah source aplikasi, APK, perangkat/data atau konfigurasi privat. UI04 tetap belum dicentang.

## Keputusan paling baru — Home disetujui, AI otomatis dan lampiran diminta

25 Oktober: pengguna menyatakan Home sudah setuju. Ini menggantikan status menunggu persetujuan Home pada catatan sebelumnya; jangan meminta persetujuan Home ulang. [Acuan/persetujuan dan preview AI v3](design/concepts/home-approved-ai-auto-attachments-2026-10-25.md). Mulai implementasi Home Al-Aqsa v5 dengan bagian gulir v4. AI diminta default Otomatis menurut koneksi dan hasil gateway, manual override cukup di label; + kiri composer membuka Kamera/Galeri/Dokumen. Vision/lampiran dibuka kembali sebagai C07, menggantikan penundaan lama, tetapi belum diimplementasikan atau diuji; daftar format awal foto/PDF/DOCX/TXT hanya usulan sampai parser dibuktikan. Model lokal saat ini Qwen/Qwen2.5-7B-Instruct dan backend hanya content string/48 KiB. Dokumentasi Featherless mendukung vision, belum bukti akses akun. Tidak mengubah key/env, provider key tetap server-only. Persetujuan Home berlaku; preview AI revisi baru ditampilkan dan belum dinilai. Putaran ini hanya desain/dokumentasi, tidak ada source runtime/APK baru. Estimasi 2–4 jam untuk Home+AI teks adalah estimasi kerja, bukan deadline atau pekerjaan otomatis di latar.

## Koreksi visual pengguna — jangan menyebut 1.9.0 sesuai referensi

Pengguna menolak perbedaan layout/aset pada hasil native 1.9.0 dan meminta sangat mirip referensi Home siang/malam v5. Kandidat 1.9.0 dan bukti fungsional di home-al-aqsa-2026-10-25 adalah checkpoint teknis yang **belum diterima secara visual**, bukan hasil final.

**Koreksi terbaru sudah dibuat dan diuji: 1.9.1/code28**, source utama `51f7ac2`, detail `6bdf360`. [Bukti native, source, APK dan batas](android/validation/home-reference-match-2026-10-25/README.md). Home-only Roboto, wordmark serif/simbol resmi, lokasi kanan, hero Al-Qibli/awan transparan/parallax, sumber lewat tahan header, countdown asli/strip lima salat dengan titik/pembatas, pintasan datar, Ruang harimu/bacaan/agenda lokal dan dock. Siang/malam/fase, gulir/agenda/kalender, font100%/200% dan layar320dp diperiksa: **8 eksekusi native/5 metode unik +5 unit lulus**, lint0 error/721 warning. Aset rekonstruksi generatif, tidak identik per piksel; jangan mengklaim sudah diterima pengguna atau astronomi/baterai/HP terbukti.

APK di folder luar `Zikra-Home-v1.9.1-beta-debug.apk`, **34.740.137 byte**, SHA-256 `bb6f86b1723ebb85e89209e6cfaf71bfd442ff9299da68770cfcf86b6942392f`, debug signature sama baseline1.8.1, SQLite4. Kandidat lama tetap ada; pra-detail privat `.local/home-reference/pre-detail-app-debug.apk`. Sepuluh file data emulator pulih cocok hash, font/ukuran layar dipulihkan, marker/helper dihapus, emulator ditutup. HP tidak diubah pada kandidat ini. WIP/env/stash audio dipertahankan; nilai rahasia tidak ditemukan pada audit terarah.

Catatan preview-only/menunggu persetujuan Home sebelumnya adalah historis: **Home sudah disetujui dan diterapkan**, UI04 keseluruhan tetap terbuka untuk revisi AI. AI runtime masih satu percakapan teks/dikte lama; mode otomatis, lampiran, multi-chat dan kreasi gambar belum tersedia (C03/C07–C09). Jangan menerapkan ulang `.local/home-ai-review-draft/tracked.patch` karena akan mengembalikan source Home lama. C01/C05 selesai hanya lingkupnya; C02/C03/C04/C06, B01, adzan D02a dan rilis publik belum selesai. Lanjutkan menurut steering pengguna, tanpa mengubah key atau membuat layanan duplikat. Checkpoint ini lokal; jangan mengklaim sudah dipush.
