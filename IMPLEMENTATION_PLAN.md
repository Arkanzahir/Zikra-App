# Rencana implementasi Zikra menuju rilis publik

Dibuat 24 Oktober; diperbarui **25 Oktober 2026 (WIB)**. Kandidat terbaru: **1.8.0 beta / code 25**, [bukti AI/deploy, source dan hash](android/validation/assistant-c-2026-10-25/README.md). Baseline konten B04 tetap 1.7.6 (`b3f13f3`), [receipt](android/validation/dua-completion-b04-2026-10-24/release.json). A04 tetap checkpoint 1.6.3 (`2b291ff`); UI01 tetap 1.6.2.

Dokumen ini menjadi **checklist pengerjaan aktif**. [PRD](ZIKRA_PRD.md) tetap menjadi daftar kebutuhan produk; [rencana lama](design/IMPROVEMENT_PLAN.md) dan laporan validasi adalah riwayat. Jika kebutuhan lama berbeda dengan keputusan terbaru pengguna, catat keputusan beserta alasannya di sini.

**B04 selesai untuk kurasi editorial dan integrasi Doa; B01 menunggu balasan klarifikasi sumber Hadis.** [Bukti 1.7.6, catatan seluruh 227 ID, koreksi dan uji](android/validation/dua-completion-b04-2026-10-24/README.md). Doa tetap 14 pilihan bawaan + 225 bacaan tambahan setelah unduh, bukan penjumlahan bacaan unik. B02/B03 selesai teknis pada adaptor kandidat beta; Hadis tetap 7 kutipan bawaan + paket 6.638 Bukhari/4.930 Muslim. Identitas edisi/ketentuan konten Hadis belum ditutup. Penutupan B04 adalah pemeriksaan editorial Arab/arti/judul/rujukan dan alur aplikasi, bukan sertifikasi seluruh sanad/harakat atau pengesahan rilis publik. [Cakupan A02](RELEASE_SCOPE.md) tetap berlaku; tahap C–G belum dianggap selesai.

## Cara memakai checklist

1. Ambil satu ID pekerjaan aktif; selesaikan implementasi, pemeriksaan relevan, dan catatan hasil sebelum berpindah. Pekerjaan lain dapat berstatus menunggu dependensi, bukan dianggap selesai.
2. `- [ ]` = belum memenuhi syarat selesai; `- [x]` = seluruh syarat selesai terpenuhi. Kalau kode sudah ada tetapi belum diperiksa, biarkan belum dicentang dan catat bagian yang belum terbukti.
3. Ketika selesai, ubah checkbox, tambahkan tanggal dan tautan bukti pada item tersebut, lalu tambahkan baris pada jurnal di bagian akhir. Simpan rencana bersama checkpoint kode. Jika perubahan berikutnya merusak hasil, buka kembali item terkait.
4. Bukti fitur adalah perilaku Android/backend sebenarnya. Gambar konsep, build berhasil, respons simulasi, dan jumlah tes saja tidak membuktikan seluruh fitur siap.
5. Jalankan tes terarah sesuai perubahan; gunakan satu worker/heap build kecil. Jangan mengulang seluruh suite atau menyalakan emulator untuk perubahan dokumen. Tutup emulator setelah digunakan; verifikasi RAM aktual karena gambar Android 37.1 dapat menaikkan batas RAM otomatis.
6. Pertahankan data pengguna, perubahan lokal yang belum selesai, dan kredensial privat. Jangan stage seluruh working tree. APK harus cocok dengan source checkpoint yang diklaim.
7. Angka **65%** pada percakapan adalah estimasi kasar kesiapan, bukan hasil audit atau persentase checkbox. Checklist memuat pekerjaan dengan ukuran berbeda; jangan membagi jumlah centang untuk mengklaim persentase total.

## Batas ukuran unduhan — permintaan pengguna 24 Oktober 2026

- **Setiap versi distribusi dengan fitur dalam cakupan rilis harus berukuran di bawah 100 MB** untuk unduhan awal. Untuk pemeriksaan APK langsung, gunakan batas konservatif `< 100.000.000 byte`; untuk distribusi AAB, ukur unduhan perangkat yang dikirim toko, bukan ukuran berkas AAB saja.
- Baseline 1.6.2: APK debug **31.216.429 byte / 31,2 MB**. Sekitar 29.953.338 byte komponen resource terkompresi, terutama ilustrasi PNG. [Hasil pemeriksaan](android/validation/apk-size-2026-10-24.md).
- Pembaruan A04 1.6.3: **31.085.269 byte / 31,1 MB**; [hash/source dan pemeriksaan](android/validation/integration-a04-2026-10-24/README.md). Batas ukuran/G04 tetap diperiksa lagi pada setiap distribusi.
- Checkpoint konten 1.7.0 beta: **31.413.900 byte / 31,4 MB**, [hash/source dan hasil uji](android/validation/content-b-2026-10-24/README.md). Paket Hadis/Doa opsional; validasi sumber publik B01/B04 belum ditutup.
- Model AI tidak dibundel ke APK. Rekaman murottal/paket luring besar diunduh sesuai pilihan pengguna; jangan memasukkan seluruh audio kitab/surah ke installer untuk mengejar klaim luring penuh.
- Ukuran cache/unduhan setelah instalasi berbeda dari ukuran installer. Pada B02–B04/D03/E01, tentukan paket bertahap, informasi ukuran, penghapusan unduhan dan pembatasan cache sesuai modul; ini belum klaim seluruh kontrol tersebut sudah tersedia.
- Ukuran unduhan kecil tidak membuktikan RAM/baterai ringan. F04/G02 tetap memerlukan pengukuran decode ilustrasi, animasi, audio dan peta pada HP.

## Baseline yang sudah terbukti, bukan sertifikasi siap rilis

