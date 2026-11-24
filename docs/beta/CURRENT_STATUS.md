# Zikra — status aktif untuk sesi berikutnya

**24 November 2026 · HOLD · belum rilis publik.** Promosi besar setelah persyaratan rilis dipenuhi. Repo aktif `E:/Zikra/Zikra-App-main`. Catatan 16 November pada handoff lain adalah riwayat.

## Fakta yang jangan dicampur

| Bagian | Keadaan yang terbukti |
|---|---|
| HP pengguna | APK signed 1.9.67/code94 terpasang lewat update USB dari 1.9.63-internal/code90. Waktu pemasangan awal, posisi baca, dan pilihan qori cocok. Uji Al-Fatihah ayat 1–2 lulus untuk ketiga qori. |
| Kandidat signed terbaru | 1.9.67/code94, 41.995.490 byte, Auth 18+ dan tombol Hapus Akun aktif. Audit signer, pemindaian rahasia, serta uji tombol ke Vercel Preview lulus. Kandidat dan manifest tetap HOLD. |
| Source Android | 1.9.67/code94 dengan pemulihan ayat Murottal yang macet, batas cache audio sorotan kata enam hari, dan penguatan blokir akun saat penyimpanan gagal. Build kandidat memakai opsi eksplisit untuk akses akun dan penghapusan. |
| Backend AI | zikra-ai.vercel.app; providerCloudflare, laporanAI produksi sudah aktif dari sesi terdahulu. Tidak dideploy ulang saat pekerjaan website |
| Layanan akun18+ | Endpoint aktif di produksi dengan akun tetap opsional. Pendaftaran sintetis dan penolakan email belum terverifikasi lulus. [Bukti deploy](ACCOUNT_PRODUCTION_2026-11-21.md). |
| Website | Preview Vercel dengan video lengkap dan Preview terpisah tanpa promo keduanya terlindungi. Unduhan APK ditutup. |
| Publikasi | Tidak ada APK/voice/promo umum yang dibuka. A–D/E/F/G tidak selesai seluruhnya |

