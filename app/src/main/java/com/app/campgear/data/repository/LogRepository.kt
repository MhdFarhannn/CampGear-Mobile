package com.app.campgear.data.repository

import com.app.campgear.data.ApiResult
import com.app.campgear.data.ApiService
import com.app.campgear.data.LogAktivitas
import com.app.campgear.data.PagedResponse
import com.app.campgear.data.apiCall

class LogRepository(private val api: ApiService) {

    suspend fun getAll(
        page: Int,
        idUser: Long?,
        from: String?,
        to: String?
    ): ApiResult<PagedResponse<LogAktivitas>> =
        apiCall { api.getLogAktivitas(page, PAGE_SIZE, idUser, from, to) }

    companion object {
        const val PAGE_SIZE = 20
    }
}
