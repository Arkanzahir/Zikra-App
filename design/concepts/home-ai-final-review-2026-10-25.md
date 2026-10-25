# Home siang/malam dan AI — usulan akhir untuk tinjauan

25 Oktober 2026. **Desain saja, belum disetujui dan belum diterapkan ke APK.** Pengguna meminta perbandingan, preview Home siang/malam dan desain AI yang lebih matang. Kata “final” pada gambar AI berarti usulan visual akhir untuk ditinjau; tidak menggantikan syarat persetujuan sebelum implementasi.

## Preview

- [Home Al-Aqsa siang/malam v5](home-al-aqsa-day-night-review-2026-10-25-v5.png).
- [Usulan AI v2: mulai percakapan dan berdiskusi](ai-final-proposal-2026-10-25-v2.png).

Keduanya dibuat menggunakan **built-in image_gen**, bukan CLI. Semua nama bacaan, waktu, tanggal, tugas, progres dan percakapan adalah contoh. Bulan sabit pada malam adalah ilustrasi, bukan fase astronomi yang diverifikasi untuk tanggal tersebut. Gambar statis tidak membuktikan animasi, parallax, transisi, baterai atau runtime. Hasil generatif juga bukan pengganti master logo: implementasi wajib menggunakan simbol resmi.

## Pertimbangan dan batas

Penilaian visual asisten: Al-Qibli/Al-Aqsa memberi identitas dan bentuk yang lebih spesifik daripada masjid generik sebelumnya; gambar lama lebih sederhana. Keunggulan ini tidak otomatis membuktikan keterbacaan atau performa pada HP. Teks perlu area gelap tenang; detail bangunan/cloud dikurangi kontrasnya di belakang teks. Home mempertahankan struktur v4 dan fungsi gulir; malam mengganti header langit/pencahayaan, bukan memaksa seluruh aplikasi ke tema gelap. Sapaan kecil tetap opsi yang ditampilkan, belum pilihan final pengguna.

AI memakai pine/mint/ivory yang sama, toolbar ringkas, mode online terlihat satu kali, petunjuk internet, saran saat kosong, percakapan dominan setelah dimulai, composer dan nav tetap terjangkau. Rancangan mencakup tindakan Salin dan saran lanjutan sebagai usulan UI, belum fungsi baru yang teruji. Mode offline tetap harus dilabeli panduan berbasis aturan dan tidak mengklaim kemampuan online. Mikrofon mengikuti ketersediaan recognizer. Tidak menambahkan tombol foto/file karena lampiran belum diimplementasikan. “Pahami materi” merupakan usulan mode online, bukan jaminan aturan offline menjawab semua materi. Jawaban matematika pada mockup ditulis sebagai contoh desain, bukan hasil API nyata.

Tidak ada build, pemasangan HP, pengujian runtime atau perubahan source aplikasi dalam putaran desain ini. APK kandidat tetap 1.8.1 yang sebelumnya diuji. F02/F03, UI04, C yang tersisa dan rilis publik belum ditutup.

## Prompt Home persis

