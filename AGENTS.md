# Workflow proyek Zikra

- Sebelum melanjutkan fitur, baca `IMPLEMENTATION_PLAN.md`; gunakan sebagai checklist aktif dan baca laporan bukti untuk modul terkait. Catatan lama tidak mengalahkan status terbaru yang sudah dibuktikan.
- Kerjakan satu ID aktif sampai syarat selesainya terpenuhi. Ketika selesai, centang item, tambahkan tanggal/tautan bukti, dan perbarui jurnal rencana bersama checkpoint kode.
- Kode tersedia, build berhasil, mockup atau respons simulasi tidak otomatis berarti fitur selesai. Biarkan item belum dicentang bila masih membutuhkan pemeriksaan HP, sumber data, atau server produksi.
- Periksa status Git dan pertahankan WIP/rahasia privat. Jangan stage seluruh working tree. Bedakan fitur lokal, koneksi API langsung, dan backend yang benar-benar sudah dihosting.
- Instruksi pengguna 27 Oktober 2026: setiap checkpoint perubahan proyek yang selesai dan diverifikasi harus di-commit dan dipush ke `origin/main`, bukan dibiarkan hanya lokal. Audit isi commit sebelum push, gunakan fast-forward tanpa force, dan verifikasi hash remote sesudahnya. WIP yang diminta dipertahankan, stash audio, konfigurasi privat dan data pengguna tetap di luar commit; jika push gagal, laporkan status sebenarnya.
- Gunakan build/tes terarah yang hemat RAM; tutup emulator selesai dipakai. Perubahan dokumen tidak memerlukan build aplikasi atau emulator.
- Urutan yang diperjelas pengguna 24 Oktober: tuntaskan A03 lalu A04 sebelum melanjutkan B01. Penelitian B01 terdahulu tidak berarti A03/A04 boleh dilewati.
- Terapkan keamanan selama pengerjaan: provider key hanya di backend, konfigurasi privat tidak masuk APK/Git/log, tidak bypass verifikasi TLS, validasi input/URL/berkas dari luar dan jangan mengunggah data pribadi tanpa alur izin yang jelas. Kode gateway pribadi bukan autentikasi produksi; pemeriksaan keamanan publik tetap C01/C02/G03.

## Prioritas pengguna27 Oktober2026: tuntaskan D dahulu

D06 selesai teknis beta dengan pembaruan APK berisi data; [bukti](android/validation/storage-d06-2026-10-27/README.md). D03/D04/D05 juga selesai teknis beta. D01/D02/D02a tetap terbuka untuk pemeriksaan fisik dan sumber/ketentuan audio. Jangan pindah ke E atau menyebut seluruh D/publik selesai; kerjakan bukti yang bisa dilakukan tanpa HP dahulu. Pengguna menunda HP: jangan berulang meminta sambungan. Semua checkpoint terverifikasi tetap commit/push origin/main; PROJECT_HANDOFF.md WIP tetap di luar commit.

## Status28 Oktober2026 — sesi HP D01

D01 selesai teknis beta terarah dengan [bukti fisik/manual/native/preservasi](android/validation/location-phone-d01-2026-10-28/README.md). Ini menggantikan status D01 terbuka pada catatan27 Oktober. D02/D02a tetap terbuka untuk pengujian yang tersisa dan ketentuan audio. HP disambungkan/diizinkan untuk sesi28 Oktober; jangan menganggap izin ini berarti otomatis tersedia pada sesi lain. Runtime tetap1.9.15; B01/C/E/F/G/publik tidak otomatis selesai.

## Status HP28 Oktober2026 — beta1.9.22

Prioritas terbaru pengguna tetap A–D dahulu. [Bukti HP/adzan terbaru](android/validation/prayer-phone-2026-10-28/README.md) menghitung uji pagi, mode/epoch, pilihan audio terakhir dan preservasi; D02/D02a tetap terbuka untuk ketentuan/sisa HP. C02 monitor terjadwal sudah teramati, laptop-mati tetap perlu. Masukan tema global/dock AI/lampu malam/tanggal strip belum diperbaiki oleh checkpoint audio; ikuti handoff. Jangan menimpa refresh lokasi/izin yang dikonfirmasi pengguna atau mengunggah chat/GPS/backup.

## Status29 Oktober2026 — tema global beta1.9.23

