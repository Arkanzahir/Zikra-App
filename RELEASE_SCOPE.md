# A02 — Cakupan penyelesaian Zikra

## Promo v2 mendatar — 12 November 2026

Pengguna mengoreksi format vertikal/gerakan kaku. [Revisi dan bukti](design/promos/2026-11-12/README.md):
1280×720/15detik, teks di samping HP, perpindahan/kemiringan/perspektif kontinu.
450frame+audio decode penuh lulus,4frame ditinjau,405frame geometri dalam layar;
cek margin65px awal terlalu ketat tetap dicatat. Narasi Putri/rekaman lama
sama, tanpa biaya/HP/runtime baru. V1 tersimpan; penerimaan v2/video lengkap/
lisensi komersial/publikasi belum selesai. Source/bukti diaudit/commit/push;
WIP/stash/audio/env/master dan seluruh gerbang aplikasi tetap.

## Cuplikan promo referensi — 12 November 2026

Pengguna meminta contoh editing 10–15 detik berdasarkan referensi video dan
memilih suara perempuan Putri Maharani. [Hasil/bukti/batas](design/promos/2026-11-12/README.md):
video lokal15detik720×1280/30fps, narasi ElevenLabs baru14,68detik melalui demo
publik gratis, rekaman Home/Quran APK1.9.40 asli; teks bertahap/perangkat perspektif
ringan bergerak kiri-kanan.450frame+audio decode penuh lulus,4frame ditinjau,
puncak-1,5dB; kecepatan narasi tetap. Bukan sampel profil suara ditempel ulang.
Ini cuplikan untuk tinjauan; video lengkap/penerimaan pengguna/lisensi komersial
belum selesai. Audio/video lokal saja, tanpa paket berbayar/publikasi/HP/emulator
baru. Source renderer dan receipt diaudit/commit/push; WIP/stash/audio/master/env
terjaga. Runtime/APK/backend/cron/model tetap; B01/C02/D02/D02a/E03/E04/F/G/publik
masih menurut gerbang terbuka, bukan seluruh A–D/E selesai.

## Draf promo video — 11 November 2026

Pengguna meminta rekaman fitur dan edit video dengan narasi Indonesia, lalu
mengosongkan RAM. [Draf/provenance/batas](design/promos/2026-11-11/README.md):30detik,
720×1280/30fps, empat cuplikan APK1.9.40 asli dari emulator baru, narasi sintetis,
subtitle, transisi dan bahan edit ZIP. Home take pertama onboarding ditolak lalu
direkam ulang; video final didekode penuh/ditinjau6frame. Helper akhir compile
lulus; tidak ada test perangkat/gerbang rilis baru yang dinyatakan selesai.
Emulator sementara ditutup/datanya dihapus setelah verifikasi marker/lokasi;
HP/emulator lama/WIP/stash/audio/rahasia/logo/master tetap. Video hanya lokal,
belum dipublikasikan; runtime APK/backend/biaya/model tetap. D02/E03 dan gerbang
publik tetap terbuka. Checkpoint source/storyboard/bukti wajib audit/commit/push.


## E03 — waktu prakiraan dan transport cuaca, beta1.9.40, 11 November 2026

Tanpa USB/HP/emulator: slot Open-Meteo wajib berurutan per jam dan mencakup
waktu yang diminta; deret kedaluwarsa/terlalu jauh di masa depan serta epoch
pecahan ditolak. Pilihan slot prakiraan terdekat mendatang tetap; BMKG tidak
diubah. Transport cuaca kini menolak redirect/retry otomatis dan read25/call25.
[Bukti/APK/batas](android/validation/weather-e03-2026-11-11/README.md):189unit lulus,
4skip,lint0error/878warning; satu GET publik host200/72slot berhasil diparse.
APK1.9.40/code67,49.680.570byte,SHA2568263961c8db002ed1b537704aced709b8c8bf1ff7a5d7a4a1d70f71313d54fcf.
Signer/audio/rahasia dikenal/WIP/stash/env/aset terjaga. HP tetap1.9.39; tidak
memasang APK/unggah Drive/deploy/provider/paid baru. Layout tetap, native tidak
 diulang. E03 parent masih[ ] untuk alert/kontrol/deduplikasi/doa/briefing;
D02/D02a/B01/C02/E04/F/G/publik tetap terbuka. Checkpoint audit/commit/push.


## D02 — observer dikoreksi, percobaan HP belum lulus, 11 November 2026

[Hasil/kegagalan/preservasi](android/validation/prayer-observer-hardening-2026-11-11/README.md):
19tes Python lulus. Command instrumentation lama tidak didukung tetapi exit0;
field false lama bukan bukti independen. Kini process dump/header/target dan
hasil run-as divalidasi; disconnect idle10detik menjadi belum konklusif.
Dua build helper lulus; runner --no-restart di HP timeout40detik sebelum membuat
alarm/state. Tidak menerapkan simulasi daya baru. Instrumentation dihentikan,
helper/marker dibersihkan, app dibuka normal,30alarm asli aktif.15pref byte-sama,
plan hanya memangkas1slot kedaluwarsa,20tabel seluruh baris sama/daya normal.
Laporan kesetaraan byte awal yang gagal tetap disimpan. Metode native eksperimen
tidak dimasukkan source; APK utama1.9.39/runtime tidak berubah. HP bebas dipakai.
D02/D02a/izin/lafaz dan B01/C02/E03/E04/F/G/publik tetap terbuka; jangan klaim
idle sudah lulus. Penerima/izin audio belum terverifikasi; draf belum dikirim.
WIP/stash/audio/desain/konfigurasi privat tetap; checkpoint audit/commit/push.


## D02 — daya pulih setelah USB kembali, 11 November 2026

Pengguna mengonfirmasi unlock dan menyambungkan USB kembali. Forced idle dan
simulasi baterai dilepas; low_power/stay-awake cocok baseline privat, layar
dibangunkan tanpa restart atau membuka Zikra. [Bukti pemulihan](android/validation/prayer-reboot-observer-phone-2026-11-11/README.md).
30alarm masa depan sesuai baseline setelah satu waktu lewat. Snapshot lengkap
pasca-sambung tidak menemukan marker pengiriman epoch pilihan/layanan adzan;
kondisi pada saat pengiriman tidak terukur. Uji idle tetap belum konklusif,
bukan lulus atau bug terbukti. Kegagalan pemulihan awal tetap dicatat, kini
status pending daya ditutup oleh bukti baru. HP bebas dipakai; jangan memulai
uji/restart baru tanpa koordinasi. APK1.9.39/runtime/audio/data pengguna tetap;
D02/D02a/izin/lafaz/B01/C02/E03/E04/F/G/publik tetap terbuka.


## D02 — restart teramati; USB putus saat idle, 11 November 2026

Satu restart USB baru disetujui pengguna. Observer laptop melihat boot baru dan
pemulihan dua set alarm stabil tanpa UI/instrumentation Zikra;31alarm sama,
16file preferensi byte-sama dan20tabel seluruh baris terpelihara.48file dicadangkan.
[Bukti dan batas](android/validation/prayer-reboot-observer-phone-2026-11-11/README.md).
Uji memakai alarm ADHAN asli berikutnya, tanpa mengubah jadwal/pilihan audio.
Idle/layar mati/setting hemat baterai terlihat sebelum USB putus; pengiriman
belum teramati. Hasil idle belum konklusif, bukan bug terbukti atau lulus.
**Pemulihan daya masih menunggu USB**: lima perintah finally gagal karena HP
terputus; retry juga belum menemukan HP. Saat tersambung, prioritaskan unforce
idle/battery reset dan pemulihan dua setting sesuai baseline privat, verifikasi;
jangan mulai tes baru/restart/install. Pengguna sudah diminta menyambungkan.
APK tetap1.9.39; runtime/helper/audio/backend/env/desain/WIP/stash tidak berubah.
D02/D02a,izin/lafaz,B01/C02/E03/E04/F/G/publik tetap terbuka. Draf izin belum dikirim.


