# Home Al-Aqsa — layout review v4

25 Oktober 2026. **Belum disetujui / belum diterapkan ke APK.**

[Preview dua keadaan Home](home-al-aqsa-layout-review-2026-10-25-v4.png): layar kiri di posisi awal, layar kanan sesudah digulir. Ini satu Home, bukan dua fitur berbeda. Seluruh tanggal, nama bacaan, tugas, progres, waktu dan countdown adalah contoh; ilustrasi statis tidak membuktikan animasi atau runtime.

Header memakai ilustrasi Al-Qibli v3, logo resmi putih sebagai referensi, lokasi, sapaan kecil sebagai opsi, tanggal dan countdown ke salat berikutnya. Bangunan ditempatkan di kanan; teks di area kiri yang lebih gelap. Delapan pintasan mengikuti header; bagian gulir memuat bacaan, progres, agenda dan fokus. Sapaan belum menjadi pilihan final pengguna. Tidak menambahkan profil/avatar di header karena Profil ada di navigasi bawah.

Dibuat memakai built-in image_gen. Referensi: ilustrasi Al-Qibli v3, simbol Zikra putih di resource, serta board Home/AI v1. Ini komposisi konsep; raster logo di hasil generatif bukan pengganti master branding. Implementasi kelak wajib memakai file simbol resmi tanpa digambar ulang. Tidak mengubah source aplikasi, data pengguna atau konfigurasi privat.

## Prompt persis

```text
Use case: ui-mockup.
Create a refined high-fidelity ANDROID HOME SCREEN DESIGN REVIEW BOARD for the Indonesian app Zikra, showing TWO equal-size phones side by side. Phone 1 is Home at the top; Phone 2 is the SAME Home after scrolling down. This is a concept mockup for approval only, not a screenshot or working app.
INPUT ROLES: image 1 is the chosen Al-Aqsa / Al-Qibli atmospheric illustration to use as the header artwork; image 2 is the existing official WHITE Zikra brand symbol, preserve its crescent + book + spark silhouette; image 3 is an earlier overall Zikra interface reference for the restrained typography, shortcuts, bottom navigation, and lower-page modules. Replace the generic mosque from image 3 with the actual artwork identity in image 1. Do not recreate or use the AI screen from image 3.
PRESENTATION: wide high-resolution board, warm ivory backdrop, restrained thin Android device outlines, straight front-facing screens, no perspective or dramatic shadows. Board heading "ZIKRA — HOME AL-AQSA" and clear subtitle "Konsep untuk ditinjau · seluruh data contoh". Bottom labels "01 · Tampilan awal" and "02 · Setelah digulir". Each screen plausible at 390 × 844 dp with safe Android status and navigation areas. Large readable accurate Indonesian type. No tiny UI.
PALETTE AND STYLE: warm ivory #F7F6F0, deep pine #15332F, mint #EAF3ED, warm sand gold. Modern humanist sans-serif, crisp outlined icons, consistent 16px functional corner radii, slim dividers, low shadow, excellent white-text contrast in hero. Let the illustration carry the emotion instead of excessive decorative cards.
PHONE 1 — HOME AT TOP:
Top hero about 350dp tall, not the full screen. Reframe image 1 for this wide short mobile hero, preserving the Al-Qibli identity: silver/lead-gray broad dome, pale limestone arched horizontal facade and quiet courtyard, olive trees and cypresses. Show the dome and enough facade clearly on the RIGHT HALF. Do NOT substitute a golden Dome of the Rock, Ottoman onion dome or generic minaret fantasy. Preserve the softly volumetric realistic-looking feathered clouds and warm sunset rim light from image 1; NO flat scalloped vector clouds. Keep the LEFT HALF dark and quiet under a subtle pine shading layer so all white UI text remains readable. Sun small enough in the upper right to avoid competing with numbers.
Header row: small existing white official logo symbol followed by lowercase wordmark "zikra", left aligned; right aligned location pin and "Surabaya ⌄". Do not add a profile avatar at top because Profile exists in the bottom navigation.
Below, one SMALL restrained "Selamat sore" (not a large headline), then small date "Minggu, 25 Oktober". This greeting is a design option, not the focal point.
Middle-left text hierarchy: small "Menuju Maghrib", LARGE bold tabular countdown "01:12:34", smaller "Pukul 17.25". Keep line length compact and avoid overlapping the Al-Qibli dome on the right. Countdown not a current clock. Status-bar time "16.12" is consistent with the example countdown.
Hero bottom: slim readable translucent dark strip with five equal prayer columns: "Subuh / 03.48", "Zuhur / 11.16", "Asar / 14.25", "Maghrib / 17.25", "Isya / 18.36". Soft sand-gold text or indicator for Maghrib, other entries white. No huge glowing pill.
Immediately below hero on ivory: eight comfortable shortcut targets in two rows of four, outlined icons with restrained mint/sand circular backgrounds and clear labels: row 1 "Quran", "Doa", "Kiblat", "Hifz"; row 2 "Zikra AI", "Protector", "Study", "Hadis". Appropriate actual icon concepts, not random emoji.
Then section heading "Ruang harimu" and compact mint "Lanjut membaca / Al-Kahf · Ayat 12" row with book icon and chevron. Allow a hint of the next section at the lower edge to signal more content beneath the fold.
Fixed bottom navigation stays above system gesture line, all five destinations readable: "Home", "Quran", "Zikra AI", "Study", "Profil", Home selected in mint. Never cover the reading card with navigation.
PHONE 2 — SAME HOME SCROLLED:
No repeated hero or mosque banner. Ivory content scrolls above the same fixed five-item Home-selected nav.
Heading "Ruang harimu" and mint reading row "Lanjut membaca / Al-Kahf · Ayat 12".
Sand-toned compact ibadah panel: title "Ibadah hari ini", small "2 dari 5 target selesai", thin progress bar filled 40%, clear "Buka checklist" action.
Section heading "Agenda berikutnya" with "2 tugas belum selesai". Two flat full-width task rows with slim divider: "Rangkuman biologi / Hari ini · 16.00", "Latihan matematika / Besok · 09.00". Small "Lihat semua →". Avoid a pile of nested rounded cards. All task names/progress are illustrative as declared on board.
A modest deep pine focus panel lower down: "Beri ruang untuk fokus", caption "Satu sesi untuk tujuanmu.", sand button "Mulai sesi →", small fine line books icon at right. Compact footer "Pelan-pelan, tetap bertumbuh."
Spacing should feel achievable in native Android, calm and readable. No AI marketing hero, no subscriptions, no extra weather widgets or clutter, no fake adzan setting or unsupported attachment controls. NO claim of animation; this is a static composition. No UI annotation arrows over the screen, no copyrighted external photos. The Al-Qibli art should integrate beautifully with the interface rather than merely squeezing the tall reference image into a tiny rectangle.
```
