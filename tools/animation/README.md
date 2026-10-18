# Karakter onboarding Zikra

Karakter dan properti dimodelkan dari bentuk dasar dalam `render_onboarding.py`.
Referensi pengguna menentukan arah visual kartun 3D dan komposisi, bukan aset yang disalin.
Tidak ada logo yang dibuat ulang: branding memakai resource master Zikra yang sudah ada.

Tiga cerita: membuka buku belajar, bersiap sholat, membaca Quran dengan rehal.
Tangan, kepala, sampul dan lembar buku memiliki pose/keyframe berbeda; ini bukan hanya
menggeser ilustrasi diam. Buku ilustrasi tidak memuat teks Arab rekaan.

Reproduksi dengan Blender 4.3.2 dan ffmpeg/libwebp:

```sh
for scene in study prayer quran; do
  out="/tmp/zikra-onboarding-$scene"
  blender -b -t 2 --python tools/animation/render_onboarding.py -- "$scene" "$out" poster
  blender -b -t 2 --python tools/animation/render_onboarding.py -- "$scene" "$out" frames
  bash tools/animation/pack_frames.sh "$scene" "$out" android/app/src/main/res/drawable-nodpi
done
```

48 frame RGBA 320×240, 24 fps, dua detik; atlas WebP 8×6 (2560×1440).
Poster akhir 640×480 untuk keadaan diam. Dekode atlas ARGB_8888 sekitar 14,1 MiB;
hanya adegan aktif dimuat di executor, lalu atlas dilepas setelah pemutaran.
Pemain Canvas native tidak memakai video, WebView, pustaka animasi atau mesin 3D runtime.
Ketuk untuk mengulang; parallax kecil merespons sentuhan/tilt. Tidak ada loop saat diam.
Background, hilang fokus, penghemat baterai dan animator scale nol menghentikan gerak.
Performa pada HP fisik belum diukur; emulator software bukan tolok ukur kelancaran.
