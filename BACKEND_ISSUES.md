# Batas dan Temuan Backend CampGear

Backend `/home/hanverseee/Projects/CampGear-Backend` tetap **READ-ONLY**. Controller/service/SQL di bawah adalah sumber kontrak. Sebagian dokumentasi backend berbeda dengan implementasi aktif; aplikasi mengikuti kode aktif. Tidak ada secret atau connection string yang disalin ke aplikasi.

## 1. CRUD Admin tidak seluruhnya tersedia

- **Sumber:** `Controllers/PeminjamanController.cs`, `Controllers/PengembalianController.cs`.
- POST peminjaman hanya Peminjam. Admin boleh melihat, PUT saat `diajukan`, DELETE saat `diajukan`/`ditolak`, approve/reject/handover, dan memproses pengembalian. Pengembalian tidak memiliki PUT/DELETE.
- **Penanganan Android:** hanya aksi yang didukung ditampilkan; edit peminjaman kini mendukung tambah/hapus alat, jumlah, dan tanggal. Riwayat pengembalian tidak menawarkan edit/hapus.
- **Usulan:** bila benar-benar diwajibkan ujian, backend perlu kontrak pembuatan peminjaman Admin serta koreksi/pembatalan pengembalian yang konsisten dengan inventaris dan denda. Klien tidak menyimulasikan endpoint tersebut.

## 2. Hasil final pengembalian Peminjam tidak bisa ditemukan

- **Sumber:** POST `/pengembalian` mengembalikan 200 kosong; GET list hanya Admin/Petugas. GET detail menerima id pengembalian, sedangkan detail loan tidak mengembalikan id itu. Tidak ada `/pengembalian/mine` atau lookup by loan.
- **Penanganan Android:** seluruh role melihat estimasi yang diberi label jelas sebelum submit. Admin/Petugas mencari hasil melalui list berpaginasi pada tanggal kembali, lalu membaca detail denda server. Peminjam mendapat feedback sukses dan status loan `selesai`; aplikasi **tidak mengklaim** estimasi sebagai denda final.
- **Usulan:** POST mengembalikan `PengembalianResponse`/id/Location, atau tambahkan endpoint milik Peminjam/lookup yang memeriksa kepemilikan.

## 3. Service pengembalian tidak mewajibkan semua unit

- **Sumber:** `Services/PengembalianService.cs:153–168` memeriksa duplicate dan unit asing, tetapi tidak membandingkan jumlah id dengan seluruh unit loan. Loan tetap menjadi `selesai` setelah subset dikirim.
- **Penanganan Android:** form memeriksa status `dipinjam`, jumlah unit terhadap jumlah detail, keunikan id, tanggal, dan selalu mengirim seluruh unit.
- **Usulan:** server membandingkan himpunan id sebelum transaksi; validasi klien saja tidak cukup untuk klien lain.

## 4. Stok tidak diperiksa pada POST peminjaman aktif

- **Sumber:** `Services/PeminjamanService.CreatePeminjaman`, bukan stored procedure contoh `sp_ajukan_peminjaman`.
- Service aktif menyimpan harga/jumlah dan biaya tanpa memeriksa stok. Pernyataan docs bahwa stok diperiksa saat pengajuan tidak sesuai kode. Handover merupakan gate stok otoritatif dengan lock.
- **Penanganan Android:** kuantitas dibatasi stok dan maksimum 1000; keranjang memuat ulang alat/harga/stok ketika checkout dibuka. Perubahan ditampilkan kepada pengguna. Tidak ada klaim bahwa pengajuan/respons approve berarti unit telah dipesan.
- **Usulan:** server memvalidasi stok saat pengajuan sebagai feedback, dan tetap memvalidasi/lock saat handover.

## 5. Tidak ada lookup aktif dan konfigurasi denda API

- **Sumber:** `Program.cs`, `Models/StatusLookup.cs`, `Controllers/StatusBarangControllers.cs`, `appsettings.json`, `query.sql`.
- Controller status-barang ada dalam sumber tetapi tidak dipetakan dan service tidak diregistrasikan. Tidak ada lookup role/gender/kondisi/ketersediaan maupun endpoint tarif denda.
- **Penanganan Android:** `MasterOptions` bersumber dari seed (role 1–3, kondisi 1–4, dst.). Tarif estimasi Rp10.000/hari; rusak ringan Rp25.000, rusak berat Rp100.000, hilang Rp250.000. Detail hasil pengembalian membaca angka server, bukan menghitung tarif ulang.
- **Usulan:** lookup terproteksi dan endpoint tarif/version konfigurasi. Bila seed/id/tarif berubah, konstanta estimasi klien harus diperbarui.

## 6. Catatan penolakan hanya tersedia di log