## D02 — observer laptop dan draf izin, 11 November 2026

Metode menunggu Doze di instrumentation dipensiunkan; observer laptop baru hanya
membaca perangkat dengan serial eksplisit. Boot menunggu unlock dan dua set alarm
stabil; timeout/nol awal bukan vonis gagal. Idle memerlukan alarm asli sebelum
waktunya, marker epoch, layanan/notifikasi dan kondisi daya. [Bukti dan batas](android/validation/prayer-observer-2026-11-11/README.md):
15 tes Python lulus, replay sintetis lulus, parser dump lama 26/31/0/31 alarm
aktif; helper Android berhasil dikompilasi, tidak dipasang. Tes awal menemukan
pencocokan paket notifikasi .test yang terlalu longgar; diperbaiki dengan regresi.
HP/emulator tidak diakses. [Draf izin audio](docs/audio-permission/request-template.md)
belum dikirim; penerima/hak/lafaz belum diverifikasi. D02/D02a tetap[ ] sampai
bukti fisik dan hak terpenuhi. APK tetap1.9.39; runtime/data/power/izin/audio/
backend/env/cron/desain/WIP/stash tetap. B01/C02/E03/E04/F/G/publik tidak ditutup.
UI10/mode orang tua tetap ditunda; checkpoint wajib audit/commit/push.


## D02/D02a — checkpoint HP belum konklusif, 11 November 2026

HP1.9.39 dicadangkan48berkas/APK; pengguna melakukan restart dan menyetujui satu
restart USB. [Bukti, kegagalan dan preservasi](android/validation/prayer-recovery-d02-2026-11-11/README.md).
5eksekusi native selesai/3metode unik hanya arm/inspeksi preferensi/restore,
10unit jadwal-zona lulus. Snapshot31→0alarm setelah boot belum membuktikan bug:
proses receiver baru mulai41,718detik setelah snapshot, sebelum tindak lanjut
independen aplikasi sudah terbuka. Forced idle/hemat baterai tercapai tetapi
host instrumentation timeout150detik tanpa hasil playing; bukan kelulusan.
Pengaturan/power dipulihkan;16file preferensi dan20tabel sama dengan cadangan
fase terakhir setelah pengguna setuju tidak menyentuh HP. Cadangan awal tetap.
Helper/berkas uji/XML dibersihkan, simulasi baterai/idle dilepas,31alarm asli
aktif setelah pembukaan normal. HP bebas dipakai; jangan menganggap siap tes
terus-menerus. Tidak mengubah autostart/izin atau memasang ulang APK utama.
[Pasangan audio aktif](android/validation/adhan-source-current-2026-11-11/README.md)
Misyari/Subuh cocok hash penerbit, izin redistribusi/lafaz tetap belum terpenuhi.
D02/D02a tetap[ ]; jangan menandai reboot/Doze selesai dari snapshot awal.
Pengguna memasang39 dan melaporkan AI membalas saat laptop ditutup; keadaan daya
bukan pengukuran independen/shutdown, C02 tetap sesuai gerbang. Migrasi Google
Maps dibatalkan pengguna karena pembayaran awal; OSM yang sudah bekerja tetap,
draf Maps privat/bukan source aktif. Tidak ada runtime APK/provider/model/env/
cron/email baru. WIP/stash/audio/konfigurasi/desain tetap. B01,E03/E04,F/G/publik
terbuka; UI10/mode orang tua ditunda. Audit/commit/push checkpoint wajib.

## C02: timeout aplikasi diperbaiki, antrean dibedakan — 11 November 2026

Client sebelumnya call45detik tetapi read timeout bawaan10detik; gateway dapat
menunggu provider35detik. Uji HTTP nyata12detik: lama SocketTimeoutException,
baru200. Factory kini connect10/read45/write20/call60, tanpa retry/redirect.
Ini celah terbukti, belum bukti seluruh429 pengguna berasal darinya. Lua dan log
membedakan queue installation/global tanpa isi atau identitas; respons APK lama
tetap try_later dan batas kuota/slot tetap. [Bukti/APK/batas](android/validation/assistant-reliability-c02-2026-11-11/README.md):
91backend/0skip,181unit/4skip,lint0error/878warning. Server produksi diperbarui;
3chat hosted200/nol lease milik uji setelah tiap respons, fixture penolakan
installation terlihat pada log; key uji dibersihkan/counter pengguna tetap.
Kegagalan akses/deadline awal dicatat, final berurutan lulus. APK1.9.39/code66,
49.443.800byte,SHA256ac0e5dff4e5f8bdcee0a283e6677e227d18a799faa179e51198be9667610de43.
Signer/audio/0 credential dikenal dan WIP/stash/arsip/env/logo/master terjaga.
HP/emulator tidak diakses; HP terakhir1.9.37, APK baru belum terpasang. C02 tetap[ ]
menunggu uji HP/pesan berurutan/sleep dan diagnosis jika berulang; tidak ada
migrasi provider/model/paket/cron/Maps. B01,D02/D02a,E03/E04,F/G/publik tetap,
bukan seluruh A–D/E selesai. UI10/mode orang tua ditunda. Audit/commit/push wajib.

## Tipografi AI selesai beta emulator1.9.38 — 11 November 2026

Balasan AI kini sans-serif sistem/Roboto16sp, hierarki heading dan Markdown
native; ** tidak tampil mentah untuk format valid. Core Markwon4.6.2 tanpa
HTML/loader gambar/WebView; link hanya HTTPS tervalidasi saat diketuk. String
history/salin dan font utama/Home/Quran/aset tetap. [Bukti/APK/screenshot/batas](android/validation/ai-typography-2026-11-11/README.md):
179unit/4skip,lint0error/878warning,5native final/3metode unik termasuk320dp/
font200% dan terang/gelap. Build lint dan startup native awal gagal tetap
dicatat; penyebab startup pertama tidak dipastikan.32berkas emulator pulih
cocok hash, setting/helper dibersihkan/emulator ditutup. HP tidak diakses,
terakhir1.9.37. APK1.9.38/code65,49.477.018byte,SHA256
`837e7b45f833d4377168c01faf859844571720d02158d99c4ebec4419d7ce3d2`;
signer/audio sama/0 credential server dikenal, WIP/stash/arsip/env/logo/master
 tetap. Tidak ada provider/model/kuota/backend/cron baru atau migrasi Groq.
C02 antrean/sleep masih[ ]; B01,D02/D02a,E03/E04,F/G/publik tetap, bukan
seluruh A–D/E selesai. UI10/mode orang tua ditunda. Audit/commit/push wajib.


## C02: try_later gateway ditemukan — 11 November 2026

Tiga event produksi09.34WIB berstatus429/try_later, yaitu pemeriksaan antrean
Zikra sebelum inference, bukan kode kuota/provider_busy Cloudflare.
[Bukti dan batas](android/validation/assistant-queue-c02-2026-11-11/README.md).
Snapshot09.37tidak menemukan lease global aktif; identitas operator belum
independen cocok dengan HP. Belum dipastikan slot instalasi/global mana yang
menolak. Pengguna mengusulkan Groq; dokumentasinya diperiksa, belum migrasi.
Mengganti provider tidak otomatis menghilangkan gate antrean. C02 tetap[ ];
tidak ada counter/batas/model/env/paket/APK/dataHP berubah atau tes baru.
B01/D02/D02a/E03/E04/F/G/publik tetap, bukan seluruh A–D/E selesai.


## C02 masih intermiten; kategori error ditelusuri — 10 November 2026

