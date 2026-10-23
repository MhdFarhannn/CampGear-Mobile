package com.app.campgear.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.campgear.data.ApiResult
import com.app.campgear.data.Identified
import com.app.campgear.data.PagedResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PagedUiState<T>(
    val items: List<T> = emptyList(),
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val hasMore: Boolean = true,
    val total: Long = 0
)

fun <T : Identified> mergePages(previous: List<T>, incoming: List<T>): List<T> {
    val unique = linkedMapOf<Long, T>()
    (previous + incoming).forEach { unique[it.id] = it }
    return unique.values.toList()
}

abstract class PagedViewModel<T : Identified> : ViewModel() {
    private val _state = MutableStateFlow(PagedUiState<T>())
    val state: StateFlow<PagedUiState<T>> = _state.asStateFlow()
    private var job: Job? = null
    private var currentPage = 0
    private var generation = 0
    private var started = false

    protected abstract suspend fun fetchPage(page: Int): ApiResult<PagedResponse<T>>

    fun start() {
        if (!started) refresh()
    }

    fun refresh() {
        started = true
        load(1, append = false, clear = false)
    }

    fun loadMore() {
        if (job?.isActive == true || !_state.value.hasMore || _state.value.error != null) return
        load(currentPage + 1, append = true, clear = false)
    }

    protected fun reload() {
        started = true
        load(1, append = false, clear = true)
    }

    private fun load(page: Int, append: Boolean, clear: Boolean) {
        job?.cancel()
        val requestGeneration = ++generation
        if (!append) currentPage = 0
        val current = if (clear) PagedUiState<T>() else _state.value
        _state.value = current.copy(
            loading = !append && current.items.isEmpty(),
            refreshing = !append && current.items.isNotEmpty(),
            loadingMore = append,
            error = null
        )
        job = viewModelScope.launch {
            val result = fetchPage(page)
            if (requestGeneration != generation) return@launch
            _state.value = when (result) {
                is ApiResult.Success -> {
                    val data = result.data
                    currentPage = data.page
                    val items = mergePages(if (append) _state.value.items else emptyList(), data.items)
                    PagedUiState(
                        items = items,
                        total = data.total,
                        hasMore = data.items.isNotEmpty() && data.page.toLong() * data.pageSize < data.total
                    )
                }
                is ApiResult.Failure -> _state.value.copy(
                    loading = false, refreshing = false, loadingMore = false, error = result.message
                )
            }
        }
    }
}
