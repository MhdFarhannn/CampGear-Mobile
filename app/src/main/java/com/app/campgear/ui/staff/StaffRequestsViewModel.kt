package com.app.campgear.ui.staff

import com.app.campgear.core.PagedViewModel
import com.app.campgear.data.ApiResult
import com.app.campgear.data.PagedResponse
import com.app.campgear.data.PeminjamanResponse
import com.app.campgear.data.repository.PeminjamanRepository

class StaffRequestsViewModel(
    private val repository: PeminjamanRepository
) : PagedViewModel<PeminjamanResponse>() {

    private var status: String? = null
    val selectedStatus: String? get() = status
    private var initialized = false

    fun initialize(pendingFirst: Boolean) {
        if (!initialized) {
            initialized = true
            status = if (pendingFirst) "diajukan" else null
        }
        start()
    }

    protected override suspend fun fetchPage(page: Int): ApiResult<PagedResponse<PeminjamanResponse>> =
        repository.getAll(page, status)

    fun setStatus(value: String?) {
        if (status == value) return
        status = value
        reload()
    }
}
