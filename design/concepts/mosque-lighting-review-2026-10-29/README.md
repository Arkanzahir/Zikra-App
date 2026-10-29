# Interior malam — revisi visual setelah penolakan

**Status terbaru:** arah referensi disetujui pengguna (“gas sudah bagus”), lalu tekstur delapan bukaan diintegrasikan ke beta1.9.27. Hanya band pintu didecode dan setiap bukaan diclip ke arsitektur asli; seluruh scene generatif tidak dijadikan latar. [Bukti native dan batas](../../../android/validation/mosque-approved-lighting-2026-10-29/README.md). HP belum diuji versi27. Catatan berikut adalah riwayat pembuatan sebelum persetujuan.

29 Oktober2026. Pengguna merespons “jelek anjir” sesudah preview UI08.
Arah koreksi sesi ini adalah pencahayaan interior; **visual belum disetujui**.

[Referensi baru](lighting-reference.png), [prompt lengkap](prompt.txt).
Dibuat dengan tool bawaan image_gen, bukan screenshot Android atau ekspor Figma.
Input target: render native `android/validation/mosque-repair-2026-10-29/home-21.jpg`.
Referensi tekstur/struktur: aset asli `home_al_qibli_day_unlit.png`.

Bukaan pintu pusat memperlihatkan kedalaman interior yang hangat. Titik lampu
kecil dan cahaya terarah menggantikan glow merata di semua pintu. Panel kayu,
bayangan tepi lengkungan dan batu tetap terbaca; pantulan lantai lebih pendek.
Hasil generatif ini ditinjau secara visual, bukan diuji sebagai fitur APK.

**Batas penting:** imagegen juga mengubah detail langit/bulan dan sebagian
geometri halus. Jangan mengganti bitmap Home dengan seluruh scene ini: bulan,
awan dan bintang native harus tetap hidup dan satu siluet masjid dipertahankan.
Jika arah ini diterima, pisahkan/selaraskan bagian pencahayaan pintu terhadap
aset asli, lalu uji masking/parallax/siang-malam pada Android. Ini referensi
pencahayaan, bukan aset produksi yang sudah lolos kecocokan piksel.

Kode/APK1.9.26 tidak berubah, HP tetap1.9.25. Sumber daylight, artwork, ikon,
WIP, stash audio dan konfigurasi privat tidak ditimpa. Pencarian/cache masjid
UI08 tetap memiliki bukti teknis terarah; penolakan visual tidak diubah menjadi
klaim kegagalan atau persetujuan terhadap fungsi tersebut. Tidak meminta USB,
menjalankan emulator/build atau melakukan lookup lokasi baru untuk preview.
