> **Status terbaru — 18 Oktober 2026:** instance diganti; perubahan lokal sesi sebelumnya tidak tersedia. Pemulihan tahap ini mencakup Study/Hifz, target surah/ayat, editor dan navigasi bersama. Reader native masih 4 surah; target lain membuka Quran.com eksternal. Baca [RECOVERY.md](../RECOVERY.md) dan [validasi terbaru](validation/recovery/README.md) sebelum catatan historis di bawah. Setup baru: [tools/cloud](../tools/cloud/README.md).

# Penyedia API pilihan Zikra

Pilihan pengguna, 17 Oktober 2026. Dokumen ini menetapkan arah integrasi; daftar ini **bukan klaim bahwa seluruh layanan sudah tersambung**. Aplikasi Android tetap Java dan layout XML.

## Penyedia dan kondisi implementasi

| Fitur | Penyedia pilihan | Endpoint | Kondisi aplikasi saat ini |
| --- | --- | --- | --- |
| Chat AI | Featherless AI | `https://api.featherless.ai/v1/chat/completions` | Belum terintegrasi; `OfflineAssistant` masih menjawab secara lokal |
| Analisis foto soal | Google Gemini API | `https://generativelanguage.googleapis.com/v1beta/` | Belum ada integrasi vision |
| Jadwal sholat online | Aladhan | `https://api.aladhan.com/v1/timingsByCity` atau `/v1/timings/{timestamp}` untuk koordinat | Masih memakai kalkulasi harian lokal `PrayerSchedule` |
| Quran lengkap | Quran.com / Quran Foundation | `https://api.quran.com/api/v4/` sebagai endpoint yang diajukan | Reader masih memuat empat surah offline |
| Gempa | BMKG Open Data | `https://data.bmkg.go.id/DataMKG/TEWS/autogempa.json` | Request background, parser, peta dan halaman detail sudah ada |
| Prakiraan cuaca | Open-Meteo | `https://api.open-meteo.com/v1/forecast` | Belum terintegrasi; cuaca yang ada masih memakai API BMKG Keputih |
| Bantuan darurat | Android Intent | `tel:112`, `https://wa.me/`, `geo:` / Google Maps | Membuka dialer, WhatsApp dan Maps sudah tersedia |

## Kontrak integrasi native

### AI: Featherless dan Gemini

- Android berkomunikasi lewat HTTPS ke backend Java yang mengelola panggilan AI. API key provider disimpan sebagai secret di backend, tidak disertakan dalam XML, source, `BuildConfig`, APK atau Git.
- `aiService.ts` tidak terdapat pada checkout yang diperiksa dan bukan bagian aplikasi native ini. Client Android nantinya memakai Java dan OkHttp.
- Llama 3.3 70B Instruct adalah keluarga model yang dipilih pengguna; ID model harus dicocokkan dengan daftar model Featherless dan hak akses akun. Endpoint yang kompatibel dengan OpenAI tidak menjamin semua nama model diterima.
- Jangan mematok `gemini-1.5-flash` sebagai model aktif. ID Gemini dipilih dari model yang masih tersedia dan mendukung gambar pada saat integrasi, serta dapat diganti melalui konfigurasi backend.
- Hak perk hackathon, sisa kredit, kuota gratis dan tarif perlu mengikuti akun/provider. Belum ada bukti aktivasi perk atau kredensial pada lingkungan ini.
- Teks Quran, terjemahan dan kutipan tafsir tetap mempunyai sumber tersendiri. Penjelasan yang dihasilkan model ditampilkan sebagai penjelasan AI.

### Aladhan

- `timingsByCity` memakai parameter kota dan negara. Untuk GPS gunakan endpoint `timings/{timestamp}` dengan `latitude` dan `longitude`.
- Sebelum memakai metode `20`, periksa `/v1/methods` dan cocokkan nama Kementerian Agama Republik Indonesia dalam respons. Simpan metode dan zona waktu bersama jadwal; metode hisab tersebut bukan bukti bahwa respons merupakan publikasi jadwal resmi Kemenag.
- Simpan jadwal per tanggal/lokasi/metode, periksa tanggal/zona waktu pada respons, lalu gunakan sumber yang sama untuk Home, daftar jadwal dan alarm.
- Kalkulasi lokal yang sudah ada dapat menjadi fallback berlabel, bukan dicampur diam-diam dengan jadwal online.

### Quran.com / Quran Foundation

- Sebelum integrasi, verifikasi base URL, versi API dan persyaratan autentikasi yang berlaku. Endpoint legacy yang diajukan belum berhasil diuji dari cloud ini; jangan menganggap seluruh endpoint Foundation dapat diakses tanpa kredensial.
- Ambil katalog 114 surah, teks ayat, metadata juz/halaman/hizb yang tersedia, serta resource terjemahan dan audio. Pilih ID terjemahan Kemenag dan reciter Mishary berdasarkan katalog resource serta atribusinya, bukan hanya label bahasa Indonesia.
- Simpan hasil unduhan lokal agar surah yang sudah diunduh tetap bisa dibaca offline. Bookmark memakai nomor surah dan ayat yang stabil.
- Pagination, unduhan yang terputus dan loading/error ditangani tanpa menjalankan network/parsing di UI thread. Tab katalog hanya diaktifkan setelah metadata dan navigasinya tersedia.
- Audio yang diputar pengguna memakai layanan audio Android; tidak memakai TTS sebagai pengganti murottal.

