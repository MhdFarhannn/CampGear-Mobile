# Inventaris API CampGear

Dokumen ini disusun dari pembacaan langsung folder backend `CampGear-Backend`
(controller, model, service, `Program.cs`, `launchSettings.json`,
`appsettings.json`, `query.sql`, dan `CampGear-Backend.http`).

## 1. Konfigurasi Dasar

| Item | Nilai |
|---|---|
| Basis URL backend (host) | `http://localhost:5162` (profil `http`) |
| Prefix API | `/api/v1` |
| Basis URL untuk emulator Android | `http://10.0.2.2:5162/api/v1/` |
| Default deployment Android dari pengguna | `http://192.168.69.58:3001/api/v1/` |
| Override build Android | `-PapiBaseUrl=http://10.0.2.2:5162/api/v1/` |
| Swagger | `http://localhost:5162/swagger` |
| Format JSON | camelCase |
| Autentikasi | `Authorization: Bearer <token>` |
| Masa berlaku token | 120 menit (`Jwt:ExpiryMinutes`) |
| Issuer / Audience | `CampGearBackend` / `CampGearClient` |
| Klaim JWT | id pengguna (`ClaimTypes.NameIdentifier`), nama (`ClaimTypes.Name`), role (`ClaimTypes.Role`), `jti` |
| Role | `Admin`, `Petugas`, `Peminjam` |

## 2. Format Respons

### Sukses

- Operasi tulis (`POST`/`PUT`/`PATCH`/`DELETE`): `200 OK`, body kosong.
- Detail: `200 OK`, objek JSON.
- Daftar berpaginasi: `200 OK`

```json
{ "items": [], "page": 1, "pageSize": 20, "total": 42 }
```

- Parameter `page` mulai dari 1, `pageSize` default 20 dan maksimum 100
  (nilai lebih besar dipotong diam-diam).
- `GET /kategori` tidak berpaginasi dan mengembalikan array.
- Laporan mengembalikan objek dengan `items`, `from`, `to`, dan total.

### Error

Error dari controller umumnya memakai problem body; authorization 401/403 dapat kosong, dan validation dapat memuat `errors`:

```json
{ "title": "Conflict", "status": 409, "detail": "Kategori already exists", "traceId": "..." }
```

| Status | Arti |
|---|---|
| 400 | Body/parameter tidak valid |
| 401 | Token hilang/kedaluwarsa, atau kredensial salah |
| 403 | Role tidak berhak atau bukan pemilik data |
| 404 | Data tidak ditemukan |
| 409 | Status tidak sesuai, duplikat, atau data masih dipakai |
| 500 | Kesalahan tak terduga |

## 3. Nilai Tetap

| Field | Nilai |
|---|---|
| `status` peminjaman | `diajukan`, `disetujui`, `ditolak`, `dipinjam`, `selesai` |
| `statusBarang` unit | `baik`, `rusak_ringan`, `rusak_berat` |
| `statusKetersediaan` unit | `tersedia`, `dipinjam`, `tidak_tersedia` |
| `statusKondisi` pengembalian | `baik`, `rusak_ringan`, `rusak_berat`, `hilang` |
| `jenisKelamin` | `Laki-laki`, `Perempuan` |
| Filter monitor | `jatuh_tempo`, `terlambat`, atau kosong (semua) |
| Format laporan | `json` (default), `csv` |
| Role id | 1=Admin, 2=Petugas, 3=Peminjam |
| Jenis kelamin id | 1=Laki-laki, 2=Perempuan |
| Status barang id | 1=baik, 2=rusak_ringan, 3=rusak_berat |
| Status ketersediaan id | 1=tersedia, 2=dipinjam, 3=tidak_tersedia |

Denda dari `appsettings.json`: terlambat Rp10.000/hari, rusak ringan
Rp25.000/unit, rusak berat Rp100.000/unit, hilang Rp250.000/unit.

