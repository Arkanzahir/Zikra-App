# Aset siang tanpa lampu

Tool: **built-in image_gen**, mode edit dengan transparansi; bukan CLI. Gambar dibuat25 Oktober2026. Input/master malam tetap `android/app/src/main/res/drawable-nodpi/home_al_qibli_foreground.png`, tidak ditimpa. Hasil proyek: [home_al_qibli_day_unlit.png](../../app/src/main/res/drawable-nodpi/home_al_qibli_day_unlit.png). Salinan asli generator tetap di folder generated_images milik Codex. Keduanya1536×1024 RGBA.

Perubahan: lampu tembok/jendela dan pantulan cahayanya dipadamkan pada varian siang. Detail alami batu/pohon dipertahankan sejauh hasil edit; generator tidak mempertahankan setiap piksel/geometri. Renderer memakai koreksi vertikal kecil pada varian siang untuk menyelaraskan kubah, dan transisi saat fajar/senja. Tidak mengklaim dua gambar sebagai lapisan yang identik per piksel atau pengukuran astronomi/cahaya fisik.

Prompt persis:

```text
Use case: production transparent Android illustration asset EDIT, extremely constrained lighting correction. EDIT TARGET is the supplied transparent 1536x1024 Al-Qibli mosque landscape. Produce the SAME composition, same dimensions, exactly same placement and scale of dome, minaret, facade arches, trees, distant hills, courtyard, and the same transparent sky/alpha silhouette. Do not recrop, zoom, move or redesign any architecture or plants. This must align with the original as an interchangeable daytime layer. ONLY CHANGE ARTIFICIAL LIGHTING: all wall lamps between the arches must be switched OFF, no orange/yellow glowing points, no orange light cast inside arches, no glowing windows, and no lamp reflections on paving. Keep doors and arch interiors naturally shaded brown/gray, keep stone natural beige, keep the existing natural sunlit highlights on stone and dome; this is clear daylight with NO electric lighting. Neutralize golden illuminated streaks on the pavement to normal daylight stone. Preserve photographic painterly detail and original camera framing. Preserve original background transparency including fine tree edges; no sky, no clouds, no opaque rectangle, no text, no UI, no border, no new objects. Keep all other pixels and geometry as unchanged as possible.
```