- [x] **BASE01 — Onboarding tiga ilustrasi dan navigasi langkah native.** Implementasi/preferensi diuji pada checkpoint 1.6.0. [Bukti](android/validation/daily-native-2026-10-24/README.md).
- [x] **BASE02 — Hadis: filter koleksi, pencarian lokal, reader dan bookmark.** Baru tujuh kutipan; belum kitab lengkap. [Bukti](android/validation/daily-native-2026-10-24/README.md).
- [x] **BASE03 — Doa: daftar, pencarian dan membuka isi.** Baru empat doa; suara TTS belum diverifikasi pada putaran tersebut. [Bukti](android/validation/daily-native-2026-10-24/README.md).
- [x] **BASE04 — Profil: nama lokal tersimpan, progres Study dan latar berwarna.** Pemeriksaan Profil terarah lulus pada 1.6.1. [Bukti](android/validation/profile-background-2026-10-24/README.md).
- [x] **BASE05 — Fondasi Quran/audio/Mushaf dan panel reader.** Ada bukti sorotan/audio asli terarah, bukan audit seluruh surah/qari/HP. [Reader](android/validation/reader-fold-ornament-2026-10-22/README.md), [Mushaf](android/validation/mushaf-word-sync-2026-10-21/README.md).
- [x] **BASE06 — Study: agenda serta animasi fokus/istirahat.** Ada pemeriksaan native timer/gerak; pengukuran HP fisik masih terbuka. [Bukti](android/validation/study-layered-motion-2026-10-22/README.md).
- [x] **BASE07 — Protector: cuaca, peta episentrum dan Shakemap.** Data dimuat/refresh; belum push gempa ketika aplikasi ditutup. [Bukti](android/validation/protector-native-2026-10-23/README.md).

Featherless pernah menjawab melalui gateway lokal pada 22 Oktober menurut catatan handoff; server uji sudah ditutup. Ini **belum** bukti AI online dari APK melalui hosting permanen. Tidak menyalin kredensial atau isi konfigurasi privat ke rencana ini.

## Urutan pengerjaan

| Tahap | Hasil yang dituju | Mulai setelah |
| --- | --- | --- |
| A — Audit dan batas rilis | Status setiap fitur jelas; baseline dapat dibangun ulang | Sekarang |
| B — Hadis dan Doa | Isi berlandaskan sumber, pencarian berguna, penyimpanan andal | A01–A04; urutan diperjelas pengguna 24 Oktober |
| C — Backend dan AI | AI online dapat dipakai dari HP tanpa laptop pengembang | A02; keputusan hosting saat konfigurasi siap |
| D — Fitur inti | Quran, salat, Study/Hifz dan lokasi terhubung dengan benar | A03–A04; tiap modul diperiksa bergiliran |
| E — Protector | Data gempa/cuaca benar; notifikasi sesuai lingkup rilis | C02 untuk push; D01/D04 untuk konteks jadwal |
| F — Konsistensi desain | Satu bahasa visual dan alur yang nyaman pada semua menu | B/D/E stabil pada layar terkait |
| G — Uji dan pengemasan rilis | APK/AAB teruji sesuai cakupan yang disepakati | Semua item wajib tahap A–F selesai |

Jangan menjalankan seluruh tahap sekaligus. Pemeriksaan ringan dan kerapian dasar dilakukan dalam setiap pekerjaan; tahap F khusus penyelarasan menyeluruh, bukan alasan menunda perbaikan layout yang menghalangi penggunaan.

## A — Audit dan batas rilis

- [x] **A01 — Inventaris fitur dan selisih terhadap PRD.** Selesai 24 Oktober 2026: [matriks aktual/bukti/gap/ID lanjutan](android/validation/release-audit/FEATURE_MATRIX.md), [inventaris source baseline](android/validation/release-audit/SOURCE_INVENTORY.json) dan [hitungan kandidat konten](android/validation/release-audit/CONTENT_COUNTS.json). Seluruh 23 kebutuhan inti dan 21 layar diperiksa; status tidak ada/sebagian/tersedia/teruji terarah dibedakan. Tidak ada sertifikasi teruji rilis. Audit dokumen/source memakai laporan runtime terdahulu, tanpa build/emulator baru; A02–G05 tetap terbuka sesuai kriterianya.
- [x] **A02 — Tetapkan cakupan beta dan rilis publik.** Selesai 24 Oktober 2026 berdasarkan delegasi pengguna untuk menentukan cakupan: [keputusan tersimpan](RELEASE_SCOPE.md). Beta adalah tahap uji; publik tetap mewajibkan modul inti, Hadis/Doa tervalidasi, AI teks/konteks/tindakan tugas, briefing, adzan, jadwal kelas dan push. Akun/sinkronisasi serta Vision ditunda eksplisit ke setelah rilis pertama; source Infaq dipertahankan di luar navigasi rilis. Tidak ada fitur runtime dianggap selesai karena keputusan ini.
- [x] **A03 — Rapikan baseline source dan pekerjaan audio tertunda.** Selesai 24 Oktober 2026: [keputusan dan bukti](android/validation/baseline-a03-2026-10-24/README.md). Lima file prefetch/audit belum teruji disimpan dalam stash + arsip privat yang diverifikasi, source stabil dipertahankan. Seluruh 470 file produksi/build cocok dengan snapshot APK 1.6.2 dan hash APK diperiksa. Optimasi prefetch belum digabung; tinjau kembali pada D03. A03 tidak berarti optimasi tersebut sudah lolos uji.
- [x] **A04 — Audit integrasi data antarmenu.** Selesai 24 Oktober 2026, APK 1.6.3: [bukti native dan batas](android/validation/integration-a04-2026-10-24/README.md). Lokasi/zona bersama, hari kalender/checklist/tenggat diselaraskan; key bacaan Mushaf dan label Home/katalog diperbaiki. Enam metode native lulus, termasuk verifikasi dari proses berbeda sesudah force-stop; fixture tes dibersihkan, produksi tidak diberi data contoh. Unit 97 lulus/3 dilewati; lint tanpa error. Presisi sensor/alarm/seluruh qari serta HP fisik tetap D/E/G, bukan dianggap selesai oleh A04.

