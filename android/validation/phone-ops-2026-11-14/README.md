# HP, operasi akses dan cakupan beta — 14 November 2026

Kandidat tetap **1.9.48/code75**,49.615.884byte, SHA256
`7a8d094170d31b2dbd25b5e0e31074b61642b3208e0b269b5ce6b90ab61b6319`.
APK utama tidak dibangun ulang/diganti pada delta ini. Debug/HOLD.
Nama penerbit Zikra, dukungan arkanzahir46@gmail.com; pengguna memilih sasaran
**13+**, termasuk remaja13–17tahun. Verifikasi usia/persetujuan untuk remaja
belum otomatis tersedia; tidak menyatakan kebijakan final atau beta siap publik.

## Hasil dan bukti

| Pekerjaan | Hasil | Batas |
|---|---|---|
| Pembaruan HP |1.9.39/code66→1.9.48/code75,install-r berhasil|Tanpa uninstall/clear data; signer debug sama, bukan migrasi release|
| Cadangan/data |5DBintegritas valid,17tabel,42baris isi asli dipertahankan|Satu timestamp `conversations.updated` berubah saat membuka AI; bukan isi chat/draf yang hilang|
| Preferensi |Nilai key pengguna lama tetap|Cache langit dan rencana alarm diperbarui normal; key baru tidak dihapus untuk memaksa byte-identik|
| Native HP |1case lulus,14kondisi layar/tema|Home,AI,Quran,Study,Profil,Kalender,Protector;light/dark;5tomboldockAI terlihat,composer di atas dock|
| Backend |134lulus,0skip/gagal|Termasuk7tes erasure baru dengan Lua aktual pada fakeredis/Lupa lokal; bukan operasi Upstash produksi|
| Website |Pemeriksaan statis lulus|Cakupan13+/batasprivasi diperbarui; tidak hosting/unduhan publik|

Log native dan ringkasan backend serta hasil data agregat ada di direktori ini.
Cadangan tar, salinan SQLite, hash/selisih preferensi dan log lengkap hanya di
`.local/phone-final-20261114/`; tidak di Git/APK dan tidak dikirim ke AI.
Tidak ada screenshot pribadi. HP Xiaomi Android nyata, bukan emulator.
Perbandingan data pertama yang menuntut semua kolom identik mendeteksi perubahan
satu timestamp chat; investigasi membatasi pengecualian hanya kolom itu dan
membuktikan seluruh isi asli tetap. Tidak memulihkan backup massal atau menimpa
pilihan pengguna. Perbandingan XML dinormalisasi agar whitespace bukan perubahan.

Tes sementara mengubah hanya keytema danshake, lalu mengembalikannya termasuk
ketiadaan key asli. Tidak mengubah jaringan,izin,volume,modebaterai atau autostart.
Marker otorisasi dan paket test `com.zikra.app.test` dibersihkan, aplikasi utama
dibuka kembali. Tidak restart HP atau mengubah boot/idle; tidak perlu mengulang
uji reboot lama. Tidak ada simulasi daya yang perlu dipulihkan pada sesi ini.

Pengguna meminta tidak mengulang audio yang dianggap aman; tidak ada pemutaran
adzan baru atau pemberian status lulus palsu pada gap selesai-alami/Doze versi
baru. Status izin OS awal lengkap tidak direkam, jadi tidak mengklaim seluruh
izin sebelum/sesudah dibandingkan. Smoke tema bukan audit visual semua piksel,
font200%,sensor,notifikasi,semua dialog atau ketepatan jadwal di semua daerah.

## Delta operator dan privasi

`installation-erasure.mjs` dan CLI operator menambahkan inspect read-only,
prepare/revoke dengan timestamp,masa tunggu120detik,cek lease aktif dan erase
terbatas satu index↔registry beserta usage terkait. Target dan konfirmasi harus
persis, scope berbatas, error tidak mencetak nilai sensitif. Tidak menghapus
counter/snapshot/push bersama atau record instalasi lain. Alat tidak diekspos
sebagai endpoint, tidak deploy backend dan tidak dipakai pada registry produksi.
[Prosedur/batas](../../../docs/beta/INSTALLATION_ERASURE.md).

[Retensi penyedia](../../../docs/beta/RETENTION_REVIEW.md) ditinjau dari sumber
primer; konfigurasi akun/log drains/backups dan penanganan remaja tetap terbuka.
Tidak menjanjikan nol log atau penghapusan semua salinan dari TTL/key lokal.

Pencarian baca-saja Gmail akun terhubung14November menemukan email terkirim
24Oktober kepada contact@gading.dev tentang sumber HadithAPI, tetapi tidak
menemukan balasan dari domain gading.dev sejak24Oktober. Ini hasil pencarian akun
terhubung, bukan bukti semua akun/kanal telah diperiksa. Tidak mengirim email
lanjutan, menerbitkan isi email, atau menganggap B01 selesai.

## Gerbang tersisa

Hak/atribusi sumber Quran/font/audio/HadisB01,izin rekaman,penanganan remaja,
retensi akun/cadangan dan proses dukungan nyata,signer/migrasi release,
sisa skenario J02/J03/E/F/J13,putusan beta dan distribusi HTTPS tetap terbuka.
Login/sinkronisasi/admin/UI10/modeorangtua tetap ditunda sesuai keputusan lama.
Jangan menyebut A–D/E/F/G atau seluruh pekerjaan selesai dari satu smoke HP.

Tambahan guard operator: penerbitan token kini menolak path source/repo di luar
.local, menolak argumen berlebih dan UUID revoke nonkanonis. Pemeriksaan real
parent bersama dipakai alat token dan backup untuk mengurangi duplikasi. Dua
tes guard tambahan lulus; final suite134lulus/0skip. Tidak membuat token baru
atau mengubah registry produksi pada sesi ini.
APK base yang diambil kembali dari instalasi HP memiliki hash identik kandidat;
bukti installed-artifact.json. Paket test sudah dihapus, aplikasi utama tetap.
