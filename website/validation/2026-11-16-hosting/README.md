# Hosting preview Zikra —16 November 2026

Pilihan pengguna: preview terlindungi dengan video lengkap bersuara dahulu; bukan publikasi APK atau video umum.

URL: https://zikra-jyr79p4ka-arkanzahir86-3257s-projects.vercel.app/
Proyek Vercel: zikra-web, terpisah dari zikra-ai. Plan Hobby diverifikasi melalui API; tidak upgrade/menambah billing. Deployment final READY, labelPreview, productionUrl null. Vercel Authentication aktif dengan Standard Protection.

## Verifikasi

- Halaman dapat dibuka dalam sesi pemilik Vercel pada browser. Font lokal dimuat.
- Video remote diputar melaluiSpace:52detik, currentTime20,558, pausedfalse, mutedfalse, readyState4, errornull; kemudian dijeda. Ini tidak membuktikan volume fisik atau penilaian suara dari pendengaran. [Receipt](playback.json), [tampilan](hosted-video.png).
- PermintaanHEAD tanpa sesi pada/,MP4,frameAI danprivacy semuanya302 menujuSSOVercel. Tidak mengembalikan konten media. [Receipt tanpa nonce/token](protection.json).
- Paket25file/5.844.310byte:0APK,0known-secretmatch,0hashmismatch; hanyaHTML/CSS/aset/media. [Receipt](package.json). Tidak mengunggah backend, env, key/signing, data pengguna atau bukti privat.
- CSP, noindex/nofollow, nosniff, no-referrer, frameDENY, private/no-store dan PermissionsPolicy diatur dalam website/vercel.json. Headerorigin tidak diverifikasi terpisah melalui requestbypass; jangan mengklaim audit keamanan lengkap.

## Alias otomatis

Vercel otomatis mengangkat deploy pertama keproduction dan membuat alias publik, walau perintah tidak memakai--prod. Alias zikra-web-roan.vercel.app segera dilepas, lalu diverifikasi404. Deploy final memakai--environment preview eksplisit. URL unik deploy pertama tetap terlindungi. Tidak ada klaim bahwa tak seorang pun mungkin sempat mengakses alias sebelum dilepas; tidak ada data/key/APK dalam paket tersebut. Jangan memakai--prod atau memasang alias publik sebelum keputusan rilis.

## Batas

UnduhanAPK tetap ditutup dan HOLD. Video/frame lokal tidak masukGit; salinan untuk review terlindungi ada diVercel sesuai izin pengguna. Belum menutup hak publikasi suara/promo. Preview memerlukan akunVercel yang berhak; jangan membuat tautan bypass publik. Retensi deploymentHobby mengikuti layanan, bukan jaminan URL permanen.
