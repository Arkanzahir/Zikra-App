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

Tambahan harness `4c64d80` menunggu frame siang/malam benar-benar tergambar sebelum capture, sesudah pemeriksaan visual menemukan screenshot rasio acuan tertinggal satu frame. Bukti native diulang; APK dan source runtime `6bdf360` tetap sama. Jangan mengganti label screenshot tanpa memeriksa isi.

Catatan preview-only/menunggu persetujuan Home sebelumnya adalah historis: **Home sudah disetujui dan diterapkan**, UI04 keseluruhan tetap terbuka untuk revisi AI. AI runtime masih satu percakapan teks/dikte lama; mode otomatis, lampiran, multi-chat dan kreasi gambar belum tersedia (C03/C07–C09). Jangan menerapkan ulang `.local/home-ai-review-draft/tracked.patch` karena akan mengembalikan source Home lama. C01/C05 selesai hanya lingkupnya; C02/C03/C04/C06, B01, adzan D02a dan rilis publik belum selesai. Lanjutkan menurut steering pengguna, tanpa mengubah key atau membuat layanan duplikat. Checkpoint ini lokal; jangan mengklaim sudah dipush.

## Koreksi lanjutan panel dan permintaan ikon — 25 Oktober

Pengguna membandingkan panel jadwal dan menunjukkan bagian bawah abu-abu. Source `a24ab66`/APK **1.9.2/code29** mengganti warna alpha67% dengan gradient gelap opak; radius/pembatas diperkecil, label lebih terang, jam12sp, sorotan emas pucat/titik3dp. [Bukti screenshot/log/audit/receipt](android/validation/home-prayer-strip-2026-10-25/README.md). 3 eksekusi satu metode sky yang sudah ada lulus pada font100%/200% dan rasio acuan; lint0 error/721 warning. APK34.401.029 byte, SHA-256 `a2aa80180f0f909e0d3b1af2724cc432b65de5e098ed9bb50cd0dbdd09ea8ef2`;1.9.1 dipertahankan.10 file emulator pulih cocok hash, helper dihapus/emulator ditutup; HP tidak diubah. UI04 belum ditutup.

Steering terbaru: pengguna kurang menyukai simbol delapan pintasan. Buat preview desain ikon yang konsisten sebelum penerapan, mengikuti permintaan sebelumnya untuk melihat desain dahulu. Kandidat1.9.2 belum memakai ikon baru. Jangan menyamakan pilihan varian preview dengan persetujuan implementasi.

[Preview simbol sudah dibuat](design/concepts/home-shortcut-icons-review-2026-10-25.md): varian Lembut dan Kartu ringkas, posisi tetap4×2; satu keluarga simbol, Quran/Hifz/Hadis dibedakan dan AI memakai gelembung chat. Asisten menyarankan Lembut; pengguna belum memilih. Dua varian diperiksa di renderer lokal pada320/390/736, delapan simbol tampil utuh dan tanpa error skrip. Tidak menambah build/runtime sesudah koreksi panel1.9.2. UI04 tetap terbuka.

**Klarifikasi terbaru:** pengguna menilai bentuk simbolnya masih tidak cocok, bukan wadahnya. Pemilihan carousel Lembut bukan persetujuan. [Usulan vektor khusus berikutnya](design/concepts/home-symbols-custom-2026-10-25/README.md) menggambar ulang8 simbol: Quran/rehal, tangan/sabit Doa, Kaabah Kiblat, mushaf bertanda hafalan, chat/sabit AI, perisai cuaca, buku/pensil Study dan gulungan Hadis. Papan dan ukuran Home dirender dari SVG yang sama;8 aset utuh pada320/390/760. Masih preview belum disetujui/diimplementasikan, APK1.9.2 tetap. Jangan memakai klaim pilihan Lembut sebelumnya untuk mengeksekusi simbol baru tanpa penilaian pengguna.

## Pilihan pengguna sudah diterapkan — pintasan garis/mint/krem

