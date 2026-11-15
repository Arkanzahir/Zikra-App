# API36 dan pelaporan AI —15 November

Kandidat internal1.9.53/code80, HOLD. APK 49575828byte, SHA256 9dae0409837936f5d4288e1e48c78867e2b31c404a237aefa4fa23281a1aac99.
AGP8.10.1/Gradle8.11.1 checksum resmi, SDK36rev2. Build aplikasi/instrumentasi berhasil.
235JVM lulus,135backend lulus tanpa skip, lint0error/907warning. Baseline52 894warning memakai lint lama; selisih13 belum ditriase.
Emulator AOSP Android16/API36 x86_64,720x1600/320dpi, font100%. Lima metode native lulus, termasuk12kondisi layar/tema, keyboard, laporan-batal, rendererPDF dan symlink.
Screenshot native-api36.png dan report-reason.png diperiksa. HP tetap1.9.48.
Kompatibilitas API26/28/35, OEM, tablet/foldable, landscape dan teks200% kandidat53 belum dibuktikan.
Lima layar customBack memakai opt-out sementara. BaseActivity mengurus safearea sistem/cutout/IME; Home mengurus insets sendiri.
Kegagalan harness tersimpan lokal: salah package AiRefreshTest menyebabkan runner gagal; model AOSP case-sensitive dan marker isolasi belum ada menyebabkan4assert gagal. Koreksi tetap membatasi emulator dan marker. Final5lulus, kegagalan awal tidak dihitung lulus.

Pelaporan online melalui Laporkan, pilih alasan, sunting kutipan maksimal500karakter, konfirmasi. Batal tidak mengirim.
Endpoint /v1/answer-reports authenticated, field allowlist,4096byte/timeout10detik, tanpa panggilan AI. Lua atomik memeriksa revokasi, deduplikasi, batas5laporan berbeda per24jam. TTL laporan7hari/rate24jam. Penghapusan instalasi mencakup laporan.
Backendtests mencakup payload ekstra/besar, receipt,TTL,kuota,revokasi dan scoped-erasure.
Server default menolak penerimaan sampai ZIKRA_REPORTS_ENABLED=true. Belum deploy/aktif produksi, belum triase manusia, belum pengujian kirim aplikasi ke server nyata. PA04 tetap PARTIAL.
Retensi backup/log akun terpisah belum terverifikasi. Tidak ada perubahan provider,billing,signer/publikasi. WIP/MP3 tidak masuk commit.