## B — Lengkapi Hadis dan Doa

- [ ] **B01 — Pilih dan validasi sumber Hadis lengkap.** Periksa sumber, hak penggunaan, cakupan Bukhari/Muslim, Arab/Indonesia, penomoran dan cara pembaruan. Hasilnya catatan sumber + contoh respons nyata. Jangan menyebut tujuh kutipan sebagai kitab lengkap. **Aktif 24 Oktober:** [pemeriksaan JSON/source pada commit tetap](android/validation/hadith-source-2026-10-24/README.md) mengonfirmasi 11.568 entri, celah nomor dan perbedaan semantik range/nomor API. Asal edisi/terjemahan, ketentuan konten dan kelengkapan belum ditutup; belum sumber produksi/izin impor distribusi. **Lanjutan:** [8 sampel Arab ditelusuri ke pembanding, perbedaan nomor dan muqaddimah dijelaskan](android/validation/source-review-b-2026-10-24/README.md). [Email klarifikasi terkirim 24 Oktober dengan izin eksplisit pengguna](android/validation/b-final-review-2026-10-24/README.md); pemeriksaan thread terakhir 24 Oktober belum menemukan balasan masuk. Kejelasan edisi/asal terjemahan/ketentuan konten belum terkonfirmasi; B01 tetap terbuka. Pekerjaan independen berikutnya: C01, lihat [handoff aktif](SESSION_HANDOFF.md).
- [x] **B02 — Implementasikan koleksi dan pencarian Hadis.** Daftar dapat memuat bertahap, memilih kitab dan mencari nomor/kata. Definisikan dengan jelas pencarian seluruh koleksi atau hanya data unduhan. Loading, tidak ditemukan, gagal, coba lagi dan data luring harus benar. **Selesai teknis 24 Oktober:** [paket asli, paging 20, nomor/kata/Arab, scope lokal, batal/korupsi dan status](android/validation/content-b-2026-10-24/README.md). Sumber masih kandidat B01; ini bukan izin distribusi atau klaim kitab lengkap.
- [x] **B03 — Pertahankan bacaan dan bookmark Hadis.** Gunakan ID sumber stabil; bookmark/resume tetap benar setelah refresh/pembaruan dataset. Reader menampilkan Arab, terjemahan, rujukan dan ukuran teks; salin/bagikan tetap membawa sumber yang tepat. **Selesai teknis 24 Oktober:** [ID stabil, reader sumber asli, simpan/hapus paket/unduh ulang](android/validation/content-b-2026-10-24/README.md). ID tujuh kutipan lama dipertahankan; namespace edisi tidak dicampur.
- [x] **B04 — Lengkapi Doa Harian dengan sumber yang jelas.** Selesai **25 Oktober 2026**, source `b3f13f3`, beta 1.7.6: [bukti dan batas](android/validation/dua-completion-b04-2026-10-24/README.md), [catatan per entri](android/validation/dua-completion-b04-2026-10-24/per-entry-review.json). Kategori belajar/makan/tidur/rumah/perjalanan/hujan/guruh/pagi/petang diwakili 14 pilihan luring; koleksi tambahan tetap 225 setelah unduh. Arab/petikan, arti Indonesia, judul dan konteks rujukan ditelaah secara editorial; 33 entri mendapat koreksi/catatan, raw/ID dipertahankan. Manifest seluruh 225 snapshot menandai data baru/berubah Belum ditinjau dan menolak koreksi lama. Pencarian mengenali lafal/arti hasil koreksi; 10 tes Doa dan 2 metode native lulus, termasuk 21 entri terdampak, kelompok/paging/cache dan rollback respons rusak. TTS dilabeli Android; keluaran suara/HP belum diklaim diuji. Ini bukan penilaian sahih baru atau sertifikasi seluruh harakat; B01/G03 tetap terpisah. APK 31.319.928 byte, di bawah 100 MB; emulator ditutup.

## C — Backend dan AI online