Permintaan terbaru pengguna untuk tema konsisten lintas fitur selesai pada F03a dengan [bukti](android/validation/global-theme-2026-10-29/README.md). Home/Profil/Quran/Mushaf memakai satu pilihan; otomatis Quran mengikuti tema perangkat, bukan lux. Palet/dialog/dock/input dan restore sakelar reader diperbaiki; draf AI terpelihara.153 unit/4 skip dan9 native lulus; lint0 error/860 warning. HP tidak diubah; gerbang A–D/F03/publik, dock AI, lampu malam dan splash native tetap mengikuti handoff. Ini menggantikan status tema-belum-diperbaiki untuk checkpoint audio, tanpa menutup pekerjaan lainnya.

## Status29 Oktober2026 — UI06 beta1.9.24

Pengguna memprioritaskan dock AI, lampu masjid malam dan splash sebelum C04; ketiganya selesai teknis beta terarah dengan [bukti native/video/batas](android/validation/ui-followup-2026-10-29/README.md).155 unit/4 skip,11 native lulus; lint0 error/862 warning. HP belum diubah/diuji untuk versi ini; Figma canvas lama tetap belum selesai. C04 berikutnya; A–D/F04/G/publik tidak otomatis selesai. Tema global1.9.23 tetap dan regresi back stack/draf lulus. Pertahankan WIP, stash, data, konfigurasi privat dan desain/ikon asli; checkpoint tetap audit/commit/push.

## Status29 Oktober2026 — UI07 beta1.9.25

Polesan splash800ms disetujui lalu diterapkan. [Bukti HP/preservasi/batas](android/validation/splash-polish-2026-10-29/README.md):4 native API36,155 unit/4 skip lulus;lint0 error/862 warning. HP aktual1.9.22 diperbarui ke1.9.25;32 berkas/17 tabel/preferensi/izin/pengaturan terjaga, helper dibersihkan. USB kemudian dilepas; jangan menganggap tersedia. Lampu fasad sudah terlihat, interior lengkungan belum bercahaya. Font Arab terjemahan masih fallback HP, berbeda dari QCF Mushaf; keduanya bukan perubahan checkpoint splash. C04/A–D/F04/G/publik tetap terbuka, WIP/stash/env/ikon/artwork tetap.

## Status29 Oktober2026 — UI08 kandidat1.9.26

Interior lengkungan malam dan jalur pencarian masjid diperbaiki sesuai masukan pengguna, [bukti/keandalan/batas](android/validation/mosque-repair-2026-10-29/README.md).160 unit/4 skip dan4 native final emulatorAPI37.1 lulus;lint0 error/862 warning. VK Maps/Overpass dengan TLS bawaan dan cache bertanggal; satu lookup baru sempat gagal, tidak menjanjikan uptime API.31 berkas emulator dipulihkan cocok hash, helper/emulator dibersihkan/ditutup. HP tetap1.9.25/USB dilepas; jangan menyatakan26 sudah dipasang. Ikon/artwork/WIP/stash/env/audio tetap, font reader belum diubah. E04/A–D/F04/publik tetap punya gerbang terbuka.

## Riwayat sebelum persetujuan1.9.27 — UI08 visual belum diterima

Pengguna menolak preview UI08. Checkbox dibuka kembali untuk visual interior, bukti teknis pencarian/cache tetap terarah. [Referensi baru/prompt/batas](design/concepts/mosque-lighting-review-2026-10-29/README.md) hanya konsep image_gen; jangan mengganti bitmap Home penuh karena bulan/langit/geometri ikut berubah. Native/source tetap1.9.26, HP1.9.25. Aset asli/ikon/WIP/stash/env tidak disentuh; tunggu tinjauan arah visual sebelum integrasi baru.

## UI08 beta1.9.27 — referensi disetujui dan diterapkan,29 Oktober2026

