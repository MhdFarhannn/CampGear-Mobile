# Audit Jujur CampGear Android — Versi Pertama

Tanggal audit: 22 Oktober 2026. Dokumen ini ditulis **sebelum perbaikan implementasi**. Temuan awal sengaja dipertahankan sebagai riwayat; penyelesaiannya dicatat di bagian tindak lanjut dan `CHANGELOG.md`.

## 1. Cakupan dan hasil pemeriksaan awal

- Dibaca: seluruh sumber Kotlin di `app/src/main/java`, seluruh layout/resource XML, manifest, konfigurasi Gradle/wrapper, keep rules, pengujian, dokumentasi, serta konfigurasi proyek Android Studio. Ikon raster bawaan diperiksa melalui ikon resolusi tertinggi; semua varian memakai aset Android bawaan yang sama.
- Backend dibaca ulang: seluruh controller, model/DTO, service, middleware, `Program.cs`, konfigurasi aplikasi/launch profile, `.http`, `query.sql`, dan dokumentasi kontrak. Folder backend hanya dibaca. `git status --short` backend pada awal audit kosong.
- Direktori hasil build, cache Gradle/Kotlin, binary wrapper, dan metadata sesi IDE bukan implementasi aplikasi; diperiksa sebagai artefak/konfigurasi, bukan diubah atau dianggap bukti fitur berfungsi.
- `JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug`: **BUILD SUCCESSFUL** (24 detik).
- `JAVA_HOME=/opt/android-studio/jbr ./gradlew lint`: **BUILD SUCCESSFUL** (11 detik). Laporan debug: `No issues found.`
- Gradle tetap memberi pemberitahuan API deprecated dari toolchain/plugin. Jadi klaim sebelumnya bahwa seluruh warning sudah hilang terlalu luas.
- `adb devices -l`: tidak ada emulator/perangkat terhubung.
- Pemeriksaan GET tanpa token ke `http://192.168.69.58:3001/api/v1/alat`: **timeout 10 detik**. Respons 401 yang pernah dicatat sebelumnya tidak membuktikan backend masih dapat diakses sekarang.
- Build dan lint **tidak membuktikan** login, transaksi, rotasi, rendering, aksesibilitas, atau ekspor bekerja end-to-end. Versi pertama belum memiliki bukti pengujian UI/integrasi tersebut.

## 2. Kontrak umum

Prefix aktif `/api/v1`, JSON camelCase, JWT `Authorization: Bearer <token>`, role `Admin`, `Petugas`, `Peminjam`. List berpaginasi memakai `items/page/pageSize/total`, halaman mulai 1, ukuran maksimum 100; kategori adalah array. Mutasi mengembalikan 200 kosong. Tanggal bisnis `yyyy-MM-dd`; timestamp dapat memuat offset dan pecahan detik.

Temuan lint bersih tidak meniadakan kelemahan berikut:

- **N01 (broken):** `apiCall()` mengganti body null dengan `Unit as T`. GET 200 kosong dapat lolos sebagai sukses lalu menyebabkan `ClassCastException` saat dipakai ViewModel.
- **N02 (broken):** default properti Kotlin tidak melindungi dari JSON `null` eksplisit yang diisi Gson. `items:null`, elemen list null, `details:null`, atau `user:null` bisa mencapai kode non-null dan crash. Respons `{}` juga bisa diperlakukan sebagai login/data valid.
- **N03 (partial):** error hanya membaca `detail/title`; error validasi `errors` tidak ditampilkan. 401/403 dengan body kosong menjadi pesan generik, bukan penjelasan sesi/hak akses.
- **N04 (partial):** interceptor menempelkan token juga pada login dan menghapus sesi tanpa membandingkan token request. Respons 401 request lama dapat menghapus sesi baru. Tidak ada `callTimeout` keseluruhan.
- **N05 (broken):** `parseApiDateTime` menggunakan pola milidetik `SSS` untuk pecahan timestamp .NET sampai tujuh digit. Ini dapat membaca pecahan sebagai milidetik berlebihan atau fallback yang mengabaikan offset; kedaluwarsa lokal tidak dapat dipercaya.
- **N06 (partial):** sesi tidak divalidasi (token/nama/role/id/expiry), inisialisasi encrypted preferences tidak memiliki pemulihan dari kunci/storage rusak, dan file token dapat ikut backup padahal kunci Keystore tidak ikut dipulihkan.

