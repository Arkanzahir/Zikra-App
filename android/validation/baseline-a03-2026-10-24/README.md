# A03 — Baseline source dan audio tertunda

24 Oktober 2026. **A03 selesai dengan keputusan menyimpan WIP terpisah**, bukan menggabungkan optimasi audio tanpa bukti. [Metadata/hash](baseline.json).

- Lima file WIP (dua source audio, MushafReaderTest, tool Java dan PowerShell) dicadangkan ke arsip privat `.local/pending-audio-a03-2026-10-24/source-wip.zip` dan stash lokal `35686be384ac549e5a5bf85be87cfe56e3b667df`. Isi arsip dan stash diperiksa cocok setelah normalisasi line ending. Stash/arsip **tidak dipush ke GitHub**; source stabil tetap di main.
- Kandidat prefetch mengubah pembatalan dan konkurensi cache/audio, tetapi skenario perpindahan/stop/destroy/network terputus belum dibuktikan. Tool audit metadata bukan bukti audio terdengar/sinkron. Optimasi ini tetap terbuka di A03-pending/D03; menggabungkannya kelak memerlukan review dan pemeriksaan tersendiri.
- **470 file produksi/build** di checkout dicocokkan dengan snapshot yang menghasilkan APK 1.6.2 (termasuk kedua source audio, tanpa pengecualian lagi). Tidak ada perbedaan selain line ending. APK tetap 31.216.429 byte, SHA-256 `e8eba21c2411531fe26fab9374012368ed4231f4645b7d5755a2bcf863123f05`. [Bukti runtime sebelumnya](../dua-list-2026-10-24/README.md).
- Tidak ada source runtime baru pada langkah A03; karena itu tidak mengulang build/emulator. A04 yang mengubah integrasi harus menghasilkan build/source/bukti baru, bukan memakai hash APK lama untuk mengklaim perubahan baru sudah terpasang.
- PROJECT_HANDOFF.md dan draft ilustrasi/prototipe lokal dipertahankan; tidak dimasukkan ke stash ini atau staging. Konfigurasi/credential privat tidak dibaca/disalin ke laporan.

Pemulihan WIP dilakukan setelah menyimpan pekerjaan aktif terlebih dahulu, lalu `git stash apply 35686be384ac549e5a5bf85be87cfe56e3b667df` pada checkout yang sesuai dan meninjau konflik. Jangan memakai `pop`/menghapus stash sebelum hasil pemulihan diperiksa; arsip privat tetap menjadi cadangan independen di mesin ini.

## Keamanan selama pengerjaan

Skill security-best-practices diterapkan sebagai pedoman pengerjaan. Stack aktual: Android Java/XML; backend Node core `http` tanpa Express; alat Python standard library, bukan Django/Flask/FastAPI. Skill tidak menyediakan referensi khusus Java Android atau Node core/Python standard library, sehingga pedoman framework yang tidak dipakai tidak dipaksakan.

Pemeriksaan terbatas terhadap dua pola credential pada source Android/backend terlacak menghasilkan nol kecocokan; ini **bukan** audit seluruh history/rahasia/dependensi atau sertifikasi aman. Provider key tetap server-only; jalur gateway lama hanya uji pribadi, belum autentikasi produksi. Rotasi key yang pernah dibagikan dan kesiapan server tetap C01/C02/G03. A04 memakai data lokal dan pemeriksaan terarah; tidak menambah endpoint, unggah profil/lokasi, bypass TLS atau menulis nilai rahasia dalam log.
