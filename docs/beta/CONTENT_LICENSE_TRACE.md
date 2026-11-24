# Penelusuran sumber dan izin

## Cakupan kandidat 1.9.65 — 24 November 2026

Audit APK signed dengan SHA-256 yang cocok dengan [manifest](internal-artifact.json)
menemukan hanya `assets/bundled-adhan/regular.mp3` dan `fajr.mp3` di antara berkas
audio dan video. Kedua rekaman ini sudah dipetakan ke kredit CC0 dan CC BY 4.0.
Rekaman Murottal dan video promo tidak ditanam di APK.

| Jalur yang dipakai | Bukti dari source dan penyedia | Keputusan untuk beta publik |
|---|---|---|
| Murottal biasa dan unduhan | `Recitation.url` dipakai untuk streaming dan unduhan per ayat. Ketiga edisi 128 kbps di aplikasi tercantum pada [CDN resmi](https://alquran.cloud/cdn). Pengguna memulai unduhan, berkas masuk `filesDir/quran-audio`, dan menu Audio offline memberi kredit serta opsi hapus. | [Ketentuan Al Quran Cloud](https://alquran.cloud/terms-and-conditions) mendukung streaming dan unduh untuk penggunaan pribadi/edukasi. Hak rekaman tetap pada pemiliknya. Konfirmasi cakupan penggunaan beta sebelum distribusi publik dan telaah ulang sebelum penggunaan komersial. |
| Murottal dengan sorotan kata | `TimedAudioRepository` mengambil data ayat dan rekaman terpisah dari Quran Foundation/Quran.com. JSON ditahan dalam memori. Maksimal tiga rekaman disimpan sementara, dibersihkan saat repositori ditutup atau dibuka lagi setelah sesi terhenti. Kredit tersedia di Mushaf. | [Ketentuan Quran Foundation §3.1](https://api-docs.quran.com/legal/developer-terms/#31-acceptable-use) membatasi cache biasa hingga satu minggu. Cache audio ini belum punya batas usia eksplisit dan pembersihan setelah proses mati baru berjalan saat repositori dibuka lagi. Perlu perbaikan dan tes kedaluwarsa sebelum menutup jalur ini. |
| Promo 52 detik | Catatan render menghubungkan suara Putri Maharani dengan demo publik ElevenLabs tanpa paket berbayar baru. Tidak ditemukan bukti hak komersial untuk output tersebut. [Ketentuan ElevenLabs](https://elevenlabs.io/terms-of-use) dan [panduan lisensi paket](https://elevenlabs.io/docs/overview/administration/billing) membatasi output gratis ke penggunaan nonkomersial dengan atribusi. | Pertahankan promo untuk tinjauan terlindungi. Untuk rilis tanpa biaya, siapkan halaman publik tanpa promo bernarasi atau pakai rekaman pengganti dengan izin yang sesuai. Publikasi versi sekarang menunggu bukti hak dan atribusi yang cocok. |

Paket persyaratan [privasi Quran Foundation](https://api-docs.quran.com/legal/developer-privacy/)
dan ketentuan pengembang meminta kebijakan privasi serta Ketentuan Penggunaan
yang dapat dijangkau publik. Draf situs lokal kini menjelaskan kedua jalur
Murottal, IP dan permintaan bacaan, unduhan privat, dan fungsi hapus. Persetujuan
draf, Ketentuan Penggunaan, audit izin data sensitif serta daftar penyedia
masih terbuka. Catatan 15 November di bawah memberi jejak sumber sebelumnya.

## Audit awal — 15 November 2026

Penelusuran primer dan pencocokan bukti proyek. Bukan putusan hukum/izin atas nama pemilik. Tidak ada penggantian konten, email atau publikasi. Semua URL diperiksa15November kecuali yang ditandai bukti terdahulu. Tidak semua sumber perlu surat izin terpisah; lisensi publik yang sesuai dapat menjadi dasarnya.

## Adzan aktif dan sejarah

Dua file aktif berasal dari [Islamdownload](https://islamdownload.net/123801-download-suara-adzan.html): Adzan-Misyari-Rasyid.mp3 normal, fajr_128_44.mp3 Subuh. Hash11November cocok byte berkas pengguna dan paket53. [Bukti](../../android/validation/adhan-source-current-2026-11-11/file-match.json).
Sumber yang pernah dipilih mecca_56_22.mp3 dan adzan_by_mishari_rashid_al-afasy-2.mp3 juga ditautkan halaman yang sama. Pasangan terakhir dipertahankan.
Halaman menawarkan unduh gratis dan label Misyari/Subuh. Pemeriksaan halaman, beranda dan pencarian ketentuan belum menemukan izin eksplisit penyertaan APK atau bukti kewenangan pemilik rekaman. /terms tidak dapat diambil. Tidak menyimpulkan izin dilarang atau bahwa halaman seperti itu tidak mungkin ada.
Jejak sumber sudah tersedia sejak27Oktober dan pasangan aktif diverifikasi11November. Meminta pengguna mengingat sumber kembali tidak diperlukan. Pengguna menyatakan rekomendasi awal berasal dari asisten; hal itu dicatat, tidak menyalahkan pengguna atas belum adanya bukti lisensi.
Tindak lanjut: verifikasi pihak berwenang dan cakupan rekaman tertentu, atau siapkan alternatif berlisensi eksplisit untuk keputusan pengguna. [Draf permintaan tersedia](../audio-permission/request-template.md), belum dikirim, penerima belum terverifikasi.

## Quran dan Hadis

| Sumber | Izin yang ditemukan | Batas/tindak lanjut Zikra |
|---|---|---|
| [Quran Foundation](https://api-docs.quran.com/legal/developer-terms/) | Penayangan dalam aplikasi diizinkan bersyarat; backend privat sendiri tidak otomatis dianggap redistribusi | Cache biasa≤1minggu; resource yang tersedia ContentSync harus memakai jalur itu, nextsync≤7hari saat tersambung. Paket database prebuilt berbeda. Font/gambarMushaf dapat dibundel terintegrasi dengan akun aktif/credit. Cocokkan resource, akun dan implementasi, bukan meminta surat untuk semua penggunaan |
| [Gading hadith-api](https://github.com/gadingnst/hadith-api) | Repository MIT dan menyebut Tafsirq sebagai sumber | [Tafsirq](https://tafsirq.com/page/source) membedakan open/scraping/partner/usergenerated tanpa memetakan edisi/penerjemah dua kitab. Rantai hak terjemahan/kelengkapan tetap B01; MIT repository saja belum menjawabnya |
| [HadeethEnc](https://hadeethenc.com/en/home) | Unduh dan terbit ulang diizinkan dengan7syarat | Pertahankan isi, sumber, versi di dokumen; laporkan koreksi, ikuti pembaruan dan tanpa iklan tidak pantas. Bukti Indonesia1.22.0 ada. LamanID mendapat429, versiEN berhasil diperiksa. Koleksi2260pilihan tidak menggantikan dua kitab lengkap |
| [EQuran](https://equran.id/terms) | API untuk aplikasi dan berbagi dengan sumber diizinkan, penggunaan wajar | Dilarang menjual data; terjemahan/tafsir/audio tetap dilindungi. APIaccess bukan izin semua rekaman pihak ketiga. Cocokkan Doa/tafsir/atribusi yang dipakai |
| CDN Islamic Network/audioQuran | Inventaris layanan mengandung jalur ini | Hak tiap rekaman dan kebijakan cache masih perlu pemeriksaan spesifik; tidak tercakup otomatis oleh izin QF |

## Peta, cuaca, jadwal dan astronomi

| Sumber | Dasar tersedia | Batas |
|---|---|---|
| [OSMdata](https://www.openstreetmap.org/copyright) | ODbL dengan credit/sharealike sesuai jenis hasil | [Tiles](https://operations.osmfoundation.org/policies/tiles/) memiliki aturan layanan terpisah: useragent,cache,atribusi, tanpa bulk/prefetch; tanpaSLA. Overpass harus dilihat peroperator |
| [NaturalEarth](https://www.naturalearthdata.com/about/terms-of-use/) | Publicdomain, modifikasi/distribusi diizinkan | Notice peta turunan lokal sudah ada; tidak memerlukan surat tersendiri |
| [Open-Meteo](https://open-meteo.com/en/terms) | DataCCBY4.0; FreeAPI nonkomersial | Batas10k/hari,5k/jam,600/menit. Iklan/langganan/promosi komersial berbeda dari aplikasi nonprofit; gratisdiunduh belum menentukan kategori |
| [BMKG](https://data.bmkg.go.id/gempabumi/) | Portal data terbuka, syarat sumberBMKG terlihat | Batas60request/menit/IP. Ini laporan kejadian, bukan prediksi atau jaminan push tepatwaktu |
| [myQuran](https://api.myquran.com/v3/doc) | APIpublik didokumentasikan | Lisensi rinci penyimpanan/distribusi tabel belum ditemukan pada halamanroot/doc yang dibaca; perantara tidak diklaim APIresmiKemenag |
| [USNO](https://aa.usno.navy.mil/data/api) | Dokumentasi menyebut hasil dapat ditanam dalam aplikasi | Peninjauan ketentuan spesifik/beban belum lengkap; dokumentasiAPI saja bukan penutupan seluruh kebijakan |

## Font, ilustrasi, AI dan promo

AmiriQuran,PlusJakartaSans,Roboto sudah memiliki buktiOFL dan notice pada [auditfont](../../android/validation/bundled-fonts-2026-11-12/README.md). FontCDNQCF mengikuti telaahQF terpisah.
Asetlogo/Home/cuaca diinventaris desain dan dipertahankan. Tidak mengklaim seluruh hakilustrasi/alatgenerasi sudah selesai hanya karena visual disetujui.
Cloudflare/modelAI, Vercel,Redis,FCM tetap mengikuti [inventaris layanan](SERVICES.md) dan [retensi](RETENTION_REVIEW.md); hakmodel/kelayakanremaja/akun harus dibedakan dari lisensiAPK.
Promo memakai demoElevenLabs menurut catatanrender. [Terms9Oktober2026](https://elevenlabs.io/terms-of-use) membatasi penggunaangratis ke nonkomersial. Belum ada bukti entitlementkomersial saatgenerasi atau hakspesifikoutput terakhir; jangan menyimpulkan upgrade sekarang otomatis melisensikan outputlama. Video dan promo belum dipublikasikan.

## Kesimpulan tindakan

Izin publik tersedia untuk beberapa komponen, jadi pertanyaan “semua harus minta surat” terlalu luas. Penghalang paling konkret ialah kewenangan/izin dua rekaman adzan dan sumber/hak terjemahan Hadis lengkap. QF/EQuran/HadeethEnc memiliki ketentuan yang dapat dipenuhi, perlu pemadanan terhadap implementasi. Status rilis tetapHOLD. Penelusuran ini belum mencakup seluruh dependencytransitif/model/artwork sampai tuntas.

## Lanjutan penelusuran15November

[AlQuranCloudterms13November](https://alquran.cloud/terms-and-conditions) mencakup API/audioCDN: teks dapat disimpan/ditampilkan tanpa perubahan dan dengan sumber; terjemahan/rekaman tetap hak pembuat. Rekaman boleh streaming/embed/download personal-edukasi; produkkomersial dapat diminta menghapus rekaman. Ini menambah bukti penggunaan edukasi, tidak menyimpulkan seluruh model komersial Zikra sudah berizin.
[ElevenLabsbilling](https://elevenlabs.io/docs/overview/administration/billing) mengonfirmasi free nonkomersial dengan atribusi, hakkomersial untuk konten yang dibuat pada paketberbayar. [Penjelasan berakhirlangganan](https://elevenlabs.io/docs/help-center/account/general/what-happens-to-my-content-after-my-subscription-ends) mempertahankan hak output yang dibuat selama langganan. Paket saat pembuatan output terakhir tetap perlu dibuktikan.
Asetcuaca master mencatat built-inimagegen23Oktober pada design/assets/protector-weather-v2/PROMPTS.md. Karakterpromo/gaya telah dicatat pada design/promos/2026-11-12/README.md. Ini membuktikan jejakgenerasi proyek; bukti akun/toolterms dan hak referensi tidak otomatis selesai.

### Kandidat alternatif adzan, belum dipilih

[Beautifuladhan](https://commons.wikimedia.org/wiki/File:Beautiful_adhan.ogg), uploaderAdam-synagda, ownwork29April2022, durasi154detik, halaman mencantumkanCC0. Ini kandidat dengan pernyataan izin eksplisit, bukan rekamanMisyari. Belum diunduh/didengar/diperiksa lafaz/dites dan tidak dianggap cocokSubuh. Tidak mengganti rekaman pilihan pengguna dari hasil pencarian saja.


### Audio bawaan pada kandidat berikutnya

Untuk Zuhur hingga Isya, Zikra menyiapkan rekaman CC0 [Beautiful adhan.ogg](https://commons.wikimedia.org/wiki/File:Beautiful_adhan.ogg) karya Adam-synagda. Rekaman dikonversi ke MP3 tanpa dipotong. Pada build sebelum adzan Subuh ditambahkan, audit menemukan satu MP3 tanpa kompresi ZIP dengan hash yang cocok. Uji emulator mencakup pemilihan suara bawaan tanpa impor, opsi OFF, serta regresi impor, notifikasi dan alarm dengan audio lokal. Sumber, lisensi, perubahan format, dan hash berkas tercatat di [kredit audio](../../android/app/src/main/assets/bundled-adhan/CREDITS.txt).

Untuk Subuh, build standar menyertakan [Developer Athan 3 (Fajr)](https://github.com/nandxorandor/Athan/blob/ad2bf1799add51cca4aaf1f123b1c212378c7786/app/src/main/assets/athan/fajr/Developer_Athan-3_Fajr.mp3) oleh Ahmed Khalaf. [Pembuat merilis rekamannya dengan CC BY 4.0](https://github.com/nandxorandor/Athan/blob/ad2bf1799add51cca4aaf1f123b1c212378c7786/README.md#licence) dan menjelaskan adanya lafaz khusus Subuh. Zikra memakai MP3 sumber tanpa perubahan. Kredit dan tautan lisensi tersedia di Tentang Zikra serta di kredit audio dalam APK. Pemeriksaan kandidat standar 1.9.60 menemukan dua MP3 berlisensi tanpa kompresi ZIP dan hash yang cocok dengan kredit audio. [Bukti APK bertanda tangan dan 11 tes emulator Android 36](../../android/validation/bundled-adhan-2026-11-21/README.md).

### Pasangan lama pada build internal 1.9.60

Build signed internal menyertakan `Adzan-Misyari-Rasyid.mp3` dan `fajr_128_44.mp3` sebagai bawaan offline. Hash kedua aset cocok dengan [receipt 11 November](../../android/validation/adhan-source-current-2026-11-11/file-match.json). MP3 dari pengguna tetap bisa dipilih untuk mengganti suara bawaan per slot.

Build internal memerlukan `-PzikraInternalAdhan=true`, `-PzikraAdhanAudio=<MP3 biasa>`, dan `-PzikraFajrAudio=<MP3 Subuh>`. Kedua input harus berupa file lokal yang sesuai. Versi APK internal berakhiran `-internal` dan artefak privat menggunakan SHA-256 `4e1f8286c1e2b175ddda3fe23b271a053ae40a63d5e8a057048da169d482e994`. Pemeriksaan paket standar setelah build internal menegaskan bahwa kedua berkas lama tidak ikut build standar. Bukti izin redistribusi untuk rekaman lama belum ditemukan.