- [x] **C01 — Siapkan konfigurasi server produksi.** Selesai persiapan konfigurasi **25 Oktober 2026**: server dapat dijalankan ulang dengan konfigurasi privat, model yang tersedia dan health check. Provider key hanya di server; nilai key/token saat ini tidak ditemukan pada APK, file Git yang diperiksa atau log uji. [Audit C01](android/validation/backend-c01-2026-10-25/README.md) dan [lanjutan 1.8.0](android/validation/assistant-c-2026-10-25/README.md) membuktikan completion/proposal/Tadabbur provider nyata. **Keputusan terbaru pengguna: gunakan key yang sudah ada; syarat rotasi dibatalkan**, menggantikan keputusan menunda sebelumnya. [Keputusan dan verifikasi penutupan](android/validation/assistant-c-2026-10-25/key-decision-2026-10-25.md): dua restart/health/akses tanpa token lulus. Ini penutupan persiapan konfigurasi; hosting nyata dan HP tetap C02/C05, bukan rilis publik.
- [ ] **C02 — Hosting HTTPS dan akses untuk distribusi.** Setelah pilihan hosting/domain ditetapkan, pasang layanan, pembatasan penggunaan dan autentikasi per pengguna/instalasi yang dapat dicabut. Kode gateway bersama hanya untuk uji pribadi. Verifikasi server tetap bekerja saat laptop pengembang mati. **25 Oktober, keputusan terbaru:** pengguna memilih Vercel. Backend source `9438aa4` sudah dihosting pada `https://zikra-ai.vercel.app`, Vercel Hobby + Redis Upstash Free tanpa kartu; auto upgrade/eviction/prodPack terkonfirmasi mati. [Bukti HTTPS/provider dan runbook](android/validation/backend-vercel-2026-10-25/README.md): health 200, tanpa token/dicabut 401, provider nyata 200, kuota 429, counter/revokasi bertahan redeploy dan storage korup 503. Kuota/lease atomik lintas fungsi memakai Redis; batas satu proses hanya untuk paket file/VPS alternatif. **Tetap terbuka:** uji HP saat laptop mati, pemantauan, backup/retensi platform serta cancellation/timeout nyata belum lengkap; deployment berhasil bukan seluruh C/rilis publik selesai.
- [ ] **C03 — Hubungkan APK ke AI online dengan alur yang jelas.** Pilihan online/offline, izin pengiriman pesan, loading, pembatalan, timeout, kuota habis dan coba lagi dapat dipahami pengguna. Kegagalan jaringan tidak diberi label seolah-olah jawaban AI online berhasil. **1.8.0:** preview per request dengan default hanya pesan saat ini, batal/retry/label online dan error spesifik [lulus native dengan respons terkendali](android/validation/assistant-c-2026-10-25/README.md). [Endpoint HTTPS nyata tersedia](android/validation/backend-vercel-2026-10-25/README.md); rantai APK → hosting → provider dan kondisi gagal pada HP belum dibuktikan, tetap terbuka bersama C05.
- [ ] **C04 — Konteks dan tindakan AI sesuai data asli.** Sesuai cakupan A02, kirim hanya konteks yang dipilih/diperlukan; nama, jadwal, tugas dan waktu salat berasal dari data aplikasi. Usulan penambahan/edit tugas ditinjau pengguna dan tersimpan sekali, tanpa penyelesaian/progres rekaan. Tafsir/hadis tidak dihasilkan sebagai kutipan tanpa rujukan terverifikasi. **1.8.0:** opt-in nama/tugas/tenggat/perhitungan salat, receipt SQLite v4 atomik, review/simpan/double-tap/restart/stale/completed serta Tadabbur EQuran bersumber [dibuktikan terarah](android/validation/assistant-c-2026-10-25/README.md). Proposal dan ringkasan provider privat nyata 200. Konteks jadwal kelas belum tersedia sebelum D04; integrasi end-to-end setelah hosting belum dibuktikan, jangan dicentang penuh.
- [ ] **C05 — Uji percakapan nyata dari HP.** APK → hosting HTTPS → Featherless → balasan tampil, beserta kondisi token salah, putus koneksi dan kuota. Simpan hasil yang sudah disamarkan; respons provider simulasi tidak cukup untuk mencentang item ini. **25 Oktober:** [hosting Vercel → Featherless nyata 200](android/validation/backend-vercel-2026-10-25/https-live.json), akses/kuota/redeploy diperiksa lewat probe komputer. Belum APK/HP dan laptop mati; tidak dicentang.
- [ ] **C06 — Input suara sesuai keputusan rilis; Vision tercatat sebagai fase berikutnya.** [A02](RELEASE_SCOPE.md) menetapkan dikte suara wajib dengan fallback ketika recognizer/izin tidak tersedia. Vision F5.4 ditunda setelah rilis pertama, bukan sudah selesai. **1.8.0:** penjelasan layanan suara, hasil tetap draf, batal/hasil salah/batas teks [diuji native](android/validation/assistant-c-2026-10-25/README.md). Recognizer terdeteksi tetapi callback disimulasikan; mikrofon/dikte nyata belum diuji, checkbox tetap terbuka. Saat Vision dibuka kembali, buktikan model, foto nyata, izin/ukuran/gagal/pembatalan; jangan memberi kemampuan scan kepada model teks saja.

## D — Stabilkan fitur inti

- [ ] **D01 — Lokasi, jadwal salat dan Kiblat.** Verifikasi metode jadwal terhadap sumber/metode yang dinyatakan, tanggal/zona dan perubahan kota. GPS ditolak tetap bisa memilih kota. Uji sensor Kiblat pada HP dengan petunjuk kalibrasi dan keadaan sensor tidak tersedia; jangan menampilkan arah palsu.
- [ ] **D02 — Pengingat salat dan adzan penuh.** Sediakan pilihan nada/adzan/getar per salat sesuai cakupan, sumber audio berizin, hentikan suara dan tangani audio focus. Uji izin ditolak, layar mati, reboot, perubahan kota/waktu serta interaksi murottal dan timer. Jeda fokus saat adzan mengikuti keputusan cakupan; alarm bergantung batasan Android yang dijelaskan dengan benar.
- [ ] **D02a — Pilih cara pengingat ketika masuk waktu salat (tambahan pengguna 25 Oktober).** Sediakan sakelar utama serta pilihan per salat: **adzan penuh / notifikasi biasa tanpa adzan / getar saja / mati**. Pilihan disimpan dan tetap berlaku setelah aplikasi ditutup, reboot, perubahan lokasi/zona serta pembaruan. Saat master mati tidak ada suara/notifikasi; mode notifikasi biasa tidak boleh memutar adzan. Mode adzan punya tombol berhenti dan tunduk izin/audio focus/senyap. Jadwal lokal tidak memerlukan backend AI. Ini rincian tugas D02, **belum selesai**; baseline hanya memiliki pengingat notifikasi biasa dan pilihan aktif/nonaktif per salat.
- [ ] **D03 — Quran, Mushaf, audio dan gulir yang serasi.** Audit teks/pemetaan 114 surah serta metadata qari/timing; uji rekaman asli, seek, repeat, pindah ayat/halaman, background dan sentuhan manual. Warna kata hanya saat timing cocok, gulir mandiri berbeda dari mengikuti audio. Unduhan/luring dan berkas rusak pulih tanpa menghapus bookmark; keterbatasan timing/font disebutkan.
- [ ] **D04 — Study, jadwal dan pengingat akademik.** Periksa tambah/edit/selesai/hapus tugas, tenggat/prioritas, kalender, jadwal mingguan dan alarm kelas sesuai PRD/cakupan. Timer fokus/break/pause pulih dengan benar setelah background/recreation; tidak menandai tugas selesai sendiri. Statistik harian memakai catatan sebenarnya.
- [ ] **D05 — Diary Hifz, target baca dan jurnal ibadah.** Periksa simpan/edit target, rentang ayat menuju reader, catatan latihan, jurnal/streak serta target khatam sesuai cakupan. Selesaikan ketergantungan yang belum ada; progres berubah hanya berdasarkan tindakan/catatan nyata dan konsisten di Home/Profil.
- [ ] **D06 — Penyimpanan dan migrasi.** Memasang pembaruan di atas versi sebelumnya mempertahankan tugas, target, bookmark, nama, lokasi dan pengaturan. Jika akun/sinkronisasi dipilih pada A02, tambahkan pekerjaan rinci identitas, konflik data, pindah HP dan penghapusan akun sebelum menyatakan ini selesai; penyimpanan lokal bukan sinkronisasi.

