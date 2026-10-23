package com.app.campgear.data

import com.app.campgear.core.Formatters
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

/** Menolak respons rusak sebelum null dari Gson mencapai model Kotlin non-null. */
class ResponseContracts : TypeAdapterFactory {
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        val required = requiredFields[type.rawType]
        val collection = List::class.java.isAssignableFrom(type.rawType)
        if (required == null && !collection) return null
        val delegate = gson.getDelegateAdapter(this, type)
        return object : TypeAdapter<T>() {
            override fun write(writer: JsonWriter, value: T?) = delegate.write(writer, value)

            override fun read(reader: JsonReader): T {
                val json = JsonParser.parseReader(reader)
                if (collection) {
                    if (!json.isJsonArray || json.asJsonArray.any { it.isJsonNull }) throw JsonParseException("Daftar tidak valid")
                } else validate(type.rawType, required.orEmpty(), json)
                return delegate.fromJsonTree(json)
            }
        }
    }

    private fun validate(type: Class<*>, required: Set<String>, element: JsonElement) {
        if (!element.isJsonObject) throw JsonParseException("Objek ${type.simpleName} tidak valid")
        val json = element.asJsonObject
        required.forEach { field ->
            if (!json.has(field) || json[field].isJsonNull) throw JsonParseException("Field $field tidak tersedia")
        }
        json.entrySet().forEach { (name, value) ->
            val isList = name in setOf("items", "details") ||
                (type == PeminjamanDetailResponse::class.java && name in setOf("idUnitAlat", "kodeInventaris"))
            if (isList) {
                if (!value.isJsonArray || value.asJsonArray.any { it.isJsonNull }) {
                    throw JsonParseException("Daftar $name tidak valid")
                }
            }
            if (!isList && name in required && name in stringFields &&
                (!value.isJsonPrimitive || !value.asJsonPrimitive.isString)
            ) throw JsonParseException("Field $name harus berupa teks")
            if (name in dateFields && !value.isJsonNull && !Formatters.isValidDate(value.asString)) {
                throw JsonParseException("Tanggal $name tidak valid")
            }
            if (name in moneyFields && !value.isJsonNull &&
                (!value.isJsonPrimitive || !value.asDouble.isFinite() || value.asDouble < 0)
            ) throw JsonParseException("Nilai $name tidak valid")
            if (name in integerFields && !isList && !value.isJsonNull) {
                if (!value.isJsonPrimitive || !value.asJsonPrimitive.isNumber || value.asBigDecimal.stripTrailingZeros().scale() > 0 || value.asLong < 0) {
                    throw JsonParseException("Bilangan $name tidak valid")
                }
            }
        }
        if (json.has("id") && json["id"].asLong <= 0) throw JsonParseException("Id tidak valid")
        if (type == PagedResponse::class.java &&
            (json["page"].asInt < 1 || json["pageSize"].asInt !in 1..100 || json["total"].asLong < 0)
        ) throw JsonParseException("Pagination tidak valid")
        if (type == LoginUser::class.java && json["role"].asString !in roles) {
            throw JsonParseException("Role tidak dikenal")
        }
        if (type == LoginResponse::class.java &&
            (json["token"].asString.isBlank() || Formatters.parseApiDateTime(json["expiresAt"].asString) == null)
        ) throw JsonParseException("Sesi tidak valid")
        if (type == PeminjamanResponse::class.java && json["status"].asString !in loanStatuses) {
            throw JsonParseException("Status peminjaman tidak dikenal")
        }
        if (type == PeminjamanDetailResponse::class.java) {
            if (json["jumlah"].asInt !in 1..1000) throw JsonParseException("Jumlah alat tidak valid")
            if (json["idUnitAlat"].asJsonArray.size() != json["kodeInventaris"].asJsonArray.size()) throw JsonParseException("Data unit tidak lengkap")
            if (json["idUnitAlat"].asJsonArray.any { !it.isJsonPrimitive || !it.asJsonPrimitive.isNumber || it.asLong <= 0 }) throw JsonParseException("Id unit tidak valid")
        }
    }

    private companion object {
        val roles = setOf("Admin", "Petugas", "Peminjam")
        val loanStatuses = setOf("diajukan", "disetujui", "ditolak", "dipinjam", "selesai")
        val dateFields = setOf("tanggalPeminjaman", "tanggalPengembalian", "tanggalKembali", "from", "to")
        val moneyFields = setOf("hargaSewa", "totalBiaya", "hargaSewaSaatTransaksi", "denda", "dendaKondisi", "totalDenda")
        val integerFields = setOf("id", "idAlat", "idRole", "idKategori", "idUser", "idPeminjam", "idPetugas", "idPeminjaman", "idUnitAlat", "page", "pageSize", "total", "stokTersedia", "jumlah", "jumlahAlat", "hariTerlambat", "keterlambatanPengembalian", "idStatusBarang", "idStatusKetersediaan")
        val stringFields = setOf("nama", "role", "token", "expiresAt", "status", "kodeInventaris", "aktivitas", "waktu") + dateFields
        val requiredFields = mapOf(
            PagedResponse::class.java to setOf("items", "page", "pageSize", "total"),
            LoginResponse::class.java to setOf("token", "expiresAt", "user"),
            LoginUser::class.java to setOf("id", "nama", "role"),
            UserResponse::class.java to setOf("id", "nama", "idRole"),
            Kategori::class.java to setOf("id", "nama"),
            AlatResponse::class.java to setOf("id", "idKategori", "nama", "hargaSewa", "stokTersedia"),
            UnitAlatResponse::class.java to setOf("id", "idAlat", "kodeInventaris", "idStatusBarang", "idStatusKetersediaan"),
            PeminjamanResponse::class.java to setOf("id", "idPeminjam", "status", "tanggalPeminjaman", "tanggalPengembalian", "totalBiaya", "details"),
            PeminjamanDetailResponse::class.java to setOf("id", "idAlat", "jumlah", "hargaSewaSaatTransaksi", "idUnitAlat", "kodeInventaris"),
            MonitorPeminjaman::class.java to setOf("id", "tanggalPeminjaman", "tanggalPengembalian", "hariTerlambat", "totalBiaya"),
            PengembalianResponse::class.java to setOf("id", "idPeminjaman", "tanggalKembali", "keterlambatanPengembalian", "denda", "details"),
            PengembalianUnitResponse::class.java to setOf("idUnitAlat", "dendaKondisi"),
            LogAktivitas::class.java to setOf("id", "idUser", "aktivitas", "waktu"),
            LaporanPeminjamanResponse::class.java to setOf("from", "to", "totalBiaya", "items"),
            LaporanPengembalianResponse::class.java to setOf("from", "to", "totalDenda", "items"),
            ReportPeminjaman::class.java to setOf("id", "tanggalPeminjaman", "tanggalPengembalian", "totalBiaya", "jumlahAlat"),
            ReportPengembalian::class.java to setOf("id", "idPeminjaman", "tanggalKembali", "keterlambatanPengembalian", "denda", "dendaKondisi")
        )
    }
}
