package com.app.campgear

import com.app.campgear.data.*
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import org.junit.Assert.*
import org.junit.Test

class ResponseContractsTest {
    private val gson = GsonBuilder().registerTypeAdapterFactory(ResponseContracts()).create()

    @Test fun login_missingUserIsRejected() {
        assertThrows(JsonParseException::class.java) { gson.fromJson("{\"token\":\"x\",\"expiresAt\":\"2026-10-22T12:00:00Z\",\"user\":null}", LoginResponse::class.java) }
    }

    @Test fun login_unknownRoleIsRejected() {
        assertThrows(JsonParseException::class.java) { gson.fromJson("{\"id\":1,\"nama\":\"Test\",\"role\":\"Owner\"}", LoginUser::class.java) }
    }

    @Test fun paged_nullItemsAndNullEntriesAreRejected() {
        val type = object : TypeToken<PagedResponse<AlatResponse>>() {}.type
        for (items in listOf("null", "[null]")) {
            assertThrows(JsonParseException::class.java) { gson.fromJson<PagedResponse<AlatResponse>>("{\"items\":$items,\"page\":1,\"pageSize\":20,\"total\":1}", type) }
        }
    }

    @Test fun unit_stringCodeIsAccepted() {
        val unit = gson.fromJson("{\"id\":1,\"idAlat\":1,\"kodeInventaris\":\"TND-001\",\"idStatusBarang\":1,\"idStatusKetersediaan\":1}", UnitAlatResponse::class.java)
        assertEquals("TND-001", unit.kodeInventaris)
    }

    @Test fun loan_invalidDateIsRejected() {
        assertThrows(JsonParseException::class.java) { gson.fromJson("{\"id\":1,\"idPeminjam\":3,\"status\":\"dipinjam\",\"tanggalPeminjaman\":\"2026-02-30\",\"tanggalPengembalian\":\"2026-03-01\",\"totalBiaya\":50000,\"details\":[]}", PeminjamanResponse::class.java) }
    }

    @Test fun paged_overlapReplacesSnapshotWithoutDuplicates() {
        val previous = listOf(Kategori(1, "Tenda"), Kategori(2, "Lampu"))
        val next = listOf(Kategori(2, "Penerangan"), Kategori(3, "Masak"))
        val merged = com.app.campgear.core.mergePages(previous, next)
        assertEquals(listOf(1L, 2L, 3L), merged.map { it.id })
        assertEquals("Penerangan", merged[1].nama)
    }

    @Test fun receipt_breakdownUsesBackendTotal() {
        val receipt = PengembalianResponse(denda = 45_000.0, details = listOf(PengembalianUnitResponse(dendaKondisi = 25_000.0)))
        assertEquals(20_000.0, receipt.lateFine, 0.0)
        assertEquals(25_000.0, receipt.conditionFine, 0.0)
    }
}
