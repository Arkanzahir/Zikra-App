# Cakupan beta publik1.9.54/code81 —15November2026

Pengguna menyetujui HadeethEnc dahulu, dua kitabGading ditunda, adzan berlisensi/notification-only. Pilihan implementasi awal ialah notifikasi biasa. HOLD.

Release memakai BuildConfig.PUBLIC_BETA_SCOPE=true yang tidak bisa dimatikan dari settingaplikasi. Debug defaultfalse menjaga jalur internal; -PzikraPublicBetaScope=true hanya untuk pengujian cakupanpublik.
Query/store.get/download nonHadeethEnc dibatasi di source, UIhero/dua kitab disembunyikan, unduhan mengarahHadeethEnc. Database/bookmark/posisi tidak dihapus. PrefADHANlama dievaluasi sebagaiNOTIFICATION di modepublik, nilai awal tetap ada. Pemilihanmodepublik menyediakan notifikasi/getar/mati. AdhanAsset menolak audio di cakupanpublik. SourceSetrelease tidak memasukkan audio privat.
Rantaiizin HadeethEnc dan kewajibanversi/update tetap harus dipenuhi. Tidak mengklaim B01selesai atau memindahkan penanda duakitab ke koleksibaru.

3nativeAPI36lulus pada debugberflagpublik: database/query/get/download guard dan rowslama, fallbackpref tanpa overwrite, serta UIkoleksi ditunda tersembunyi. Tes menggunakan database terpisah, emulatorranchu dan memulihkan preferensi. Screenshot padafont200% ditinjau untuk pembatasankoleksi; teksbesar masihpadat, bukan klaim polishfinal. Sesudahtes, dua label sumber/subtitle diperjelas dan dikompilasiulang; screenshot ialah sebelum labeltersebut. Pengujian pertama sebelumboot gagalpackage/activityservice belumtersedia. Setelah boot, install+3teslulus.
Internaldebug235unit lulus pada runawal. Bukti unitrelease/lint/paketfinal dilengkapi receipt. Backend tidak berubah pada checkpointini,135backendrun53 bersifat historis.

Paket unsignedrelease hanya inspeksi, belum dapat diinstal/distribusi. Tidak membuat signer, tidak menyentuhHP, tidak mengirimemail, tidak deploy/publication/provider/billing. Sisa13+privasi,retensi/triaseAI, signer/migrasiHP dan matrikskompatibilitas tetapgate.

## Hasil akhir paket inspeksi

Release unit tests 235, failures 0, errors 0, skipped 0. Lint release 0 error / 909 warning. Build akhir berhasil setelah perbaikan label sumber. APK unsigned 37908470 byte, SHA256 3c934c6fd5e3c95a7c2a4f487895d4364b44365c7b9f8bc43e07e22f54fbe8d1.
Pemeriksa APK memastikan identitas, minimum API26, target36, non-debug, non-test, backup/cleartext mati, nol file konfigurasi privat dan nol credential dikenal. audioSha256 kosong. Pemeriksaan artefak tetap gagal secara benar karena paket unsigned dan signer belum dipilih.