## 3. Audit setiap endpoint aktif

Semua path di tabel relatif terhadap `/api/v1`. Kolom status menilai **kode versi pertama**, bukan hasil runtime. Semua GET JSON terdampak N01/N02; seluruh error terdampak N03. Header dipasang interceptor pada request terlindungi, dengan kelemahan N04.

| Method / path | Hak akses & kontrak | Panggilan Android versi pertama / penilaian |
|---|---|---|
| POST `/auth/login` | Publik; `noTelepon,password`; `token,expiresAt,user{id,nama,role}` | Path/body benar. **partial**: menerima respons tidak lengkap; parse expiry salah; token lama bisa ikut. |
| GET `/auth/me` | Semua token; `UserResponse` tanpa password | Benar. **partial**: error setelah ada data tidak terlihat, profil tidak diperbarui saat kembali. |
| POST `/auth/logout` | Semua token; kosong, log saja | Benar, menghapus lokal setelah request. **partial**: logout offline menunggu jaringan; pembatalan coroutine dapat melewati clear. |
| GET `/users` | Admin; `page,pageSize,search,idRole`; paged user | Path/parsing/page benar. **partial**: picker hanya 100 pertama, pencarian dapat mengubah query sebelum reload. |
| GET `/users/{id}` | Admin; objek user | Dideklarasikan dengan benar tetapi tidak dipakai layar edit (memakai snapshot list). Tidak wajib dipanggil bila data list cukup. |
| POST `/users` | Admin; `idRole,idJenisKelamin?,nama,password,noTelepon,alamat?` | Body benar. **partial**: hanya cek kosong; batas nama/password/telepon belum dipatuhi, dialog ditutup sebelum hasil. |
| PUT `/users/{id}` | Admin; body sama, password kosong/omit tetap | Benar. **partial**: masalah formulir/rotasi/error sama; perubahan akun sendiri tidak menyegarkan sesi. |
| DELETE `/users/{id}` | Admin; 409 bila riwayat | Benar, ada konfirmasi. **partial**: event sukses bisa hilang ketika STOPPED. |
| GET `/kategori` | Semua token; array `{id,nama}` | Array benar. **partial**: pull refresh langsung berhenti, gagal memuat kategori katalog tidak memiliki retry khusus. |
| GET `/kategori/{id}` | Semua token; satu kategori | **Tidak dideklarasikan/dipakai**; layar CRUD memakai objek list. Bukan fitur layar yang hilang. |
| POST `/kategori` | Admin; `nama` <=100 | Body benar. **partial**: batas panjang tidak diperiksa; dialog/input tidak dipertahankan. |
| PUT `/kategori/{id}` | Admin; `nama` | Benar. **partial**: masalah formulir/error sama. |
| DELETE `/kategori/{id}` | Admin; 409 bila dipakai alat | Benar. **partial**: error refresh setelah list berisi tidak ditampilkan. |
| GET `/alat` | Semua token; `page,pageSize,search,idKategori`; paged alat + stok | Query benar. **partial**: query/dataset pagination tidak atomik; tidak ada deduplikasi; stok keranjang stale. |
| GET `/alat/{id}` | Semua token; alat + `imagePath,stokTersedia` | Dipakai form edit/harga edit loan. **partial**: detail alat Peminjam tidak ada; harga gagal diambil diam-diam fallback lama. |
| POST `/alat` | Admin; multipart `idKategori,nama,deskripsi?,hargaSewa,gambar?` | Multipart/field/file benar. **partial**: nama <=150, harga finite/2 desimal, I/O background, preview dan state belum benar. |
| PUT `/alat/{id}` | Admin; multipart, omit gambar mempertahankan lama | Benar. **partial**: kategori/input tersimpan di Fragment, prefill dapat menimpa edit setelah rotasi. |
| DELETE `/alat/{id}` | Admin; 409 bila unit/riwayat | Benar. **partial**: tidak ada guard busy, dapat mengirim mutasi berulang. |
| GET `/unit-alat` | Semua token; `page,pageSize,idAlat,idStatusKetersediaan` | Paging/idAlat benar. Filter ketersediaan belum dideklarasikan. **partial** untuk pengelolaan/filter. |
| GET `/unit-alat/{id}` | Semua token; satu unit | **Tidak dideklarasikan/dipakai**; CRUD memakai snapshot list. |
| POST `/unit-alat` | Admin; `idAlat,kodeInventaris,idStatusBarang`; awal tersedia | Body benar. **partial**: kode <=50 belum dicek; dialog hilang saat rotasi/gagal. |
| PUT `/unit-alat/{id}` | Admin; `kodeInventaris,idStatusBarang,idStatusKetersediaan`; locked dipinjam | Body benar, lock dibatasi di UI. **partial**: formulir/error/state; pemilihan dipinjam manual perlu mengikuti kontrak dan konsekuensinya dijelaskan. |
| DELETE `/unit-alat/{id}` | Admin; 409 bila riwayat | Path benar, konfirmasi/lock ada. **partial**: feedback/event/background sama. |
| GET `/peminjaman` | Admin/Petugas; `page,pageSize,status,idPeminjam,from,to` | Paging/status/tanggal benar. `idPeminjam` belum dideklarasikan; default layar pengajuan semua status. **partial**. |
| GET `/peminjaman/mine` | Peminjam; `page,pageSize,status` | Benar. **partial**: filter UI/VM dapat tidak sinkron saat view kembali; sukses submit tidak memastikan list lama reload. |
| GET `/peminjaman/monitor` | Admin/Petugas; `page,pageSize,status=jatuh_tempo/terlambat` | Query benar. **partial**: label on-time salah untuk jatuh tempo hari ini; backend juga memasukkan disetujui. |
| GET `/peminjaman/{id}` | Semua token, Peminjam milik sendiri; detail + unit paralel | Parsing field benar. **partial**: load berulang/tumpang tindih; error refresh tidak terlihat; tidak ada timeline. |
| POST `/peminjaman` | Peminjam; `tanggalPeminjaman,tanggalPengembalian,items[{idAlat,jumlah}]` | Body/rumus minimum 1 hari benar untuk input normal. **partial**: max1000/stok/harga baru belum divalidasi ulang; event bisa hilang. |
| PUT `/peminjaman/{id}` | Admin, diajukan; body sama mengganti seluruh items | **partial**: hanya kuantitas/tanggal; add/remove item didukung backend tetapi belum ada di UI; reload menimpa draft. |
| DELETE `/peminjaman/{id}` | Admin, diajukan/ditolak | Benar dengan role/status/konfirmasi. **partial**: event hapus bisa hilang saat STOPPED. |
| PATCH `/peminjaman/{id}/approve` | Admin/Petugas, diajukan; kosong | Benar. **partial**: load/mutasi bisa overlap, busy tidak memblokir semua aksi. |
| PATCH `/peminjaman/{id}/reject` | Admin/Petugas, diajukan; `catatan?` | Benar. **partial**: dialog tidak rotation-safe, alasan hanya log backend. |
| PATCH `/peminjaman/{id}/borrow` | Admin/Petugas, disetujui; kosong, pilih unit server | Benar. **partial**: kegagalan stok 409 belum diberi penyajian baik; feedback raw bahasa Inggris. |
| POST `/pengembalian` | Semua role, Peminjam milik sendiri; `idPeminjaman,tanggalKembali,catatan?,units[{idUnitAlat,statusKondisi,catatan?}]` | Body/id unit/nilai kondisi benar. **broken** pada rotasi: load ulang mengembalikan kondisi baik. Hanya memeriksa units tidak kosong, bukan jumlah penuh/status/duplikasi; hasil denda server tidak ditampilkan. |
| GET `/pengembalian` | Admin/Petugas; `page,pageSize,from,to`; paged detail | Query benar. **partial**: draft tanggal langsung dipakai loadMore tanpa Terapkan; nomor hasil POST tidak dicari untuk menampilkan denda final. |
| GET `/pengembalian/{id}` | Semua token, Peminjam milik sendiri | Benar. **partial**: hanya total/per-unit, tidak memisahkan denda terlambat dari total server. |
| GET `/log-aktivitas` | Admin; `page,pageSize,idUser,from,to` | Field/path/page benar. **partial**: picker 100 pertama/gagal diam-diam, draft filter mengubah loadMore, waktu bukan relatif dan offset diabaikan. |
| GET `/laporan/peminjaman` | Admin/Petugas; `from,to,format=json/csv`; `totalBiaya,items` | JSON/CSV benar. **partial**: filter dapat berubah di tengah request; hasil/file salah periode/jenis, stream dapat bocor. |
| GET `/laporan/pengembalian` | Admin/Petugas; kontrak sama; `totalDenda,items[].dendaKondisi` | JSON/CSV benar. **partial**: masalah konkurensi sama; ringkasan nol tertutup empty overlay; breakdown kondisi belum ditampilkan. |