**Menggantikan status preview simbol di atas:** pengguna mengirim [referensi baru](design/approved-2026-10-25-home-shortcuts/README.md) dan berkata “jadi kekgituaja”. Yang dipilih ialah garis hijau tua dengan lingkaran mint/krem, bukan usulan Lucide/duotone asisten. Sudah diterapkan pada **1.9.3/code30**, source `1cace78`:8 vector Home, lingkaran56dp/padding9dp, baris88dp; bentuk/urutan/warna mengikuti gambar. Master logo/halaman lain/ID tujuan tidak diubah. [Hasil APK, screenshot native, receipt dan batas](android/validation/home-shortcuts-2026-10-25/README.md). Bukan klaim identik per piksel.

APK `Zikra-Home-v1.9.3-beta-debug.apk` di folder luar, **34.408.836 byte**, SHA-256 `19650c988be7302044125de2d53f07fa51d23d65b57b65f93ff1b201a53e1dc5`; sertifikat debug sama, SQLite4, APK1.9.2 tetap ada.6 eksekusi native/3 metode unik lulus pada font100%/200%,320dp dan rasio acuan; lint0 error/721 warning.10 file emulator pulih cocok hash, font/ukuran dikembalikan/helper dihapus, emulator ditutup; HP tidak diubah. WIP/env/stash dipertahankan dan audit nilai rahasia saat ini bersih. UI04 keseluruhan/AI/C/B01/D02a/F/G/publik tetap terbuka. Checkpoint lokal sampai push dibuktikan.

## Koreksi terbaru — panel lebih ringan, posisi dan lampu masjid

Pengguna meminta gradasi/posisi mengikuti [acuan ini](design/approved-2026-10-25-home-alignment/README.md) serta lampu padam siang/menyala malam. Sudah dibuat **1.9.4/code31**, source `b282fd8`: panel56dp dengan atas transparan/tengah-bawah opak untuk menghindari seam cream, angka medium; hero/masjid/teks/logo/pintasan/bacaan dirapikan. Ikon pilihan terbaru tetap. Aset siang baru tanpa glow dibuat dengan built-in image_gen; master malam tidak ditimpa. Renderer memilih/crossfade menurut daylight lokasi, bitmap cached; bukan identik per piksel. [Bukti native, source, aset/prompt dan batas](android/validation/home-lighting-layout-2026-10-25/README.md).

APK `Zikra-Home-v1.9.4-beta-debug.apk`, **36.594.943 byte**, SHA-256 `76a1367d52354fc24757b71221347766c7d2912160a1527015ffe1417ffc26b3`;1.9.3 tetap, signer sama, SQLite4.6 eksekusi native/3 metode unik+5 unit Sky lulus; lint0 error/721 warning. Piksel glow lampu aktual1 siang vs2.469 malam (100%);1 vs2.376 (200%). Ini proksi visual/fixture, bukan fotometri/fase astronomi/seluruh twilight.10 file emulator dipulihkan cocok hash, font/ukuran dikembalikan/helper dihapus, emulator ditutup, HP tidak diubah. WIP/env/stash tetap; audit nilai rahasia saat ini bersih. UI04/AI/C/B01/D02a/F/G/publik tetap terbuka. Source checkpoint lokal, belum push dibuktikan.

## Checkpoint terbaru — AI dan Home 1.9.5, C08 selesai

Menggantikan status runtime AI lama di catatan sebelumnya. Pengguna meminta AI
dikerjakan serta ketebalan teks/ukuran logo Home disesuaikan. **Source `a5d070a`,
beta1.9.5/code32** sudah dibuat. [Bukti, screenshot dan batas lengkap](android/validation/ai-home-refresh-2026-10-25/README.md).

- UI AI krem/pine/mint, riwayat/chat baru, mode Otomatis/Online/Offline,
  + Kamera/Galeri/Dokumen, preview/hapus/consent per request. Offline tetap aturan,
  bukan model generatif. Network callback tidak auto-send draf; error layanan jelas.
