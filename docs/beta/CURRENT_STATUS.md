# Zikra — status aktif untuk sesi berikutnya

**21 November 2026 · HOLD · belum rilis publik.** Promosi besar setelah persyaratan rilis dipenuhi. Repo aktif `E:/Zikra/Zikra-App-main`. Catatan 16 November pada handoff lain adalah riwayat.

## Fakta yang jangan dicampur

| Bagian | Keadaan yang terbukti |
|---|---|
| HP pengguna | Terakhir diverifikasi pada signed APK 1.9.55/code82. Data berhasil dipulihkan. |
| Kandidat signed terbaru | 1.9.58/code85, 38.929.647 byte, akun 18+ aktif pada kandidat. [Bukti paket](../../android/validation/signed-account-candidate-2026-11-21/README.md). Uji HP dan produksi masih terbuka. |
| Source Android | 1.9.58/code85 dengan akses akun mandiri pada build akun. Aktivasi akun pada build release memerlukan opsi eksplisit dan konfigurasi klien Firebase produksi. Kandidat signed lama belum memuat perubahan ini. |
| Backend AI | zikra-ai.vercel.app; providerCloudflare, laporanAI produksi sudah aktif dari sesi terdahulu. Tidak dideploy ulang saat pekerjaan website |
| Layanan akun18+ | Endpoint aktif di produksi dengan akun tetap opsional. Pendaftaran sintetis dan penolakan email belum terverifikasi lulus. [Bukti deploy](ACCOUNT_PRODUCTION_2026-11-21.md). |
| Website | Preview Vercel terlindungi sudah READY, video52detik lengkap bersuara; tombolAPKdisabled |
| Publikasi | Tidak ada APK/voice/promo umum yang dibuka. A–D/E/F/G tidak selesai seluruhnya |

Preview: https://zikra-jyr79p4ka-arkanzahir86-3257s-projects.vercel.app/ (loginVercel pemilik). Proyek `zikra-web` terpisah dari `zikra-ai`, plan Hobby diverifikasi. [Bukti hosting](../../website/validation/2026-11-16-hosting/README.md). User memilih **preview terlindungi dengan video lengkap dahulu**, bukan web publik tanpa suara. Jangan membuat tautan bypass atau menonaktifkan proteksi.

## Keputusan pengguna yang masih berlaku

- AI online beta **18+ dahulu**. Alur wali dan review usia manual dibatalkan/ditunda. Fitur lokal tidak otomatis semuanya menjadi18+.
- KonfirmasiDOB adalah deklarasi, bukan bukti identitas/usia terverifikasi. Jangan mengganti label assurance menjadi verified atau mengklaim kepatuhan selesai.
- B01 dua kitabGading masih menunggu sumber/hak. Cakupan beta terbatas **HadeethEnc dahulu**, tanpa menunggu Gading untuk menyiapkan cakupan terbatas. Data/bookmark lama tetap dijaga.
- Adzan memakai MP3 pilihan lokal pengguna, biasa/Subuh terpisah; tidak dibundel/diunggah. Website menampilkan informasi ini dengan jujur.
- PenerbitZikra, dukunganpublik arkanzahir46@gmail.com. GoogleMaps/billing ditolak; tetapOSM. Tidak ada upgrade berbayar.
- Desain terbaru disetujui cukup baik: gaya susunanEnte, logo dan delapan ikon asli,6panel+12topik fitur, video52detik. Pertahankan identitas tersebut. Situs tetapHTML/CSS/nativecontrols, tanpaframework/analytics/JS.
- Semua perubahan selesai perlucheckpointGitHub. Jangan menyebut seluruh pekerjaan selesai hanya karena sebuahbuild/test lulus.

## Bukti terbaru