Rumus biaya sewa: `hargaSewa x jumlah x max(1, selisih hari)`.
Rumus denda: `keterlambatan x 10.000 + total denda kondisi unit`.

## 4. Daftar Endpoint per Role

### 4.1 Publik

| Method | Path | Body | Respons |
|---|---|---|---|
| POST | `/auth/login` | `{ "noTelepon", "password" }` | `{ token, expiresAt, user: { id, nama, role } }` |

Login salah mengembalikan 401 dengan pesan generik
`Phone number or password is incorrect`.

### 4.2 Semua token (semua role)

| Method | Path | Query | Respons |
|---|---|---|---|
| GET | `/auth/me` | — | `UserResponse` |
| POST | `/auth/logout` | — | kosong |
| GET | `/kategori` | — | array `{ id, nama }` |
| GET | `/kategori/{id}` | — | `{ id, nama }` |
| GET | `/alat` | `page`, `pageSize`, `search`, `idKategori` | paged `AlatResponse` |
| GET | `/alat/{id}` | — | `AlatResponse` |
| GET | `/unit-alat` | `page`, `pageSize`, `idAlat`, `idStatusKetersediaan` | paged `UnitAlatResponse` |
| GET | `/unit-alat/{id}` | — | `UnitAlatResponse` |
| GET | `/peminjaman/{id}` | — | `PeminjamanResponse` (Peminjam hanya miliknya) |
| GET | `/pengembalian/{id}` | — | `PengembalianResponse` (Peminjam hanya miliknya) |

### 4.3 Admin

| Method | Path | Query/Body | Catatan |
|---|---|---|---|
| GET | `/users` | `page`, `pageSize`, `search`, `idRole` | paged `UserResponse` |
| GET | `/users/{id}` | — | `UserResponse` |
| POST | `/users` | `UserRequest` | password wajib |
| PUT | `/users/{id}` | `UserRequest` | password opsional (kosong = tetap) |
| DELETE | `/users/{id}` | — | 409 jika punya riwayat |
| POST | `/kategori` | `{ "nama" }` | 409 jika duplikat |
| PUT | `/kategori/{id}` | `{ "nama" }` | 409 jika duplikat |
| DELETE | `/kategori/{id}` | — | 409 jika dipakai alat |
| POST | `/alat` | multipart: `idKategori`, `nama`, `deskripsi?`, `hargaSewa`, `gambar?` | Admin |
| PUT | `/alat/{id}` | multipart, `gambar` opsional | tanpa gambar = gambar lama dipertahankan |
| DELETE | `/alat/{id}` | — | 409 jika punya unit/riwayat |
| POST | `/unit-alat` | `{ idAlat, kodeInventaris, idStatusBarang }` | unit baru selalu `tersedia` |
| PUT | `/unit-alat/{id}` | `{ kodeInventaris, idStatusBarang, idStatusKetersediaan }` | 409 jika unit sedang `dipinjam` |
| DELETE | `/unit-alat/{id}` | — | 409 jika punya riwayat |
| PUT | `/peminjaman/{id}` | `PeminjamanRequest` | hanya status `diajukan` |
| DELETE | `/peminjaman/{id}` | — | hanya `diajukan`/`ditolak` |
| GET | `/log-aktivitas` | `page`, `pageSize`, `idUser`, `from`, `to` | paged `LogAktivitasResponse` |
| PATCH | `/peminjaman/{id}/approve` | — | juga Petugas |
| PATCH | `/peminjaman/{id}/reject` | `{ "catatan" }` | juga Petugas |
| PATCH | `/peminjaman/{id}/borrow` | — | juga Petugas |
| GET | `/laporan/peminjaman` | `from`, `to`, `format` | juga Petugas |
| GET | `/laporan/pengembalian` | `from`, `to`, `format` | juga Petugas |

### 4.4 Petugas