- C08 **[x]**: SQLite chat terpisah v1, migrasi history preference sekali tanpa
  hilang metadata, buat/pilih/cari judul/rename/batal-hapus/hapus/reopen dan draf
  per chat lulus native. Batas40 chat/200 pesan/128.000 karakter tidak silent eviction.
  SQLite utama tetap4. Chat lain tidak ikut konteks otomatis.
- Foto JPG/PNG/WebP, PDF ≤3 halaman, DOCX/TXT ≤12.000 karakter, sumber ≤8MB dan
  penyimpanan lampiran25MB. Normalisasi/EXIF/PDF/arsip/mime/dimensi/URL diperiksa;
  kamera melalui grant URI privat, tanpa izin file luas. C07 **masih [ ]** untuk
  kamera/picker/izin pada HP dan variasi OCR. Native provider kamera sintetis dan
  parser lulus; bukan kamera fisik.
- Backend vision sudah dipasang pada proyek Vercel lama, ID
  `GXHBqWcUXxyy5SZFSLFzkpcUpYpZ`, alias `https://zikra-ai.vercel.app`.
  `FEATHERLESS_VISION_MODEL=Qwen/Qwen2.5-VL-7B-Instruct` hanya server; model teks
  lama/key asli tidak diganti. Foto/PDF scan sintetis berhasil APK → Vercel →
  provider nyata, tanpa interceptor; TXT/DOCX nyata berhasil. Endpoint daftar
  model Featherless sempat404; akses dibuktikan lewat completion langsung.
- Akurasi bukan selesai: satu percobaan tidak menampilkan jawaban, berikutnya
  menjawab hitungan43 tetapi melewatkan kode referensi. Tes live terakhir membuktikan
  hitungan pada gambar/PDF saja; jangan mengklaim seluruh OCR/instruksi selalu benar.
- Home: bobot judul/countdown/bacaan600, ukuran bacaan/warna dan proporsi simbol
  resmi dirapikan. Ikon/aset/siang-malam yang disetujui tetap. Bukan pixel-identical.

APK folder luar `Zikra-AI-v1.9.5-beta-debug.apk`, **37.175.496 byte**, SHA-256
`a25f0db2ac004721c4a531aab044920a6a28add5f16f2fa2cdb34828f55ab13f`.
Signer sama baseline, APK1.9.4/lama tetap.15 eksekusi native pada APK final
(13 metode unik);4 eksekusi Home/live pada build tepat sebelum fix khusus keyboard.
116 unit lulus/4 skip,36 Node lulus/4 skip,lint0 error/752 warning. Screenshot
menemukan tombol terpotong pada font200% walau tes awal lulus sebagian; sudah
difix, tes tinggi penuh dan screenshot200%/sempit lulus.10 file emulator pulih
cocok hash/font/ukuran/jaringan kembali/helper dihapus/emulator ditutup. HP tidak
diubah. Audit nilai rahasia terarah bersih; WIP/env/stash/audio dipertahankan.

Pengguna bertanya Gemini/AI Studio untuk gambar. **C09 [ ]**, belum key/akses/biaya
gambar terpilih. Dokumentasi Google yang ditinjau tidak memberi Free Tier untuk
model image-generation API; jangan menyamakan web gratis dengan API gratis atau
mengaktifkan billing. Belum memasang tombol palsu. C03 Wi-Fi tanpa internet/
timeout/konfigurasi UI, C02 operasional/laptop mati, C04 jadwal/akurasi, C06 dikte
fisik, B01 sumber Gading, D02a adzan dan F/G/publik tetap terbuka. UI04 keseluruhan
belum ditutup. Checkpoint lokal; belum push dibuktikan. Jangan membuat ulang
hosting/database atau mengubah key/env. Fixture Redis sintetis live sudah dihapus.

## Checkpoint terbaru — C09 Cloudflare Free, beta1.9.6