- **Sumber:** `Services/PeminjamanService.RejectPeminjaman`, `Models/RejectRequest.cs`.
- `catatan` ditulis di log, tidak menjadi field respons loan yang dapat dibaca Peminjam.
- **Penanganan Android:** dialog memberi penjelasan ini, mengirim catatan opsional, dan menampilkan status Ditolak tanpa alasan fiktif.
- **Usulan:** simpan alasan di peminjaman dan sertakan pada respons detail dengan hak akses yang sesuai.

## 7. Monitor tidak sama dengan jumlah sedang dipinjam

- **Sumber:** `GetMonitorPeminjaman` memuat `disetujui` **dan** `dipinjam`, termasuk filter jatuh tempo/terlambat.
- **Penanganan Android:** angka dashboard “Sedang dipinjam” berasal dari GET peminjaman `status=dipinjam`; angka monitor diberi label “Monitor ...” dan keterangan cakupannya. Tidak menganggap total monitor sebagai borrowed count.

## 8. Laporan hanya JSON/CSV; CSV belum di-escape

- **Sumber:** `Controllers/LaporanController.cs`, `Services/LaporanService.ToCsv`.
- Tidak ada PDF/HTML siap cetak. CSV menggunakan `;` dan `ToString()` tanpa escaping quote/newline/semicolon atau format invariant khusus. Nama/text dengan karakter tersebut dapat merusak kolom spreadsheet.
- **Penanganan Android:** tampilkan laporan, total dan jumlah baris; unduh CSV yang memang disediakan, lalu share dengan FileProvider. Tidak menambahkan generator PDF dan tidak mengklaim semua isi CSV valid pada semua spreadsheet/locale.
- **Usulan:** escaping CSV benar dan format angka/tanggal invariant; output printable bila diperlukan.

## 9. Format error belum konsisten

- **Sumber:** controller memanggil `Problem.Fault` yang meneruskan `exception.Message` untuk 500; middleware memberi generic error. 401/403 dari authorization dapat memiliki body kosong.
- **Penanganan Android:** parse `detail` dan `errors`, sediakan fallback Indonesia berdasarkan HTTP status; 401 clear token/ke login, 403 mempertahankan sesi. Pesan 500 disajikan generik. Pesan detail yang belum dikenal dapat tetap berasal dari bahasa backend.
- **Usulan:** handler error/validasi/authorization konsisten, tidak mengekspos exception internal.

## 10. Log ditulis sesudah transaksi commit

- **Sumber:** create/update/handover/return/delete services.
- Kegagalan penulisan log setelah commit dapat mengembalikan error walaupun data bisnis sudah tersimpan. Tidak ada idempotency key mutasi.
- **Penanganan Android:** tidak auto-retry POST/PUT/PATCH/DELETE; submit dibatasi busy, konfirmasi dan refresh disediakan. Bila timeout/error setelah submit, periksa list/status terbaru sebelum mengulangi.
- **Usulan:** penulisan log dalam transaksi yang sama atau outbox, serta idempotency bila diperlukan.

## 11. Logout tidak membatalkan JWT di server

- **Sumber:** `AuthController`/`AuthService.Logout` hanya menulis log, JWT stateless.
- **Penanganan Android:** best-effort logout dengan batas 5 detik, token lokal tetap dihapus pada gagal/cancel, back stack dibersihkan. Timer expiry dan interceptor 401 mengakhiri sesi. Tidak ada refresh token.
- **Usulan:** revocation/refresh token hanya bila spesifikasi backend membutuhkannya.

## 12. URL deployment dan file gambar bergantung lingkungan

- Launch profile sumber: localhost:5162/HTTPS:7185; URL dari pengguna: `http://192.168.69.58:3001/api/v1/`.
- URL LAN timeout dari mesin audit pada pemeriksaan terakhir; ini adalah kondisi konektivitas/deployment, bukan bukti bug endpoint. Pengujian berhasil dengan backend sumber yang dibangun ke `/tmp` dan dijalankan localhost:5162/5163.
- Gambar `/uploads/alat/...` berada di disk; seed/backup database tidak membawa file gambarnya.
- **Penanganan Android:** base URL configurable `-PapiBaseUrl=...`, cleartext hanya host pengembangan yang terdaftar; Coil menampilkan placeholder ketika gambar kosong/404/gagal. Gambar upload belum dianggap terbukti UI end-to-end hanya dari placeholder.

## Temuan yang bukan bug klien

- Peminjam tidak dapat membatalkan pengajuan: DELETE loan memang Admin-only.
- Unit dengan riwayat/category yang dipakai tidak dapat dihapus: 409 adalah aturan bisnis, bukan alasan melakukan retry otomatis.
- Admin form tidak menyediakan status dipinjam manual; status itu ditetapkan alur handover agar tidak menciptakan inventaris “dipinjam” tanpa loan.
- Nilai denda final dan harga transaksi tetap otoritas server.

Git backend diperiksa sebelum dan sesudah pekerjaan: tidak ada perubahan sumber yang dibuat agen.
