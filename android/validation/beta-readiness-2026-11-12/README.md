# Persiapan beta / G04 — 12 November 2026

**HOLD, belum paket siap distribusi.** Pengguna memprioritaskan kesiapan beta
sebelum website download. Ini pekerjaan persiapan G04/G03, tidak menutup B01,
C02, D02/D02a, E03/E04, F global, G01–G05 atau seluruh A–D.

## Hasil konkret

- Jalur signing release membaca environment privat saja, menolak nilai kosong/
  sebagian. Kandidat unsigned memerlukan opsi eksplisit dan tidak boleh bersamaan
  signing environment. Keystore di dalam repo wajib di `.local/` yang di-ignore.
  Ignore tambahan mencakup JKS/keystore/P12/PFX. Belum membuat keystore baru.
- Pemeriksa paket menolak APK debug/debug signer, signer berbeda/tidak dipatok,
  paket unsigned, identitas versi salah, manifest backup/cleartext tidak aman,
  private filename, credential dikenal dan ZIP berlebihan/ambigu. Exit berhasil
  pemeriksaan artefak pun **tidak** memberi persetujuan distribusi otomatis.
- [Runbook, urutan, migrasi signer dan gerbang](../../../docs/beta/README.md),
  [draf privasi](../../../docs/beta/PRIVACY_DRAFT.md),
  [inventaris layanan](../../../docs/beta/SERVICES.md),
  [catatan beta/panduan instal](../../../docs/beta/RELEASE_NOTES.md) disiapkan.
  Penerbit/kontak/retensi dan keputusan signer/migrasi belum disahkan. Jangan
  menampilkan draf sebagai kebijakan final.

## Bukti terarah

- [Build](build.txt) berhasil assembleRelease, lintRelease dan testDebugUnitTest.
  [Ringkasan](checks.json) memuat 189 unit lulus, 4 skip, 0 gagal/error; lint
  release 0 error/fatal dan 879 warning. Ini varian release, bukan perbandingan
  langsung lint debug sebelumnya. Tidak mengulang native/emulator/HP.
- [Backend](backend-tests.txt) 91/91 tanpa skip memakai Redis/Lua tiruan lokal.
  Tidak memanggil inference/cloud Redis nyata. [Percobaan awal](backend-first-attempt.txt)
  gagal satu karena izin direktori temp dan 13 skip tanpa fixture; tetap disimpan.
- [Delapan regresi audit](audit-tests.txt) mencakup flag debug/test-only,
  signer palsu/debug/tidak dipatok, secret UTF-16, file privat dan entri ZIP
  duplikat. [Signing kosong](signing-guard.txt) dan
  [signing sebagian](partial-signing.txt) benar-benar ditolak Gradle;
  keystore/password sah untuk signing release belum diuji.
- [Health](health.json) HTTPS 200 dan lookup akses tak dikenal 401, tanpa key
  aktif/inference/kuota AI. [Probe awal](health-first-attempt.json) gagal di
  lingkungan akses jaringan terbatas; retry berizin berhasil, bukan bukti
  perbaikan atau perubahan server. [Build awal](build-first-attempt.txt) gagal
  akses cache drive E; retry berizin mencapai guard yang memang diharapkan gagal.
- [Pemeriksaan email B01](b01-mail-check.json) menemukan hanya satu pesan SENT
  pada thread klarifikasi, belum balasan masuk. Tidak mengirim follow-up baru,
  dan ID/mailbox privat tidak dimasukkan Git.

## Kandidat dan keterbatasan

[Kandidat unsigned](unsigned-apk.json), 1.9.40/code67, **37.809.274 byte**,
SHA-256 `d0a3fa423bfe4fe4c6c6bc4da30f721842a15ebb552c839d86d4be58d25efc6e`.
Manifest non-debug/non-test, backup/cleartext false; tidak menemukan file
konfigurasi privat atau nilai server credential yang dikenal pada seluruh
entri terurai. **Tidak ditandatangani, tidak dapat dipasang, tidak memuat dua
rekaman adzan privat**, sehingga bukan paket beta final atau pengganti debug.
Tetap di `android/app/build/outputs/apk/release/app-release-unsigned.apk`.

[APK debug lama](debug-apk.json) tetap 49.680.570 byte/SHA sama, dua hash audio
tetap, signer debug terverifikasi. Audit dengan benar menolak status release.
Pemindaian nilai yang dikenal tidak membuktikan tidak ada seluruh kemungkinan
secret/fixture. Tidak mengubah sumber audio atau desain untuk mengejar kelulusan.

[Snapshot source](source-snapshot.json) terikat base commit dan hash input
workspace; bukti rebuild terpisah memeriksa input/hash paket tetap. WIP Android
lama yang berbeda line ending tidak direset atau ikut commit. Commit checkpoint
yang memuat source/bukti menjadi rujukan review, bukan klaim checkout bersih.

PROJECT_HANDOFF WIP, stash/arsip audio, env legacy, logo dan foreground cocok
hash bukti sebelumnya. Konfigurasi privat lain disnapshot lokal untuk memastikan
tidak berubah. HP tidak diakses, terakhir dibuktikan 1.9.39; emulator tidak
dijalankan. Tidak ada deployment, rotasi key, upgrade paket, upload Drive,
pembuatan key signing, email baru atau publikasi APK/video/website.

## Sisa sebelum beta dibagikan

Keputusan sumber/hak Hadis dan adzan, lafaz, uji HP idle yang belum konklusif,
privasi/kontak/pemenuhan syarat layanan, akses tester dan C02 yang terbuka,
sertifikat release/migrasi data, lalu smoke/upgrade pada paket final signed.
Sumber konten tetap wajib lolos sebelum distribusi termasuk beta sesuai cakupan.
Promo sudah diterima secara visual oleh pengguna; hak publikasi suara demo
masih terpisah. Tidak menerbitkan promo sebagai efek persiapan beta.
