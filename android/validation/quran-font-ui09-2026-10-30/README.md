# UI09 — Amiri Quran dan pembaruan HP 1.9.29

30 Oktober 2026. Pengguna menyetujui **font saja**, menunda perubahan sorotan,
dan mengizinkan pembaruan lewat USB. Font yang sama dengan preview dipasang
pada teks Arab reader, judul/nama surah Quran dan preview ukuran teks.
**HP aktual diperbarui 1.9.25 → 1.9.29/code56**, tanpa uninstall aplikasi utama.

## Font dan cakupan

Amiri Quran Regular1.003 dibundel, tidak mengikuti pilihan font Arab bawaan HP
atau menunggu unduhan. [Sumber/hash130.184byte](font-source.json),
[audit font](font-audit.json). File memakai URL statis Google Fonts yang sama
dengan preview disetujui; lisensi/copyright SIL OFL1.1 dari
[repositori resmi Google Fonts](https://github.com/google/fonts/tree/main/ofl/amiriquran)
dibundel utuh di `assets/licenses/amiri-quran-OFL.txt`. Font tidak dimodifikasi.

Ukuran pilihan lama tetap; font padding mengakomodasi harakat. Tidak mengubah
teks/terjemahan, nomor/urutan ayat, ukuran preferensi atau pemetaan audio.
Mushaf tetap memakai QCFv2 per halaman. [Batas perubahan](scope.json): kode
sorotan, warna emas dan latarnya tetap. UI10 menyimpan permintaan peningkatan
kontras yang ditunda; preview mint sebelumnya tidak menjadi implementasi APK.

## Bukti

[Reader HP](phone-reader.jpg), [pengaturan HP](phone-settings.jpg),
[render terang](font-light.jpg), [render gelap](font-dark.jpg).
Semua gambar berasal dari Android API36 pada HP yang diizinkan. JPEG hanya
diperkecil. Dua render ayat dibuat dalam context terpisah; bukan perubahan
tema/font sistem HP atau klaim semua layar fisik sudah diperiksa.

[Dua metode native lulus](native-results.json), [264 layout font](font-shaping.json):
empat surah bawaan, ukuran24/32/44 dan konfigurasi font1×/2×, terang/gelap.
Teks identik sumber, Typeface cocok resource, glyph/harakat berada dalam batas,
preview pengaturan dan reader Activity memakai font yang sama; font Latin
terjemahan tetap. Pemetaan empat kata Al-Fatihah2 ke timing lama tetap cocok.
Tidak memutar atau mengklaim menguji rekaman audio baru.

Audit cmap menemukan38 codepoint Arab pada sumber empat surah bawaan/nama,
semuanya tersedia. Ini bukan audit setiap varian Unicode seluruh6236 ayat.
[163 unit lulus/4 skip, lint0 error/862 warning](checks.json).
Build app/helper/unit/lint berhasil. [Percobaan awal](native-attempts.json)
memperlihatkan assertion lebar helper menghitung spasi akhir tak terlihat;
dikoreksi ke lebar baris terlihat dan diuji ulang. Tidak mengubah app/font
untuk menyembunyikan kegagalan itu. Ukuran/font sistem HP tidak diubah.

## Data HP dan paket

[Preservasi](preservation.json): 32 berkas internal dan APK lama dicadangkan
privat sebelum update.17 tabel mempertahankan isi asli; preferensi non-turunan,
izin, pengaturan sistem dan kebijakan baterai tetap. Hanya rencana alarm
turunan diperbarui oleh aplikasi. Navigasi uji Al-Fatihah memulihkan empat
nilai bookmark sebelumnya bila pengguna tidak mengganti bookmark saat uji.
Tidak mengembalikan database secara membabi buta atau mengunggah backup/chat/GPS.
[Helper/marker/gambar uji dibersihkan](cleanup.json); Zikra terbaru dibiarkan terbuka.

APK lokal `D:\Download\Zikra-App-main\Zikra-QuranFont-v1.9.29-beta-debug.apk`:
**45.415.563byte / 45,4MB**, di bawah100.000.000byte.
SHA256 `61389f725ad10c545eef0f11fa75d6f4639dbd9fda97cf320c231e50dc10457c`.
Signer debug sama `c7888fe2ca40e14698726ed20b2f93947033cef0bdee9cfd127c429d0ea0ca1b`.
[Audit seluruh entry APK](apk.json): 0 nilai privat yang diketahui, font dan
lisensi cocok sumber, kedua rekaman adzan tetap cocok hash pilihan terakhir.
APK/helper/audio/key tetap lokal, tidak masuk Git.

UI09 selesai teknis beta terarah. WIP PROJECT_HANDOFF, stash/arsip audio,
env privat, ikon/artwork dan desain lain dipertahankan. B01, C02/C07,
D02/D02a, UI10, F/G serta seluruh A–D/publik tetap menurut gerbang terbuka.
Tidak ada deploy backend, inference AI atau pemeriksaan email baru.
[Receipt](receipt.json) mengikat APK/source/uji; commit receipt hanya dokumen.
