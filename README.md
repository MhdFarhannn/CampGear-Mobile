# CampGear Android — UKK RPL 2026

Klien rental perlengkapan camping untuk role **Admin, Petugas, dan Peminjam**. Kotlin, XML + ViewBinding (tanpa Compose), MVVM/Repository, Retrofit/OkHttp/Coroutines, Navigation Component, serta dependency injection manual.

## Hasil revisi

- Material 3 **DayNight**, palette forest/sand, token warna/spacing/typography, ilustrasi vector camping, adaptive icon, SplashScreen.
- Peminjam: bottom navigation Beranda/Jelajah/Pinjaman/Profil. Admin/Petugas: drawer berikon, nama/role, konfirmasi keluar.
- Dashboard mengambil `total` dari endpoint yang tersedia; tidak menampilkan angka contoh.
- Katalog pencarian/kategori server-side, foto/placeholder, detail alat dan keranjang, estimasi biaya/sticky submit.
- Pemrosesan loan sesuai status/role: approve, reject, handover, edit item/tanggal, delete; detail dengan alur status.
- Pengembalian seluruh unit, kondisi/catatan, estimasi yang diberi label, serta breakdown final server pada detail pengembalian.
- CRUD pengguna/kategori/alat/unit dengan draft form ViewModel, inline validation, busy guard, FAB dan overflow.
- Monitor, riwayat/filter tanggal, activity log/filter pengguna/relative time, laporan date-range/JSON/CSV share.
- Validasi respons Gson sebelum mencapai model Kotlin, GET kosong menjadi error, parsing expiry .NET tujuh digit, pagination deduplikasi, retry read-only, 401/403 jelas, sesi tidak ikut backup.

## Persyaratan

- Android Studio dan SDK 37; perangkat/emulator minimal API 24.
- Gradle wrapper 9.8.2, AGP 9.4.1 (Kotlin bawaan AGP; **jangan menambahkan plugin Kotlin Android**).
- Gunakan JBR Android Studio. Mesin audit memakai `/opt/android-studio/jbr`.
- Backend dengan database/seed tersedia. Folder sumber backend adalah read-only bagi pengerjaan Android.

## Konfigurasi base URL

Default proyek tetap URL yang diberikan:

```
http://192.168.69.58:3001/api/v1/
```

Konfigurasi `BuildConfig.API_BASE_URL` di `app/build.gradle.kts` dapat dioverride tanpa mengedit source:

```bash
# Backend lokal berjalan pada port 5162, emulator Android
JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug -PapiBaseUrl=http://10.0.2.2:5162/api/v1/

# Perangkat fisik, backend LAN (default)
JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug
```

Untuk telepon fisik, gunakan IP LAN komputer server dan port sebenarnya. Perangkat harus dapat menjangkau server; backend harus listen di interface yang dapat diakses. `localhost` di telepon bukan komputer server. Prefix `/api/v1/` dan trailing slash diperlukan (Gradle menormalkan slash akhir).

HTTP pengembangan diizinkan hanya untuk host `192.168.69.58`, `192.168.1.11`, `192.168.1.14`, `10.0.2.2`, `localhost`, `127.0.0.1` di `app/src/main/res/xml/network_security_config.xml`. IP lain perlu ditambahkan ke konfigurasi domain. HTTPS tidak memerlukan pengecualian HTTP tersebut.

Gambar dibangun terhadap origin server **tanpa** `/api/v1`, misalnya `http://192.168.69.58:3001/uploads/alat/...`.

## Build, pasang, jalankan

Dari root proyek:

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug
JAVA_HOME=/opt/android-studio/jbr ./gradlew installDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Untuk emulator backend lokal, gunakan override yang sama saat build **dan** install:

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew installDebug -PapiBaseUrl=http://10.0.2.2:5162/api/v1/
```

Di Android Studio: Sync Gradle, pilih JBR sebagai Gradle JDK, pilih perangkat, lalu Run `app`. Hapus override bila ingin kembali memakai default LAN. Jangan memakai APK hasil override lokal untuk telepon fisik.

## Akun dari seed backend

| Role | Nomor telepon | Kata sandi |
|---|---|---|
| Admin | `081200000001` | `admin123` |
| Petugas | `081200000002` | `petugas123` |
| Peminjam | `081200000003` | `peminjam123` |

Nomor berformat `0812-0000-0003` juga diterima backend. Tidak ada fitur registrasi publik; pengguna dibuat Admin.

## Pengujian dan hasil yang benar-benar diverifikasi

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug lint testDebugUnitTest

# Memerlukan emulator/perangkat dan backend uji yang aktif
JAVA_HOME=/opt/android-studio/jbr ./gradlew connectedDebugAndroidTest -PapiBaseUrl=http://10.0.2.2:5163/api/v1/
```