Pengguna menyetujui arah pencahayaan (“gas sudah bagus”). Tekstur pintu didaftarkan per bukaan ke arsitektur asli; hanya band703×180 sekitar506KB didecode. Glow pilar/pantulan diperkecil; siang mati/senja berangsur/malam menyala, tema tidak menentukan lampu. Langit/bulan/awan/bintang/parallax tetap native. [Bukti/registrasi/APK/batas](android/validation/mosque-approved-lighting-2026-10-29/README.md).160 unit/4 skip,4 native emulatorAPI37.1 lulus;lint0 error/862 warning. APK1.9.27/code54 lokal45.336.132byte, SHA256`0ab5cb0955b80fe28ac5edea488e639a92bc57947f54b9f967f412692132d1a5`.32 berkas emulator pulih cocok hash, pengaturan kembali/helper dibersihkan/emulator ditutup. HP tidak disentuh; terakhir diuji agen1.9.25, jangan klaim27 terpasang. Tidak ada lookup lokasi baru; kode pencarian26/batas API tetap. Artwork/ikon/WIP/stash/audio/env tetap. Font reader/C04 belum diubah; A–D/E04/F04/G/publik belum selesai seluruhnya. Ini menggantikan status visual-menunggu pada catatan di bawah.

## C04 selesai teknis beta1.9.28 — 30 Oktober2026

Konteks kelas sudah opt-in per pengiriman,7 hari kalender/maksimal10 kelas dari store asli, tanpa guru/ruang/SKS/koordinat/ID; preview snapshot, server strict dan usulan tugas tetap ditinjau/disimpan sekali. Penanda sumber salat kini sesuai Day tanpa mengubah tabel/alarm; APK lama tetap kompatibel. Dua Tadabbur hosted1:2/113:1 ditelaah, konteks Basmalah ayat sebelumnya dibedakan oleh prompt. Bukan jaminan seluruh ayat/model. [Bukti/hosting/APK/preservasi/batas](android/validation/assistant-context-c04-2026-10-30/README.md).163 unit/4 skip,69 backend tanpa skip,13 native akhir/10 unik lulus;lint0 error/862 warning. Dialog consent/review font200 diperbaiki lalu tombol diuji sentuhan nyata; kegagalan fixture v3 awal dicatat dan helper diperbaiki, migrasi produksi tidak berubah. APK lokal `Zikra-AI-v1.9.28-beta-debug.apk`,45.617.459byte,SHA256`439c0c2cded30d38c97a4d31ef5918d7fa8398cbf2264791640d11f6d2346ab8`. Backend Vercel diperbarui kode, alias READY dicek; env/key/plan tetap.32 berkas emulator pulih cocok hash, input/marker/helper dibersihkan/emulator ditutup; HP tidak disentuh, terakhir diuji agen1.9.25. WIP/stash/audio/env/aset/ikon tetap. UI09 font reader berikutnya; C02 laptop mati/C07/B01/D02/D02a/E/F/G/publik belum ditutup. Tidak membaca email baru atau mengubah status sumber B01.

## UI09 font selesai dan HP diperbarui1.9.29 — 30 Oktober2026

Pengguna menyetujui font Amiri Quran dari preview saja; sorotan dianggap masih kurang jelas dan ditunda (UI10), jangan menerapkan mint atau mengulang permintaan persetujuannya sekarang. Amiri Quran Regular1.003 dibundel+OFL pada Arab reader/judul/nama/preview; font padding menjaga harakat. Teks/terjemahan/ukuran pilihan/sorotan/audio mapping/Mushaf QCF tetap. [Bukti/data/font/APK/batas](android/validation/quran-font-ui09-2026-10-30/README.md).163 unit/4 skip dan2 native HP API36/264 layout lulus;lint0 error/862 warning. HP aktual1.9.25→1.9.29/code56, bukan lagi25;32 berkas/APK lama dicadangkan privat,17 tabel dan preferensi non-turunan/izin/pengaturan/baterai terjaga. Hanya rencana alarm turunan berubah; bookmark uji dipulihkan dengan guard perubahan pengguna, tanpa restore DB membabi buta. Helper/marker/gambar uji dibersihkan, Zikra terbaru terbuka. APK lokal `Zikra-QuranFont-v1.9.29-beta-debug.apk`,45.415.563byte,SHA256`61389f725ad10c545eef0f11fa75d6f4639dbd9fda97cf320c231e50dc10457c`. Signer sama/0 nilai privat dikenal/dua audio hash cocok. WIP/stash/arsip/env/ikon/artwork tetap. USB diizinkan untuk sesi ini, jangan menganggap selalu tersedia. B01/C02/C07/D02/D02a/E/F/G/publik tetap menurut checklist; update HP tidak membuktikan ulang semua modul terdahulu.