### Controller yang ada tetapi bukan endpoint aktif

`StatusBarangControllers.cs` mendefinisikan GET/POST `/status-barang`, PATCH/DELETE `/status-barang/{id}`, tetapi **`Program.cs` tidak memanggil `MapStatusBarang` dan service tidak diregistrasikan**. Aplikasi benar tidak memanggilnya. Ini tidak dapat dipakai sebagai lookup. Model `StatusLookup` juga tidak membuktikan adanya endpoint lookup.

## 4. Status layar wajib sebelum perbaikan

`complete` berarti implementasi memenuhi kontrak normal secara statis; bukan klaim sudah diuji runtime. Karena kelemahan lintas layar di atas, mayoritas masih `partial`.

| Role | Layar | Status awal | Kekurangan |
|---|---|---|---|
| Semua | Login | partial | Validasi respons/sesi, feedback field, branding belum matang. |
| Semua | Profil + logout | partial | Refresh/error, backup sesi, logout offline. |
| Semua | Loading/empty/error | broken | Tombol retry berada dalam container error yang GONE saat empty; empty action tidak terlihat. |
| Peminjam | Beranda/dashboard | missing | Tidak ada ringkasan peminjaman aktif. |
| Peminjam | Bottom navigation | missing | Memakai drawer walau hanya sedikit tab. |
| Peminjam | Katalog/search/kategori | partial | Kategori dropdown, detail tidak ada, data stale setelah transaksi. |
| Peminjam | Detail alat | missing | Tap kartu langsung menambah item. |
| Peminjam | Keranjang/form peminjaman | partial | Tidak ada sticky total, validasi ulang stok/harga, hasil/rotation event. |
| Peminjam | Peminjaman saya | partial | Paging/filter/restorasi/refresh hasil. |
| Semua sesuai role | Detail peminjaman | partial | Timeline hilang, refresh/action race. |
| Semua sesuai role | Form pengembalian | broken | Kondisi unit hilang saat rotasi, hasil denda final tidak dibaca. |
| Petugas | Dashboard | missing | Tidak ada hitungan pengajuan/dipinjam/overdue dari API. |
| Petugas | Pengajuan/approve/reject/handover | partial | Default semua status, state dan feedback. |
| Petugas | Monitor | partial | Jatuh tempo hari ini dianggap belum jatuh tempo, filter panjang bisa terpotong. |
| Petugas/Admin | Riwayat + detail pengembalian | partial | Draft filter/paging bercampur, breakdown belum lengkap. |
| Petugas/Admin | Laporan + CSV | partial | Race periode/jenis, stream/event/lifecycle, rentang bukan Material range picker. |
| Admin | Dashboard | missing | Tidak ada ringkasan master/transaksi. |
| Admin | Daftar/detail/edit/hapus peminjaman | partial | Edit add/remove item belum tersedia, rotation state. |
| Admin | Kelola pengembalian | partial — batas backend | Proses/lihat ada; PUT/DELETE memang tidak tersedia dari API. |
| Admin | CRUD alat/form/gambar | partial | State form/prefill, I/O main thread, preview, busy. |
| Admin | CRUD unit | partial | Form tidak rotation-safe, inline server errors hilang. |
| Admin | CRUD kategori | partial | Tanpa search/FAB; refresh/error/form. |
| Admin | CRUD pengguna | partial | Validasi panjang, stale data, dialog/form. |
| Admin | Log aktivitas | partial | Filter/user picker/error/relative time. |
| Semua | Splash dan ikon camping | missing | Masih adaptive icon Android bawaan, tidak ada SplashScreen API. |
| Semua | Dark theme | missing | Theme.Material3.Light, status bar selalu light. |

