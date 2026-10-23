package com.app.campgear.ui.admin

import androidx.lifecycle.viewModelScope
import com.app.campgear.core.PagedViewModel
import com.app.campgear.data.AlatResponse
import com.app.campgear.data.ApiResult
import com.app.campgear.data.PagedResponse
import com.app.campgear.data.repository.AlatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlatManageViewModel(
    private val repository: AlatRepository
) : PagedViewModel<AlatResponse>() {

    private var search: String? = null
    private var searchJob: Job? = null

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val _deleted = Channel<Unit>(Channel.BUFFERED)
    val deleted = _deleted.receiveAsFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()

    protected override suspend fun fetchPage(page: Int): ApiResult<PagedResponse<AlatResponse>> =
        repository.getAlat(page, search, null)

    fun setSearch(query: String) {
        val next = query.trim().ifBlank { null }
        searchJob?.cancel()
        if (next == search) return
        searchJob = viewModelScope.launch {
            delay(400)
            search = next
            reload()
        }
    }

    fun delete(id: Long) {
        if (_busy.value) return
        _busy.value = true
        _actionError.value = null
        viewModelScope.launch {
            when (val result = repository.delete(id)) {
                is ApiResult.Success -> {
                    _actionError.value = null
                    _deleted.send(Unit)
                    refresh()
                }

                is ApiResult.Failure -> _actionError.value = result.message
            }
            _busy.value = false
        }
    }

    fun clearActionError() {
        _actionError.value = null
    }
}
