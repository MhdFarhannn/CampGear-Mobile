package com.app.campgear.data

interface Identified { val id: Long }

data class PagedResponse<T>(
    val items: List<T> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 20,
    val total: Long = 0
)

data class LoginRequest(
    val noTelepon: String,
    val password: String
)

data class LoginResponse(
    val token: String = "",
    val expiresAt: String = "",
    val user: LoginUser = LoginUser()
)

data class LoginUser(
    val id: Long = 0,
    val nama: String = "",
    val role: String = ""
)

data class ProblemResponse(
    val title: String? = null,
    val status: Int? = null,
    val detail: String? = null,
    val traceId: String? = null
)

data class UserResponse(
    override val id: Long = 0,
    val nama: String = "",
    val noTelepon: String? = null,
    val alamat: String? = null,
    val idRole: Long = 0,
    val role: String? = null,
    val idJenisKelamin: Long? = null,
    val jenisKelamin: String? = null,
    val createdAt: String? = null
) : Identified

data class UserRequest(
    val idRole: Long,
    val idJenisKelamin: Long?,
    val nama: String,
    val password: String?,
    val noTelepon: String,
    val alamat: String?
)

data class Kategori(
    override val id: Long = 0,
    val nama: String = ""
) : Identified

data class KategoriRequest(
    val nama: String
)

data class AlatResponse(
    override val id: Long = 0,
    val idKategori: Long = 0,
    val kategori: String? = null,
    val nama: String = "",
    val deskripsi: String? = null,
    val hargaSewa: Double = 0.0,
    val imagePath: String? = null,
    val stokTersedia: Int = 0,
    val createdAt: String? = null
) : Identified

data class UnitAlatResponse(
    override val id: Long = 0,
    val idAlat: Long = 0,
    val namaAlat: String? = null,
    val idStatusKetersediaan: Long = 0,
    val statusKetersediaan: String? = null,
    val kodeInventaris: String = "",
    val idStatusBarang: Long = 0,
    val statusBarang: String? = null,
    val createdAt: String? = null
) : Identified

data class UnitAlatRequest(
    val idAlat: Long,
    val kodeInventaris: String,
    val idStatusBarang: Long
)

data class UnitAlatUpdateRequest(
    val kodeInventaris: String,
    val idStatusBarang: Long,
    val idStatusKetersediaan: Long
)

data class PeminjamanRequest(
    val tanggalPeminjaman: String,
    val tanggalPengembalian: String,
    val items: List<PeminjamanItemRequest>
)

data class PeminjamanItemRequest(
    val idAlat: Long,
    val jumlah: Int
)

data class PeminjamanResponse(
    override val id: Long = 0,
    val idPeminjam: Long = 0,
    val peminjam: String? = null,
    val idPetugas: Long? = null,
    val petugas: String? = null,
    val status: String = "",
    val tanggalPeminjaman: String = "",
    val tanggalPengembalian: String = "",
    val totalBiaya: Double = 0.0,
    val createdAt: String? = null,
    val details: List<PeminjamanDetailResponse> = emptyList()
) : Identified

data class PeminjamanDetailResponse(
    val id: Long = 0,
    val idAlat: Long = 0,
    val alat: String? = null,
    val jumlah: Int = 0,
    val hargaSewaSaatTransaksi: Double = 0.0,
    val idUnitAlat: List<Long> = emptyList(),
    val kodeInventaris: List<String> = emptyList()
)

data class RejectRequest(
    val catatan: String?
)

data class MonitorPeminjaman(
    override val id: Long = 0,
    val peminjam: String? = null,
    val noTelepon: String? = null,
    val tanggalPeminjaman: String = "",
    val tanggalPengembalian: String = "",
    val hariTerlambat: Int = 0,
    val totalBiaya: Double = 0.0
) : Identified

data class PengembalianRequest(
    val idPeminjaman: Long,
    val tanggalKembali: String,
    val catatan: String?,
    val units: List<PengembalianUnitRequest>
)

data class PengembalianUnitRequest(
    val idUnitAlat: Long,
    val statusKondisi: String,
    val catatan: String? = null
)

data class PengembalianResponse(
    override val id: Long = 0,
    val idPeminjaman: Long = 0,
    val peminjam: String? = null,
    val tanggalKembali: String = "",
    val keterlambatanPengembalian: Int = 0,
    val denda: Double = 0.0,
    val catatan: String? = null,
    val details: List<PengembalianUnitResponse> = emptyList()
) : Identified {
    val conditionFine: Double get() = details.sumOf { it.dendaKondisi }
    val lateFine: Double get() = (denda - conditionFine).coerceAtLeast(0.0)
}

data class PengembalianUnitResponse(
    val idUnitAlat: Long = 0,
    val kodeInventaris: String? = null,
    val alat: String? = null,
    val statusKondisi: String? = null,
    val dendaKondisi: Double = 0.0,
    val catatan: String? = null
)

data class LogAktivitas(
    override val id: Long = 0,
    val idUser: Long = 0,
    val namaUser: String? = null,
    val aktivitas: String = "",
    val waktu: String = ""
) : Identified

data class LaporanPeminjamanResponse(
    val from: String = "",
    val to: String = "",
    val totalBiaya: Double = 0.0,
    val items: List<ReportPeminjaman> = emptyList()
)

data class ReportPeminjaman(
    val id: Long = 0,
    val peminjam: String? = null,
    val petugas: String? = null,
    val status: String? = null,
    val tanggalPeminjaman: String = "",
    val tanggalPengembalian: String = "",
    val totalBiaya: Double = 0.0,
    val jumlahAlat: Int = 0
)

data class LaporanPengembalianResponse(
    val from: String = "",
    val to: String = "",
    val totalDenda: Double = 0.0,
    val items: List<ReportPengembalian> = emptyList()
)

data class ReportPengembalian(
    val id: Long = 0,
    val idPeminjaman: Long = 0,
    val peminjam: String? = null,
    val tanggalKembali: String = "",
    val keterlambatanPengembalian: Int = 0,
    val denda: Double = 0.0,
    val dendaKondisi: Double = 0.0
)