- **Akses AI mandiri lokal.** 176 tes backend dan 237 tes JVM release lulus tanpa skip. Regresi memakai Redis fixture untuk chat, gambar, laporan, kuota lintas token Firebase, token instalasi lama, dan pencabutan. Belum ada aktivasi produksi, kandidat signed baru, atau pengujian perangkat untuk build ini.
- [Pembaruan emulator 1.9.55 → 1.9.58](MIGRATION_EMULATOR_2026-11-21.md): empat database sintetis dan lima layar lulus dari rebuild signed 1.9.55.
- [Kandidat signed 1.9.58](../../android/validation/signed-account-candidate-2026-11-21/README.md): kunci rilis yang sama, ukuran di bawah 100 MB, 237 tes JVM release lulus, pemeriksaan kredensial dan bundel adzan bersih. Build akun menolak konfigurasi Firebase yang hilang.
- [Endpoint akun produksi](ACCOUNT_PRODUCTION_2026-11-21.md): empat rute menolak permintaan tanpa token, token instalasi lama masih memperoleh capabilities, akun sintetis berhasil dihapus. Verifikasi email dan alur positif belum lulus.
- [Akun18+](../../android/validation/adult-accounts-2026-11-15/README.md):175backend/237JVM/5native,0skip; lint0error/912warning; npm audit0vulnerability pada run tercatat. Native menggunakan FirebaseAuthEmulator resmi, AdminSDK, Redisfixture dan AIstub sintetis, **bukan produksi/HP**. LokalNode22.17.0; targetVercel24.x belum diuji lokal.
- [Adzan57](../../android/validation/local-adhan-2026-11-15/README.md):10native pada signed releaseAPI36 termasuk alarm→audio→Stop. PemilihanMP3/SAF/suara padaHP 57 belum diverifikasi.
- [WebsiteEnte](../../website/validation/2026-11-16-ente/README.md):6panel,12topik,1440/390/320CSSpx tanpaoverflow, ikon/anchor diperiksa. [Pemetaanfitur](../../website/FEATURE_MAP.md).
- [Hosting](../../website/validation/2026-11-16-hosting/README.md): pemilik dapat membuka dan memutarvideo; anonymous root/video/frameAI/privacy302keSSO; alias publik otomatis sudah dilepas dan404. Paket5.844.310byte/25file,0APK/0knownsecretmatch. Ini bukan audit seluruh retensi/keamanan penyedia.
- [MigrasiHP 55](../../android/validation/release-migration-2026-11-15/README.md): backup terenkripsi dan restore nyata,5database/9tabel dibandingkan. Kunci/consent/izinOS tidak dipindahkan.

## Prioritas berikutnya, berurutan

| Urut | Pekerjaan nyata yang belum selesai | Perlu HP/input? |
|---|---|---|
|1| Kunci Auth Reader baru sudah dipindah ke `.local/firebase-auth-reader.json` dan diabaikan Git. Uji baca Firebase dengan UID acak menghasilkan `auth/user-not-found`, sesuai harapan. Pemeriksaan inventaris IAM dengan Auth Reader menghasilkan HTTP 403. Status kunci lama perlu dilihat dengan akses IAM pemilik. | Konsol pemilik untuk status kunci lama |
|2| Selesai. Subject key stabil disimpan privat. Endpoint akun aktif dengan `ENABLED=true` dan `REQUIRED=false`. [Bukti deploy dan rollback](ACCOUNT_PRODUCTION_2026-11-21.md). | Tanpa HP |
|3| [Uji update emulator](MIGRATION_EMULATOR_2026-11-21.md) dari rebuild signed 1.9.55 ke kandidat 1.9.58 lulus. Empat database sintetis dan lima layar terverifikasi. Lanjut uji dari APK historis di HP dan data pengguna. | HP setelah koordinasi |
|4| Pendaftaran sintetis dan penolakan akun belum terverifikasi lulus di produksi. Lanjutkan uji pengiriman/verifikasi email, status, deklarasi, perpanjangan, dan pencabutan pada akun terverifikasi. Akses mandiri sudah diimplementasikan dan diuji dengan Redis fixture. Berikutnya uji build Android bersih dan backend dengan akun terverifikasi pada rollout terkontrol. Produksi saat ini masih memerlukan token instalasi. | Endpoint produksi diperlukan untuk pengujian akhir |
|5| Finalkan privasi/risk usia 18+ dan retensi/kuota/provider, jalur penghapusan akun/metadata dan penanganan laporan. Selfdeclaration bukan jaminan anti-pemalsuan; holdSharedPreferences.commit failure belum fault-tested | Sebagian kode/dokumen tanpa HP; sebagian keputusan/layanan |
|6| Tutup gate konten/audio/ilustrasi/promo yang berlaku untuk cakupan terbatas. Hak suaraElevenLabs pada saatgenerasi belum terkonfirmasi; video hanya untukreview terlindungi. [Jejak sumber](CONTENT_LICENSE_TRACE.md), [audit](PRODUCT_AUDIT.md) | Bukti luar; jangan menganggap filegratis=izinredistribusi |
|7| SesiHP terarah untuk kandidat final/update/data dan sisa kasus adzan/izin. Gunakan bukti historis yang masih relevan; jangan ulang semua audio/restart demi jumlah tes | HP setelah koordinasi; restart hanya izin khusus |
|8| Salinan pemulihan signing di media terpisah belum ada; jangan buat ulang key. Finalisasi keputusanbeta/paketmanifest/privasi, kemudian baruaktifkan downloadweb dan ujiHTTP/hash/installupdate | Media/pilihan pengguna; gate publik |

