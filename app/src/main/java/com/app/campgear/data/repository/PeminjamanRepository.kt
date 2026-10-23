package com.app.campgear.data.repository

import com.app.campgear.data.ApiResult
import com.app.campgear.data.ApiService
import com.app.campgear.data.MonitorPeminjaman
import com.app.campgear.data.PagedResponse
import com.app.campgear.data.PeminjamanRequest
import com.app.campgear.data.PeminjamanResponse
import com.app.campgear.data.RejectRequest
import com.app.campgear.data.actionCall
import com.app.campgear.data.apiCall

class PeminjamanRepository(private val api: ApiService) {

    suspend fun getAll(
        page: Int,
        status: String?,
        from: String? = null,
        to: String? = null
    ): ApiResult<PagedResponse<PeminjamanResponse>> =
        apiCall { api.getPeminjaman(page, PAGE_SIZE, status, from, to) }

    suspend fun getMine(
        page: Int,
        status: String?
    ): ApiResult<PagedResponse<PeminjamanResponse>> =
        apiCall { api.getMyPeminjaman(page, PAGE_SIZE, status) }

    suspend fun getMonitor(
        page: Int,
        status: String?
    ): ApiResult<PagedResponse<MonitorPeminjaman>> =
        apiCall { api.getMonitor(page, PAGE_SIZE, status) }

    suspend fun getById(id: Long): ApiResult<PeminjamanResponse> =
        apiCall { api.getPeminjamanById(id) }

    suspend fun create(request: PeminjamanRequest): ApiResult<Unit> =
        actionCall { api.createPeminjaman(request) }

    suspend fun update(id: Long, request: PeminjamanRequest): ApiResult<Unit> =
        actionCall { api.updatePeminjaman(id, request) }

    suspend fun delete(id: Long): ApiResult<Unit> =
        actionCall { api.deletePeminjaman(id) }

    suspend fun approve(id: Long): ApiResult<Unit> =
        actionCall { api.approvePeminjaman(id) }

    suspend fun reject(id: Long, catatan: String?): ApiResult<Unit> =
        actionCall { api.rejectPeminjaman(id, RejectRequest(catatan)) }

    suspend fun borrow(id: Long): ApiResult<Unit> =
        actionCall { api.borrowPeminjaman(id) }

    companion object {
        const val PAGE_SIZE = 20
    }
}
