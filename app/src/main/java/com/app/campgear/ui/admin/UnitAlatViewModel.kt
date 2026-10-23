package com.app.campgear.ui.admin

import androidx.lifecycle.viewModelScope
import com.app.campgear.core.PagedViewModel
import com.app.campgear.data.ApiResult
import com.app.campgear.data.PagedResponse
import com.app.campgear.data.UnitAlatResponse
import com.app.campgear.data.repository.UnitAlatRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class UnitAction {
    DELETE
}

class UnitAlatViewModel(
    private val repository: UnitAlatRepository
) : PagedViewModel<UnitAlatResponse>() {

    private var alatId: Long = 0
    var selectedAvailability: Long? = null
        private set

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _actions = Channel<UnitAction>(Channel.BUFFERED)
    val actions = _actions.receiveAsFlow()

    fun initialize(idAlat: Long) {
        alatId = idAlat
        start()
    }

    protected override suspend fun fetchPage(page: Int): ApiResult<PagedResponse<UnitAlatResponse>> =
        repository.getUnits(alatId, page, selectedAvailability)

    fun setAvailability(value: Long?) {
        if (value != selectedAvailability) {
            selectedAvailability = value
            reload()
        }
    }

    fun delete(unitId: Long) {
        perform(UnitAction.DELETE) { repository.delete(unitId) }
    }

    fun clearActionError() {
        _actionError.value = null
    }

    private fun perform(action: UnitAction, request: suspend () -> ApiResult<Unit>) {
        if (_busy.value) return
        _busy.value = true
        _actionError.value = null
        viewModelScope.launch {
            when (val result = request()) {
                is ApiResult.Success -> {
                    _busy.value = false
                    _actions.send(action)
                    refresh()
                }

                is ApiResult.Failure -> {
                    _busy.value = false
                    _actionError.value = result.message
                }
            }
        }
    }
}