Web tidak memerlukanpembangunanulang. `website/README.md`, `website/package-preview.cjs`, `website/vercel.json` dan receipt hosting menjelaskan deploy. Gunakan `--environment preview` eksplisit. Deploy pertama Vercel sempat otomatis diberialiasproduksi; aliasdilepas. Jangan menganggap perintah tanpa --prod pasti tidak membuat alias publik pada proyekbaru.

## Kontrak akun yang harus dipertahankan

Server: `backend/adult-accounts.mjs`, `adult-account-runtime.mjs`, `account-http.mjs`, `api/adult-account.js`, `server.mjs`. Android: paket `account`, `AssistantConnection`, `AssistantHttp`, `ImageGenerationClient`, `OnlineAssistantClient`, `AiActivity`.

- Endpoint GET/v1/adult-account; POST /declare,/renew,/withdraw. VerifiedFirebaseemail/tokenrevocation/currentstate; loginbaru≤5menit untukmutasi. DOB tidak disimpan; metadataUID/HMAC/ageband/policy/revisionTTL90hari sejak perubahan, konfirmasi30hari.
- `ZIKRA_ADULT_ACCOUNTS_ENABLED=true` mengaktifkan endpoint akun. `ZIKRA_ADULT_ACCOUNTS_SELF_SERVICE=true` mengaktifkan chat, gambar, capabilities dan laporan melalui sesi Firebase tanpa token instalasi. Flag baru default mati. Pertahankan `ZIKRA_ADULT_ACCOUNTS_REQUIRED=false` selama transisi agar token instalasi lama tetap berlaku.
- Rahasia: `ZIKRA_AUTH_SERVICE_ACCOUNT_JSON`, `ZIKRA_ACCOUNT_SUBJECT_KEY`. HanyaAuth reader, bukanFCMsender. Jangan pakai emulator/TLSoff/subject keyfixturea*64 di produksi.
- Pada Android release, opsi `-PzikraReleaseAdultAccounts=true` mengaktifkan login dan akun 18+ dengan konfigurasi Firebase produksi. Default tetap mati dan opsi emulator selalu mati pada release. JWT Firebase hanya untuk backend Zikra.
- Permintaan diikatUID/revisi; callbacklama ditolak. Localhold persisten sebelumwithdrawRPC. Jejak under18 bukan penolakanpermanen seumurhidup; record punyaTTL dan deklarasi tetapself reported.

## Lingkungan dan yang wajib dijaga

WindowsPowerShell. NodeD:/node.exe; PythonbundledCodex; JDK C:/Users/Lenovo/.jdks/jbr-17.0.14; SDK E:/AndroidSDK; GRADLE_USER_HOME E:/.gradle. API36/AGP8.10.1/Gradle8.11.1/min26. [Build/test](../../android/validation/adult-accounts-2026-11-15/README.md).

`.local` tidak diGit: signing/credential.clixml, cloudflare.env, vercel-production.env, firebase-android.json, testlogs dan media. Jangan mencetak nilainya, menaruhnya diAPK/Git, atau meminta lewat chat. Key release sama; backup terenkripsi pengguna harus dijaga. Janganfactoryreset/uninstall/restoreHP tanpa izin terbaru yang sesuai.

**PROJECT_HANDOFF.md tetapWIP terpisah**: SHA e6df1b9dfc227883098e6db19ea7413cac6206a61fa6834eeb1bc2a7fae63650. Jangan stage/reset/overwrite. Stashaudio35686be384ac549e5a5bf85be87cfe56e3b667df tetapada; janganpop/drop. Desain/aset/audioasli/berkaspribadi tidak dihapus. UntrackedPNG/lama/crashlogs/__pycache bukan bahanstage massal.

Proses emulator miliktes (5592/ADB5048), AuthEmulator danfixture9098 sudah dihentikan saat handoff; jangan menjalankan skenario lama begitu saja. HP/defaultADB5037 tidak dipakai sesiwebsite. Previewlokal64882 dapat berhenti saat sesi berakhir; URLVercel tidak bergantung laptop.

Bagian catatan lama yang menyebut13+/wali/manualreview/websitepause atauAPK48 adalah riwayat. Dokumen ini dan receipt baru mengunggulinya untuk keadaan yang tercatat, bukan menghapus bukti sejarah.