26 Oktober: pengguna memilih opsi gratis Cloudflare, login sendiri, membuat token
Workers AI dan menyimpannya di `.local/cloudflare.env`, lalu mengonfirmasi Workers
Free. **C09 [x] untuk beta**; status belum ada layanan gambar pada catatan1.9.5
di atas kini historis. Runtime `717a9c5`, harness keyboard `1c42900`.
[Bukti/source/hash dan batas](android/validation/image-generation-2026-10-26/README.md).

- FLUX.1 Schnell nyata berhasil **APK → Vercel → Cloudflare**, JPEG1024×1024
  didecode/disimpan/ditampilkan. + atau saran **Buat gambar**, draf mode terpisah,
  tinjauan prompt saja, hasil berlabel ilustrasi AI, preview/save/share dan riwayat
  lokal. Tidak auto-kirim ketika jaringan pulih; profil/history/lampiran tidak ikut
  request gambar. Text-to-image, belum edit foto atau janji hasil selalu akurat.
- Model/4 langkah tetap, Redis Lua3 percobaan/instalasi/hari dan20 total/hari,
  nonce dedup, reset UTC00/WIB07, fail-closed ketika kuota rusak/Redis gagal.
  Batal/gagal/timeout dapat memakai percobaan; retry perlu konfirmasi baru.
  Cloudflare Free juga memiliki kuota akun bersama. Tidak ada paid fallback atau
  perubahan billing/paket. Token hanya backend, bukan APK/Git.
- Hasil privat maksimal32MB; metadata JSON hanya ref. Save memakai pemilih dokumen
  Android; share memakai chooser + URI read-only. Provider test-export **hanya di
  APK helper tes**, bukan app. Validasi MIME/decode/dimensi/base64/path dan batas2MB;
  penghapusan chat membersihkan file, termasuk AtomicFile `.new`/`.bak` terputus.
- Hosting existing `https://zikra-ai.vercel.app`, deployment
  `Du6X24ysdLE3Zc6Ua6A6Q5Nt6CLR`; immutable
  `https://zikra-1i4jk6stt-arkanzahir86-3257s-projects.vercel.app`.
  Tambahan `/v1/images` dan `/v1/capabilities` terautentikasi. Kode handler
  Vercel/Redis; CLI privat tetap chat. Tidak membuat proyek/database/Worker baru,
  tidak mengganti key Featherless atau token instalasi pengguna.

APK folder luar **`Zikra-AI-v1.9.6-beta-debug.apk`**, code33, **37.013.519 byte**,
SHA-256 `dc55e1e817f17dece0765d06b434b846291d53663fde6220adb05a899ff67c80`.
Signer debug sama, SQLite utama4/chat1; APK1.9.5/lama tetap.47 backend lulus termasuk
Lua asli di fakeredis/Lupa lokal (bukan konkurensi Upstash produksi),19 eksekusi
native/18 metode unik lulus;116 unit lulus/4 skip,lint0 error/753 warning. Ekspor
byte nyata melalui provider fixture terpisah; chooser/save-picker diintersep,
tidak mengirim ke aplikasi pihak ketiga. Pemilihan tujuan aktual/share app di HP
tetap G02. Probe Upstash langsung dari komputer timeout; alur Vercel nyata lulus.
Prefix probe sintetis terpisah berttl48jam, cleanup langsung tidak terkonfirmasi;
tidak ada profil/prompt/data pengguna pada prefix itu atau perubahan registry.

Tes awal bermasalah pada chaining Intent void, filter MIME/category, layar emulator
tertidur dan startup resize/jaringan fixture. Tidak dihitung lulus. Build/filter,
layar aktif/fokus dan assertion IME benar-benar terbuka dipertajam; font200% lebar
dan sempit kemudian lulus.10 file asli emulator pulih cocok hash; helper/marker
dihapus, font/ukuran/jaringan/stay-awake/setting keyboard kembali, emulator ditutup.
HP tidak diubah. WIP PROJECT_HANDOFF.md/env lama/stash/audio/master logo/aset malam
dan APK lama dipertahankan; audit nilai rahasia terarah bersih.

