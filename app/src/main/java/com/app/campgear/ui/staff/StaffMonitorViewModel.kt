package com.app.campgear.ui.staff

import com.app.campgear.core.PagedViewModel
import com.app.campgear.data.ApiResult
import com.app.campgear.data.MonitorPeminjaman
import com.app.campgear.data.PagedResponse
import com.app.campgear.data.repository.PeminjamanRepository

class StaffMonitorViewModel(
    private val repository: PeminjamanRepository
) : PagedViewModel<MonitorPeminjaman>() {

    private var status: String? = null
    val selectedStatus: String? get() = status

    protected override suspend fun fetchPage(page: Int): ApiResult<PagedResponse<MonitorPeminjaman>> =
        repository.getMonitor(page, status)

    fun setStatus(value: String?) {
        if (status == value) return
        status = value
        reload()
    }
}
