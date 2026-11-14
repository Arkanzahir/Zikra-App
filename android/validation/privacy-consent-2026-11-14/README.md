# Izin AI dan tinjauan privasi — 14 November 2026

Kandidat internal **1.9.51/code78**, **50.364.855byte**, SHA256
`99eeba78ed0f240ae9feea50db8f6592c442b5894fa6800565e908a1e15afb7c`.
APK lokal `D:/Download/Zikra-App-main/Zikra-Privacy-v1.9.51-beta-debug.apk`.
**Debug/HOLD**; HP tetap1.9.48, tidak dipakai atau diubah. Website ditunda.

## Perbaikan terarah

P01 — pemeriksaan izin semula hanya di alur dialog. Request konteks/gambar yang
masih tertunda belum mengikat review pada layanan/credential/revisi yang sama;
client belum melakukan pemeriksaan terakhir sebelum transport antre berjalan.
Dampak: review lama berpotensi diteruskan setelah konfigurasi/izin berubah.
Tidak ada bukti data pengguna benar-benar terkirim melalui skenario ini.

- AssistantConsent menyimpan revisi acak lokal setiap accept/revoke. Receipt
  immutable menyegel endpoint, revisi dan identitas credential terenkripsi,
  tanpa menambah PII/tanggal lahir atau mengirim receipt ke backend.
- sendBasicOnline/showSelection menangkap receipt sebelum snapshot dikirim;
  sendOnline dan interceptor client memeriksanya lagi. Tidak mengaktifkan
  consent baru otomatis bila receipt lama tidak berlaku.
- Chat yang sudah antre tidak dapat memakai izin yang baru diberikan kembali.
  Balasan setelah pencabutan dibuang dari callback, bukan diklaim ditarik dari
  penyedia. Request yang telah melewati pemeriksaan transport mungkin sudah
  dikirim; tidak menjanjikan penghapusan/recall lewat cancel.
- Konfirmasi gambar tetap perprompt dan tidak memberi basicchat consent;
  snapshot layanan diikat ke modal, dicek sebelum send dan saat antre berjalan.
- CONSENT memakai pesan khusus/label Perlu tinjau izin, bukan kuota/provider penuh.
- Izin lama yang valid tetap berlaku tanpa memaksa dialog lagi. Mode otomatis
  mengikuti assistant_mode_v2, tidak menghidupkan kembali flagonline legacy yang
  sempat menjadi sumber masalah versi lama. Offline eksplisit tetap memblokir.
- Tidak mengubah skema body/headers/rute, model, kuota atau backend produksi.

## Bukti kandidat yang sama

| Pemeriksaan | Hasil |
|---|---|
| Seluruh JVM |230lulus,0gagal/0skip;6baru untuk grant/revoke/reaccept/binding/legacy/unrelated prefs |
| Native permission |9metode lulus: tanpa izin, valid, antre revoke→accept, antre layanan berubah, late reply setelah revoke, modeauto legacyflagfalse, gambar tanpa basicgrant, gambar revoke sebelum send dan gambar antre layanan berubah |
| Native regresi |5metode lulus: status sukses/auth/quota/timeout/malformed;Cloudflare error;consent sekali perendpoint;body-readerror;UI review usulan tersimpan hanya setelah konfirmasi/doubletap tetap sekali |
| Lint |0error/894warning; warning bukan seluruhnya selesai |
| Audit APK |0entri konfigurasi privat/credential dikenal, signer debug sama, kedua hash audio tetap; <100.000.000byte |

Emulator ZikraPrivacyQA/API37.1, ADB5043/loopback5585; image memaksa4GB walau
konfigurasi meminta1536MB. Namespace preferensi nativepermission unik, marker
wajib dan model emulator diperiksa. Lima regresi memakai data/transport contoh
pada emulator milik tugas. Tidak mengirim pesan ke Cloudflare/Featherless atau
menghapus registry produksi; tidak menjalankan uji adzan/restartHP baru.

Build pertama chat berhasil. Saat melengkapi image ada compile error import
AssistantConnection yang belum ditambahkan; import diperbaiki lalu build/JVM/
lint/native akhir lulus. Tidak menganggap build gagal sebagai verifikasi sukses.
Tes transport existing kini memberi izin eksplisit di fixture directclient;
jalur UI tetap melalui izin/review normal, bukan melonggarkan guard produksi.

## Privasi13+ dan batas

[Tinjauan remaja dan sumber primer](../../../docs/beta/TEEN_PRIVACY_REVIEW.md)
membedakan target13+, basicconsent, wali, OSpermission dan retensi. Lokasi presisi,
mekanisme usia/wali, kebijakan/ketentuan model, evaluasi keselamatan, log/backups
akun dan penghapusan penyedia belum ditutup. Jangan mengganti flag itu menjadi
selesai karena kontrol chat ini lulus. Tidak mengumpulkan DOB/KTP/wajah untuk uji.

Belum uji HP/seluruhOEM, actualprovider/stress, seluruhUI consent200%/dark, native
powerloss/ENOSPC atau seluruh jalur PDF/kamera. Capabilities dapat meminta metadata
layanan tanpa isi pesan; pencabutan basicchat bukan pemadaman semua koneksi fitur
Quran/cuaca/push. Batas temporal setelah request mulai tetap berlaku.

Hasil backup1.9.50/Quran1.9.49/HP1.9.48 tetap bukti versi masing-masing; tidak
menjumlahkannya sebagai hasil kandidat ini. Finalsigner/hak/B01/audio/J13/J14/
publik tetap terbuka. Backend tidak berubah;134tes terdahulu tidak diulang.
Bukti mentah privat `.local/privacy-20261114`; tidak commit konfigurasi/pesan/key.

## Penutupan lingkungan

Folder/data E:/Zikra-PrivacyQA-20261114 telah dihapus setelah proses headless
dan parent milik task dihentikan. PID berasal dari lockfile AVD yang ber-marker
dan executable SDK cocok. Skrip mengalami konversi waktu JSON di PowerShell;
identitas PID/path/marker sudah terikat task, tetapi pengecekan waktu tambahan
itu tidak dihitung berhasil. Keberadaan folder akhir diperiksaFalse. HP dan
aplikasi pengguna tidak disentuh. Tujuh hash konfigurasi privat, WIP handoff
dan arsip audio cocok baseline; stash tetap.
