# Riwayat Revisi CampGear

## Revisi audit/desain ulang — 22 Oktober 2026

### Fase 1 — audit jujur

- Membaca seluruh implementasi Android dan kontrak sumber backend, bukan hanya hasil build.
- Menambahkan `AUDIT.md` dengan setiap endpoint, status layar, bug lifecycle/networking, UI yang belum lengkap, dan batas backend.
- Mengoreksi klaim “semua selesai/clean” versi pertama: build/lint awal bersih tetapi masih ada kesalahan runtime dan fitur missing.
- Mencatat deployment LAN timeout dan tidak menganggap respons terdahulu bukti konektivitas saat ini.

### Fase 2 — fungsi dan state

- GET body kosong sekarang Failure; `Unit as T` dihapus. Factory Gson memeriksa required/null/list/tipe/status/date sebelum model digunakan.
- JWT expiry .NET tujuh digit/offset diparse benar; formatter tidak memakai instance date/number formatter bersama yang tidak thread-safe.
- Sesi validasi role/id/token/expiry, recover key/prefs rusak, timer expiry, stale401 compare token, backup exclusions, login tanpa token lama, 403 terlokalisasi.
- Event mutasi sebelumnya SharedFlow tanpa replay diganti antrean Channel atau state saved yang tidak hilang ketika STOPPED.
- Draft return/edit/form alat tidak ditimpa load setiap onViewCreated; master form penuh memakai VM/SavedStateHandle dan error inline.
- Pagination deduplikasi/generation; draft filter returns/log dipisah dari applied filter; picker pengguna tidak terpotong 100 pertama.
- Cart maksimum1000 dan revalidate harga/stok; edit loan dapat tambah/hapus item, mengambil harga baru tanpa fallback diam-diam.
- Form return memvalidasi seluruh unit/unik/status, catatan DiffUtil payload menjaga cursor, konfirmasi dan input lock.
- File CSV dibuat/ditutup dalam VM IO, snapshot jenis/periode dikaitkan dengan generation; summary empty nol tetap tersedia.
- Adapter RecyclerView dilepas saat onDestroyView; retry empty action yang dahulu tidak terlihat diperbaiki.

### Fase 3 — UI/UX

- Desain system Material3.DayNight, forest/sand/off-white/charcoal dengan varian malam dan surface-container tokens.
- Spacing8dp/radius/touch-target/typography ditetapkan di resource token; varian landscape/tablet, icon/tint/ripple.
- Ilustrasi vector tenda/gunung/api unggun, icon adaptive camping, SplashScreen resmi; Coil menggantikan pemuat bitmap global.
- Bottom navigation Peminjam; drawer berikon Admin/Petugas dengan nama/role/konfirmasi keluar dan guard destination.
- Dashboard role dengan total API nyata; detail alat baru; kartu katalog search/category chips/image/stok, form sticky total/submit.
- Detail loan timeline, kartu status yang konsisten; return detail late/condition/total dari server.
- Semua master CRUD memakai FAB/overflow/form yang kohesif, preview upload; profil/log/monitor/reports/empty-error states didesain ulang.
- Material single/range date picker, listener dipasang ulang setelah restoration, tanggal tidak membuka keyboard.

### Bug nyata saat pengujian emulator

- Menemukan dan memperbaiki crash `Destination with route Int cannot be found`: overload generik navigasi terpilih untuk ID XML. Options kini memakai `navigate(id, args, NavOptions)` eksplisit.
- Mengoreksi padding navigation bar berlipat di bottom navigation.
- Menemukan catatan unit terbalik/terpotong ketika rebind saat mengetik; diperbaiki melalui DiffUtil payload/focus/cursor-preserving bind.
- Memastikan kondisi/catatan return, estimasi loan, dan master draft tetap utuh saat rotasi.

### Fase 4 — verifikasi dan dokumentasi

- Menghapus ExampleUnitTest/ExampleInstrumentedTest trivial; menambah response/date/merge regression dan test Android yang bermakna.
- Menjalankan test per role/401/403/expiry/null di emulator API36, ditambah flow UI dan API nyata.
- Backend dibangun dengan output/intermediate di `/tmp`; sumber tidak diubah. Pemeriksaan tambahan memakai skema seed sementara, bukan reset database lama.
- Menambahkan `docs/MODULES.md`, `docs/TESTING.md`, memperbarui README/API inventory/BACKEND_ISSUES dan mencatat hasil yang tidak diverifikasi.
- Tidak menambahkan endpoint palsu, PDF generator, Compose, DI framework, atau fitur di luar spec.

### Library yang ditambahkan

- Coil2.7.0 (satu image library yang diizinkan): lifecycle, cache/downsample/fallback.
- AndroidX SplashScreen1.2.0: splash resmi.
- Library Material/AndroidX/networking yang sudah ada tetap digunakan.

### Batas yang tetap diketahui

PUT/DELETE pengembalian, Admin POST loan, Peminjam receipt lookup, lookup/tarif API dan PDF tidak tersedia. CSV server belum escaping; log-after-commit dapat memberi error sesudah mutasi tersimpan. Deployment LAN/telepon fisik dan sebagian edge cases masih perlu pengujian lingkungan pengguna. Semua detail di `BACKEND_ISSUES.md` dan `docs/TESTING.md`.