Pengguna mengoreksi laporan dua pesan berhasil saat sleep: masih seperti
sebelumnya. C02 tetap[ ], laporan positif singkat tidak menutupnya. Event
server kini hanya assistant_rejected/status/kode allowlist, tanpa konten/token/
identitas pengguna. [Bukti/batas](android/validation/assistant-diagnostics-c02-2026-11-10/README.md):
89backend/0skip,deploy berhasil, satu429/rate_limited buatan dengan identitas
uji terpisah terlihat di runtime. Ini memvalidasi diagnostik, bukan menemukan
penyebab error pengguna. Kunci/counter uji dihapus, counter pengguna tetap.
Diminta satu pesan saat laptop menyala agar kode error aktual bisa dicocokkan.
APK1.9.37/model/env/kuota/paket/cron/dataHP/aset/audio tetap; tidak ada uji HP
baru. B01,D02/D02a,E03/E04,F/G/publik tetap; bukan seluruh A–D/E selesai.


## C02 perbaikan lease; uji sleep belum lulus — 10 November 2026

Pengguna melaporkan layanan penuh/connection error pada1.9.37 saat laptop
sleep; Wi-Fi router/internet lain tetap. CDN menunjukkan Android200 lalu5×429
sekitar17.12WIB, tanpa body error/identitas HP yang dipastikan. Ditemukan reply
berakhir sebelum Redisrelease; probe lama sesudah respons kedua masih1lease.
Reply kini menunggu release sebelum mengakhiri respons, finally tetap menangani
cancel. [Bukti dan batas](android/validation/assistant-lease-c02-2026-11-10/README.md):
87backend/0skip,3chat berurutan hosted200 dan0lease hidup sesudah setiap respons.
Server sudah diperbarui; APK1.9.37/dataHP/model/env/kuota/paket/cron/aset/audio
 tetap. Identitas uji dibersihkan, counter pengguna tidak direset. Ini bukan
bukti seluruh429 berasal dari celah ini; provider tetap bisa sibuk.
C02 tetap[ ] menunggu uji ulang pengguna dua pesan saat laptop sleep/mati.
B01,D02/D02a/E03/E04/F/G/publik tetap; bukan seluruh A–D/E selesai.


## C03b uji HP Cloudflare — 10 November 2026

APK 1.9.36 → 1.9.37 dipasang tanpa uninstall. Dua metode HP API36 lulus:
izin sekali/revoke/konteks defaultfalse dengan4HTTP sintetis, serta client asli
ke Vercel/Cloudflare untuk chat, foto, PDF3halaman dan TXT buatan dengan
identitas terpisah. [Bukti dan kegagalan](android/validation/assistant-cloudflare-phone-2026-11-10/README.md).
Compile/helper awal dan4NETWORK awal gagal tetap dicatat; foreground kemudian
lulus, penyebab awal belum dibuktikan.48berkas dicadangkan; isi awal17tabel,
preferensi/izin/setting/baterai terpelihara. Token/marker/helper dan registry/
usage/lease uji dibersihkan, gateway/mode lama dipulihkan; counter pengguna tetap.
APK/code/hash tetap1.9.37/64/e6576726,49.377.416byte; WIP/stash/arsip/env/aset/audio
 tetap. C03b dicentang sebatas beta ini; C02 laptop mati masih membutuhkan
pengguna. B01,D02/D02a hak audio/sisa fisik/vendor,E03/E04,F/G/publik tetap
terbuka, bukan seluruh A–D/E selesai. UI10/mode orang tua ditunda.


## Migrasi Cloudflare — beta 1.9.37, 10 November 2026

Pengguna memilih Cloudflare Workers AI untuk chat/foto/PDF setelah premium
Featherless berakhir, lalu meminta APK dahulu/HP nanti. Backend produksi READY,
chat/vision dikonfigurasi `@cf/qwen/qwen3.8-27b`; paket Free, tidak ada upgrade
atau fallback berbayar. Adapter/izin sekali per endpoint diperbarui; izin lama
bukan izin penerima baru dan APK lama ditolak409 sebelum kuota/inference.
Kuota harian, alokasi akun gratis, batas menit dan antrean dibedakan; counter
pengguna tetap. [Bukti, kegagalan dan batas](android/validation/assistant-cloudflare-2026-11-10/README.md):
86 backend/0skip,177 unit/4skip,3 native emulator lulus,lint0error/878warning.
Hosted awal11/12 strict lulus(PDF504); ulang8/9 strict lulus(PDF200/kode benar,
format menambahkan teks). Bukan klaim seluruh dokumen/format akurat.
APK1.9.37/code64,49.377.416byte,SHA256
`e65767269d53d91fc126dde387dd7f57c805da7cd35dcfecb283676463e55e94`;
signer/audio tetap,0 credential server dikenal.32berkas emulator pulih cocok
hash/pengaturan/helper dibersihkan/emulator ditutup; HP tidak diakses/terakhir
terbukti1.9.36. WIP/stash/arsip/env/logo/master tetap; E02 cron tetap.
Uji HP penyedia baru dan C02 laptop mati belum selesai. B01,D02/D02a hak audio/
sisa fisik/vendor,E03/E04,F/G serta publik tetap terbuka; bukan seluruh A–D/E
selesai. UI10/mode orang tua ditunda. Checkpoint wajib audit/commit/push.


## C07 dan E02 selesai teknis beta terarah — 10 November 2026

C07: foto, PDF tiga halaman dan DOCX UTF-16 dipilih melalui DocumentsUI asli
di HP. Grant hanya berkas terpilih, pembatalan menjaga draf, 0 request AI.
Digabungkan dengan bukti hosted/kamera terdahulu, C07 selesai beta terarah;
bukan klaim OCR sempurna atau RAG permanen. HP diperbarui 1.9.31 → 1.9.36 tanpa
uninstall. 41 berkas dicadangkan; baris asli 17 tabel, preferensi, izin dan
pengaturan terpelihara. Cache efemeris tanggal baru ditinjau pada koordinat/
zona sama. Helper, empat fixture dan registrasi FCM uji sudah dibersihkan.

E02: push FCM nyata hanya ke token HP dengan label UJI masuk saat proses app
tidak berjalan dan layar Dozing; duplikat tidak memperbarui waktu notifikasi,
ContentIntent membuka kejadian sama. Pengguna menyetujui pengganti cron Cloudflare
hanya Free dan secret pemicu di sana. Dashboard Free / $0 / Current plan
terverifikasi. Token deploy baru hanya Workers Scripts Write pada akun ini,
kedaluwarsa 10 Desember 2026, disimpan lokal privat; token AI tetap. Worker
memiliki satu secret pemicu, cron */5, workers.dev/preview mati; source terpasang
cocok hash. Pemicu awal gagal karena redirect:error tidak didukung runtime,
diperbaiki manual/tetap menolak3xx tanpa mengatur ulang jadwal. Dua pemicu
otomatis sesudah perbaikan, 09.40 dan 09.45 WIB, sukses dengan jarak300detik.
GitHub cadangan tetap aktif; dua run lamanya berjarak9jam4menit54detik.
E02 selesai teknis beta terarah, bukan jaminan latensi, semua subscriber/vendor
atau peringatan sebelum guncangan. Pengamatan jangka panjang tetap G02/operasi.

Adzan biasa dan Subuh saat ini selesai alami232.028ms/187.272ms; MAY_DUCK
melanjutkan, fokus lebih kuat menghentikan tanpa restart. 7 metode fisik unik
lulus, 79 backend/0skip dan5 Worker lulus; helper akhir dikompilasi. Kegagalan
awal, timeout cleanup dan audit cache tetap dicatat. [Bukti, receipt dan batas](android/validation/phone-closeout-2026-11-10/README.md).
APK1.9.36/code63 tetap50.062.820byte, signer/audio tetap, 0 nilai rahasia server
dikenal. WIP PROJECT_HANDOFF/stash/archive/env/logo/master tetap.

C02 laptop mati, B01, D02/D02a hak audio dan sisa fisik/vendor, E03/E04, F/G
serta publik tetap terbuka. Bukan seluruh A–D/E selesai. Penundaan UI10 dan mode
orang tua tetap. Semua checkpoint terverifikasi wajib audit/commit/push.


