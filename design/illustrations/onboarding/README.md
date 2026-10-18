# Ilustrasi onboarding statis

Arahan pengguna 18 Oktober 2026: pilih tiga adegan dari kolase 2D yang dibagikan dalam chat;
gambar diam sepenuhnya. Karakter berbeda boleh mewakili beragam pengguna Zikra.

| Cerita | Pilihan referensi | Resource Android |
| --- | --- | --- |
| Belajar lebih terarah. | Kiri atas, pelajar berkacamata menulis di meja | `onboarding_study_static.webp` |
| Ibadah tetap terjaga. | Kiri bawah, perempuan bermukena di sajadah | `onboarding_prayer_static.webp` |
| Dekat, ayat demi ayat. | Tengah bawah, laki-laki berpeci membaca Quran dengan rehal | `onboarding_quran_static.webp` |

Aset terpusat pada `android/app/src/main/res/drawable-nodpi/`. Gambar terpisah 1254×1254
disiapkan dengan alat gambar dari panel pilihan, mempertahankan adegan dan gaya garis/warna
referensi; ini bukan crop yang identik piksel dengan kolase kecil. Tidak ada UI, teks atau logo
pada aset ilustrasi. Encoding WebP lossless mempertahankan RGBA dari PNG hasil edit persis;
dimensi/ukuran/hash dicatat di [validasi aset](../../../android/validation/static-onboarding/asset-verification.json).

Gunakan ImageView `fitCenter`, tanpa memotong kepala/tangan/buku/sajadah, tanpa lingkaran latar,
animasi, float, parallax, tilt, sensor atau pemutar ulang. Tinggi area ilustrasi mengikuti tinggi
viewport, sementara judul/deskripsi dapat menggulung pada font besar. Navigasi tetap di bawah.
Tidak memuat tiga gambar sekaligus atau atlas frame; gambar aktif disampling sesuai display.

Ivory `#FAF7EF`, teks/CTA Pine `#15332F`, teks sekunder `#53655D`. Logo master tetap memakai
`view_zikra_brand.xml` di kiri atas. Ilustrasi bukan pengganti logo. Lihat [branding](../../branding/README.md).
Sumber percobaan 3D disimpan sebagai arsip; aset/pemutarnya tidak ikut APK terbaru.
