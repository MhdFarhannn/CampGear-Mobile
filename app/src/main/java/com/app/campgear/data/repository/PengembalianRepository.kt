package com.app.campgear.data.repository

import com.app.campgear.data.ApiResult
import com.app.campgear.data.ApiService
import com.app.campgear.data.PagedResponse
import com.app.campgear.data.PengembalianRequest
import com.app.campgear.data.PengembalianResponse
import com.app.campgear.data.actionCall
import com.app.campgear.data.apiCall

class PengembalianRepository(private val api: ApiService) {

    suspend fun create(request: PengembalianRequest): ApiResult<Unit> =
        actionCall { api.createPengembalian(request) }

    suspend fun getAll(
        page: Int,
        from: String?,
        to: String?
    ): ApiResult<PagedResponse<PengembalianResponse>> =
        apiCall { api.getPengembalian(page, PAGE_SIZE, from, to) }

    suspend fun getById(id: Long): ApiResult<PengembalianResponse> =
        apiCall { api.getPengembalianById(id) }

    suspend fun findByLoan(loanId: Long, returnDate: String): ApiResult<PengembalianResponse?> {
        var page = 1
        while (true) {
            when (val result = getAll(page, returnDate, returnDate)) {
                is ApiResult.Failure -> return result
                is ApiResult.Success -> {
                    result.data.items.firstOrNull { it.idPeminjaman == loanId }?.let { return ApiResult.Success(it) }
                    if (result.data.items.isEmpty() || page.toLong() * result.data.pageSize >= result.data.total) return ApiResult.Success(null)
                    page++
                }
            }
        }
    }

    companion object {
        const val PAGE_SIZE = 20
    }
}