| Method | Path | Query/Body |
|---|---|---|
| GET | `/peminjaman` | `page`, `pageSize`, `status`, `idPeminjam`, `from`, `to` |
| GET | `/peminjaman/monitor` | `page`, `pageSize`, `status` (`jatuh_tempo`/`terlambat`) |
| PATCH | `/peminjaman/{id}/approve` | — |
| PATCH | `/peminjaman/{id}/reject` | `{ "catatan" }` |
| PATCH | `/peminjaman/{id}/borrow` | — |
| POST | `/pengembalian` | `PengembalianRequest` |
| GET | `/pengembalian` | `page`, `pageSize`, `from`, `to` |
| GET | `/laporan/peminjaman` | `from`, `to`, `format` |
| GET | `/laporan/pengembalian` | `from`, `to`, `format` |

### 4.5 Peminjam

| Method | Path | Query/Body |
|---|---|---|
| POST | `/peminjaman` | `{ tanggalPeminjaman, tanggalPengembalian, items: [{ idAlat, jumlah }] }` |
| GET | `/peminjaman/mine` | `page`, `pageSize`, `status` |
| POST | `/pengembalian` | `PengembalianRequest` (hanya peminjaman miliknya) |

## 5. Bentuk Data Penting

### LoginResponse

```json
{ "token": "...", "expiresAt": "2026-10-20T09:47:16.082Z",
  "user": { "id": 3, "nama": "Peminjam CampGear", "role": "Peminjam" } }
```

### UserResponse

```json
{ "id": 3, "nama": "Peminjam CampGear", "noTelepon": "081200000003",
  "alamat": "Jl. Merdeka No. 3, Bandung", "idRole": 3, "role": "Peminjam",
  "idJenisKelamin": 2, "jenisKelamin": "Perempuan",
  "createdAt": "2026-10-20T07:58:03+07:00" }
```

`UserRequest`: `{ idRole, idJenisKelamin?, nama, password?, noTelepon, alamat? }`.

### AlatResponse

```json
{ "id": 1, "idKategori": 1, "kategori": "Tenda", "nama": "Tenda Dome 4P",
  "deskripsi": "Tenda dome kapasitas 4 orang, tahan hujan.", "hargaSewa": 50000.00,
  "imagePath": null, "stokTersedia": 3, "createdAt": "..." }
```

URL gambar relatif terhadap host tanpa prefix `/api/v1`, contoh
`http://10.0.2.2:5162/uploads/alat/xxx.png`. Batas unggah 2 MB, tipe
jpg/jpeg/png/webp.

### UnitAlatResponse

```json
{ "id": 1, "idAlat": 1, "namaAlat": "Tenda Dome 4P",
  "idStatusKetersediaan": 1, "statusKetersediaan": "tersedia",
  "kodeInventaris": "TND-DM4-001", "idStatusBarang": 1,
  "statusBarang": "baik", "createdAt": "..." }
```

### PeminjamanResponse

```json
{ "id": 1, "idPeminjam": 3, "peminjam": "Peminjam CampGear",
  "idPetugas": 2, "petugas": "Petugas CampGear", "status": "dipinjam",
  "tanggalPeminjaman": "2026-10-21", "tanggalPengembalian": "2026-10-24",
  "totalBiaya": 324000.00, "createdAt": "...",
  "details": [
    { "id": 1, "idAlat": 1, "alat": "Tenda Dome 4P", "jumlah": 2,
      "hargaSewaSaatTransaksi": 50000.00,
      "idUnitAlat": [1, 2], "kodeInventaris": ["TND-DM4-001", "TND-DM4-002"] }
  ] }
```

`idUnitAlat`/`kodeInventaris` kosong sampai proses serah terima (`borrow`).

### MonitorPeminjaman

```json
{ "id": 1, "peminjam": "Peminjam CampGear", "noTelepon": "081200000003",
  "tanggalPeminjaman": "2026-10-21", "tanggalPengembalian": "2026-10-24",
  "hariTerlambat": 0, "totalBiaya": 324000.00 }
```

