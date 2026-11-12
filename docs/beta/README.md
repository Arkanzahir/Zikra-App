# Persiapan distribusi beta Zikra

12 November 2026. **Status HOLD, belum boleh dipublikasikan sebagai APK siap beta.**
Permintaan terbaru memprioritaskan kesiapan aplikasi sebelum website unduhan.
Website bukan pengganti gerbang konten, privasi, paket dan pengujian.
Daftar ini memperjelas gerbang beta pada [cakupan](../../RELEASE_SCOPE.md),
tanpa menghapus fitur wajib atau mencentang seluruh A–D/E/F/G.

## Pekerjaan yang memblokir distribusi

| Gerbang | Bukti tersedia | Yang masih diperlukan |
| --- | --- | --- |
| Hadis B01 | Audit kandidat/paging/penomoran, korespondensi pernah dikirim | Identitas edisi, terjemahan, cakupan dan ketentuan konten. Jangan menganggap lisensi kode otomatis melisensikan kitab |
| Audio D02/D02a | Dua rekaman aktif, sumber/hash dan beberapa uji HP | Hak distribusi dua rekaman dan pemeriksaan lafaz, bukti idle/hemat baterai yang belum konklusif. [Daftar izin](../audio-permission/README.md) |
| Alur inti G01/G02 | Bukti modul dan pembaruan terdahulu | Smoke lintas fitur dan upgrade pada **paket final yang sama**, HP dengan izin ditolak/background/putus jaringan. Hasil versi lama tetap bukti versi lama |
| AI C02/G03 | Hosting/akses instalasi/kuota; health 200 dan akses tanpa izin 401 pada pemeriksaan ini | Konfirmasi kasus laptop mati yang masih terbuka, alur memperoleh akses per tester, retensi/operator dan batas kapasitas beta. Jangan membagikan kode akses bersama dalam APK |
| Privasi/dukungan G03 | Izin online sekali, konteks opt-in, proteksi backup dan audit terdahulu | Nama penerbit, kontak dukungan, kebijakan publik akurat dan peninjauan layanan. [Draf berbasis implementasi](PRIVACY_DRAFT.md) |
| Paket G04 | Build debug 1.9.40/code67 dan bukti modul; jalur release disiapkan | Sertifikat release tetap yang dicadangkan privat, build signed final, fingerprint/hash, audit dan pengujian ulang. Kandidat unsigned tidak dapat dipasang |
| Sumber layanan G03 | Atribusi OSM/BMKG/Open-Meteo pada fitur, API dan cache | Tinjau syarat sumber Quran/audio/font/Hadis/Doa dan skala beta; verifikasi identitas FCM APK release. [Inventaris](SERVICES.md) |

E03/E04 dan F global tetap mengikuti checklist. Tidak boleh mengklaim fitur
alert/briefing atau seluruh pengujian desain selesai bila belum terbukti.
Login/sinkronisasi/dashboard admin tetap antrean setelah stabilitas inti,
bukan alasan menahan beta bila syarat beta lainnya benar-benar terpenuhi.

## Urutan pelaksanaan

1. Tutup sumber/hak konten, tentukan penerbit/kontak dan sertifikat distribusi.
   Jika sumber belum memperoleh kejelasan, pertahankan HOLD; penggantian atau
   pengurangan cakupan memerlukan keputusan pengguna, bukan dihapus diam-diam.
2. Build signed dengan versi meningkat saat kandidat final dipilih. Jangan
   memasang release di HP pengguna di atas debug tanpa rencana perpindahan data.
3. Audit paket final, jalankan smoke dan sisa uji HP terarah pada hash yang sama.
   Simpan kegagalan/limit beserta keberhasilan. Jangan mengulang restart tanpa
   koordinasi atau memaksa penghemat daya ketika HP sedang dipakai.
4. Isi [catatan beta](RELEASE_NOTES.md), hash/source, skenario lulus, limit dan
   petunjuk instal. Uji unduh/install dari kanal distribusi setelah disiapkan.
5. Bagikan beta bertahap setelah gerbangnya terpenuhi. Pantau keluhan/kuota,
   sediakan pencabutan akses per tester dan cara menghentikan penyebaran versi
   bermasalah. Jangan menjanjikan beta publik/Play Store selesai dari checklist ini.

## Build release tanpa membocorkan signing

`android/release-signing.gradle` membaca hanya environment berikut, bukan file
source atau argumen password pada command. Nilai tidak ditulis ke laporan.

- `ZIKRA_RELEASE_STORE_FILE`
- `ZIKRA_RELEASE_STORE_PASSWORD`
- `ZIKRA_RELEASE_KEY_ALIAS`
- `ZIKRA_RELEASE_KEY_PASSWORD`

Gunakan path absolut untuk `ZIKRA_RELEASE_STORE_FILE`. Keystore dalam repo
wajib berada di `.local/` yang di-ignore; alternatifnya di luar repo. Jangan
memakai keystore debug sebagai identitas distribusi. Simpan
cadangan keystore dan password secara terpisah dengan akses terbatas. Jangan
memakai `--debug`, build scan atau mengunggah log/environment untuk signing.

Build release normal menolak konfigurasi kosong/sebagian. Untuk **inspeksi
lokal tanpa signing saja**, jalankan dari direktori `android`:

```powershell
./gradlew.bat :app:assembleRelease -PzikraUnsignedCandidate=true --no-daemon --max-workers=1
```

Mode unsigned tidak boleh dijalankan bersama signing environment. Audio privat
saat ini hanya masuk source set debug. Kandidat release **belum memuat dua
rekaman adzan pilihan**, sehingga build sukses bukan kesetaraan fitur atau izin
mendistribusikannya. Integrasi audio release baru dilakukan setelah sumber lolos.

`tools/audit_beta_release.py` memeriksa paket, flag manifest, ukuran, private
filename, nilai rahasia yang dikenal dan signer yang dipatok. Daftar nilai
rahasia dipasok JSON lewat stdin oleh operator lokal, jangan lewat argumen atau
Git. Audit menolak input rahasia kosong. Contoh opsi, tanpa isi credential:

```text
--apk <candidate.apk> --version <version> --version-code <code>
--aapt2 <aapt2.exe> --java <java.exe> --apksigner <apksigner.jar>
--expected-signer <sha256-sertifikat-release> --output <laporan-baru.json>
```

Exit 0 hanya menyatakan pemeriksaan artefak terpenuhi. `distributionApproved`
selalu false karena tool ini tidak mengesahkan sumber/hak, privasi atau uji HP.
Exit 1 berarti pemeriksaan artefak belum terpenuhi; exit 2 berarti audit gagal
dijalankan. Laporan tidak boleh ditimpa agar jejak percobaan tetap ada.

## Menjaga data penguji yang sudah memakai debug

Android memerlukan signer yang cocok untuk pembaruan aplikasi dengan ID sama.
Release dengan sertifikat baru tidak dapat memperbarui debug yang sekarang.
Jangan menyarankan uninstall sebagai langkah biasa karena data lokal dapat hilang.
F06 cadangan/pemulihan pengguna belum tersedia; memilih key debug demi update
mudah bukan penyelesaian keamanan. Tentukan migrasi yang diuji atau perangkat
penguji baru untuk release pertama, sambil mempertahankan instalasi lama.

Tidak ada keystore baru dibuat, APK dipasang, data HP dibaca, layanan diubah,
email dikirim, billing diaktifkan atau paket dipublikasikan pada persiapan ini.
