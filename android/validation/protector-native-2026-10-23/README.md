# Protector Android 1.5.0 — 23 Oktober 2026

Preview yang disetujui diterapkan ke Activity Android asli, tanpa WebView.
Referensi: [preview v4](../../../design/assets/protector-weather-v1/preview.html)
dan [board pilihan pengguna](../../../design/references/protector-layout-user-2026-10-22.png).

## Perubahan

- Hero memakai PNG kota dan dua lapisan awan yang sama, dengan skala/posisi mengikuti
  preview. Body memakai Roboto yang sama; judul memakai serif bawaan Android.
  Serif Android bukan Times New Roman Windows: tidak mengklaim identik piksel.
- Kondisi mengikuti prakiraan model Open-Meteo untuk lokasi pilihan aplikasi:
  cerah tanpa awan, berawan bergerak, gerimis/hujan, hujan lebat/petir dengan laju
  dan jumlah partikel berbeda. Kota generik, bukan foto kota aktual.
- Matahari/bulan mengikuti jam lokal lokasi (batas dekoratif 06.00/18.00, bukan
  perhitungan astronomis terbit/terbenam). Fase bulan dihitung dengan umur sinodik
  rata-rata dari epoch 6 Januari 2000. Ini pendekatan dekoratif, bukan penentu hilal.
- Gerak maksimal20fps, berhenti saat Activity/jendela tersembunyi, hero keluar area
  gulir, penghemat daya aktif atau animasi sistem dimatikan. PNG disalin tanpa edit;
  decoder memakai inSampleSize2 tanpa pembesaran berdasarkan density perangkat.
- Peta episentrum osmdroid/OSM dapat digeser, pinch/zoom, dan kembali ke episentrum
  pada layar detail. Peta ringkas menyembunyikan kontrol zoom. Marker hanya dibuat
  dari koordinat latitude/longitude BMKG yang valid. Batas Natural Earth menjadi
  fallback luring berlabel, dengan detail berbeda dari tile OSM.
- Tab guncangan memuat gambar Shakemap laporan terpilih dari BMKG. Nama berkas
  divalidasi, respons dibatasi4MiB dan bitmap maksimal1200px per sisi. Request
  dibatalkan saat dijeda; gagal/kosong ditampilkan jelas dengan aksi coba lagi.
- Waktu kejadian dan pengambilan terpisah. Laporan terakhir Indonesia bukan
  peringatan dini lokasi pengguna. Potensi/dirasakan tetap teks sumber BMKG.
  Darurat112, pesan keluarga dan masjid terdekat tetap melalui bantuan cepat.

## Screenshot APK

[Protector dan data jaringan](ProtectorLive.png) ·
[Hujan lebat, simulasi berlabel](ProtectorRainFixture.png) ·
[Episentrum](EarthquakeLive.png) · [Guncangan BMKG](ShakemapLive.png) ·
[Fixture pengisian kartu](ProtectorDesignFixture.png).

`Live` merekam respons saat pengujian; laporan dapat berubah kemudian. Fixture
berlabel memakai data uji. Gambar Shakemap ditampilkan utuh; gestur zoom berlaku
pada peta episentrum. Tidak memakai gambar peta ilustrasi sebagai laporan nyata.
Koreksi terakhir menambahkan tanggal pada jam kejadian di kartu ringkas;
screenshot diambil sebelum penambahan label tanggal tersebut. Build/lint APK
setelah koreksi lulus; emulator tidak dinyalakan ulang untuk perubahan label ini.

## Pemeriksaan dan batas

Build APK/testAPK/lint lulus:1worker, heap512MiB/metaspace256MiB. Empat tes JVM
terarah lulus (fase bulan, URL gambar, magnitudo, urutan koordinat). Enam tes native
binding/navigasi lulus, ditambah dua tes jaringan/tab dan penghentian jam animasi.
Setelah koreksi ilustrasi terakhir, tiga metode terarah diperiksa ulang.
[Hasil final, hash APK, dan lint](result.json).

Satu emulator Android37.1 x86_64, guestRAM1536MiB/2core,360×800dp. HP pengguna
tidak dipakai. Belum audit baterai/performa HP atau aksesibilitas penuh. APK debug
1.5.0/code12 ada di luar repo; sumber build terisolasi mengecualikan eksperimen
audio lama yang belum dikomit. Tidak menyertakan kredensial AI/.local.

## Sumber

[BMKG](https://data.bmkg.go.id/gempabumi/) ·
[OSM tile policy](https://operations.osmfoundation.org/policies/tiles/) ·
[Open-Meteo](https://open-meteo.com/en/docs).
Attribution OSM selalu tampak; hanya meminta tile viewport dengan cache lokal,
tanpa unduhan massal. Roboto OFL disertakan di assets/licenses. Natural Earth
domain publik; asal PNG dan prompt ada di folder aset desain.