C01/C05/C08/C09 selesai hanya lingkupnya. C02/C03/C04/C06/C07, sumber Hadis B01,
adzan D02a, UI04 keseluruhan/F/G dan publik tetap terbuka. Jangan menyebut semua C
selesai. Tidak memakai Gemini atau mengaktifkan billing. Checkpoint lokal sampai
push dibuktikan; jangan mengulang hosting/proyek dari nol atau mencetak env privat.

## Checkpoint terbaru — C03 selesai, lanjut C06 di HP

26 Oktober2026: C03[x], beta1.9.7/code34, source `249b523` lokal.
[Bukti/receipt/source/hash](android/validation/assistant-c03-2026-10-26/README.md).
Mode/koneksi/batal/timeout/retry, Wi-Fi tanpa VALIDATED dan portal Android nyata
(probe localhost AOSP), font200%/layar sempit/keyboard lulus19 eksekusi native/
17 metode unik;116 unit lulus/4 skip,lint0 error/753 warning.10 berkas emulator dan
pengaturan pulih, helper dihapus/emulator ditutup. WIP/env/stash/audio/aset tetap.
APK `Zikra-AI-v1.9.7-beta-debug.apk` di folder luar,37.218.400 byte;
SHA256 `686101f643a13b49ec9ff05623a8b52c8ea3e9f2f1689763ee24ee7d952ea833`.
Signer debug sama; SQLite utama4/chat1. Backend tidak diubah/deploy ulang.

Pengguna meminta eksekusi berurutan dengan clean code/security, lalu mengonfirmasi
HP terhubung/terbuka. Read-only probe menemukan satu HP API36, APK lama code26 dan
recognizer tersedia.12 berkas asli dicadangkan privat ke `.local/phone-c06/`;
`original.tar`/`original-hashes.json`/`device-private.json` tidak boleh dicetak atau
masuk Git. HP belum diisolasi, belum dipasang APK baru, belum dites mikrofon pada
checkpoint ini. Jangan menganggap backup lama C05 sebagai keadaan HP terkini.
Aktif berikutnya C06: cadangan baru diverifikasi dahulu, tidak mengunggah profil/
percakapan asli. Bukti mic harus recognizer/mikrofon nyata, bukan callback simulasi.

C01/C03/C05/C08/C09 selesai sesuai lingkup. C02/C04/C06/C07 tetap terbuka;
C04 mengikuti D04. B01 diperiksa26 Oktober melalui Gmail: thread masih hanya email
terkirim24 Oktober; pencarian termasuk spam/sampah tidak menemukan balasan domain
Gading. Tidak mengirim follow-up baru atau memasukkan ID Gmail ke Git.
Memori permanen masih usulan; D02a/UI04/F/G dan rilis publik belum selesai.

## Lanjutan — HP terputus, C06 ditunda pembuktiannya

Pengguna memilih **lanjut tanpa HP dahulu** setelah ADB tidak lagi mendeteksi
perangkat. `PhoneVoiceTest` opt-in sudah dikompilasi dengan flag phoneTests,
[bukti persiapan dan batas](android/validation/assistant-c06-2026-10-26/README.md).
Belum menjalankan mikrofon/recognizer fisik. Percobaan driver berhenti sebelum
isolasi/install; data/pengaturan HP tidak diubah, APK terakhir teramati code26.
Backup12 berkas privat tetap ada dan harus diverifikasi ulang saat HP kembali.
C06/C07 tetap terbuka. Pekerjaan aktif beralih ke C02 operasional tanpa HP;
uji laptop mati tetap membutuhkan perangkat lain. Jangan menyalakan paket berbayar.

## Checkpoint terbaru — C02 operasional tanpa HP