## E02 aktivasi disetujui; observasi jadwal — 9 November 2026

Observasi terakhir12.54WIB: aktivasi true/branchutama main/workflow
active dan enable API204, tetapi belum ada event schedule setelah20.9menit.
Manual sukses tidak menutup observasi ini; ekspresi telah disederhanakan ke2/5
dengan slot sama, sebab pasti belum diketahui. [Bukti observasi](android/validation/earthquake-e02-2026-11-09/schedule-observation.json).
Jangan menjanjikan pengiriman rutin5menit atau menyebut E02 selesai. Tidak ada
Androidtest/HP/deploy baru; pemeriksaan selanjutnya harus membaca run schedule
aktual lalu memeriksa langkah pollnya, tanpa meminta ulang persetujuan opt-in.

Pengguna kini menyetujui laporan nyata kepada seluruh pelanggan topic beta yang
opt-in, termasuk uji produksi/jadwal. Penolakan review sebelumnya diselesaikan
dengan persetujuan ini; jangan meminta ulang. Variable GitHub true, poll200/
checked/accepted0/uncertain0; workflow manual42568061078 dan langkah poll sukses.
[Bukti aktivasi](android/validation/earthquake-e02-2026-11-09/activation.json).
Run ber-event schedule belum teramati; jangan menyamakan uji manual dengan cron.
E02 tetap[ ] untuk observasi jadwal dan uji HP. Tidak mengirim kejadian palsu ke
topic; penerimaan semua subscriber belum diklaim. APK tetap1.9.36/code63/50.062.820byte.
Tidak ada build/HP/emulator/inference/key/model/paket baru; WIP/stash/audio/env/
aset/data tetap. Gerbang B01/C02/C07/D02/D02a/E03/E04/F/G/publik tetap.

## E02 parsial beta 1.9.36 — 9 November 2026

Push BMKG/Redis/FCM dan pengaturan opt-in sudah diterapkan; APK50.062.820byte,
SHA256`77d76f1879f75363f8c2737169206a4c371289bdeb90c341c8a4fa20512be2b5`.
[Bukti, kegagalan, batas dan APK](android/validation/earthquake-e02-2026-11-09/README.md):
79backend/0skip,176unit/4skip,11native akhir/9metode unik,lint0error/878warning.
Push nyata hanya ke token emulator: proses app tidak berjalan/layar mati,
duplikat tidak memperbarui notifikasi dan ketukan membuka kejadian sama offline.
Vercel baru dibuktikan health/401/405 dan baseline200/accepted0, bukan pengiriman
massal. **Jadwal GitHub tetap false; E02 tetap[ ].** Review persetujuan otomatis
menolak poll sesudah baseline karena bisa broadcast ke seluruh pelanggan topic;
perlu persetujuan eksplisit sebelum poll/aktivasi, lalu observasi run nyata dan
uji HP. Jangan melewati penolakan melalui workflow/jalur lain.
Pengirim FCM berizin terbatas disetujui pengguna; server key hanya lokal privat/
Vercel, pemicu terenkripsi GitHub, identitas client SDK sengaja di APK. AI/key/
model/paket tetap. Registrasi Google Play sempat gagal/timeout, pulih setelah
reboot; riwayat gagal dipertahankan. Registrasi uji dihapus,32berkas emulator
pulih cocok hash/setting dan izin diperiksa/helper dibersihkan/emulator ditutup.
HP tidak diakses, terakhir terbukti1.9.31. WIP/stash/audio/env/logo/master tetap.
B01/C02/C07/D02/D02a/E03/E04/F/G/publik serta penundaan UI10 tetap terbuka;
bukan klaim A–D/E selesai. Checkpoint terverifikasi tetap audit/commit/push.

## UI11 native selesai teknis beta 1.9.35 — 8 November 2026

Pengguna mengizinkan implementasi cuaca. Panel prakiraan Protector, detail langit/
transparan, rentang suhu tiga hari dari slot provider dan grafik lipat sudah
native; dock putih/hijau gelap mengikuti tema, langit mengikuti jam lokasi.
[Bukti/APK/batas](android/validation/weather-ui11-2026-11-08/README.md):173 unit
lulus/4 skip,6 native final/4 metode unik (termasuk320dp/font200),lint0 error/
883 warning. APK1.9.35/code62,45.924.363 byte,SHA256
`20e21d21b4c0e13ffe1ca67e4640ff49cf33ff7af8d4f001a3f7a691d4531ab3`.
Uji native memakai fixture/jaringan mati, bukan probe API aktual; client tetap
Open-Meteo/cache6jam dan tidak ada notifikasi/deploy/izin/model/paket baru.
32 berkas emulator pulih cocok hash, setting/helper dibersihkan/emulator ditutup;
HP tidak diakses. WIP/stash/audio/env/logo/master tetap, signer sama/0 nilai
credential dikenal di APK. UI11 ditutup sebatas beta ini, E03 alert/doa tetap
terbuka; E02 berikutnya sesuai rencana. B01/C02/C07/D02/D02a/F/G/publik dan
penundaan UI10 tetap; bukan klaim A–D/E/publik selesai seluruhnya.


## E01 selesai teknis beta — 2 November 2026

Pengguna mengizinkan lanjut bagian E sementara HP ditunda. Ini menggantikan
larangan urutan E pada catatan lama, tanpa menutup sisa A–D. Laporan/pin/waktu/
Shakemap kini satu snapshot, cache bertanggal maksimal 24 jam, detail mempertahankan
laporan pilihan dan redirect gambar dibatasi CDN BMKG/file sama. Source
`1ea08ee0b68a84e9ef6d2b920c3888c5b4109e5a`; [bukti dan batas](android/validation/earthquake-e01-2026-11-02/README.md).
169 unit/4 skip dan 21 eksekusi native final/18 metode unik lulus; lint 0 error/
861 warning. Feed/gambar nyata diuji 30 Oktober; font 200%/unit akhir/preservasi
2 November. Kegagalan probe awal, memori build dan native awal tetap dicatat.
APK lokal 1.9.34/code61, 45.738.506 byte, SHA256
`af44a35b6553b082a32699481fabc3e64227eab507e86b96954c3d51b074ec1c`.
32 berkas asli emulator pulih cocok hash; pengaturan/helper dibersihkan dan
emulator ditutup. HP tidak diakses, terakhir dibuktikan 1.9.31. WIP/stash/audio/
rahasia/ikon/artwork tetap. Tidak ada deploy/email/model/biaya berubah.
**Berikutnya E02**, lalu E03/E04; bukan klaim E atau A–D/publik selesai.
B01/C02 laptop mati/C07 fisik/D02/D02a/F/G tetap terbuka; UI10/mode orang tua
masih ditunda. Setiap checkpoint terverifikasi tetap audit/commit/push origin/main.


## C07 vision hosted tuntas terarah tanpa USB — 30 Oktober2026

Pengguna sedang memakai HP; jangan akses/berulang meminta USB. Model vision production kini`Qwen/Qwen3-VL-8B-Instruct`, sourcekonfigurasi`43f821a554ab15d35eeb5f0f97ca9912c5134a83`, setelah6sampel langsung dan7request hosted200/strictpass;72backend/0skip lulus.[Bukti/rollout/monitor/mail/audio](android/validation/non-usb-closeout-2026-10-30/README.md). Hanya variabel vision diganti,19entri env aplikasi lain snapshot-sama(maskedsecret tidak didekripsi);key/modelchatteks/provider/kuota/paket tetap. DeploymentREADY,health200/tanpatoken401;tidak ada build/HP/Androidtest baru,APK tetap1.9.33/HP terakhir1.9.31. C07bagianhosted sampel terpenuhi tetapi parent[ ] untuk picker/grant/izin HP;hasil gagal lama tetap. Monitor3scheduleterbaru success;C02laptopmati belum. B01mail30Oktober belum ada balasan;D02izin audio belum,draf lokal belum dikirim. WIP/stash/audio/rahasia/aset/dataHP tetap;B01/C02/D02/D02a/E/F/G/publik/UI10/modeorangtua sesuai gerbang/penundaan. Audit/commit/push tetap wajib.


