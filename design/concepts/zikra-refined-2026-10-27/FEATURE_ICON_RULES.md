# Identitas ikon Zikra — keputusan28 Oktober2026

Logo utama dipoles disetujui. Untuk fitur, pakai bentuk Home yang sudah disetujui
di seluruh tujuan fitur: pintasan, navigasi, Profil, header identitas, kartu masuk,
dan keadaan kosong. Ukuran mengikuti konteks; bentuk/path tidak berubah.

| Fitur | Simbol acuan | Resource Android yang sudah ada |
| --- | --- | --- |
| Quran | Mushaf terbuka di rehal | `home_shortcut_ref_quran` |
| Doa | Dua telapak tangan | `home_shortcut_ref_dua` |
| Kiblat | Kompas | `home_shortcut_ref_qibla` |
| Hifz | Buku dengan penanda halaman | `home_shortcut_ref_hifz` |
| Zikra AI | Dua kilau | `home_shortcut_ref_ai` |
| Protector | Perisai dengan centang | `home_shortcut_ref_protector` |
| Study | Topi wisuda | `home_shortcut_ref_study` |
| Hadis | Kitab dengan ornamen | `home_shortcut_ref_hadith` |

Home dan Profil mempunyai simbol navigasi sendiri, dipakai konsisten. Warna aktif
dan latar pilihan boleh berubah; identitas fitur tetap. Simpan ikon sebagai satu
acuan per fitur, bukan salinan berbeda yang diedit per halaman.

Ikon tindakan menggambarkan fungsinya: kembali/panah, tambah/plus, cari/kaca
pembesar, edit/pensil, simpan/bookmark, berhenti/stop. Bookmark sebagai tindakan
menyimpan Hadis boleh dipakai; **kartu menuju fitur Hadis** tetap memakai kitab.
Lokasi memakai pin, pengaturan memakai roda gigi, Tentang memakai informasi;
jangan mendaur ulang kompas Kiblat atau kilau AI untuk tujuan yang tidak terkait.

## Status penerapan

Referensi sudah memakai delapan sumber bentuk yang sama di21 penempatan.
[Hasil pemeriksaan](checks.json) memastikan masing-masing fitur hanya mempunyai
satu sumber gambar. Path dan grup mirror asli tetap pada
[receipt sumber](shortcut-preservation.json).

Native belum diterapkan. Audit baca saja menemukan: Study memakai topi pada Home
tetapi checklist pada dock lain; Quran Profil/dock memakai buku umum; Hadis Profil
memakai bookmark; Doa memakai keluarga lain. Lokasi/Masjid/Pengaturan Profil masih
mendaur ulang kompas. Catat sebagai pekerjaan F01/F02, bukan sudah diselesaikan
oleh referensi. Penerapan harus memeriksa seluruh pemanggil termasuk tema gelap,
keadaan aktif/nonaktif, teks besar dan ukuran sentuh, sambil menjaga data pengguna.
