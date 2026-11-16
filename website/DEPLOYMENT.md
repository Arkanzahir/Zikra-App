# Deploy website Zikra

## Keadaan16 November 2026

Preview: https://zikra-jyr79p4ka-arkanzahir86-3257s-projects.vercel.app/
Dashboard: https://vercel.com/arkanzahir86-3257s-projects/zikra-web
Project ID prj_xGKxCejvUBZONXArKLjSUMT7cDEX; scope arkanzahir86-3257s-projects. Metadata ini bukan credential. Backend zikra-ai terpisah dan tidak disentuh.

Plan Hobby, deployment finalREADY/Preview, Vercel Authentication Standard Protection. Memerlukan pemilik/team yang berhak; bukan tautan publik untuk calonpengguna. Video52detik bersuara dapat diputar sesudah tindakanuser. APK tidak disertakan dan tombolunduh ditutup. [Bukti lengkap](validation/2026-11-16-hosting/README.md).

## Menyiapkan perubahan berikutnya

1. Baca docs/beta/CURRENT_STATUS.md. Jangan buka unduhan atau melepasprotection karena website sudahbagus.
2. Media lokal: `D:/node.exe website/prepare-preview.cjs`; sumber/hash pada script. Foldermedia diabaikanGit, tidak mengandungAPK/key. Pada clone tanpa media gunakan check --source-only, bukan klaim video berjalan.
3. Periksa plan dan **protection terkini melalui Vercel**, lalu refresh metadata lokal .local/website-deploy-20261116/project-response.json. package-preview hanya memeriksa snapshot ini, bukan jaminan settingremote belum berubah.
4. `D:/node.exe website/package-preview.cjs` memeriksa file/ukuran dan membuat stage baru pada.local. Uploadreceipt menyebut lokasi tepat; jangan deploy root repo/backend atau memasukkan.localkehosting.
5. CLI65.2.0 yang dipakai tersimpan lokal pada npm-cache. Deploy stage dengan `vercel deploy --environment preview --yes --scope arkanzahir86-3257s-projects --cwd <stage>`. Jangan gunakan --prod. Jangan membuat bypasssecret/linkpublik. AuthCLI memakai sesi pemilik, tidak lewatkey chat.
6. Verifikasi READY/protection, requestanonim302keSSO padahalaman/MP4/privasi, lalu bukapreviewdalam sesi pemilik dan ujivideo. Simpan receipt tanpa cookie/token/nonce.

## Pengamanan dan batas

vercel.json mengatur noindex/noarchive, CSP self-only/no scripts, nosniff, no-referrer, frameDENY, private/no-store dan menonaktifkan kamera/mikrofon/lokasi web. WebtidakmemintaizinAPK. Ini bukan auditkeamanan penuh seluruhVercel.

Deploy pertama proyek otomatis ditempatkanVercel pada production meski tidak--prod; alias publik yang muncul langsungdilepas. Alias zikra-web-roan.vercel.app sekarang404. Deployment kedua/final adalahPreview dan productionUrl null. Jangan mengulangi asumsi default pada proyekbaru. URLuniquedeploylama tetapdilindungi. Tidak adaAPK/data/key dalam paket yang sempatpunyaalias.

Publikasiweb/APK berikutnya memerlukankeputusan baru sesuaiHOLD dan hakpromo/privasi/paketrilis. Tautanunduh harus menunjuk APK final, signer/ukuran/SHA256cocok, <100MB, serta diujiupdate tanpa menghapusdatapengguna. Jangan memakai debug58 emulator atau signed57 yang belum disahkan sebagai releasepublik.