## C07 lanjutan tanpa HP — 30 Oktober2026

Pengguna menunda HP; lanjut tanpa HP dan jangan berulang meminta USB. Source`61e16a41de22f0a2009b83acace1d0a0b6ba46ac` memperjelas HTTP503 upstream menjadi503/provider_busy,header60,tanpa auto-retry/body bocor.72backend/0skip lulus,server READY;[bukti aktual](android/validation/vision-reliability-c07-2026-10-30/README.md) menerima503/provider_busy dalam15.240ms, bukan foto berhasil. Model production Qwen2.5-VL-7B dibaca/dikonfirmasi,env tetap;API katalog404 dan kandidat Gemma403/perlu OAuth dicatat tanpa menerima ketentuan/mengganti model. **C07 tetap[ ].** Tidak ada Android/build/HP baru;APK lokal terakhir1.9.33,HP terakhir1.9.31. WIP/stash/audio/data/rahasia/aset tetap;C02/D02/D02a/B01/E/F/G/publik masih mengikuti gerbang terbuka,UI10/mode orang tua ditunda. Checkpoint terverifikasi tetap audit/commit/push.


## C07 parsial: kapasitas vision, beta1.9.33 — 30 Oktober2026

[Uji hosted/perbaikan/batas](android/validation/vision-capacity-c07-2026-10-30/README.md), source`200b42972420f94da6912c8cd8c04ae6f0bf1425`:HTTP200 berisi error kapasitas kini503/provider_busy dengan jeda60detik; Android membedakan layanan penuh dari kuota dan tidak auto-retry.71backend/0skip,163unit/4skip lulus,lint0error/861warning;APK lokal45.420.472byte,SHA256`5e3e820688c2ea9c3d1c03520456a4f2be8da48d24e8fb6f818831f2ff38256d`. Backend READY diperbarui,health200/tanpa token401,env/model/paket tetap. Uji4kasus hosted awal hanya2lulus strict;scan hitungan benar tetapi format gagal,foto jelas502dua kali;diagnosis model tercatat menunjukkancapacity_exhausted,probe setelahdeploy504. **Kesiapan vision belum lulus/C07 tetap[ ].** HP tidak tersambung/tidak diubah,terakhir1.9.31;native tidak diulang. Identitas uji dibersihkan,WIP/stash/audio/rahasia/aset tetap. C02 laptop mati/D02/D02a izin audio dan sisa fisik/B01/E/F/G/publik tetap terbuka;UI10 dan mode orang tua ditunda. Checkpoint terverifikasi tetap audit/commit/push.


## Tinjauan privasi kandidat1.9.32 — 30 Oktober2026

Permintaan pengguna memperhatikan keamanan/privasi dan batas file. [Laporan](security_best_practices_report.md), [bukti/APK/batas](android/validation/privacy-review-2026-10-30/README.md):aturan backup legacy+Android12 cloud/D2D mengecualikan9 domain, allowBackup tetapfalse. Source dan resource/referensi APK diperiksa;30 backend terarah/0skip lulus,lint0 error/861 warning. Java runtime tidak berubah; unit/native/transfer OEM fisik tidak diulang atau diklaim. APK lokal1.9.32/code59,45.420.244byte,SHA256`d616f0e6c80e44388ce4fc5104dcea0c63194d74ee4ca069806107e9c6747a2c`;signer sama/0 credential dikenal. HP tidak disentuh/terakhir1.9.31. Tidak ada deploy/backend/key/paket/data/desain/audio/WIP/stash berubah. Batas tetap1file8MB,PDF3halaman,DOCX/TXT12.000karakter,cachelampiran25MB; bukan indeks RAG permanen. Debug/release, privasi formal/usia/Data Safety/retensi seluruh layanan dan G03 tetap gerbang terbuka. C02/C07/B01/D02/D02a/E/F/G/publik tidak otomatis selesai; UI10 ditunda. Semua checkpoint terverifikasi tetap audit/commit/push.


## C03a izin sekali selesai beta1.9.31 — 30 Oktober2026

Pengguna menolak tinjauan tiap pesan. Izin dasar kini sekali per endpoint; setelah setuju pesan/lampiran pilihan langsung terkirim, data tambahan tetap melalui Alat → Kirim dengan konteks/defaultfalse dan izin dapat dicabut. [Bukti/APK/temuan/preservasi](android/validation/assistant-consent-once-2026-10-30/README.md):163 unit/4 skip,lint0 error/862 warning;1 metode native HP API36/3 eksekusi sukses (akhir4 request sintetis, bukan provider nyata). HP1.9.29→30→31/code58,45.418.699byte,SHA256`77f91b1d3846a194725308815940772573eb69401562894c6c75b3cc708aea4b`; signer sama/0 credential dikenal. Audit preservasi awal gagal karena mode Offline/gatewayflagfalse tak terjelaskan; pengguna menyangkal memilih Offline. Laporan asli tetap gagal, upaya mengubah status audit ditolak otomatis. Dua nilai mode dipulihkan terarah/backup/guard, lalu uji ulang mempertahankan Online dan verifikasi baru membuktikan17 tabel awal serta chat PDF pengguna tetap.33 berkas awal terjaga, setting/izin/baterai sama; helper/marker/fixture dibersihkan. Bukan klaim mode tidak pernah terganggu. Dua percobaan picker sebelumnya gagal/dihentikan, tidak dihitung lulus; helper eksperimen tetap privat. PDF manual pengguna dua halaman dibalas Online, bukan sertifikasi akurasi. C02/C07/B01/D02/D02a/E/F/G/publik tetap terbuka; UI10 ditunda. Backend/env/stash/audio/WIP/ikon/artwork tetap. Semua checkpoint terverifikasi tetap audit/commit/push.


## C07 parsial beta1.9.30 — 30 Oktober2026

Prioritas pengguna: pekerjaan terdekat A–D dahulu, lalu E. Parser DOCX kini menjaga tab/baris/kolom tabel, mendukung UTF-16 dan namespace Word strict/transitional, serta menolak DOCTYPE/entity dan anggaran berlebih. Dipisahkan menjadi AssistantDocx agar pengelola lampiran tetap terarah. [Bukti/APK/batas](android/validation/attachments-c07-2026-10-30/README.md):163 unit/4 skip,8 metode native emulatorAPI37.1 dan5 backend lampiran lulus;lint0 error/862 warning. Dua kegagalan prasyarat jaringan helper dicatat; helper diperbaiki agar memeriksa setelah Activity terbuka, jaringan produksi tetap. Draf gagal/batal dan foto saat putus/pulih tanpa auto-upload teruji.32 berkas asli emulator pulih cocok hash, settings/helper/marker dibersihkan/emulator ditutup. APK lokal `Zikra-Attachments-v1.9.30-beta-debug.apk`,45.465.323byte,SHA256`5fa27e7faa466dc4d56d42e99890156c571cd1fba121f0089eb2d78a5cd46661`,signer sama/0 credential dikenal. HP tidak disentuh, terakhir dibuktikan1.9.29. Tidak ada inference baru/backend deploy. C07 tetap[ ] untuk picker/grant/penolakan izin HP dan variasi OCR; C02 laptop mati/B01/D02/D02a/E/F/G/publik tetap terbuka. Sorotan UI10 ditunda, font/desain/ikon/artwork/audio/env/WIP/stash tetap. Semua checkpoint terverifikasi wajib audit/commit/push origin/main.


