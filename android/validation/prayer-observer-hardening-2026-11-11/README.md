# D02 — koreksi observer dan percobaan HP yang belum lulus

## Hasil

Observer diperbaiki, 19 tes Python lulus. **Uji idle fisik belum lulus; D02/D02a
tetap terbuka.** Tidak ada perubahan aplikasi produksi/APK 1.9.39, jadwal asli,
mode audio, backend, konfigurasi privat atau desain. HP sudah dibersihkan dan
daya normal; jangan menganggap masih siap diuji tanpa koordinasi berikutnya.

## Temuan alat

Subperintah `dumpsys activity instrumentation` ternyata tidak didukung pada HP
dan mengembalikan pesan error dengan exit nol. Observer lama membaca itu sebagai
tidak ada instrumentation. Pengamatan baru memakai dump `activity processes`,
memvalidasi header dan memeriksa record target instrumentation. Sinyal lama
instrumentation=false bukan bukti independen. Bukti alarm/preservasi restart
sebelumnya tetap dicatat dengan koreksi, bukan dihapus atau dipakai menutup D02.

Pembacaan penyimpanan `run-as ls` kini wajib menghasilkan `files`; output error
dengan exit nol tidak dianggap terbuka. USB hilang selama sepuluh detik saat idle
segera menghasilkan belum konklusif. Tes mencakup paket target berbeda, command
tidak didukung, penyimpanan gagal, disconnect singkat/panjang dan status terminal.
Satu tes baru awalnya gagal karena fixture reconnect tidak memiliki preferensi
pengingat; fixture dikoreksi menjadi snapshot terhubung tetapi belum lengkap.

## Percobaan HP

Pengguna menyatakan siap uji. Alarm asli berikutnya masih sekitar 8,5 jam lagi;
direncanakan satu alarm sintetis melalui receiver yang sama, tanpa mengganti
waktu salat asli. Backup 48 berkas dibuat dan diperiksa. Instalasi helper pertama
gagal, retry berhasil. Dua build helper sukses; tidak memasang ulang aplikasi utama.

Mode instrumentasi `--no-restart` dipilih untuk menghindari force-stop pada akhir
persiapan. [Sumber AOSP](https://android.googlesource.com/platform/frameworks/base/+/master/cmds/am/src/com/android/commands/am/Am.java)
mengenali opsi tersebut; [pengelola instrumentation](https://android.googlesource.com/platform/frameworks/base/+/75e09d14c90b302dda264480d52c426629c5d8e9/services/core/java/com/android/server/am/ActivityManagerService.java)
membedakan akhir sesi dengan flag tersebut. Dukungan sumber bukan bukti bahwa
runner bekerja pada HP ini: percobaan timeout 40 detik sebelum metode uji membuat
state/hasil/alarm. Pemeriksaan dump yang benar menemukan instrumentation masih
aktif. Tidak ada simulasi daya baru diterapkan. Percobaan membuka activity internal
langsung ditolak karena tidak exported; tidak mengubah batas akses tersebut.

Instrumentation dihentikan, aplikasi dibuka normal untuk memulihkan jadwal,
helper dihapus dan marker milik sesi dibersihkan. Metode native eksperimental
tidak dipertahankan di source; salinan privat disimpan untuk diagnosis. Tidak ada
klaim alarm sintetis berhasil, pemutaran, suara terdengar atau rekaman selesai.

## Preservasi dan batas

[checks.json](checks.json): 15 berkas preferensi byte-sama; satu file plan hanya
menghapus satu slot yang sudah lewat beserta metadata epoch/lokasi/revisi/sumber.
Kesetaraan byte semua 16 file awalnya gagal dan laporan awal disimpan; pemeriksaan
lanjutan membuktikan tidak ada perubahan preferensi lain atau penambahan slot.
Seluruh baris 20 tabel sama, setting daya sama, forced idle/simulasi baterai mati,
tidak ada instrumentation aktif, 30 alarm masa depan aktif. Tidak mengembalikan
file plan kedaluwarsa sekadar untuk membuat hash cocok.

Penelaahan cadangan sebelumnya menemukan slot, epoch, revisi dan signature sumber
tercatat; itu belum membuktikan semua kondisi valid saat pengiriman. Penyebab
pengiriman lama yang tidak teramati belum dipastikan. Pengujian fisik memerlukan
metode persiapan yang terukur atau pengamatan alarm asli dengan koneksi stabil.

[Halaman penerbit audio](https://islamdownload.net/123801-download-suara-adzan.html)
menawarkan unduhan gratis dan menamai rekaman, tetapi izin distribusi aplikasi
dan penerima berwenang belum dibuktikan. Draf tetap belum dikirim; verifikasi
lafaz penuh dan hak rekaman tetap terbuka. Tidak ada pesan pihak ketiga dikirim.