## 5. Bug dan kelemahan logika/lifecycle

| ID | Lokasi | Temuan dan dampak |
|---|---|---|
| F01 | `PagedViewModel.request` | Append `old + new` tanpa distinct id; offset pagination pada dataset berubah dapat menggandakan item. |
| F02 | VM search/filter, Returns/Log | Query draft berubah sebelum reload; loadMore/refresh bisa menggabungkan halaman dari filter berbeda. |
| F03 | VM aksi `MutableSharedFlow(extraBufferCapacity=1)` | Tidak ada replay/subscriber saat STOPPED; buffer extra bukan antrean ketika tidak ada subscriber. Sukses/navigasi bisa hilang. |
| F04 | Return/EditLoan/AlatForm `load()` dipanggil di setiap onViewCreated | Rotation/view recreation memuat ulang dan menimpa draft; requests lama tidak dibatalkan. |
| F05 | `AlatFormFragment` | `prefilled`, kategori dipilih, nama/deskripsi/harga bukan state VM. Prefill setelah request menimpa state yang dipulihkan Android; view dibuat ulang pada Fragment yang sama juga bisa kehilangan prefill. |
| F06 | Dialog CRUD/reject | Builder dialog biasa tidak dikelola FragmentManager, tidak punya draft state VM. Rotasi menutup/hilangkan input. Dialog ditutup sebelum request; server gagal mengharuskan isi ulang. |
| F07 | LoanDetail/Profile/Kategori/ReturnDetail | Kegagalan load setelah data ada tidak ditampilkan; user melihat data lama sebagai sukses. |
| F08 | `ReportViewModel`/`ReportFragment` | Request tidak dikaitkan dengan snapshot periode/jenis; hasil lama bisa berada di filter baru. `ResponseBody` dikirim event, dapat hilang/bocor; share coroutine memakai lifecycle Fragment dan `requireContext()` setelah detach. |
| F09 | `BaseFragment`, semua RecyclerView | Binding dasar dinullkan, **tidak ada kebocoran langsung field binding itu**. Namun adapter tidak dilepas dari RecyclerView saat onDestroyView, adapter yang ditahan Fragment dapat menahan view/observer lama. Scope Fragment dipakai untuk pekerjaan milik view. |
| F10 | `ReturnUnitAdapter` | DiffUtil sengaja mengabaikan catatan, watcher/setText bisa mengganggu cursor ketika kondisi berubah. Busy tidak memblokir input unit. |
| F11 | `CartViewModel` | Batas hanya stok lama, tidak max1000; harga tidak diperbarui untuk item yang sudah ada; checkout tidak revalidate alat/stok terbaru. |
| F12 | `Formatters` | Singleton SimpleDateFormat/NumberFormat tidak thread-safe. Parsing tanggal lenient mengizinkan tanggal tidak valid; perhitungan milidetik rentan DST. |
| F13 | `MainActivity` | Menyembunyikan menu bukan guard destination. Role tidak dikenal jatuh ke katalog; pengguna yang logout lalu login lain berisiko menyisakan state VM Activity selain cart. |
| F14 | `StateLayout.bindRetry` | Retry dalam stateError yang GONE ketika empty; empty tidak memiliki tombol aksi walaupun callback diberikan. |
| F15 | `KategoriFragment` | Spinner refresh disetop segera, error refresh tidak terlihat kalau list sudah ada. |
| F16 | `ImageLoader`/upload | Coroutine global menahan ImageView, download tak dibatalkan saat recycling; membaca seluruh bytes dan sampling hanya lebar. Upload/query URI/penyalinan file di main thread, tanpa batas pembacaan sebelum copy. |
| F17 | `EditLoanViewModel` | Gagal fetch harga terkini fallback harga transaksi tanpa memberitahu, membuat estimasi tidak sesuai biaya PUT server. |

