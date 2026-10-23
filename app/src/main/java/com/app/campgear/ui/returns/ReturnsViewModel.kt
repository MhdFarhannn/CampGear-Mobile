package com.app.campgear.ui.returns

import com.app.campgear.core.PagedViewModel
import com.app.campgear.data.ApiResult
import com.app.campgear.data.PagedResponse
import com.app.campgear.data.PengembalianResponse
import com.app.campgear.data.repository.PengembalianRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ReturnFilterDates(
    val from: String? = null,
    val to: String? = null
)

class ReturnsViewModel(
    private val repository: PengembalianRepository
) : PagedViewModel<PengembalianResponse>() {

    private val _dates = MutableStateFlow(ReturnFilterDates())
    val dates: StateFlow<ReturnFilterDates> = _dates.asStateFlow()
    private var appliedDates = ReturnFilterDates()

    protected override suspend fun fetchPage(page: Int): ApiResult<PagedResponse<PengembalianResponse>> =
        repository.getAll(page, appliedDates.from, appliedDates.to)

    fun setFrom(value: String?) {
        _dates.update { it.copy(from = value) }
    }

    fun setTo(value: String?) {
        _dates.update { it.copy(to = value) }
    }

    fun applyFilter() {
        val next = _dates.value
        if (next.from != null && next.to != null && next.to < next.from) return
        appliedDates = next
        reload()
    }

    fun resetFilter() {
        _dates.value = ReturnFilterDates()
        appliedDates = ReturnFilterDates()
        reload()
    }
}