> **UI09 beta1.9.29 — 30 Oktober2026:** font Amiri Quran yang disetujui sudah dibundel dan HP aktual1.9.25 diperbarui ke1.9.29.163 unit/4 skip dan2 native HP API36/264 layout lulus;lint0 error/862 warning. [Bukti/data/font/APK](android/validation/quran-font-ui09-2026-10-30/README.md).17 tabel/preferensi/izin/pengaturan terjaga, helper dibersihkan. Sorotan ditunda pengguna (UI10), Mushaf QCF tetap; A–D/publik belum selesai seluruhnya.

> **C04 beta1.9.28 — 30 Oktober2026:** konteks kelas opt-in, preview snapshot, validasi backend dan telaah dua sampel Tadabbur selesai teknis terarah.163 unit/4 skip,69 backend tanpa skip,13 native akhir/10 metode unik lulus;lint0 error/862 warning. [Bukti/hosting/APK/batas](android/validation/assistant-context-c04-2026-10-30/README.md). APK45.617.459byte; HP tidak disentuh. C02/C07/B01/D02/D02a/F/G dan seluruh A–D/publik tetap mengikuti gerbang terbuka.

> **UI08 beta1.9.27 — 29 Oktober2026:** arah referensi disetujui pengguna dan tekstur pintu diterapkan pada arsitektur asli. Lampu siang mati/malam menyala, langit/parallax native tetap; 160 unit/4 skip dan4 native emulator lulus, lint0 error/862 warning. [Bukti dan batas](android/validation/mosque-approved-lighting-2026-10-29/README.md). APK45.336.132byte; HP tidak disentuh, belum diuji versi27. A–D/E04/F04/G/publik tetap belum selesai seluruhnya.

> **Riwayat sebelum persetujuan1.9.27 — tinjauan UI08:** pengguna menolak tampilan cahaya interior1.9.26. [Referensi revisi](design/concepts/mosque-lighting-review-2026-10-29/README.md) tersedia untuk tinjauan, belum disetujui/diintegrasikan. APK tetap; bukti teknis pencarian/cache tidak berarti penerimaan visual.

> **UI08 kandidat1.9.26 — 29 Oktober2026:** interior malam/siang dan pencarian masjid diperbaiki,160 unit/4 skip dan4 native final emulator lulus;lint0 error/862 warning. [Bukti, keandalan API dan batas](android/validation/mosque-repair-2026-10-29/README.md). APK43.857.302byte, HP tetap1.9.25; belum dipasang versi baru.

> **UI07 beta1.9.25 — 29 Oktober2026:** splash800ms disetujui dan lulus4 uji native HP API36.155 unit/4 skip,lint0 error/862 warning; update1.9.22 berisi terjaga. APK lokal folder pembungkus`Zikra-Splash-v1.9.25-beta-debug.apk`,43.638.534byte, SHA256`a7f54e6233ef42ba8b12266099de47e8532bafa0d2d3e1b31924468cbadcf583`. [Bukti/video/preservasi/batas](android/validation/splash-polish-2026-10-29/README.md). Seluruh A–D/F/G/publik belum otomatis selesai.

> **29 Oktober2026 — UI06:** tiga tindak lanjut visual tersedia pada beta1.9.24: dock AI, lampu malam, splash native. [Bukti dan batas](android/validation/ui-followup-2026-10-29/README.md). Ini tidak menutup gerbang HP, A–D, F04/G02 atau publik.


> **Checkpoint29 Oktober2026:** F03a tema global tersedia pada beta1.9.23, dengan uji emulator/kontras/draf/restart dan batas tertulis. [Bukti](android/validation/global-theme-2026-10-29/README.md). Pilihan aplikasi berlaku lintas halaman; langit tetap mengikuti waktu lokasi. Gerbang HP/vendor, aksesibilitas menyeluruh, sumber dan publik tidak ditutup oleh checkpoint ini.


Keputusan: **24 Oktober 2026**. Pengguna mendelegasikan penetapan cakupan: “yang penting selesai saja dulu silahkan kamu tentukan dlu saja”. Rincian di bawah adalah keputusan pengerjaan berdasarkan delegasi tersebut, bukan klaim pengguna telah memilih setiap detail secara terpisah. Acuan pekerjaan: [implementation plan](IMPLEMENTATION_PLAN.md), [audit A01](android/validation/release-audit/FEATURE_MATRIX.md), [PRD](ZIKRA_PRD.md).

Tujuan pertama adalah **rilis publik pertama Zikra yang lengkap dalam cakupan tertulis ini**, Android Java/XML, tanpa menambah desain/fitur baru di luar daftar selama penyelesaian. Beta adalah tahap pembuktian menuju tujuan itu, bukan pengganti rilis publik. Versi pengembangan yang sekarang 1.6.2 tidak diubah oleh keputusan dokumen ini; nomor versi distribusi final ditentukan pada G04 agar pembaruan Android tetap naik.

## Wajib untuk rilis publik pertama

| Modul | Hasil yang harus dapat digunakan | Pekerjaan / PRD |
| --- | --- | --- |
| Home, onboarding, lokasi, Profil | Onboarding jelas; jenjang pelajar/mahasiswa/umum dan nama lokal; lokasi/zona konsisten; jadwal, bacaan terakhir dan progres memakai data asli; pengaturan tersimpan | A03–A04, D01/D04/D06, F01–F03; F1.1/F3.1 |
| Salat dan Kiblat | Metode jadwal dinyatakan dan divalidasi; kota/GPS/manual; countdown; pengingat lokal luring terjadwal 7 hari dengan penjadwalan ulang; sakelar utama dan pilihan **adzan penuh / notifikasi biasa tanpa adzan / getar / mati** per salat, stop suara dan audio focus; mode senyap sesuai jam kelas; Kiblat dengan status sensor/kalibrasi. Diperjelas pengguna 25 Oktober, tugas D02a | D01–D02/D02a, G02; F1.1–F1.4 |
| Quran | 114 surah/30 juz, teks Arab/Indonesia dan transliterasi opsional bersumber; per ayat serta Mushaf pada perangkat yang mendukung; bookmark/bacaan terakhir; audio, seek/repeat/rentang/background; sorotan dan mengikuti audio saat timing cocok; gulir baca mandiri; paket unduhan/luring yang jelas | A03–A04, D03/D06; F2.1–F2.3 |
| Ibadah dan Hifz | Checklist/jurnal, streak harian/mingguan berdasar catatan; target Hifz/rentang dan latihan; target khatam 30/90/365 hari dengan progres yang dicatat pengguna | D05–D06; F1.3/F2.4 |
| Hadis | Dua koleksi **Bukhari dan Muslim** dari edisi/sumber yang tervalidasi, Arab/Indonesia, nomor/rujukan, cari nomor/kata, reader, bookmark/resume dan unduhan luring per koleksi. Label lengkap hanya sesudah cakupan sumber dibuktikan; tujuh kutipan tidak cukup | B01–B03 |
| Doa dan dzikir | Kategori belajar, makan, tidur, rumah, perjalanan, hujan/petir, musibah dan pagi/petang; Arab/Latin/arti/rujukan yang diperiksa, cari dan simpan luring. Jumlah final mengikuti entri yang lolos validasi, tidak dipaksakan sama dengan angka dokumentasi API. TTS dilabeli TTS, bukan rekaman qari | B04, E03 |
| Study | CRUD tugas, mata pelajaran/kuliah, deadline/prioritas/urutan; jadwal kelas mingguan; alarm kelas H-30/H-15 dan tenggat H-1/Hari-H; fokus/break/pause serta animasi; pulih background; pilihan jeda fokus saat adzan aktif tanpa menandai tugas selesai otomatis | D02/D04/D06, F04; F3.1–F3.4 |
| AI teks | Offline berbasis aturan dilabeli jelas; online Featherless melalui hosting HTTPS permanen, kuota/akses per instalasi tanpa meletakkan provider key di APK; error/batal/retry jelas; konteks nama/tugas/jadwal/salat hanya dengan izin; usulan tambah/edit tugas perlu tinjauan dan disimpan sekali; input suara bila recognizer tersedia | C01–C06; F5.1–F5.3/F5.5 |
| AI foto dan dokumen (tambahan 25 Oktober) | + pada composer untuk Kamera/Galeri/Dokumen; preview/hapus sebelum Kirim, tidak mengunggah otomatis. Foto melalui model vision yang aksesnya diuji; PDF/DOCX/TXT sesuai parser dan batas ukuran/halaman nyata, izin minimal dan gagal/batal jelas. Dukungan dokumen scan diuji dan batas pembacaan dijelaskan. Ini menggantikan penundaan Vision lama; belum implementasi atau selesai | C07, UI04, G03 |
| Tadabbur | Alur tanya makna dari ayat dengan sumber tafsir terverifikasi dan rujukan; batas jawaban jelas ketika sumber tidak tersedia. Jangan membuat kutipan keagamaan dari perkiraan model | C04, G03; F2.5 |
| Protector | Prakiraan kota dan animasi sesuai kondisi; BMKG episentrum/Shakemap/angka/waktu dari satu kejadian; waktu pembaruan/stale/error; push kejadian baru ketika aplikasi ditutup, deduplikasi dan detail ID yang tepat; panduan serta kontak/masjid dengan tindakan pengguna | E01–E04, C02/G02; F4.1–F4.2 |
| Pengingat kontekstual | Briefing pagi pilihan pengguna dari jadwal/tugas/cuaca/salat; doa perjalanan dari jadwal berangkat; dzikir pagi/petang; cuaca ekstrem memakai sumber/ambang yang dinyatakan. Semua dapat dimatikan, deduplikasi dan tidak menjanjikan prediksi gempa | D04/E03/B04; F3.5/F4.2–F4.4 |
| Desain dan kualitas | Pertahankan referensi visual yang disetujui; tata letak/font konsisten, Arab terbaca, teks besar/keyboard/izin/error terjangkau; animasi berhenti di background dan mengikuti pengaturan sistem; pengukuran HP untuk RAM/baterai | F01–F04, G01–G02 |
| Paket distribusi | Pembaruan mempertahankan data; signed release/hash/source cocok, atribusi/privasi/dukungan; **unduhan awal <100.000.000 byte** setiap versi. Audio/paket besar opsional, tampilkan ukuran dan hapus unduhan/cache | D06, G03–G05 |