## E — Protector dan koneksi data

- [ ] **E01 — Verifikasi laporan gempa dan peta.** Pin, magnitudo, kedalaman, wilayah, waktu dan Shakemap berasal dari laporan yang sama. Periksa urutan koordinat, gempa di luar kota pilihan, kegagalan tile/gambar dan data lama; tampilkan sumber serta waktu pembaruan. Laporan kejadian bukan prediksi gempa.
- [ ] **E02 — Notifikasi gempa saat aplikasi ditutup.** Jika masuk cakupan rilis, server memantau feed resmi dengan cache/jeda yang patuh batas sumber, menyimpan ID kejadian untuk mencegah kirim ulang, lalu mengirim push. HP membuka detail kejadian yang dipilih, bukan laporan terbaru yang berbeda. Uji izin ditolak, duplikat, server restart dan keterlambatan; jangan menjanjikan peringatan pasti sebelum guncangan.
- [ ] **E03 — Cuaca dan pengingat kontekstual.** Kondisi/animasi mengikuti prakiraan untuk lokasi dan waktu yang benar; bedakan prakiraan dari pengukuran langsung. Sesuai cakupan, selesaikan alert cuaca, doa perjalanan, pagi/petang dan morning briefing dengan dependensi jadwal/lokasi serta kontrol pengguna; hindari notifikasi berulang.
- [ ] **E04 — Bantuan darurat dan masjid terdekat.** Tautan/panggilan/arah dapat digunakan, izin lokasi ditolak punya fallback, hasil kosong/gagal jelas, data komunitas tidak dianggap lengkap. Pesan keluarga/panggilan tetap melalui tindakan pengguna. Nomor/tautan bantuan dan atribusi diperiksa.

## F — Satu bahasa desain, bukan hiasan tambahan

- [x] **UI01 — Perbaiki daftar Doa yang terlalu membulat/generik.** Selesai 24 Oktober 2026, APK 1.6.2: satu daftar dengan divider, ikon sesuai isi, kategori/rujukan, warna bacaan terbuka dan Arab rata kanan. Build/lint serta tes buka/cari terarah lulus; [screenshot dan batas](android/validation/dua-list-2026-10-24/README.md). Dataset/API belum lengkap; F02/B01–B04 tetap terbuka.

- [x] **UI02 — Rapikan desain Doa setelah preview pengguna.** Selesai 24 Oktober 2026, APK 1.7.3: [screenshot asli, pemeriksaan dan batas](android/validation/dua-refined-2026-10-24/README.md). Tombol koleksi menyatu dengan halaman, kategori/ikon/ruang baca diperjelas, Arab/Latin/arti dipisah dan detail sumber dapat dilipat pada kedua koleksi. Dua metode native / tiga eksekusi (normal dan font 200%) lulus; build/lint tanpa error, emulator ditutup. Data 14/225 tidak diubah; bukan penyelesaian kurasi B01/B04 atau seluruh F03.

- [x] **UI03 — Rapikan pustaka dan reader Hadis setelah keluhan desain pengguna.** Selesai 24 Oktober 2026, APK 1.7.4: [screenshot asli, tes dan batas](android/validation/hadith-refined-2026-10-24/README.md). Kartu kitab lebih ringkas/adaptif, pencarian di atas, daftar terpadu, sumber di atas Arab dan Aa pada toolbar. Satu metode native pada font 100%/200% lulus (pilih/cari/baca/ukuran/salin/bookmark/resume); build/lint tanpa error, emulator ditutup. Teks/ID dan 14/225 Doa tetap. B01/B04/F03 tidak ditutup; footer Bagikan dan label navigasi masih membungkus pada font 200%.

- [ ] **F01 — Bekukan panduan visual berdasarkan referensi yang diterima.** Catat font, ukuran/jarak, ikon, warna, radius dan keluarga ilustrasi per konteks; bedakan variasi fungsi yang disengaja dari gaya yang bertabrakan. Pertahankan logo dan referensi yang sudah disetujui. Hasil: dokumen acuan + daftar layar yang perlu diselaraskan.
- [ ] **F02 — Selaraskan seluruh layar dan teks.** Periksa Home, Quran/reader/Mushaf, Study/fokus/break/Hifz, AI, Protector/peta, Hadis/reader, Doa, Profil, salat, Kiblat, lokasi/masjid, kalender dan pengaturan. Rapikan ruang kosong, kartu berulang, orb/dekorasi generik serta kalimat template. Gunakan isi nyata, bukan statistik contoh.
- [ ] **F03 — Ukuran layar, teks besar dan aksesibilitas.** Kontrol tetap terjangkau, target sentuh memadai, Arab/Indonesia tidak terpotong, kontras dan label aksesibel. Periksa font besar, tema yang tersedia, keyboard, dialog, loading/kosong/gagal dan scroll. Simpan screenshot native pada ukuran yang diuji.
- [ ] **F04 — Gerak dan performa visual.** Awan/celestial, orb dan karakter fokus/break berhenti saat layar tidak aktif serta menghormati pengaturan animasi/penghemat daya. Ukur penggunaan memori/baterai di HP; jangan mempertahankan animasi terus berjalan untuk dekorasi tersembunyi.

