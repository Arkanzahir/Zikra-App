# Rencana implementasi Zikra menuju rilis publik

Dibuat/diperbarui: **24 Oktober 2026 (WIB)**. Baseline fitur terbaru: **1.6.2 / code 16** (UI01). Checkpoint sebelumnya: 1.6.1, commit `7d45b57`.

Dokumen ini menjadi **checklist pengerjaan aktif**. [PRD](ZIKRA_PRD.md) tetap menjadi daftar kebutuhan produk; [rencana lama](design/IMPROVEMENT_PLAN.md) dan laporan validasi adalah riwayat. Jika kebutuhan lama berbeda dengan keputusan terbaru pengguna, catat keputusan beserta alasannya di sini.

**Pekerjaan aktif berikutnya: B01 — validasi dan pilih sumber Hadis.** A02 selesai berdasarkan delegasi pengguna 24 Oktober: [cakupan beta/publik dan penundaan eksplisit](RELEASE_SCOPE.md). Akun/sinkronisasi serta Vision ditunda; AI teks/tindakan tugas, briefing, adzan penuh, jadwal kelas dan push gempa tetap wajib untuk publik. A03–A04 wajib ditutup sebelum tahap D. A01: [matriks 23 kebutuhan PRD dan 21 layar](android/validation/release-audit/FEATURE_MATRIX.md). B01–B04 belum selesai; kandidat 227 Doa/dzikir dan 11.568 entri dua koleksi belum konten tervalidasi/diimpor.

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
| B — Hadis dan Doa | Isi berlandaskan sumber, pencarian berguna, penyimpanan andal | A01–A02 |
| C — Backend dan AI | AI online dapat dipakai dari HP tanpa laptop pengembang | A02; keputusan hosting saat konfigurasi siap |
| D — Fitur inti | Quran, salat, Study/Hifz dan lokasi terhubung dengan benar | A03–A04; tiap modul diperiksa bergiliran |
| E — Protector | Data gempa/cuaca benar; notifikasi sesuai lingkup rilis | C02 untuk push; D01/D04 untuk konteks jadwal |
| F — Konsistensi desain | Satu bahasa visual dan alur yang nyaman pada semua menu | B/D/E stabil pada layar terkait |
| G — Uji dan pengemasan rilis | APK/AAB teruji sesuai cakupan yang disepakati | Semua item wajib tahap A–F selesai |

Jangan menjalankan seluruh tahap sekaligus. Pemeriksaan ringan dan kerapian dasar dilakukan dalam setiap pekerjaan; tahap F khusus penyelarasan menyeluruh, bukan alasan menunda perbaikan layout yang menghalangi penggunaan.

## A — Audit dan batas rilis

- [x] **A01 — Inventaris fitur dan selisih terhadap PRD.** Selesai 24 Oktober 2026: [matriks aktual/bukti/gap/ID lanjutan](android/validation/release-audit/FEATURE_MATRIX.md), [inventaris source baseline](android/validation/release-audit/SOURCE_INVENTORY.json) dan [hitungan kandidat konten](android/validation/release-audit/CONTENT_COUNTS.json). Seluruh 23 kebutuhan inti dan 21 layar diperiksa; status tidak ada/sebagian/tersedia/teruji terarah dibedakan. Tidak ada sertifikasi teruji rilis. Audit dokumen/source memakai laporan runtime terdahulu, tanpa build/emulator baru; A02–G05 tetap terbuka sesuai kriterianya.
- [x] **A02 — Tetapkan cakupan beta dan rilis publik.** Selesai 24 Oktober 2026 berdasarkan delegasi pengguna untuk menentukan cakupan: [keputusan tersimpan](RELEASE_SCOPE.md). Beta adalah tahap uji; publik tetap mewajibkan modul inti, Hadis/Doa tervalidasi, AI teks/konteks/tindakan tugas, briefing, adzan, jadwal kelas dan push. Akun/sinkronisasi serta Vision ditunda eksplisit ke setelah rilis pertama; source Infaq dipertahankan di luar navigasi rilis. Tidak ada fitur runtime dianggap selesai karena keputusan ini.
- [ ] **A03 — Rapikan baseline source dan pekerjaan audio tertunda.** Tinjau perubahan service/timing/MushafReaderTest/tool audit yang masih lokal; putuskan gabung setelah diperiksa atau simpan terpisah. Source commit, versi APK dan bukti harus selaras tanpa menghilangkan WIP.
- [ ] **A04 — Audit integrasi data antarmenu.** Satu pilihan lokasi/zona dipakai Home, salat, cuaca, Kiblat dan masjid; bacaan terakhir sama pada Home/katalog/reader; tugas/progres konsisten di Study/Profil. Uji pulih setelah aplikasi dibuka ulang, tanpa data contoh.

## B — Lengkapi Hadis dan Doa

