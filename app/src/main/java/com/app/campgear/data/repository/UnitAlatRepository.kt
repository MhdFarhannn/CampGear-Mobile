package com.app.campgear.data.repository

import com.app.campgear.data.ApiResult
import com.app.campgear.data.ApiService
import com.app.campgear.data.PagedResponse
import com.app.campgear.data.UnitAlatRequest
import com.app.campgear.data.UnitAlatResponse
import com.app.campgear.data.UnitAlatUpdateRequest
import com.app.campgear.data.actionCall
import com.app.campgear.data.apiCall

class UnitAlatRepository(private val api: ApiService) {

    suspend fun getUnits(idAlat: Long, page: Int, availability: Long? = null): ApiResult<PagedResponse<UnitAlatResponse>> =
        apiCall { api.getUnitAlat(page, PAGE_SIZE, idAlat, availability) }

    suspend fun getById(id: Long): ApiResult<UnitAlatResponse> = apiCall { api.getUnitAlatById(id) }

    suspend fun create(idAlat: Long, kode: String, idStatusBarang: Long): ApiResult<Unit> =
        actionCall { api.createUnitAlat(UnitAlatRequest(idAlat, kode, idStatusBarang)) }

    suspend fun update(
        id: Long,
        kode: String,
        idStatusBarang: Long,
        idStatusKetersediaan: Long
    ): ApiResult<Unit> =
        actionCall {
            api.updateUnitAlat(
                id,
                UnitAlatUpdateRequest(kode, idStatusBarang, idStatusKetersediaan)
            )
        }

    suspend fun delete(id: Long): ApiResult<Unit> =
        actionCall { api.deleteUnitAlat(id) }

    companion object {
        const val PAGE_SIZE = 20
    }
}