## G — Pembuktian sebelum rilis

- [ ] **G01 — Uji alur pengguna lintas fitur.** Instal baru → onboarding/lokasi → Home → baca/putar Quran → Study/tugas → pengingat → Protector → Hadis/Doa → Profil → tutup/buka ulang. Uji juga pembaruan dari APK lama. Catat hasil per alur dan perbaiki bug sebelum mengulang bagian yang terdampak.
- [ ] **G02 — Uji HP fisik dan kegagalan nyata.** Gunakan HP ketika pengguna tersedia: layar mati/background, izin ditolak, penghemat daya, headset/telepon, sinyal putus/lambat dan penyimpanan terbatas. Matriks perangkat/versi Android dan batas yang belum diuji harus tertulis; emulator saja tidak mencentang ini.
- [ ] **G03 — Tinjau layanan dan data untuk penggunaan publik.** Backend memiliki batas penggunaan, penanganan kegagalan dan pemantauan minimum tanpa log sensitif; sumber Hadis/Quran/audio/peta/cuaca memiliki atribusi dan penggunaan yang sesuai. Nyatakan dengan jelas fitur luring, server, serta data yang dikirim/disimpan.
- [ ] **G04 — Siapkan paket distribusi yang dapat dilacak.** Build release bertanda tangan/AAB bila diperlukan, versi baru, hash, catatan perubahan dan instruksi pembaruan. Verifikasi unduhan awal setiap versi **di bawah 100 MB** sesuai batas ukuran di atas. Kunci signing disimpan privat. Bukti source/build cocok, tidak ada secret/fixture pengguna pada paket, dan informasi privasi/dukungan sesuai fitur aktual.
- [ ] **G05 — Gerbang rilis publik.** Semua item wajib cakupan A02 memenuhi syarat; tidak ada bug yang menghalangi alur inti atau merusak data; layanan produksi dan HP telah diperiksa; keterbatasan tertulis dan hasil beta ditindaklanjuti. Sebelum ini, sebut build sebagai beta, bukan seluruh proyek 100% selesai.

## Jejak kebutuhan PRD agar tidak ada fitur yang terlupakan

Pemetaan ini adalah tujuan pekerjaan, **bukan klaim fitur sudah selesai**. Hasil A01 ada pada [matriks bukti dan kesenjangan](android/validation/release-audit/FEATURE_MATRIX.md).

| Kebutuhan PRD | Pekerjaan penutup |
| --- | --- |
| F1.1 lokasi/jadwal/metode | A04, D01 |
| F1.2 notifikasi/adzan | D02, G02 |
| F1.3 jurnal/streak | D05 |
| F1.4 Kiblat | D01 |
| F2.1 teks/terjemahan/transliterasi/Mushaf/luring | D03 |
| F2.2 murottal/download/repeat | A03, D03 |
| F2.3 bookmark/bacaan terakhir | A04, D03 |
| F2.4 target khatam | D05 |
| F2.5 tadabbur/tafsir | C04; sumber terverifikasi |
| F3.1 jenjang pendidikan | A01, D04, F02 |
| F3.2 jadwal mingguan | D04 |
| F3.3 alarm kelas | D04, G02 |
| F3.4 tugas/deadline/prioritas | D04 |
| F3.5 morning briefing | D04, E03 |
| F4.1 gempa/push/panduan | E01, E02, E04 |
| F4.2 cuaca/doa kontekstual | B04, E03 |
| F4.3 doa perjalanan sesuai jadwal | B04, D04, E03 |
| F4.4 dzikir pagi/petang | B04, E03 |
| F5.1 persona AI | C03, F02 |
| F5.2 konteks pengguna | C04 |
| F5.3 aksi melalui chat | C04 |
| F5.4 Vision/scan PR | A02, C06 |
| F5.5 input suara | C06 |
| Tambahan chat: Hadis lengkap dan desain baru | B01–B03, F01–F03 |
| Tambahan chat: parallax, fase bulan dan gerak Study | BASE06–BASE07, F04; batas dekoratif tetap jujur |

PRD lama menyebut Expo/React Native, MongoDB dan provider AI lain. Implementasi aktif tetap **Android Java/XML + SQLite lokal + gateway Node/Featherless**. Rencana ini tidak memerintahkan migrasi framework atau database hanya untuk menyamai stack dokumen lama.

Fitur post-MVP yang memang tercantum terpisah di PRD—XP/badge, keuangan anak kos, dan Ramadan mode—tetap backlog; jangan mengerjakannya sebelum cakupan rilis inti jelas. Pomodoro sudah ada, tetapi integrasi jeda karena adzan masih harus ditutup pada D02 bila termasuk cakupan.

## Jurnal penyelesaian dan keputusan