### Protector: BMKG dan Open-Meteo

- BMKG tetap menjadi sumber laporan gempa dan informasi potensi tsunami yang terdapat dalam respons. Laporan terakhir tidak diperlakukan sebagai prediksi atau peringatan gempa mendatang di lokasi pengguna.
- Open-Meteo menjadi penyedia prakiraan cuaca pilihan untuk integrasi berikutnya. Migrasi ini belum mengganti `BmkgClient` yang ada.
- Prakiraan meminta koordinat, zona waktu dan field cuaca yang diperlukan: suhu, kelembapan, angin, kode cuaca, curah hujan dan probabilitas presipitasi. Waktu prakiraan serta sumber harus terlihat pada UI.
- Pengingat hujan/petir merupakan interpretasi prakiraan dengan ambang yang jelas, bukan peringatan resmi cuaca ekstrem. Tautan peringatan resmi BMKG tetap tersedia.
- Simpan laporan terakhir yang berhasil diterima dan waktu pengambilannya. Saat gagal, tampilkan sebagai data tersimpan dengan umurnya; jangan membuat nilai cuaca atau gempa contoh menjadi data live.
- Layanan Open-Meteo publik gratis untuk penggunaan nonkomersial dengan ketentuan pemakaian/atribusi. Tidak diasumsikan gratis tanpa batas untuk semua penggunaan.

### Intent bantuan

- `ACTION_DIAL` dengan `tel:112` membuka dialer; pengguna melakukan panggilan.
- `ACTION_VIEW` dengan `wa.me` membuka WhatsApp dan mengisi draf pesan. Skema ini **tidak mengirim atau menyiarkan SOS ke keluarga secara otomatis**. Jika kontak tujuan ditambahkan, pengguna memilihnya dan mengonfirmasi pengiriman di WhatsApp.
- `geo:` dan tautan Maps membuka aplikasi tujuan yang tersedia. Tangani perangkat tanpa handler dengan pesan yang jelas.

## Hubungan dengan pekerjaan desain

Pilihan API tidak mengganti acuan visual pengguna. Dashboard/detail Protector, dial Kiblat dan onboarding parallax sudah dikerjakan. **Prioritas terbaru: redesign onboarding** mengikuti [galeri referensi](../design/README.md), karena pengguna masih belum puas dengan kualitas visualnya. Sesudahnya lanjutkan Quran: katalog/reader, 114 surah, mode Mushaf/Terjemahan, bookmark/cache dan murottal. Sesudahnya Study Hub/Hifz, halaman ibadah/Profil, serta kelengkapan layanan Protector dan AI secara bertahap. Urutan dan kriteria lengkap terdapat di [README utama](../README.md#urutan-pekerjaan-berikutnya). Home yang sudah disetujui dipertahankan; Infaq tidak ditampilkan pada shortcut.

Mode Mushaf harus mengikuti edisi dan metadata susunan halaman/baris yang konsisten dengan font/aset berlisensi. Menyembunyikan terjemahan pada daftar ayat belum memenuhi mode Mushaf. Verifikasi ketersediaan resource layout/halaman di provider sebelum memilih implementasi. Pertahankan pemetaan surah/ayat/halaman saat berpindah mode; ketentuan lengkap ada di [persyaratan mode Mushaf](../README.md#persyaratan-mode-mushaf).

## Bukti pemeriksaan dan prasyarat

- Kode native diperiksa pada commit `5376397`: hanya BMKG dan Android Intent dari daftar layanan di atas yang sudah mempunyai integrasi; provider AI, Aladhan, Quran.com dan Open-Meteo belum dihubungkan.
- Dokumentasi resmi repository [Open-Meteo](https://github.com/open-meteo/open-meteo#readme) menyatakan akses nonkomersial tanpa API key. Dokumentasi SDK resmi [Google Gen AI](https://github.com/googleapis/python-genai#readme) menegaskan kebutuhan API key; pemilihan SDK tersebut tidak berarti aplikasi Android akan memakai Python.
- Request ke dokumentasi/provider Featherless, Gemini, Aladhan, Quran.com dan Open-Meteo terhenti pada proxy cloud dengan `403 Forbidden`. Itu bukan hasil pengujian status layanan dari internet umum. Belum ada respons live dari kelima provider yang dapat dinyatakan terverifikasi.
- Referensi yang perlu diperiksa setelah akses tersedia: [Featherless](https://docs.featherless.ai/), [model Gemini](https://ai.google.dev/gemini-api/docs/models), [Aladhan](https://aladhan.com/prayer-times-api), [Quran Foundation](https://api-docs.quran.com/), [BMKG](https://data.bmkg.go.id/), dan [Open-Meteo](https://open-meteo.com/en/docs).
- Integrasi AI membutuhkan backend, konfigurasi model dan kredensial yang dapat digunakan. Integrasi provider lain tetap dapat dikerjakan tanpa menunggu AI setelah akses endpoint tersedia.
