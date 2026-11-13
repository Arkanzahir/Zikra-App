# Acuan visual Zikra —13November2026

Acuan ini membekukan keputusan visual yang sudah diterima pengguna dan token source saat checkpoint. Bukan desain baru, penerimaan semua screenshot, atau bukti F02/F03/F04 lulus. Perubahan tampilan baru tetap dibedakan dari perbaikan lifecycle/keterbacaan.

## Identitas dan ikon

Logo dipoles tetap dari design/concepts/zikra-refined-2026-10-27/logos/logo-polished.svg, simbol native drawable-nodpi/zikra_symbol_pine.png dan zikra_symbol_white.png; artwork/logo utama tidak diganti. Delapan ikon fitur mengacu alias feature_icon_* dalam android/app/src/main/res/values/refined_ui.xml menuju home_shortcut_ref_* (Quran, Doa, Kiblat, Hifz, AI, Protector, Study, Hadis). Halaman lain memakai keluarga yang sama untuk identitas fitur. Back, chevron, play/pause, alarm dan aksi dokumen adalah kontrol fungsional, bukan logo fitur alternatif. Jangan menebalkan atau menggambar ulang ikon/ilustrasi yang sudah diterima.

## Teks dan warna

Theme.Zikra.Base dan dialog memakai Plus Jakarta Sans; font asli res/font/plus_jakarta_sans.ttf. Arab reader memakai Amiri Quran (amiri_quran.ttf); Mushaf QCF V2 halaman1–604 berbeda karena edisi/glyph, tidak disubstitusi fontUI. Protector mempertahankan keluarga Roboto yang sudah ada bila gaya lokal memakainya, bukan alasan mengganti semua font. Notice font tetap.

| Token | Terang | Gelap |
|---|---|---|
| app_accent | #1C5C4A | #A4D7BE |
| app_surface | #FFFFFF | #20372C |
| background | #F4F6F8 | #13261E |
| text | #15332F | #F0F7EE |
| muted | #586C63 | #B8CDBD |

Source tokens: values/colors.xml, values/app_appearance.xml dan values-night/app_appearance.xml. Jangan mengganti warna foreground/background satu sisi hingga ikon kehilangan kontras. Preferensi terang/gelap/perangkat tetap satu via AppAppearance; back stack mengikuti pilihan itu. Langit/jam/awan/matahari/bulan/lampu masjid mengikuti waktu/lokasi pilihan, terpisah dari tema permukaan aplikasi. Reader yang menyediakan kontrol bacaan khusus harus menjaga pilihan eksplisit pengguna.

## Struktur, ukuran dan gerak

Shared FeatureHeader: tinggi minimum64dp, padding horizontal12dp/vertikal8dp, back/aksi48×48dp, judul20sp tebal dan subtitle13sp; judul boleh membungkus. Token screen_inset22dp/touch_target48dp tetap; variasi inset20dp pada Protector dan komposisi Home yang disetujui disengaja, bukan instruksi merombak semua layar. Jarak tambahan memakai langkah4/8dp. NavDock shared: lima tab Home/Quran/ZikraAI/Study/Profil, pemilihan aktif konsisten; tinggi dinamis64–96dp mengikuti fontScale. Keyboard/halaman immersive tidak boleh menghilangkan satu-satunya jalan kembali.

Logo/aset/day-night/parallax/splash yang sudah diterima tetap. Motion deklaratif berhenti ketika tidak foreground/terlihat/fokus, layar padam, animasi OS mati atau hemat daya aktif. Sensor parallax hanya saat gerak terlihat; listener/frame dibersihkan ketika detach. Penghentian dekorasi tidak menghentikan timer belajar/audio/alarm yang memang berjalan. Tidak menjanjikan angka penghematan baterai tanpa pengukuran HP.

## Matriks pelaksanaan berikutnya

| Kelompok | Aturan source yang dipertahankan | Bukti final yang masih diperlukan |
|---|---|---|
| Home/splash/onboarding | Logo/aset asli, satu bangunan, waktu/langit terpisah tema; motion policy | Terang/gelap/teksbesar/offscreen/animasi0/baterai |
| Quran/reader/Mushaf | Amiri vs QCF sesuai edisi, ukuran bacaan dan bookmark tetap | Harakat/scroll/sorotan lama/layout+font cache+audio |
| Study/fokus/Hifz | Shared dock/scene, status timer asli; dekorasi tidak mengendalikan progres | Font200/landscape/keyboard/hidden/restore |
| AI | Desain Seimbang, composer/lampiran/riwayat dan dock sesuai keputusan | Mode/theme/keyboard/dialog/loading/gagal |
| Protector/cuaca/gempa/pengingat | Ilustrasi langit/peta dan header/dock yang ada; sumber/timestamp nyata | Data usang/gagal/opt-in/tema/izin/screen reader |
| Doa/Hadis | Ikon alias, referensi dan Arab/Latin/arti terpisah | Teksbesar/filter/koleksi opsional/footer |
| Profil/salat/Kiblat/lokasi/masjid/kalender | Ruang pribadi, pilihan nyata dan fallback; tidak ada statistik contoh | Semantik fokus/tombol/rotation/permission/native final |

Ini daftar layar yang perlu diselaraskan dan diperiksa, bukan status seluruh layar sudah seragam. UI10 sorotan dan F05–F08 akun/sync/admin tetap ditunda. Uji native dan pengukuran fisik mengikuti sesi gabungan yang diminta pengguna.