| Tanggal | ID | Hasil/keputusan | Bukti/checkpoint |
| --- | --- | --- | --- |
| 24 Okt 2026 | BASE01–BASE04 | Halaman harian diterapkan; data konten masih terbatas | 1.6.0 `4729730`, 1.6.1 `7d45b57`; tautan bukti di baseline |
| 24 Okt 2026 | PLAN | Rencana aktif, urutan dependensi, syarat selesai dan jejak PRD disimpan | Dokumen ini; belum mencentang pekerjaan A–G |
| 24 Okt 2026 | UI01 | Permintaan koreksi daftar Doa diprioritaskan; layout diperbaiki dan diperiksa. Distribusi konten dicatat sebagai usulan, bukan integrasi selesai. | APK 1.6.2; [bukti native](android/validation/dua-list-2026-10-24/README.md), [rencana konten](design/CONTENT_DELIVERY_PLAN.md) |
| 24 Okt 2026 | SIZE / G04 | Batas unduhan awal setiap versi ditetapkan pengguna: <100 MB. APK 1.6.2 diukur 31,2 MB; belum klaim ukuran semua fitur mendatang atau penggunaan RAM HP. | [Baseline ukuran](android/validation/apk-size-2026-10-24.md) |
| 24 Okt 2026 | A01 | Audit 23 kebutuhan PRD dan 21 layar selesai; fitur parsial/tidak ada dibedakan dari bukti terarah. APK tetap 4 Doa/7 kutipan; kandidat 227 Doa/dzikir + 11.568 entri dua koleksi belum diimpor/disahkan lengkap. Berikutnya A02, tanpa menghapus kebutuhan PRD diam-diam. | [Matriks dan batas](android/validation/release-audit/FEATURE_MATRIX.md), [inventaris](android/validation/release-audit/SOURCE_INVENTORY.json), [metadata hitungan](android/validation/release-audit/CONTENT_COUNTS.json); baseline `5884b52`, tanpa emulator/build baru |
| 24 Okt 2026 | A02 | Pengguna mendelegasikan pilihan cakupan. Modul inti diselesaikan untuk publik; akun/sinkronisasi dan Vision setelah rilis pertama. Beta hanya tahap uji; AI teks/tindakan, briefing dan push tetap wajib publik. Berikutnya B01, lalu konten; A03–A04 sebelum D. | [Cakupan wajib/bersyarat/ditunda dan gerbang selesai](RELEASE_SCOPE.md); keputusan dokumentasi, APK tetap 1.6.2 |
| 24 Okt 2026 | B01 — checkpoint, belum selesai | Dua JSON dipatok commit/hash dan diperiksa bentuk/nomor. Range API memakai posisi baris, bukan selalu nomor sumber. Kandidat belum final; bukti edisi/asal terjemahan serta penggunaan masih dicari. | [Laporan sumber dan langkah berikutnya](android/validation/hadith-source-2026-10-24/README.md); tanpa impor APK/server atau emulator |
| 24 Okt 2026 | A03 / koreksi urutan | Pengguna meminta perhatian keamanan serta A03–A04 sebelum B01. WIP audio dipertahankan privat, baseline stabil cocok dengan APK; berikutnya A04. Security diterapkan selama pengerjaan; C01/C02/G03 tetap gerbang produksi. | [Baseline, cadangan dan batas keamanan](android/validation/baseline-a03-2026-10-24/README.md) |
| 24 Okt 2026 | A04 | Zona/tanggal harian, tenggat dan bacaan terakhir antarmenu diselaraskan; data terbukti tetap ada setelah force-stop/proses berbeda. A03–A04 selesai sebelum kembali B01. APK 1.6.3 31,1 MB; emulator sudah ditutup. | Source `2b291ff`; [6 tes native, build dan batas](android/validation/integration-a04-2026-10-24/README.md). Keamanan server produksi/HP tetap terbuka |

| 24 Oktober 2026 | B02/B03; progres B01/B04 | Adaptor kandidat Hadis 11.568 entri opsional, pencarian/paging, ID dan bookmark luring diuji; koleksi Doa 225 bacaan tambahan dengan rollback atomik. B01/B04 tetap terbuka sesuai batas sumber/kurasi. | [Bukti beta 1.7.0](android/validation/content-b-2026-10-24/README.md); Hadis 1 metode native + konten tambahan 2 metode, unit/lint pada release.json; source `fcaf20f` |

| 24 Oktober 2026 | B01/B04 — validasi lanjutan | Delapan sampel Hadis menunjukkan perbedaan skema nomor; muqaddimah diberi catatan. Lima penanganan Doa terikat snapshot sumber, termasuk satu harakat, konteks angin, penomoran dan atribusi penilaian. Kedua ID tetap terbuka; bukan penutupan semua konten. | [Laporan dan build 1.7.1](android/validation/source-review-b-2026-10-24/README.md), source `3ef14dc`, tanpa emulator/email |

| 24 Oktober 2026 | B04 — kategori terisi, review belum selesai | Empat pilihan bawaan diperluas menjadi 14 termasuk pagi/petang/guruh; Arab/Latin/arti, sumber dan kategori luring. Inventaris 227 ID memisahkan field lengkap dari penelaahan isi; 213 entri tambahan belum ditelaah. B01/B04 tetap terbuka; tanpa emulator atau email. | [Bukti beta 1.7.2](android/validation/dua-selection-b04-2026-10-24/README.md), source `91c8739`, APK 31,3 MB, 110 unit lulus/3 dilewati |

| 24 Oktober 2026 | UI02 | Permintaan pengguna memperbaiki desain Doa diprioritaskan. Navigasi koleksi, hierarki bacaan dan sumber lipat diterapkan pada APK; dataset tetap. | [Bukti native 1.7.3](android/validation/dua-refined-2026-10-24/README.md), source `3c577ea`, APK 31,1 MB, 2 metode/3 eksekusi, tanpa uji HP |

| 24 Oktober 2026 | UI03 | Permintaan pengguna memperbaiki Hadis diprioritaskan; katalog/reader dirapikan, Arab rata kanan dan ukuran via Aa. Doa 14/225 tetap, pemeriksaan sumber tidak disamakan dengan tes UI. | [Bukti 1.7.4](android/validation/hadith-refined-2026-10-24/README.md), source `0c9827d`, APK 31,3 MB, 1 metode/2 eksekusi, tanpa HP |