- [ ] **B01 — Pilih dan validasi sumber Hadis lengkap.** Periksa sumber, hak penggunaan, cakupan Bukhari/Muslim, Arab/Indonesia, penomoran dan cara pembaruan. Hasilnya catatan sumber + contoh respons nyata. Jangan menyebut tujuh kutipan sebagai kitab lengkap.
- [ ] **B02 — Implementasikan koleksi dan pencarian Hadis.** Daftar dapat memuat bertahap, memilih kitab dan mencari nomor/kata. Definisikan dengan jelas pencarian seluruh koleksi atau hanya data unduhan. Loading, tidak ditemukan, gagal, coba lagi dan data luring harus benar.
- [ ] **B03 — Pertahankan bacaan dan bookmark Hadis.** Gunakan ID sumber stabil; bookmark/resume tetap benar setelah refresh/pembaruan dataset. Reader menampilkan Arab, terjemahan, rujukan dan ukuran teks; salin/bagikan tetap membawa sumber yang tepat.
- [ ] **B04 — Lengkapi Doa Harian dengan sumber yang jelas.** Inventaris doa belajar, makan, tidur, rumah, perjalanan, hujan/petir serta pagi/petang; tentukan daftar yang benar-benar disertakan. Setiap entri punya Arab/arti/rujukan yang diperiksa. Pencarian/kategori/luring bekerja; pelafalan dilabeli TTS atau rekaman sesuai sumber aslinya.

## C — Backend dan AI online

- [ ] **C01 — Siapkan konfigurasi server produksi.** Server dapat dijalankan ulang dengan konfigurasi privat, model yang tersedia dan health check. Provider key hanya di server; rotasi key yang pernah dibagikan sebelum hosting publik. Tidak ada nilai rahasia pada APK, Git atau log.
- [ ] **C02 — Hosting HTTPS dan akses untuk distribusi.** Setelah pilihan hosting/domain ditetapkan, pasang layanan, pembatasan penggunaan dan autentikasi per pengguna/instalasi yang dapat dicabut. Kode gateway bersama yang sekarang ada hanya untuk uji pribadi. Verifikasi server tetap bekerja saat laptop pengembang mati.
- [ ] **C03 — Hubungkan APK ke AI online dengan alur yang jelas.** Pilihan online/offline, izin pengiriman pesan, loading, pembatalan, timeout, kuota habis dan coba lagi dapat dipahami pengguna. Kegagalan jaringan tidak diberi label seolah-olah jawaban AI online berhasil.
- [ ] **C04 — Konteks dan tindakan AI sesuai data asli.** Sesuai cakupan A02, kirim hanya konteks yang dipilih/diperlukan; nama, jadwal, tugas dan waktu salat berasal dari data aplikasi. Usulan penambahan/edit tugas ditinjau pengguna dan tersimpan sekali, tanpa penyelesaian/progres rekaan. Tafsir/hadis tidak dihasilkan sebagai kutipan tanpa rujukan terverifikasi.
- [ ] **C05 — Uji percakapan nyata dari HP.** APK → hosting HTTPS → Featherless → balasan tampil, beserta kondisi token salah, putus koneksi dan kuota. Simpan hasil yang sudah disamarkan; respons provider simulasi tidak cukup untuk mencentang item ini.
- [ ] **C06 — Input suara sesuai keputusan rilis; Vision tercatat sebagai fase berikutnya.** [A02](RELEASE_SCOPE.md) menetapkan dikte suara wajib dengan fallback ketika recognizer/izin tidak tersedia. Vision F5.4 ditunda setelah rilis pertama, bukan sudah selesai. Saat Vision dibuka kembali, buktikan model, foto nyata, izin/ukuran/gagal/pembatalan; jangan memberi kemampuan scan kepada model teks saja.

## D — Stabilkan fitur inti

- [ ] **D01 — Lokasi, jadwal salat dan Kiblat.** Verifikasi metode jadwal terhadap sumber/metode yang dinyatakan, tanggal/zona dan perubahan kota. GPS ditolak tetap bisa memilih kota. Uji sensor Kiblat pada HP dengan petunjuk kalibrasi dan keadaan sensor tidak tersedia; jangan menampilkan arah palsu.
- [ ] **D02 — Pengingat salat dan adzan penuh.** Sediakan pilihan nada/adzan/getar per salat sesuai cakupan, sumber audio berizin, hentikan suara dan tangani audio focus. Uji izin ditolak, layar mati, reboot, perubahan kota/waktu serta interaksi murottal dan timer. Jeda fokus saat adzan mengikuti keputusan cakupan; alarm bergantung batasan Android yang dijelaskan dengan benar.
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

Format baris berikutnya: `tanggal | ID | hasil singkat + batas yang tersisa | tautan laporan/screenshot/tes + commit fitur`. Keputusan menunda/mengubah cakupan juga dicatat di tabel ini.