26 Oktober2026, source operator `12d0fff` lokal; runtime Vercel tetap deployment
C09 sebelumnya, tidak redeploy. [Bukti/runbook](android/validation/backend-operations-c02-2026-10-26/README.md).
58 tes backend lulus/0 skip, termasuk Lua asli melalui fixture bersama lokal.
Probe production HTTPS200+lookup Redis menolak401 tanpa token aktif/kuota/inference
lulus. Backup registry AES-256-GCM dibuat privat, restore Upstash nyata hanya ke
namespace baru; seluruh impor revoked, kuota gambar target disegel hari berjalan,
source/credential asli tetap, key target dibersihkan. Abort request cloud diamati:
lease lepas325ms, retry malformed400 tanpa inference kedua. Request pertama dapat
memakai provider, dua percobaan aplikasi terhitung; bukan jaminan pembatalan biaya.

API read-only memastikan Hobby/Node24.x/Upstash Free, autoUpgrade/eviction/prodPack
false, credential production-only, expiration deployment30 hari. Sampel runtime
10 menit berisi0 record; jangan memperluasnya jadi jaminan seluruh log/tracing.
Kebijakan operator mingguan/30 hari manual, bukan scheduler backup aktif. Kunci/
blob/namespace/response log/raw metadata tetap di `.local/operations-c02/` dengan
ACL operator/SYSTEM/admin; tidak menyalin nilainya ke Git/APK/chat. Tidak mengganti
key Featherless/token instalasi, database atau paket, tidak menyalakan paid feature.

Workflow `.github/workflows/backend-health.yml` baru lokal: empat kali sehari,
checkout dipin, contents:read, tanpa secret dan tanpa inference. **Belum aktif
sebelum push/default branch/Actions**; proyek Vercel tidak terhubung Git, deploy
Vercel berbeda dari push GitHub. Pertanyaan status GitHub pengguna diperiksa fetch:
`origin/main` masih83312da, kala itu33 commit lokal belum dipush. Jangan mengklaim
push/merge/rilis tanpa hasil nyata. WIP PROJECT_HANDOFF.md/prototipe lama tidak
ikut checkpoint ini. Audit nilai rahasia yang diketahui mencakup source/APK dan
objek Git baru sejak origin/main, bukan deteksi semua kemungkinan secret historis.

C02 masih[ ]: monitor aktif/bukti job, timeout cloud panjang dan HP saat laptop
mati. C06/C07 menunggu HP; helper C06 baru build, belum mikrofon. C04 membutuhkan
D04. C01/C03/C05/C08/C09[x] sesuai lingkup, B01/D02a/UI04/F/G/publik tetap terbuka.
APK terbaru tetap1.9.7/code34,37.218.400 byte/source249b523; jangan rebuild untuk
dokumen/tool operator. Emulator sudah mati; HP tidak diisolasi/diupdate/dihapus.

## Checkpoint terbaru — sudah dipush ke GitHub, 26 Oktober2026

Pengguna meminta push terlebih dahulu. **35 commit pekerjaan83312da..ee2b751
berhasil dipush ke origin/main**; ref remote diverifikasi sama dengan HEAD.
[Bukti push/audit/workflow](android/validation/checkpoint-push-2026-10-26/README.md).
Ini menggantikan catatan belum-push sebelumnya. WIP PROJECT_HANDOFF.md, stash,
konfigurasi/key/backup privat dan prototipe lama tetap lokal/tidak diikutkan.
Audit sebelum push:486 path,1018 objek Git baru, nol nilai rahasia yang diketahui
pada source/evidence/APK; bukan jaminan semua secret historis. Working tree dan
stash/hash WIP identik sebelum/sesudah push.

Workflow backend-health sudah ada di GitHub dengan state active menurut API,
tetapi belum ada hasil job sukses yang diverifikasi. Jangan menyebut C02/seluruh C
atau rilis publik selesai; HP/laptop mati dan timeout cloud panjang masih gerbang.
Proyek Vercel tidak terhubung Git, tidak ada redeploy atau rilis APK publik pada
push ini. APK1.9.7/source249b523 tetap lokal37.218.400 byte; HP terakhir code26.