| 24 Oktober 2026 | B01/B04 lanjutan | Halaman sumber/kebijakan TafsirQ diperiksa, edisi/penerjemah belum jelas. Tiga ID Doa tambahan 2/3/5 dibandingkan; ditemukan variasi lafal/penilaian dan nomor. Total 15 perbandingan awal, 210 belum dibandingkan; bukan tashih penuh. | [Antrean dan catatan](android/validation/b-final-review-2026-10-24/README.md); tanpa perubahan APK/email |

| 24 Oktober 2026 | B04 editorial/koreksi tampilan | Petunjuk Indonesia dipisahkan dari Arab pada empat ID; kurung ID 225 dirapikan, raw/ID tetap dan koreksi terikat fingerprint. Pemeriksaan editorial berbeda dari validasi rujukan penuh; B01/B04 tetap terbuka. | [Bukti dan temuan per entri](android/validation/b-final-review-2026-10-24/README.md), tiga tes snapshot lulus tanpa skip, build/lint lulus; belum APK distribusi/HP |

| 24 Oktober 2026 | B01/B04 — klarifikasi terkirim, belum selesai | Pengguna mengizinkan email; pertanyaan sumber terkirim melalui Gmail, belum dianggap persetujuan. Empat judul Doa dirapikan; 78 calon pembanding lafal Hisnul Muslim ditemukan dengan normalisasi, bukan 78 validasi penuh. | [Bukti 1.7.5 dan batas](android/validation/b-final-review-2026-10-24/README.md), 3 tes snapshot/4 tes alat lulus, build/lint lulus, tanpa emulator/HP |

| 25 Oktober 2026 | B04 — selesai editorial/integrasi | 14 pilihan bawaan dipertahankan; 225 tambahan ditelaah editorial, 33 penanganan. Manifest menandai sumber baru/berubah; pencarian nilai hasil koreksi, raw dan rollback cache diperiksa. B01 tetap menunggu jawaban, bukan seluruh B/rilis publik sudah disahkan. | Source `b3f13f3`, beta 1.7.6, [10 unit/2 native/screenshot/hash dan batas](android/validation/dua-completion-b04-2026-10-24/README.md); emulator ditutup, tanpa HP |

| 25 Oktober 2026 | C01 — persiapan lokal, belum selesai | Gateway lama diaudit: validasi env/CLI, error client/provider dipisahkan, runbook/template restart; 20 tes lulus, proses CLI privat dapat diulang, completion provider nyata 200. Pengguna mengonfirmasi key belum dirotasi; checkbox tetap terbuka, C02/HP/publik belum dikerjakan. WIP/stash/env tetap; APK 1.7.6 tidak berubah dan <100 MB. | [Laporan, receipt live/restart, TAP, hash source dan pemindaian](android/validation/backend-c01-2026-10-25/README.md); base `83312da`, checkpoint kode bersama dokumen ini |

| 25 Oktober 2026 | C01–C06 — kode dan paket, gerbang eksternal terbuka | Pengguna meminta seluruh C dilanjutkan; rotasi ditunda sampai sebelum publik dan hosting belum ada, pilih paket deploy dahulu. Token instalasi/revokasi/kuota persisten, preview konteks, batal/retry/error, review tugas atomik/sekali, Tadabbur bersumber dan draf suara disiapkan. 26 tes Node, 114 unit lulus/4 skip, 6 native AI + 11 migrasi dan 2 native font 200% lulus. Provider privat memberi proposal/Tadabbur 200. C tetap terbuka untuk rotasi, hosting/HP, jadwal kelas D04 dan dikte nyata; bukan seluruh C/rilis publik selesai. | [Bukti/source/hash 1.8.0 beta](android/validation/assistant-c-2026-10-25/README.md), 31.362.014 byte; lint 0 error/716 warning, emulator ditutup; parent `20c21fc` |
| 25 Oktober 2026 | D02a — tambahan tugas, belum selesai | Permintaan pengingat saat masuk salat diperjelas: sakelar utama + adzan penuh / notifikasi biasa tanpa adzan / getar / mati per salat. D02a merinci D02, bukan menganggap adzan sudah tersedia pada APK 1.8.0. | Kriteria D02a di atas dan [cakupan salat](RELEASE_SCOPE.md); implementasi/uji audio/alarm/HP tetap terbuka |

| 25 Oktober 2026 | C01 — selesai persiapan konfigurasi, keputusan diperbarui | Pengguna membatalkan syarat rotasi dan memilih memakai key yang sudah ada. Validasi konfigurasi dan dua restart ulang lulus: health 200, tanpa token 401, proses berhenti tanpa secret di log; tidak mengulang request provider. Bukti completion dan pemindaian sebelumnya tetap berlaku. Key tidak dirotasi; hosting/HP, seluruh C dan publik belum selesai. | [Keputusan terbaru, bukti penutupan dan batas](android/validation/assistant-c-2026-10-25/key-decision-2026-10-25.md); riwayat keputusan sebelumnya tetap sebagai catatan historis |

| 25 Oktober 2026 | C02 — Vercel terpasang, pemeriksaan distribusi tersisa | Pengguna memilih Vercel; menyelesaikan login/ketentuan Upstash. Proyek Hobby + Redis Free tersedia, auto upgrade mati. 34 tes backend/lokal Lua lulus; build dua fungsi Node 24, Redis nyata, HTTPS health 200/provider 200, token 401, kuota 429 dan revokasi/counter bertahan redeploy. WIP/env/stash/APK tetap. C02/C05 tetap terbuka untuk HP/laptop mati dan operasional, bukan seluruh C/publik selesai. | Source backend `9438aa4`; [deployment, receipt uji, paket dan preservasi](android/validation/backend-vercel-2026-10-25/README.md); APK tetap 1.8.0 beta/31.362.014 byte dari source `7982e10` |

Format baris berikutnya: `tanggal | ID | hasil singkat + batas yang tersisa | tautan laporan/screenshot/tes + commit fitur`. Keputusan menunda/mengubah cakupan juga dicatat di tabel ini.
