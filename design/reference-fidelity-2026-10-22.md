# Koreksi kesesuaian Home/Quran

Acuan: [konsep pilihan pengguna](approved-2026-10-21/README.md).
Hasil aktual: [screenshot dan batas pemeriksaan](../android/validation/reference-fidelity-2026-10-22/README.md).

Aset masjid baru dibuat dengan built-in `image_gen.imagegen`, output asli
RGBA1536x1024/1.82MB, transparansi dipertahankan tanpa pengeditan eksternal.
File project: [home_mosque_landscape_v2.png](../android/app/src/main/res/drawable-nodpi/home_mosque_landscape_v2.png).
Android mendecode inSampleSize2 satu kali; tidak mendecode pada tiap frame.
PNG hanya lanskap, tidak memuat teks UI/awan/bulan/matahari. Gerak masih native.
Ilustrasi buku diperbaiki sebagai vector Android, mengikuti sistem ikon aplikasi.
Ilustrasi Study dan orb AI masih berbeda dari konsep dan belum selesai secara visual.

Prompt final (input referensi: home-quran-ai-concept.png, transparent_background=true):

Create a production illustration asset for the Zikra Android home screen. Use case: illustration-story. The provided image is STYLE AND SUBJECT REFERENCE ONLY: match the painterly Islamic mosque landscape in the LEFT phone, with elegant emerald/teal mosque domes, several slender minarets, warm tiny golden window lights, subtle atmospheric green hills and palm foliage. Deliver ONLY the landscape foreground cutout on a real transparent background, no phone, no UI, no text, no letters, no numbers, no logo, no sun, no moon, no stars, no clouds. Wide landscape approximately 3:2 canvas. Place the mosque on the RIGHT half with its tallest minaret around 65% of canvas width; left half is LOW layered foothills and palms, keeping upper left completely transparent for live text. The architecture occupies the bottom 70%, cropped naturally at bottom edge, base extends seamlessly to left and right edges. Sophisticated editorial painted illustration, fine architectural detail, organic silhouettes, depth, not simplistic geometric clip art, not photorealism, not glossy 3D. Deep pine and emerald, dusty sage, restrained warm gold details. This transparent landscape will be placed over a native green gradient; all celestial bodies and moving clouds will be native separate layers. Do not include any sky-colored background or checkerboard pattern.