## 6. Audit UI/UX dan kualitas proyek

- U01: tidak ada desain DayNight, dimensi token, skala typography milik aplikasi, atau layout lebar/landscape. Ukuran/spacing 2/4/6/10/12/14/18/20dp tersebar, radius berbeda; belum ada 8dp grid kohesif.
- U02: `StatusUi` mewarnai disetujui hijau dan dipinjam biru; tidak sesuai pemetaan warna yang diminta. Warna status tidak memiliki varian malam.
- U03: ikon menu tidak ada, search memakai drawable sistem lama, empty state hanya tanda dash; ikon peluncur masih robot Android bawaan.
- U04: kartu katalog sangat tinggi dengan placeholder tent yang dicrop, kategori/name horizontal mudah sempit, tidak ada detail image/description/CTA.
- U05: formulir panjang meletakkan aksi di dasar scroll; total pengajuan tidak sticky. Semua CRUD memakai button tambah besar dan tombol edit/delete berulang pada setiap row; tanpa overflow/FAB.
- U06: login punya dasar branding/Material/password toggle, tetapi hierarki/padding dan progress terpisah belum rapi; label masih campuran `Users`, `Role`, `Password`.
- U07: detail loan hanya kartu teks, tidak ada status timeline. Return hanya estimasi, bukan breakdown final backend.
- U08: filter report/log/returns memakan tinggi viewport besar sehingga list sempit di landscape; monitor ChipGroup singleLine tidak dibungkus horizontal scroll.
- U09: stepper edit 44dp (<48dp); text fixed-width dan row horizontal berpotensi clipping pada font besar. Content description gambar generik bukan nama alat.
- Q01: README/API inventory ada, tetapi **AUDIT, CHANGELOG, dokumentasi modul Android dan tabel uji UI belum ada**. Catatan lama menyatakan kerja selesai walau belum ada runtime QA.
- Q02: `ExampleUnitTest` hanya 2+2; `ExampleInstrumentedTest` hanya packageName. Ini bukan bukti alur UKK. Test format tidak memeriksa tanggal invalid, nanosecond expiry, timezone atau concurrency.
- Q03: beberapa fungsi clearError/getById tidak digunakan; template keepRules/backup berisi contoh/TODO. Resource tidak terpakai telah dihapus, tetapi kode/template yang lemah masih ada.
- Q04: deprecation EncryptedSharedPreferences disembunyikan dengan suppress. Library tetap dapat dipakai kompatibel, tetapi perlu penanganan backup/key corruption dan dokumentasi; suppress bukan migrasi.
- Q05: cleartext telah dibatasi host yang diberikan; benar untuk LAN saat ini. Base URL hard-coded build membuat pengujian emulator perlu konfigurasi build yang jelas.

