# Dokumentasi Modul Android — Input, Process, Output

Kode sederhana mengikuti pola: Fragment memegang ViewBinding dan interaksi; ViewModel memegang state/pekerjaan; Repository memanggil API; interceptor menempelkan JWT. Backend tidak diubah.

| Modul / kelas utama | Input | Process | Output |
|---|---|---|---|
| Auth (`LoginFragment/ViewModel`, `AuthRepository`) | Nomor telepon dan kata sandi | Validasi kosong, POST login, validasi kontrak response/role/expiry, simpan sesi terenkripsi | Inline error/loading atau sesi aktif menuju Beranda |
| Session/routing (`SessionManager`, `MainActivity`) | Token, user id/nama/role, expiresAt; HTTP 401 | Parse pecahan .NET/offset, expiry timer, compare token request saat clear, guard destination, hapus saved back stack saat keluar | Login ketika sesi selesai; bottom nav Peminjam/drawer staff/admin; 403 tidak logout |
| Networking (`ApiService`, `ApiClient`, `ApiResult`, `ResponseContracts`) | Method/path/query/body, JWT | Timeout 20/30/45s, parse JSON typed dengan required fields/list validation, parse problem/detail/errors, cancellation diteruskan | `ApiResult.Success` atau `Failure` Indonesia; GET kosong tidak menjadi Unit palsu |
| Dashboard (`DashboardRepository/ViewModel/Fragment`) | Role pengguna | GET list pageSize=1 dan ambil `total`; requests bersamaan tanpa menginventasikan angka | Empat summary cards dan shortcut role; error/retry bila request gagal |
| Katalog/detail (`Catalog*`, `EquipmentDetail*`, `AlatRepository`) | Search nama/kategori/id alat | Debounce search server-side, category chips, paged list, GET detail, Coil resolve origin image | Kartu image/category/price/stok; detail besar dan CTA pinjam |
| Cart/borrow (`CartViewModel`, `BorrowForm*`) | Alat/jumlah/date range | Maksimum stok/1000, revalidate harga/stok saat checkout, material picker, hari minimum1; estimasi `sum(harga × jumlah) × hari`; POST snapshot items | Sticky total, inline tanggal error, disabled submit, sukses ke mine dan refresh |
| Mine (`MyLoans*`) | Status/halaman | GET `/peminjaman/mine`, filter aktif, pull refresh, distinct id saat merge | Hanya loan sendiri, status card, detail; 403 detail milik lain ditangani backend |
| Loan/action (`LoanDetail*`) | Loan id, role/status, confirm action, catatan reject | GET detail; tampilkan timeline tanpa timestamp fiktif; PATCH approve/reject/borrow atau Admin DELETE; guard busy; refresh | Status/data terbaru, feedback aksi, unit inventaris setelah handover |
| Edit loan Admin (`EditLoan*`, `LoanItemPickerFragment`) | Loan diajukan, items baru, jumlah/tanggal | GET current price tiap alat, tidak fallback diam-diam; tambah/hapus alat; PUT seluruh items dengan tanggal valid | Estimasi terkini, sticky save, draft rotasi, refresh detail/list |
| Return (`ReturnForm*`, `ReturnUnitAdapter`) | Loan dipinjam, tanggal aktual, seluruh unit dan kondisi/catatan | Pastikan semua unit/unik, validasi tanggal, hitung estimasi late + condition, confirm POST; catatan update payload tidak mereset cursor | Sukses/status selesai; untuk staff/admin cari receipt by loan/date lalu detail; Peminjam dibatasi kontrak kosong |
| Riwayat/detail return (`Returns*`, `ReturnDetail*`) | Filter tanggal aktif, return id | Paging GET list; GET detail; kondisi fine dijumlah dari respons; late fine = `denda server - sum(dendaKondisi server)` | List/receipt final dengan hari terlambat, late/condition/total dan unit |
| Master pengguna/kategori/unit (`MasterForm*`, list VMs) | Jenis form, id, draft field | GET detail terbaru; SavedStateHandle draft non-password, validasi panjang/format/role/kode, create/update Repository; duplicate error inline | Form tetap terbuka pada gagal, busy disabled, sukses result refresh list; delete confirm dan 409 friendly |
| Alat/multipart (`AlatForm*`, `AlatRepository`) | Nama<=150, kategori, harga>=0 dua desimal, description, file opsional<=2MB | Baca URI/copy terbatas di IO; preview Coil; multipart field persis kontrak; omit gambar pada edit mempertahankan lama | Loading/inline errors, foto/file status, save feedback dan refresh list |
| Unit list (`UnitAlat*`) | Alat id, search kode loaded pages | GET paged unit idAlat; search client-side pada halaman terambil; dipinjam locked, menu edit/delete | Status kondisi/ketersediaan, FAB; manual availability hanya tersedia/tidak tersedia |
| Kategori list (`Kategori*`) | Search nama lokal | GET array lengkap; filter lokal, spinner sampai request selesai, CRUD melalui form | List kategori/FAB/overflow, konfirmasi delete, refresh result |
| Users list (`Users*`) | Search nama/telepon server | Debounce dan paging; edit form; delete guard busy; akun sendiri berubah meminta relogin | Akun/role/phone cards, inline form/feedback, hak akses sesuai sesi baru |
| Log (`LogAktivitas*`, `LogRepository`) | Pengguna/tanggal draft, Terapkan | Fetch seluruh users untuk picker, paging log berdasarkan filter applied; timestamp parse/localize/relative | List kegiatan/nama/waktu relatif; accessibility absolute time, error picker/retry |
| Laporan (`Report*`, `LaporanRepository`) | Jenis, date range, export | Material range picker; cancel request lama/generation; JSON total/items; CSV stream ditulis di IO VM lalu ditutup, FileProvider read grant | Summary nol tetap terlihat, laporan row/status/breakdown, Sharesheet CSV |
| Profil (`Profile*`) | Sesi aktif | GET auth/me; format field kosong; refresh saat kembali; best effort logout 5s/finally clear token | Profil/name/role dan data user, confirm keluar, login ketika token habis |
| UI bersama (`BaseFragment`, `StateLayout`, date/confirm helpers) | UiState dan callback pengguna | Bind lifecycle view, detach RecyclerView adapter, Material transition, designed load/empty/error/action; picker/dialog restore listener | View tidak memegang binding lama; touch/icon/typography konsisten |