### PengembalianRequest

```json
{ "idPeminjaman": 1, "tanggalKembali": "2026-10-26", "catatan": "telat 2 hari",
  "units": [
    { "idUnitAlat": 1, "statusKondisi": "baik" },
    { "idUnitAlat": 2, "statusKondisi": "rusak_ringan", "catatan": "sobek" }
  ] }
```

### PengembalianResponse

```json
{ "id": 1, "idPeminjaman": 1, "peminjam": "Peminjam CampGear",
  "tanggalKembali": "2026-10-26", "keterlambatanPengembalian": 2,
  "denda": 295000.00, "catatan": "telat 2 hari",
  "details": [
    { "idUnitAlat": 1, "kodeInventaris": "TND-DM4-001", "alat": "Tenda Dome 4P",
      "statusKondisi": "baik", "dendaKondisi": 0.00, "catatan": null }
  ] }
```

### Laporan

Peminjaman: `{ from, to, totalBiaya, items: [{ id, peminjaman, petugas,
status, tanggalPeminjaman, tanggalPengembalian, totalBiaya, jumlahAlat }] }`.

Pengembalian: `{ from, to, totalDenda, items: [{ id, idPeminjaman, peminjam,
tanggalKembali, keterlambatanPengembalian, denda, dendaKondisi }] }`.

`format=csv` mengembalikan file `text/csv` dengan pemisah titik koma.

### LogAktivitas

```json
{ "id": 8, "idUser": 2, "namaUser": "Petugas CampGear",
  "aktivitas": "Menyetujui peminjaman #1", "waktu": "..." }
```

## 6. Alur Status

```
diajukan -> disetujui -> dipinjam -> selesai
diajukan -> ditolak
```

- Approve/reject hanya dari `diajukan`.
- Handover (`borrow`) hanya dari `disetujui`, memilih unit fisik dan
  mengubahnya menjadi `dipinjam`.
- Pengembalian hanya dari `dipinjam`, memproses seluruh unit dalam satu
  transaksi dan mengubah status menjadi `selesai`.
- Stok = jumlah unit `tersedia` dengan kondisi bukan `rusak_berat`.

## 7. Daftar Layar Aplikasi Android

| Role | Layar | Endpoint utama |
|---|---|---|
| Semua | Login | `POST /auth/login` |
| Semua | Profil + Logout | `GET /auth/me`, `POST /auth/logout` |
| Peminjam | Katalog Alat | `GET /alat`, `GET /kategori` |
| Peminjam | Keranjang & Form Pengajuan | `POST /peminjaman` |
| Peminjam | Peminjaman Saya | `GET /peminjaman/mine` |
| Peminjam | Detail Peminjaman | `GET /peminjaman/{id}` |
| Peminjam | Form Pengembalian | `GET /peminjaman/{id}`, `POST /pengembalian` |
| Petugas | Daftar Pengajuan | `GET /peminjaman` |
| Petugas | Detail + Aksi | approve/reject/borrow, `GET /peminjaman/{id}` |
| Petugas | Monitor Pengembalian | `GET /peminjaman/monitor` |
| Petugas | Riwayat Pengembalian | `GET /pengembalian`, `GET /pengembalian/{id}` |
| Petugas/Admin | Laporan | `GET /laporan/peminjaman`, `GET /laporan/pengembalian` |
| Admin | Peminjaman | `GET /peminjaman`, detail, edit, hapus, proses kembali |
| Admin | Pengembalian | `GET /pengembalian`, `GET /pengembalian/{id}` |
| Admin | Users | CRUD `/users` |
| Admin | Kategori | CRUD `/kategori` |
| Admin | Alat | CRUD `/alat` (multipart) |
| Admin | Unit Alat | CRUD `/unit-alat` + `GET /alat` |
| Admin | Log Aktivitas | `GET /log-aktivitas` |