## 7. Temuan backend yang tidak boleh ditutupi

1. Tidak ada PUT/DELETE pengembalian, Admin POST peminjaman, lookup aktif, return/mine, maupun PDF.
2. POST pengembalian hanya 200 kosong, tanpa id/result; Peminjam tidak punya cara sah menemukan detail denda final setelah POST.
3. Service pengembalian tidak memastikan seluruh unit disertakan; klien harus memvalidasi sendiri.
4. POST peminjaman **tidak mengecek stok dalam service aktif**, walaupun docs backend mengatakan mengecek saat pengajuan. Gate otoritatif stok baru saat handover. Klien bisa revalidate untuk feedback, bukan menjamin reservasi.
5. Monitor menggabungkan `disetujui` dan `dipinjam`; total monitor bukan jumlah sedang dipinjam. Dashboard harus memakai endpoint status dipinjam untuk angka itu.
6. CSV tidak meng-escape titik koma/newline/quote pada nama/text; tidak boleh diklaim selalu valid spreadsheet.
7. Controller mengembalikan exception.Message untuk 500, sedangkan middleware menjanjikan generic error. Body 401/403 dari authorization bisa kosong.
8. Penulisan log dilakukan setelah commit; kegagalan log bisa menghasilkan error walaupun transaksi sudah tersimpan. Klien tidak boleh auto-retry mutasi.

Rincian serta penanganan akan diperbarui di `BACKEND_ISSUES.md`; kontrak tidak akan diubah.

## 8. Rencana penyelesaian

Fase 2 memperbaiki N01–N06/F01–F17, melengkapi detail/beranda dan form yang didukung kontrak. Fase 3 menerapkan U01–U09 ke setiap layar dengan token, ikon, SplashScreen, Material date picker, bottom navigation/drawer, kartu, sticky CTA, serta state yang dirancang. Fase 4 menambahkan uji regresi bermakna, tabel hasil jujur, dokumentasi Input/Process/Output, screenshot checklist dan langkah menjalankan. Kekurangan endpoint backend tetap ditandai sebagai batas backend, bukan dianggap fitur selesai.

## 9. Tindak lanjut

Bagian ini diisi setelah fase terkait selesai dan diverifikasi. Audit awal di atas tidak dihapus atau diubah menjadi klaim pengujian runtime.

- Pengujian emulator menemukan crash nyata sesudah login: `navigate(Int) { ... }` pada Navigation 2.10 dipilih sebagai overload route generik `Int`, bukan ID XML. Pemanggilan tersebut juga ada di versi pertama pada routing/submit. Semua navigasi XML dengan options diubah menjadi overload eksplisit `navigate(id, args, NavOptions)`; pengujian ulang dilakukan sebelum dinyatakan berhasil.
