package com.app.campgear.data.repository

import com.app.campgear.data.ApiResult
import com.app.campgear.data.ApiService
import com.app.campgear.data.apiCall
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

data class DashboardMetric(val key: String, val value: Long)

class DashboardRepository(private val api: ApiService) {
    suspend fun load(role: String): ApiResult<List<DashboardMetric>> = coroutineScope {
        val requests = when (role) {
            "Admin" -> listOf(
                async { count("equipment") { api.getAlat(1, 1, null, null) } },
                async { count("users") { api.getUsers(1, 1, null, null) } },
                async { count("borrowed") { api.getPeminjaman(1, 1, "dipinjam", null, null) } },
                async { count("returns") { api.getPengembalian(1, 1, null, null) } }
            )
            "Petugas" -> listOf(
                async { count("pending") { api.getPeminjaman(1, 1, "diajukan", null, null) } },
                async { count("borrowed") { api.getPeminjaman(1, 1, "dipinjam", null, null) } },
                async { count("overdue") { api.getMonitor(1, 1, "terlambat") } },
                async { count("due") { api.getMonitor(1, 1, "jatuh_tempo") } }
            )
            else -> listOf(
                async { count("pending") { api.getMyPeminjaman(1, 1, "diajukan") } },
                async { count("approved") { api.getMyPeminjaman(1, 1, "disetujui") } },
                async { count("borrowed") { api.getMyPeminjaman(1, 1, "dipinjam") } },
                async { count("completed") { api.getMyPeminjaman(1, 1, "selesai") } }
            )
        }
        val results = requests.map { it.await() }
        results.filterIsInstance<ApiResult.Failure>().firstOrNull()
            ?: ApiResult.Success(results.filterIsInstance<ApiResult.Success<DashboardMetric>>().map { it.data })
    }

    private suspend fun <T : Any> count(key: String, block: suspend () -> retrofit2.Response<com.app.campgear.data.PagedResponse<T>>): ApiResult<DashboardMetric> =
        when (val response = apiCall(block)) {
            is ApiResult.Success -> ApiResult.Success(DashboardMetric(key, response.data.total))
            is ApiResult.Failure -> response
        }
}
