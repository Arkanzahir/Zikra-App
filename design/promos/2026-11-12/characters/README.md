# Karakter promo Zikra

Empat ilustrasi karakter mahasiswa laki-laki kartun 2D dibuat memakai
built-in ImageGen. Pengguna mengoreksi kandidat semirealistis dan meminta
kartun. Warna hijau-krem dipilih untuk menyatu dengan materi promo.

- student-confused.png menunjukkan kebingungan saat melihat materi.
- student-understood.png menunjukkan membaca jawaban melalui HP.
- student-focused.png menunjukkan menulis saat mengerjakan tugas.
- student-outdoor.png membawa tas sambil mengecek cuaca melalui HP.

Seluruh PNG memiliki alpha transparan. Prompt lengkap dan peran referensi ada
di [prompts.json](prompts.json). Karakter dasar menjadi referensi dua pose lain.
Tokoh adalah ilustrasi sintetis; tidak berasal dari foto pengguna atau mahasiswa nyata.

Video memakai gerak masuk12px/fade0,4detik dan pergantian pose. Gerak anggota
tubuh tidak dirender. Study ke Focus mempertahankan posisi pose untuk
menghindari kemunculan ulang. Susunan dibaca dari ../characters-edit.json.
Naskah, VO Putri, durasi52detik dan bahan aplikasi dari versi sebelumnya.

Pose luar ruangan memakai [prompt tambahan](outdoor-prompt.json). PNG ini
muncul saat pertanyaan ingin beraktivitas di luar lalu bertahan pada Protector.
Awan/tetes hujan digambar sebagai ilustrasi bergerak memakai clock video yang
sama pada kedua adegan. Ini ilustrasi narasi, bukan pengambilan data cuaca baru.
