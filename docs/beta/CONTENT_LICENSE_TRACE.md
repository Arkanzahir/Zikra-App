# Penelusuran sumber dan izin —15 November2026

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