## Rumus yang dapat dijelaskan saat ujian

1. Lama sewa = maksimum 1 dan selisih tanggal kembali rencana − tanggal pinjam.
2. Estimasi total = penjumlahan harga per hari × kuantitas × lama sewa; server menyimpan harga saat transaksi dan menghitung ulang total.
3. Estimasi keterlambatan = maksimum 0 dan tanggal aktual − tanggal rencana.
4. Estimasi denda = lateDays × Rp10.000 **sekali per loan** + denda tiap unit (baik0/ringan25.000/berat100.000/hilang250.000).
5. Receipt final membaca total/per-unit dari server; breakdown terlambat adalah selisih total server dengan jumlah kondisi server.
6. Pagination merge berdasarkan id agar offset yang bergeser tidak menghasilkan duplikat, dan `hasMore` memakai metadata halaman server.

## Batas state

ViewModel mempertahankan state saat rotasi; validasi inline dan coroutine tidak dibuat ulang dari nol. Master form juga menyimpan draft non-password ke SavedStateHandle. Tidak ada klaim seluruh draft/image/cart persisten setelah proses dibunuh sistem; data server dapat dimuat ulang, tetapi draft volatile perlu diisi kembali. Sesi JWT bertahan di encrypted preferences dan dikecualikan dari backup karena kunci Keystore bersifat perangkat-lokal.