- Unit test menguji parsing tanggal/expiry/timezone, response contract/null, pagination overlap, dan biaya/denda.
- Android test menguji login/routing/rotasi per role terhadap API nyata, 401 invalid token, 403, empty fields, null response, no-connection mapping dan expiry timer.
- UI diuji di emulator API 36: pengajuan dengan estimasi, approve/handover/return dengan denda, draft rotasi, CRUD master, edit item, reject/delete, laporan/share, log, terang/gelap.
- Pengujian API terpisah pada skema sementara memverifikasi biaya Rp324.000, denda Rp295.000, return tanpa denda, role restriction, reports dan pagination.
- **Belum terbukti:** konektivitas deployment LAN yang sedang timeout, perangkat fisik, seluruh rentang API/ukuran layar, restore setelah process death di semua form, serta pembukaan CSV pada aplikasi spreadsheet pihak ketiga.
- Lihat hasil lengkap dan screenshot yang perlu diambil di [`docs/TESTING.md`](docs/TESTING.md). Build sukses tidak otomatis berarti semua skenario runtime berhasil.

## Library tambahan

**Coil 2.7.0** adalah satu-satunya image-loading library tambahan, menggantikan coroutine/bitmap loader global. Digunakan untuk lifecycle-aware request, cache, downsampling dan placeholder. AndroidX SplashScreen 1.2.0 digunakan untuk splash resmi; komponen lain Material/AndroidX yang diminta. Tidak ada library dashboard, PDF, skeleton, DI framework, atau Compose.

## Struktur yang dapat dijelaskan saat ujian

```
app/src/main/java/com/app/campgear/
├── MainActivity.kt        # routing sesi, bottom nav/drawer, hak akses
├── core/                  # manual DI, session, formatter, pagination, status
├── data/                  # DTO, ApiService, interceptor, response/error contract
│   └── repository/        # request API per modul + ringkasan dashboard
└── ui/
    ├── auth/              # login
    ├── home/              # dashboard per role
    ├── peminjam/          # katalog, detail alat, cart, form, mine
    ├── loan/              # loan detail/action, return form/unit
    ├── staff/             # requests dan monitor
    ├── admin/             # master CRUD/form, edit loan, log
    ├── returns/           # riwayat/detail denda server
    ├── reports/           # laporan + file CSV
    ├── profile/           # profil/logout
    └── common/            # ViewBinding base, state, date/confirm dialogs
```

Resource desain: `res/values/{colors,dimens,themes}.xml`, varian `values-night`, `values-land`, `values-sw600dp`; layout XML per layar. State formulir berada di ViewModel selama rotasi; form master juga memakai SavedStateHandle (kata sandi tidak disimpan ke disk saved-state).

## Batas backend dan cakupan filter

- Detail hasil pengembalian Peminjam tidak dapat ditemukan dari POST kosong; hanya estimasi sebelum submit dan status selesai tersedia. Admin/Petugas dapat membaca hasil final.
- Admin tidak bisa POST loan atau PUT/DELETE return karena endpoint tidak tersedia. Tidak ada PDF; output resmi CSV.
- Kategori search client-side atas seluruh array kategori. Unit search client-side atas halaman yang dimuat dan dilabeli demikian. Katalog nama/kategori dan pengguna search adalah server-side.
- Riwayat/tag log menggunakan filter **aktif** yang baru berubah setelah Terapkan; perubahan draft tidak mencampur halaman pagination.
- Konstanta lookup/tarif estimasi bersumber dari seed/appsettings, sebab API lookup/tarif belum ada.
- Keranjang revalidate harga/stok saat checkout; unit tidak dipesan sampai handover server. Jika terjadi timeout mutasi, periksa data terbaru sebelum mengulang.
- Semua rincian kontrak/batas ada di [`BACKEND_ISSUES.md`](BACKEND_ISSUES.md).

## Dokumentasi

- [`AUDIT.md`](AUDIT.md): temuan versi pertama, termasuk klaim yang tidak terbukti dan crash runtime.
- [`API_INVENTORY.md`](API_INVENTORY.md): endpoint, payload, status, role.
- [`BACKEND_ISSUES.md`](BACKEND_ISSUES.md): keterbatasan backend dan penanganan klien.
- [`docs/MODULES.md`](docs/MODULES.md): Input/Process/Output tiap modul.
- [`docs/TESTING.md`](docs/TESTING.md): hasil uji dan daftar screenshot.
- [`CHANGELOG.md`](CHANGELOG.md): perubahan dari versi pertama.
