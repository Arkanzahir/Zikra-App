# Font dan format balasan AI — beta1.9.38, 11 November 2026

Pengguna menunjukkan balasan dengan **Markdown** mentah dan menilai font kurang
profesional. Tampilan balasan kini memakai sans-serif sistem/Roboto16sp normal,
line spacing1,35× dan heading bertingkat. Markwon4.6.2 core merender bold,
italic, daftar, heading dan code sebagai span native. Tidak menggunakan WebView,
HTML plugin, loader gambar atau network plugin. HTML tetap teks literal,
gambar Markdown hanya alt text. Tautan hanya HTTPS tanpa credential, kontrol/
spasi mentah atau URL berlebih; dibuka browser hanya saat diketuk. Tidak ada
script/intent/file/data URL otomatis. Dua lisensi resmi disertakan dalam aset.

Bubble pengguna memakai sans-serif sama dan tetap teks literal. Font utama
Plus Jakarta Sans, Home, Quran/Arab, ikon, warna/desain pilihan, composer serta
format sumber/history tersimpan tetap. Pesan lama dirender dari string asli;
tombol salin tetap menyalin Markdown asli. Tidak ada migrasi database atau
perubahan cara mengirim data ke AI. Renderer dibuat ulang mengikuti tema Activity.
Dukungan ini Markdown dasar, bukan tabel GFM/LaTeX atau semua fitur ChatGPT.

## Bukti

- 179unit lulus/4skip,0gagal/error; termasuk tautan aman/berbahaya.
- Lint akhir0error/0fatal/878warning baseline. Helper final dikompilasi.
- Lima eksekusi native final,3metode unik: suite normal3, layar320dp/font200%
  terang dan gelap masing-masing1. Baris terukur tidak meluber; jawaban besar
  dapat dijangkau dengan scroll. Arab dan literal code terjaga, tidak ada
  AsyncDrawable pada markup gambar/script sintetis. Ini bukan uji seluruh
  bahasa, TalkBack, perangkat/API atau inferensi provider.
- Screenshot light/dark100 dan200 adalah fixture offline pada halaman AI asli,
  bukan respons cloud terbaru. Header Offline dan label balasan ONLINE sengaja
  merepresentasikan riwayat online yang ditampilkan saat offline. Belum uji HP.
- 32berkas asli emulator dipulihkan cocok hash; setting/viewport/izin/helper/
  screenshot sendiri dibersihkan, emulator ditutup. HP tidak disentuh.

Build awal gagal lint karena konstanta break strategy, diperbaiki dengan API29
 guard tanpa menonaktifkan lint. Upaya pertama emulator tertahan RAM; pengguna
mengosongkan RAM. Run native awal berakhir Process crashed sebelum hasil metode;
penyebab awal tidak dipastikan dan tetap dicatat. Renderer dan layar terpisah
kemudian lulus, suite3 normal dan dua ukuran besar lulus. Sesudah memperkuat
assert scroll/wrapping, lima pemeriksaan final lulus termasuk proses cold
force-stop. Log percobaan asli tetap privat dan ringkasannya native-attempts.json.
Jangan menyebut semua percobaan sejak awal lulus atau mengklaim crash awal
memiliki penyebab yang sudah diperbaiki.

## APK

1.9.38/code65,49.477.018byte, SHA256
`837e7b45f833d4377168c01faf859844571720d02158d99c4ebec4419d7ce3d2`.
Salinan lokal D:\Download\Zikra-App-main\Zikra-AI-Typography-v1.9.38-beta-debug.apk.
Di bawah100MB. Signer, audio, WIP PROJECT_HANDOFF/stash/arsip/env/logo/master
sama. Scan semua entry APK tidak menemukan nilai credential server dikenal
atau konfigurasi privat; Firebase client IDs memang untuk SDK. Bukan audit
keamanan absolut. Library core/transitif dipin melalui versi Maven dan license
Apache2.0/BSD2Clause disertakan; tanpa plugin HTML/images/analytics baru.

Provider/model/kuota/backend/cron E02 tidak diubah atau dideploy. Groq/Gemini
belum diintegrasikan. C02 tetap terbuka untuk try_later/antrean dan uji HP saat
laptop sleep/mati; perbaikan font tidak menyelesaikan koneksi. B01,D02/D02a,
E03/E04,F/G/publik serta penundaan UI10/mode orang tua tetap. Bukan seluruh
A–D/E selesai. Checkpoint ini penyelesaian typography sebatas beta emulator.

Referensi resmi: https://noties.io/Markwon/docs/v4/core/getting-started.html,
https://noties.io/Markwon/docs/v4/core/theme.html,
https://github.com/noties/Markwon/tree/v4.6.2.