Pencarian Hadis harus membedakan seluruh koleksi dari paket di perangkat. Semua data pengguna lokal tetap dapat dipakai tanpa akun. Akses AI/push dapat memiliki identitas instalasi teknis yang bisa dicabut; ini bukan akun pengguna/sinkronisasi profil.

## Beta tertutup sebelum publik

Beta boleh dibagikan untuk menguji hasil per modul setelah syarat dasarnya terpenuhi:

- Jalur instal baru/onboarding/lokasi/Home, Quran baca/audio, Study/tugas, Hadis/Doa dan Profil stabil; tidak merusak data pada pembaruan. A03–A04 dan kasus relevan B/D/F/G harus mempunyai bukti.
- Data contoh diberi label; sumber konten harus lolos B01/B04 sebelum distribusi paket, termasuk dalam beta. Jangan membagikan provider key, token gateway bersama, atau konfigurasi pengembang dalam APK.
- Modul AI/push yang belum memiliki hosting diuji privat dan dinyatakan belum tersedia; tombol publik tidak berpura-pura berfungsi. Rilis publik tetap menunggu hasil wajib di tabel, tanpa menghapus fiturnya dari cakupan.
- Kondisi gagal/izin ditolak/luring tidak menyebabkan crash atau statistik palsu. Keterbatasan qari/timing/Mushaf/recognizer/sensor disebut sesuai hasil pemeriksaan.
- Setiap APK beta memiliki versi, hash, source checkpoint, daftar skenario lulus dan kekurangan. Emulator membantu pemeriksaan; G02 dan publik tetap memerlukan HP fisik.

## Ditunda ke pembaruan setelah rilis pertama

| Fitur | Keputusan dan alasan | Jejak pekerjaan |
| --- | --- | --- |
| Akun/login, cloud backup dan sinkronisasi antar-HP | Ditunda agar model lokal stabil lebih dahulu. Tidak otomatis mengirim profil, jadwal, tugas dan bookmark ke server. Wajib menjaga data lokal/migrasi untuk rilis pertama; belum menjanjikan pemulihan ketika HP hilang | D06 bagian lokal tetap wajib; pengguna28 Oktober meminta antrean F05–F07 untuk login opsional, cadangan/pemulihan dan sinkronisasi. Belum implementasi atau otomatis syarat rilis pertama |
| Dashboard admin web khusus | Antrean lanjutan28 Oktober; mulai dari layanan/kuota/gangguan dan hak akses operator, tanpa akses bawaan ke chat/jurnal pribadi. Alat operator backend yang ada berbeda dari dashboard web | F08; belum membuat layanan baru atau mengaktifkan biaya |
| Infaq/pembayaran | Di luar navigasi rilis pertama; jangan menyambungkan pembayaran atau membuat klaim donasi. Source panduan lama dipertahankan sampai keputusan pengembangan berikutnya; jangan hapus data/draft sebagai efek keputusan ini | Temuan Infaq A01; bukan kebutuhan inti PRD |
| XP/badge, keuangan anak kos, mode Ramadan khusus | Tetap post-MVP sesuai PRD. Target khatam umum dan Pomodoro tetap wajib; tidak ditunda bersama fitur ini | Backlog PRD fase 2 |
| Koleksi Hadis tambahan dan variasi ilustrasi provinsi | Dua koleksi dan keluarga aset yang disetujui lebih dahulu; tambah setelah sumber dan kinerja dasar andal | B01–B03/F04 untuk baseline; ekspansi sesudah rilis |

Ini adalah penundaan berdasarkan kewenangan yang diberikan pengguna pada 24 Oktober, bukan checkbox selesai. Kebutuhan tetap dapat dilacak di PRD/plan dan dibuka lagi pada fase yang disebut.

## Bersyarat terhadap kemampuan perangkat/layanan

- Mushaf QCF halaman dipakai pada Android yang didukung (saat ini 12+); perangkat lain tetap mendapat reader per ayat, tanpa tombol mati yang membingungkan. Luring menyebut paket yang benar-benar tersimpan.
- Sorotan kata hanya untuk rekaman dan timing cocok; jika tidak, gunakan sorotan ayat yang benar dan status yang jujur. D03 wajib menguji sumber yang ditawarkan sebagai didukung.
- Dikte suara dan sensor Kiblat memiliki keadaan tidak tersedia/izin ditolak serta fallback mengetik/petunjuk arah; tidak ada janji mendukung sensor yang tidak dimiliki HP.
- Notifikasi bergantung izin dan batas sistem; jadwal/layanan dipulihkan sesuai kemampuan Android. Push gempa tetap wajib, tetapi bukan jaminan datang sebelum guncangan atau deteksi dari sensor HP.
- Konten/peta/cuaca/provider memerlukan pemeriksaan sumber, penggunaan dan kegagalan. Bila suatu kandidat tidak memenuhi syarat, ganti sumber atau eskalasi keputusan cakupan; jangan menandai fitur wajib selesai karena API pernah merespons.

## Urutan kerja dan definisi selesai

1. **A03 → A04 terlebih dahulu**, sesuai koreksi pengguna 24 Oktober setelah keputusan cakupan: rapikan WIP audio dan buktikan integrasi data. WIP dipertahankan terpisah bila belum lolos; keamanan diperhatikan selama pengerjaan. Penelitian B01 yang sudah ada tetap disimpan.
2. **B01**, lanjut validasi/pilih sumber Hadis setelah A04; tulis keputusan serta bukti. Bila terhambat sumber, laporkan bagian yang belum terbukti dan kerjakan persiapan yang tidak bergantung izin; jangan mengimpor distribusi secara diam-diam.
3. **B02 → B03 → B04**, lengkapkan konten dan penyimpanan dengan uji terarah. Setiap item ditutup sendiri; jangan menggabungkan seluruh tahap menjadi satu klaim selesai.
4. **C → D → E** mengikuti dependensi plan. C02 menyiapkan hosting saat konfigurasi konkret siap; keputusan yang membutuhkan akun hosting/biaya dibawa kepada pengguna pada titik itu.
5. **F → G**, penyelarasan menyeluruh dan uji beta/HP sebelum signed release publik. Kerapian dasar/error penting diperbaiki dalam modul yang sedang dikerjakan.