[Preview dengan video](https://zikra-jyr79p4ka-arkanzahir86-3257s-projects.vercel.app/) dan [Preview tanpa promo untuk meninjau draf privasi dan ketentuan](https://zikra-qmb8trn9u-arkanzahir86-3257s-projects.vercel.app/) memerlukan login Vercel. Proyek `zikra-web` terpisah dari `zikra-ai` dan memakai Hobby. [Bukti hosting awal](../../website/validation/2026-11-16-hosting/README.md).

## Keputusan pengguna yang masih berlaku

- AI online beta **18+ dahulu**. Alur wali dan review usia manual dibatalkan/ditunda. Fitur lokal tidak otomatis semuanya menjadi18+.
- KonfirmasiDOB adalah deklarasi, bukan bukti identitas/usia terverifikasi. Jangan mengganti label assurance menjadi verified atau mengklaim kepatuhan selesai.
- B01 dua kitabGading masih menunggu sumber/hak. Cakupan beta terbatas **HadeethEnc dahulu**, tanpa menunggu Gading untuk menyiapkan cakupan terbatas. Data/bookmark lama tetap dijaga.
- Beta standar memakai adzan bawaan Ahmed Khalaf untuk Subuh dan Adam-synagda untuk Zuhur sampai Isya. Pengguna dapat memilih MP3 lokal secara opsional. Salinan web perlu mengikuti kandidat terbaru menjelang publikasi.
- PenerbitZikra, dukunganpublik arkanzahir46@gmail.com. GoogleMaps/billing ditolak; tetapOSM. Tidak ada upgrade berbayar.
- Desain terbaru disetujui cukup baik: gaya susunanEnte, logo dan delapan ikon asli,6panel+12topik fitur, video52detik. Pertahankan identitas tersebut. Situs tetapHTML/CSS/nativecontrols, tanpaframework/analytics/JS.
- Semua perubahan selesai perlucheckpointGitHub. Jangan menyebut seluruh pekerjaan selesai hanya karena sebuahbuild/test lulus.

## Bukti terbaru

- **Kandidat signed 1.9.67, 24 November.** Update USB dan sampel Murottal lulus di HP. Tombol Hapus Akun lulus di emulator dengan Firebase Spark dan Vercel Preview. Konfirmasi yang benar menghapus akun sintetis. Sebanyak 248 tes JVM lulus. Audit signer, paket, dan unduhan HTTPS lokal lulus. [Bukti](../../android/validation/signed-beta-1.9.67/README.md).

- **Uji akun signed tuntas di Preview, 24 November.** Android API 36 lulus chat dengan token akun, pencabutan, penghapusan akun, dan penolakan token serta login lama. Uji memakai Firebase Spark dan Redis terpisah serta jawaban AI sintetis. Akses sementara dicabut setelah pembersihan. Permintaan konfirmasi izin Murottal dikirim ke Islamic Network dan menunggu balasan. [Uji staging](../../android/validation/signed-self-service-staging/README.md).

- **Persiapan staging dan cadangan signing, 24 November.** APK instrumen 1.9.66 dengan skenario hapus akun sintetis lulus build dan verifikasi signer. Vercel Preview masih belum memiliki variabel Redis dan Firebase. Penerbit menyetujui draf privasi dan ketentuan untuk beta gratis. Cadangan PKCS#12 menghasilkan sertifikat rilis yang sama saat diuji dan sudah disimpan di folder Google Drive privat. [Persyaratan tersisa](PUBLIC_RELEASE_BLOCKERS.md).

- **Redis hosted dan review privasi, 24 November.** Kontrak otorisasi chat dan hapus akun lulus pada backend lokal dengan Redis Upstash terpisah, identitas sintetis, dan pembersihan semua kunci uji. Preview situs terbaru READY dan tiga rute meminta login. Uji menyeluruh di backend Preview dengan Firebase nyata masih terbuka. [Bukti](../../android/validation/signed-self-service-staging/README.md).

- **Kandidat signed 1.9.66, 24 November.** Lulus audit paket dan signer, 248 tes JVM release, update signed API 36, dua uji native Murottal, dan unduhan HTTPS lokal. Preview website tanpa promo berstatus READY dan tiga halaman memerlukan login. [Bukti](../../android/validation/signed-beta-1.9.66/README.md).

- **Cache sorotan kata dan review situs, 24 November.** Cache rekaman sorotan kata dibersihkan setelah enam hari saat aplikasi dapat berjalan. Unduhan offline tetap terpisah. Sebanyak 26 tes JVM audio dan dua tes Android API 36 lulus. Draf Ketentuan Penggunaan dan tiga halaman review lokal tanpa promo lulus pemeriksaan Edge pada tiga lebar layar. [Bukti](../../android/validation/word-audio-cache-2026-11-24/README.md).

- **Kandidat signed 1.9.65, 24 November.** Audit paket dan sertifikat lulus. Sebanyak 246 tes JVM release lulus tanpa skip dan lint 0 error/913 warning. Emulator API 36 lulus update dari kandidat signed 1.9.64, preservasi preferensi baca, uji Putar pada ayat yang macet, dan tombol Berikutnya. HEAD/GET HTTPS lokal cocok hash. HP, Preview akun, cadangan pemulihan, dan unduhan publik tetap terbuka. [Bukti](../../android/validation/signed-beta-1.9.65/README.md).

- **Murottal macet, 24 November.** Pemutar dalam APK signed 1.9.67 memberi pesan dan dapat memuat ulang ayat yang sama jika posisi decoder tidak berubah selama 15 detik. Sebanyak 24 tes JVM audio dan dua tes Android API 36 lulus dengan fixture sintetis. Sampel di HP lulus, dan surah, ayat, serta qori kejadian awal belum dikonfirmasi. [Bukti dan batas](../../android/validation/murottal-stall-2026-11-24/README.md).

- **Regresi akun 24 November.** Sebanyak 177 tes backend lulus tanpa skip dan 10 tes Android API 36 lulus pada build debug dengan Firebase Auth Emulator dan Redis sintetis. Cakupan mencakup chat tanpa token instalasi, timeout dan percobaan ulang, pencabutan, serta penolakan login setelah akun dihapus. Vercel CLI sudah terautentikasi dan `zikra-ai` belum punya deployment atau variabel Preview. Deployment yang dulu disebut staging berlabel Production. Redis kedua tidak tersedia pada paket gratis yang ditawarkan integrasi untuk akun ini. Uji hosted signed dan rilis publik tetap terbuka. [Bukti dan batas](../../android/validation/signed-self-service-staging/README.md).

- **Pencabutan akses saat penyimpanan gagal, 23 November.** Source 1.9.64 kini mempertahankan blokir AI dalam proses ketika commit penanda lokal gagal, termasuk saat pengaktifan ulang. UI menampilkan kegagalan penyimpanan, dan pencabutan/penghapusan tetap mencoba server. Sebanyak 243 tes JVM release termasuk enam fault injection lulus tanpa skip; kompilasi Android instrumentation lulus, lint release 0 error/913 warning. Belum ada APK baru atau pengujian UI fault injection native. Commit yang gagal sebelum proses mati dan sebelum RPC selesai tetap belum menjamin blokir pada proses baru. [Bukti dan batas](../../android/validation/account-hold-2026-11-23/README.md).

- **Persiapan distribusi 22 November.** Unduhan APK signed 1.9.64 melalui HTTPS loopback memberi HTTP 200 untuk HEAD dan GET, tipe paket Android dan 41.994.958 byte. SHA-256 serta sertifikat sesuai manifest. Pemasangan ulang pada emulator API 36 mempertahankan waktu pemasangan awal dan aplikasi dapat dibuka. Paket preview web lulus pemeriksaan lokal dengan 26 berkas tanpa APK. Permintaan anonim ke beranda, privasi dan video pada preview Vercel diarahkan ke login. Sebanyak 177 tes backend lulus. Pemeriksaan IAM mengonfirmasi akun layanan yang tersedia bisa membaca Firebase Auth, tanpa izin menghapus akun atau menyiapkan IAM. Pengujian Hapus Akun di staging dan publikasi tetap tertunda.

- **Chat signed 1.9.64 dengan email terverifikasi lulus.** Pada emulator API 36, hash APK terpasang cocok dengan manifest. Firebase mencatat email akun uji terverifikasi setelah pemilik membuka tautan. Klien Android login lewat Firebase SDK dan menerima jawaban Cloudflare yang sesuai dengan prompt aritmetika sintetis. Satu tes lulus pada percobaan pertama dalam 6,144 detik untuk login dan chat; kuota instalasi uji mencatat pemakaian. Uji memakai token instalasi sementara karena konfigurasi akses produksi tetap sama. Akun Firebase milik uji, token dan data kuota uji, dan paket instrumen dibersihkan. Mode mandiri tanpa token instalasi pada APK signed dan penghapusan akun melalui fitur Zikra di staging masih terbuka.

- **Chat online kandidat signed 1.9.64, 22 November.** Hash APK terpasang cocok dengan manifest. Pada emulator Android API 36, klien signed mengembalikan UNAUTHORIZED sebelum permintaan mencapai kuota gateway saat hanya diberi token instalasi. Interceptor build akun membutuhkan sesi Firebase dengan email terverifikasi untuk chat. Cek backend terpisah dengan token uji sementara menghasilkan capabilities HTTP 200, chat HTTP 504, lalu HTTP 200 dengan jawaban sintetis Cloudflare pada percobaan ulang (35,2 detik). Token dan data uji khusus instalasi dihapus, dan konfigurasi produksi tidak diubah. Percobaan tanpa sesi tersebut berhenti sebelum inferensi.

- **Uji 22 November 2026.** Pemutar Edge lokal menampilkan seluruh 17 caption pada titik tengah cue dan tidak menampilkan caption pada 51,8 detik. Pada MP4 final, pemeriksaan FFmpeg menggunakan ambang -38 dB dan jeda minimal 180 ms menemukan selisih awal caption maksimum 18 ms dari akhir jeda dan selisih akhir caption maksimum 139 ms dari awal jeda. APK signed 1.9.64/code91 dipasang dan dibuka di emulator API 36. Dari Beranda, Zikra AI menjawab prompt rencana belajar lewat panduan lokal. Pemilihan mode Online membuka layar akses 18+ yang meminta email terverifikasi dan konfirmasi usia. Tidak ada inferensi online pada uji APK ini. Publikasi tetap HOLD.

- **Kesiapan web.** Manifest dan metadata web mencatat APK signed 1.9.64/code91 (41.994.958 byte), sesuai hash, audit signer, dan bukti update emulator. Halaman privasi menjelaskan usia 18+, retensi catatan dan hapus akun. Caption Indonesia mengikuti jeda audio promo 52 detik. Tinjauan pemutaran caption, retensi penyedia, izin distribusi suara promo dan publikasi tetap terbuka.

- **Chat Android tanpa token instalasi.** 10 tes instrumentasi lulus di emulator Android API 36 dengan Firebase Auth Emulator, Redis dan provider AI sintetis. Setelah verifikasi email dan konfirmasi usia, klien Android berhasil mengirim chat tanpa token instalasi. Respons HTML 504 dipetakan ke timeout, satu permintaan mencapai provider sintetis, dan permintaan baru yang dikirim manual berhasil. Pencabutan akses memblokir chat pada klien dan server. Probe anonim produksi memberi 200 untuk health dan 401 untuk capabilities serta akun. Uji chat APK signed 1.9.64 di staging masih terbuka karena APK memakai URL produksi.

- **Hapus akun 1.9.64.** Menu Akun Zikra meminta konfirmasi dan autentikasi ulang dengan sandi. Endpoint menghapus akun Firebase, metadata deklarasi usia dan penghitung kuota akun. Penanda pseudonim 24 jam memblokir akses sambil memberi ruang percobaan ulang. Data di HP tetap lokal dan laporan moderasi mengikuti TTL tujuh hari. 177 tes backend, 237 JVM release dan 10 tes Android API 36 lulus. Tes Android memakai Firebase Auth Emulator dan akun sintetis, termasuk pembatalan dialog dan penolakan login setelah penghapusan. Draf privasi diperbarui. Tombol dan endpoint penghapusan produksi masih mati sampai izin akun layanan dan uji staging tersedia. [Aktivasi](../../backend/deploy/SELF_SERVICE_ACCOUNTS.md).

- **Alarm otomatis di HP 1.9.63-internal.** AlarmManager fisik membunyikan Subuh saat layar mati dan keyguard terkunci. Pemutar melaporkan output headset saja. Aksi Berhenti di notifikasi menghentikan layanan, dan preferensi pengingat pulih. Pada 22 November, tes speaker HP lulus setelah alat uji diberi waktu menunggu rute audio. Alarm Subuh terjadwal memulai pemutar saat layar mati dan keyguard terkunci. Android melaporkan output hanya speaker, dan pengguna mendengar adzan dari speaker. Aksi Berhenti menghentikan layanan, preferensi pengingat pulih, dan paket alat uji dilepas. Percobaan pertama pada hari itu menghentikan pemutar terlalu cepat dan tidak mengonfirmasi suara. Bukti ini untuk 1.9.63-internal.
- **Akun dan kandidat 1.9.63.** Pemilihan sesi memakai FirebaseAuth yang sama untuk emulator bernama. Tes pendaftaran mengikuti dialog usia. 9 tes Android dengan akun dan jawaban AI sintetis lulus, termasuk verifikasi email, login, batas usia, dan pencabutan akses. 176 tes backend serta 237 tes JVM release lulus. Kandidat 1.9.63 belum memiliki alur hapus akun dari UI. Unduhan lokal kandidat standar cocok hash dan lolos pasang baru serta update di emulator. Situs tetap preview terlindungi.

- **Pilihan pengingat 1.9.63.** Menu menampilkan Adzan penuh jika ada audio siap pakai, termasuk bawaan. Pengguna melihat keempat mode di HP setelah 1.9.62. Build 1.9.63 menambah petunjuk lokasi tombol Ganti suara adzan dengan MP3 dan memperbarui keterangan volume. 12 instrumentasi emulator Android 36 lulus, termasuk tampilan petunjuk, memilih dan menyimpan mode Adzan penuh tanpa impor, serta regresi audio. Signed 1.9.63-internal berhasil memperbarui HP.

- **Rute audio adzan 1.9.61.** Pratinjau memakai jalur media. Alarm dengan earphone tersambung memakai jalur media dan, pada API yang mendukung pemeriksaan rute, menahan suara sampai output earphone terkonfirmasi. Alarm tanpa earphone tetap memakai jalur alarm. Perubahan/kegagalan rute dapat menghentikan audio. APK internal signed diaudit dan dipasang di HP. 237 tes JVM dan 11 tes emulator API 36 lulus. Pengguna mendengar pratinjau Subuh hanya di earphone Bluetooth. Alarm otomatis dengan earphone dan layar terkunci belum diuji di HP.

- APK signed `1.9.60-internal`/code87 memuat pasangan Misyari dan Subuh yang byte-nya cocok dengan Zikra lama. Tanpa impor MP3, keduanya bisa dipilih lewat mode Adzan penuh. Audit APK, 237 tes JVM, dan 11 tes instrumen emulator API 36 lulus. Paket standar diuji terpisah dengan hasil kedua MP3 lama absen. APK internal awal tidak dipublikasikan. Versi internal yang dibangun ulang dengan opsi akun kandidat standar sudah terpasang di HP atas pilihan pengguna. [Sumber dan cakupan](CONTENT_LICENSE_TRACE.md).
- [Adzan bawaan 1.9.60](../../android/validation/bundled-adhan-2026-11-21/README.md): update offline 1.9.59 → 1.9.60 di emulator Android 36, audit signer dan dua MP3, serta 11 tes adzan lulus. Alarm Subuh memutar rekaman bawaan tanpa impor dan berhenti melalui aksi notifikasi. Izin pada HP terverifikasi setelah update. Pengguna mendengar dan menghentikan pratinjau Subuh di HP. Alarm fisik dengan layar terkunci masih terbuka. [Jejak lisensi](CONTENT_LICENSE_TRACE.md).
- [Kandidat signed 1.9.59](../../android/validation/signed-self-service-candidate-2026-11-21/README.md). 237 tes JVM, audit paket, update emulator 1.9.58 → 1.9.59, empat database sintetis, dan lima layar lulus.
- **Akses AI mandiri lokal.** 176 tes backend dan 237 tes JVM release lulus tanpa skip. Regresi memakai Redis fixture untuk chat, gambar, laporan, kuota lintas token Firebase, token instalasi lama, dan pencabutan. Belum ada aktivasi produksi, kandidat signed baru, atau pengujian perangkat untuk build ini.
- [Pembaruan emulator 1.9.55 → 1.9.58](MIGRATION_EMULATOR_2026-11-21.md): empat database sintetis dan lima layar lulus dari rebuild signed 1.9.55.
- [Kandidat signed 1.9.58](../../android/validation/signed-account-candidate-2026-11-21/README.md): kunci rilis yang sama, ukuran di bawah 100 MB, 237 tes JVM release lulus, pemeriksaan kredensial dan bundel adzan bersih. Build akun menolak konfigurasi Firebase yang hilang.
- [Endpoint akun produksi](ACCOUNT_PRODUCTION_2026-11-21.md): empat rute menolak permintaan tanpa token, token instalasi lama masih memperoleh capabilities, akun sintetis berhasil dihapus. Uji dengan akun terverifikasi lulus untuk deklarasi, perpanjangan, dan pencabutan. Chat, gambar, dan laporan tanpa token instalasi berhasil pada staging terlindungi. Chat sempat HTTP 504 pada jalur akun dan token lama.
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
|3| [Uji update emulator](MIGRATION_EMULATOR_2026-11-21.md) dari rebuild signed 1.9.55 ke kandidat 1.9.58 lulus. Empat database sintetis dan lima layar terverifikasi. Update dari APK historis 1.9.55 ke 1.9.59 lulus di HP; integritas lima database dan jumlah baris sepuluh tabel cocok. | Selesai untuk update dan perbandingan jumlah baris |
|4| Verifikasi email, deklarasi, perpanjangan, dan pencabutan lulus pada endpoint produksi. Chat, gambar, dan laporan tanpa token instalasi berhasil di staging terlindungi. APK 1.9.59 sudah dibangun dari arsip bersih dan lulus uji update emulator. Update HP dari 1.9.55 lulus. Verifikasi timeout chat dan alur AI online dari APK sebelum rollout. Produksi masih memerlukan token instalasi. | Uji online dan rollout terkontrol |
|5| Finalkan privasi/risk usia 18+ dan retensi/kuota/provider, jalur penghapusan akun/metadata dan penanganan laporan. Selfdeclaration bukan jaminan anti-pemalsuan. Kegagalan commit local hold sudah diuji sintetis dan blokir proses diperkuat; kegagalan disk fisik + jaringan + proses mati tetap terbuka | Sebagian kode/dokumen tanpa HP; sebagian keputusan/layanan |
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