```text
Use case: ui-mockup, precise lighting variation.
Create a high-fidelity DESIGN REVIEW BOARD containing exactly TWO front-facing Android phones side by side, both showing the SAME top-of-Home Zikra layout. This is a refinement of the supplied design board. The first supplied image is the target Home layout with Al-Qibli artwork. The second is a style/reference board only; ignore its AI screen and generic mosque. Preserve the first image's layout, official-looking white crescent/book/spark logo with lowercase wordmark "zikra", two rows of shortcuts, reading row and bottom nav. Change the lighting/time state only between the two new phone screens. Do not show the scrolled-down second screen from the original board.
Board title "ZIKRA — HOME SIANG & MALAM". Subtitle "Konsep untuk ditinjau · semua waktu dan data contoh". Labels under phones "01 · Siang" and "02 · Malam". Warm ivory presentation backdrop, very thin black Android device borders, no perspective, crisp high-resolution typography, each phone plausible at 390x844 dp. Both screens share the same structure and dimensions so the day/night change can be assessed directly.
COMMON UI:
A 350dp tall atmospheric mosque header at top, calm and elegantly integrated with white text. Use the SAME recognizable AL-QIBLI mosque facade and silver-gray hemispherical dome from the target image, same exact camera/composition in both states. This is Al-Qibli at Al-Aqsa, not the golden Dome of the Rock and not generic Ottoman towers. Mosque mainly on the RIGHT HALF, quiet dark pine gradient on LEFT behind text; illustration can be less contrasty than the earlier reference so numbers remain dominant. Olive/cypress trees and limestone courtyard, no tropical jungle.
Top row: small white original crescent + open book + spark symbol, lowercase "zikra"; location pin and "Surabaya ⌄" right. No avatar. Greeting remains one understated small line followed by date. Small "Menuju ..." label, very large tabular countdown, small actual prayer time. Full-bleed hero bottom with a slim dark translucent five-column prayer strip; no gratuitous glows. Five prayer entries in both screens: Subuh 03.48, Zuhur 11.16, Asar 14.25, Maghrib 17.25, Isya 18.36. Only upcoming prayer highlighted subtle sand gold.
Below header, keep the SAME LIGHT warm ivory body in BOTH screens. Night sky is a clock-based header illustration state, not a forced app-wide dark theme. Two rows of four outline-icon shortcuts on restrained soft mint/sand circles, readable labels: "Quran", "Doa", "Kiblat", "Hifz"; second row "Zikra AI", "Protector", "Study", "Hadis". Then heading "Ruang harimu", small caption "Satu langkah baik, setiap hari.", a compact mint reading row "Lanjut membaca / Al-Kahf · Ayat 12". A hint of additional scroll content can be visible below. All content above fixed safe bottom nav, no covered controls. Bottom nav five items "Home", "Quran", "Zikra AI", "Study", "Profil", Home selected mint. Keep compact but comfortable 48dp targets, no tiny typography.
PHONE 1 DAY:
Status time "12.30". Greeting "Selamat siang", date "Minggu, 25 Oktober", next-prayer text "Menuju Asar", countdown "01:55:00", small "Pukul 14.25". Highlight Asar in strip.
Daytime scene has soft believable daylight, blue-green / desaturated teal sky with a small warm-white sun on the right, soft naturally irregular cumulus cloud edges and volumetric shading, a silver-gray dome with a gently lit limestone facade. Clearly DAY, not sunset and not a dark night with a sun pasted on it. Soft pale-blue light in right sky, darker pine on text side. No orange sunset haze in day. Do not use flat scalloped cartoon clouds.
PHONE 2 NIGHT:
Status time "19.10". Greeting "Selamat malam", same date, next-prayer text "Menuju Subuh", countdown "08:38:00", small "Besok · 03.48". Highlight Subuh. Do NOT label Isya as future since 18.36 has already passed.
The exact same mosque and camera position, now deep navy/pine atmospheric night sky, modest soft crescent moon on right with a VERY subtle halo, restrained sparse tiny stars, soft dimly moonlit natural clouds, warm subtle lamps/windows on the mosque facade. Moon no larger than daytime sun. No daytime sun left behind, no huge glowing sphere, no full solar rays, no golden dome. Preserve dark yet readable silver-gray dome detail. The crescent shape is illustrative for this concept; do not claim an astronomically verified phase/date.
Overall look: premium calm native Android product, pine/mint/ivory/sand visual identity, crisp type and spacing, realistic-soft clouds integrated with illustrated architecture. No animation arrows, no motion claims, no new features, no watermarks, no UI loading or screenshots of desktop apps. All data illustrative, both are STATIC concept mockups.
```

## Prompt AI persis