“Selesai proyek rilis pertama” berarti seluruh syarat wajib di dokumen ini dan G05 terpenuhi. Jumlah centang, screenshot konsep, respons API, build berhasil atau total ukuran APK saja tidak memenuhi definisi itu. Akun dan backlog yang masih ditunda tetap dicatat sebagai rencana setelah rilis; bukan diam-diam dianggap sudah terimplementasi.

**A02 selesai sebagai keputusan cakupan.** Belum ada fitur runtime baru atau perubahan versi APK dalam dokumen ini.

## Perubahan keputusan 25 Oktober — Home dan lampiran AI

Pengguna menyetujui Home Al-Aqsa siang/malam dan meminta foto/file ditambahkan, sehingga penundaan Vision pada keputusan 24 Oktober tidak lagi berlaku untuk kebutuhan lampiran ini. AI otomatis memakai ketersediaan internet dan hasil layanan, dengan status jelas dan opsi panduan offline terbatas. Persetujuan Home bukan bukti runtime; C07, C03 dan UI04 tetap terbuka. [Keputusan terbaru](design/concepts/home-approved-ai-auto-attachments-2026-10-25.md).

Permintaan lanjutan menambahkan riwayat beberapa chat lokal (C08) serta kreasi gambar online (C09). Keduanya tetap antrean dengan kriteria uji di implementation plan; rendering gambar memerlukan layanan/model/akses/biaya yang dibuktikan sebelum tersedia. [Konsep dan batas](design/concepts/ai-history-creation-review-2026-10-25-v4.md). Koreksi Home 1.9.1 sudah mempunyai [bukti native](android/validation/home-reference-match-2026-10-25/README.md), tetapi bukan bukti implementasi fitur AI tersebut atau penutupan UI04/F/G.

**Status terbaru 1.9.5/sourcea5d070a:** C08 riwayat lokal sudah selesai dengan bukti
migrasi/native/consent; menggantikan status antrean C08 di atas. AI otomatis dan
lampiran foto/PDF/DOCX/TXT sudah diterapkan serta diuji ke provider lewat Vercel,
tetapi C03/C07 tetap terbuka untuk gerbang yang tersisa. C09 generasi gambar belum
tersedia, Gemini API baru kandidat tanpa aktivasi biaya. [Bukti dan batas](android/validation/ai-home-refresh-2026-10-25/README.md).
Ini checkpoint beta, tidak mengubah cakupan wajib rilis publik atau menutup C/F/G.

**26 Oktober, beta1.9.6:** pengguna memilih Cloudflare Workers AI Free untuk C09,
memasukkan token secara lokal dan mengonfirmasi paket Free. C09 kini selesai
sebatas beta: FLUX.1 Schnell nyata dari APK lewat Vercel, prompt-only consent,
kuota3 per instalasi/20 total per hari, dedup/batal/error, riwayat/preview/save/share
privat dan batas installer dibuktikan. Ini menggantikan status antrean C09/Gemini
sebagai kandidat di atas. [Bukti dan batas](android/validation/image-generation-2026-10-26/README.md).
Tidak mengaktifkan billing atau menambah Worker/hosting/database baru. Pengujian
pemilih tujuan/share app di HP tetap G02; C lain dan rilis publik belum ditutup.

**26 Oktober2026, beta1.9.9:** D04 selesai untuk implementasi/uji beta jadwal
akademik lokal, jenjang/istilah/SKS, prioritas/deadline, alarm kelas dan tenggat,
serta regresi timer/progres nyata. [Bukti dan batas](android/validation/study-d04-2026-10-26/README.md).
HP/Doze/reboot/vendor tetap G02, jeda fokus karena adzan tetap D02/D02a;
konteks jadwal ke AI tetap C04 dengan opt-in. Tidak menutup publik, C02/C07/B01
atau menganggap preservasi baseline emulator kosong sebagai upgrade data HP.

**26 Oktober2026, beta1.9.10:** D01 masih parsial. Guard kepercayaan sensor Kiblat
serta Ashar kutub diperbaiki dan diuji emulator/rujukan hitungan; sensor/GPS/arah
fisik masih memerlukan HP. [Bukti/batas](android/validation/location-qibla-d01-2026-10-26/README.md).
Tidak menutup D01 atau D02/D02a/D03/D05/D06/publik dan tidak mengubah jadwal
menjadi feed resmi Kemenag. Prioritas terbaru pengguna mengarah bagian D.

**27 Oktober2026, D06:** pembaruan APK asli1.7.6/1.9.13 ke1.9.14 serta reinstall terbaru mempertahankan fixture terisi (tugas/target/bookmark/nama/lokasi/pengaturan/AI, kelas dan jurnal bila tersedia). Selesai teknis penyimpanan lokal beta; bukan cloud backup/sinkronisasi/uninstall/downgrade/HP/publik. [Bukti dan batas](android/validation/storage-d06-2026-10-27/README.md). Prioritas pengguna: selesaikan D sebelum E; D01/D02/D02a tetap terbuka.

**28 Oktober2026, beta1.9.15:** atas permintaan pengguna, subset penyelarasan
Home/Hifz/kalender/Profil dan logo/ikon diterapkan dengan data nyata. [Bukti
native/upgrade/source dan batas](android/validation/refined-ui-2026-10-28/README.md).
Bukan perubahan keputusan penundaan akun/cloud atau penutupan F global/publik.
Google login dan dashboard admin dibahas sebagai kemungkinan lanjutan; belum
ada otorisasi implementasi/layanan baru atau sinkronisasi data pengguna.

**28 Oktober2026, D01:** bukti HP POCO F7/API36 melengkapi uji hitungan/emulator,
sehingga D01 ditutup teknis beta terarah. Perkiraan awal timeout lalu retry
berhasil; acuan arah merupakan konfirmasi manual. [Bukti dan batas](android/validation/location-phone-d01-2026-10-28/README.md).
Tidak menutup adzan/sumber audio/D02/D02a atau gerbang publik. APK tetap1.9.15.

**28 Oktober2026, beta1.9.18:** pengguna memilih Kemenag sebagai acuan Indonesia. Tabel kota myQuran v3 (deklarasi sumber Kemenag) kini diprioritaskan setelah pembandingan enam kota-bulan langsung terhadap endpoint publik Bimas Islam. Cache dua bulan tetap offline dan konsumen Home/countdown/alarm memakai resolver sama; impor tidak diberi tambahan2menit. Wilayah/tanggal tanpa tabel valid memakai hitungan lokal cadangan yang dilabeli, tidak mengaku jadwal resmi. [Bukti/receipt/batas](android/validation/prayer-source-2026-10-28/README.md). Bukan akses API resmi terautentikasi atau verifikasi semua wilayah/tanggal; sumber audio, HP/vendor dan gerbang publik tetap mengikuti checklist.

**28 Oktober2026, beta1.9.22:** [checkpoint HP/adzan/preservasi](android/validation/prayer-phone-2026-10-28/README.md) menyimpan uji pagi,20mode/30epoch, fokus dan pilihan rekaman biasa pengganti. Isi data tetap; refresh lokasi/izin pengguna dipertahankan. Sumber/ketentuan distribusi audio dan sisa fisik tetap D02/D02a; seluruh A–D/publik belum ditutup. [Monitor C02 terjadwal](android/validation/backend-monitor-2026-10-28/README.md) sudah teramati sukses, uji laptop mati masih perlu.
