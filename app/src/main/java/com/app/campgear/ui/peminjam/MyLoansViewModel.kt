package com.app.campgear.ui.peminjam

import com.app.campgear.core.PagedViewModel
import com.app.campgear.data.ApiResult
import com.app.campgear.data.PagedResponse
import com.app.campgear.data.PeminjamanResponse
import com.app.campgear.data.repository.PeminjamanRepository

class MyLoansViewModel(
    private val repository: PeminjamanRepository
) : PagedViewModel<PeminjamanResponse>() {

    private var status: String? = null
    val selectedStatus: String? get() = status

    protected override suspend fun fetchPage(page: Int): ApiResult<PagedResponse<PeminjamanResponse>> =
        repository.getMine(page, status)

    fun setStatus(value: String?) {
        if (status == value) return
        status = value
        reload()
    }
}