```text
Use case: ui-mockup.
Design a polished final-design PROPOSAL board for ZIKRA AI, an Indonesian Android assistant for learning, study planning and daily routines. This is the final proposed visual direction FOR USER REVIEW, not a claim of implemented or approved code. Produce ONE very high-resolution landscape presentation board with TWO equally sized front-facing Android screens side by side. Left: empty new conversation. Right: an actual readable example chat. References: supplied image 1 is the earlier concept to improve, especially the rightmost AI screen; image 2 is the current Al-Aqsa Home board for shared pine/mint/ivory brand consistency. Do NOT put the mosque or a landscape into the AI screen. Do NOT reproduce the Home screen in this output.
Board heading "ZIKRA AI — RANCANGAN FINAL"; subtitle "Usulan desain untuk ditinjau · percakapan contoh". Device labels "01 · Mulai percakapan" and "02 · Saat berdiskusi". Warm ivory board backdrop, minimal narrow black Android borders, no perspective, calm confident typography. Each screen plausible at 390x844 dp, with generous but useful breathing room, readable 16sp message text and clear tap targets. This should look like a thoughtfully designed native product, not a template marketing landing page.
BRAND: deep pine #15332F, warm ivory #F7F6F0, soft mint #EAF3ED, warm sand accents only sparingly. No lavender, no purple orb, no glassmorphism, no neon, no giant hero, no heavy shadows, no pile of oversized cards. Delicate unified outlined icons, restrained 12–16dp corners. Crisp contemporary humanist sans-serif. Distinct confident hierarchy and precise consistent spacing.
COMMON CHROME ON BOTH SCREENS:
Status bar "09.41" and small Android system icons. Compact toolbar: back arrow on left, title "Zikra AI" left aligned in bold 22sp, a small understated new-conversation square-pencil icon at far right. No giant bot avatar or person photo. Below toolbar a compact clickable status badge "● Online ⌄"; next to or below it a quiet small label "Pesan dikirim melalui internet". Mode status is clear and appears ONCE, do not repeat online next to every message.
Main area is a scrollable conversation/content area above a fixed composer. Composer at bottom above navigation, thin pine-gray outline, ivory/white surface, radius 16dp, minimum 56dp tall. It contains a subtle outlined mic button on left, readable placeholder "Tulis pesanmu…", and a 44–48dp deep-pine circular send button on right with a white upward arrow. No plus, camera, paperclip, file upload icons or attachment capability, because uploads are not implemented. No unsupported voice-call or image-generation controls. One tiny privacy line immediately above composer: "Konteks tambahan hanya dengan izinmu." Never claim end-to-end encryption.
Fixed shared bottom dock, five readable destinations: "Home", "Quran", "Zikra AI", "Study", "Profil"; AI selected with subtle mint background. Composer and chat content never obscured by the dock. System gesture line below nav, no keyboard shown for these two design-review screens.
LEFT SCREEN NEW CHAT:
Use a modest small outlined spark inside a 40dp mint square above the welcome heading, NOT an orb. Place starting content comfortably in the upper/middle area after the status row, leaving conversational space.
Heading in two sensible lines, 27sp, bold pine: "Kita mulai dari mana?"
Caption 15sp muted sage: "Belajar, ibadah, dan rencana harian."
Below at 24dp distance show THREE concise full-width suggestion rows in one quiet lightly outlined grouped list with separators, not three enormous cards. Each row has a small meaningful line icon on a very pale mint/sand spot at left, a bold 16sp title, smaller 13sp caption and subtle right chevron:
1. "Pahami materi" / "Jelaskan dengan langkah sederhana" — book icon.
2. "Susun rencana" / "Bagi tugas jadi langkah kecil" — checklist icon.
3. "Jaga rutinitas" / "Atur target ibadah atau hafalan" — gentle calendar/target icon.
Below the suggestions a quiet line "Atau tulis pertanyaanmu sendiri." White space below feels calm, with the composer clearly inviting input. Do not show any sample assistant bubble on empty chat, do not fill the space with disclaimers, greetings repeated from Home, or redundant buttons.
RIGHT SCREEN CONVERSATION:
Same header/mode badge. No welcome hero or suggestion list once conversation exists. Small restrained date separator "Hari ini".
One right-aligned mint user bubble, readable 16sp: "Jelaskan persamaan linear dengan contoh sederhana."
Then one left aligned assistant answer, mostly on the flat ivory surface with plenty of vertical rhythm. Tiny pine spark and label "Zikra AI", no repeated online metadata. Answer text readable, neatly broken into short paragraphs:
"Persamaan linear mencari nilai yang membuat dua sisi sama."
Then an inset very pale mint formula row, gentle 12dp corners and no huge card:
"2x + 4 = 10"
Then two numbered steps in normal body type:
"1. Kurangi kedua sisi dengan 4."
"    2x = 6"
"2. Bagi kedua sisi dengan 2."
"    x = 3"
Then a small distinct check paragraph:
"Cek: 2(3) + 4 = 10."
All math must be correct and copy accurate. DO NOT use broken LaTeX markup or huge decorative formula typography. Do not invent religious quotes. End with a small unobtrusive copy icon and text "Salin", then two modest optional follow-up outline chips "Contoh lain" and "Coba latihan". Keep answer clear, not overly dense, no nested bubbles or thick borders.
No WhatsApp read ticks, no online avatar status, no fake file attachments, no five message timestamps or irrelevant feedback widgets. Use body layout that would scale to larger Android fonts. Both screens should feel visually consistent with Zikra Home but focused on reading and composing. This is a static mockup; no claims of real API results, microphone testing, implementation or release completion.
```
