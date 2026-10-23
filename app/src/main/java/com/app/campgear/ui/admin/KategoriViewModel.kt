package com.app.campgear.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.campgear.data.ApiResult
import com.app.campgear.data.Kategori
import com.app.campgear.data.repository.KategoriRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class KategoriAction {
    SAVE,
    DELETE
}

data class KategoriUiState(
    val items: List<Kategori> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val busy: Boolean = false
)

class KategoriViewModel(
    private val repository: KategoriRepository
) : ViewModel() {

    private val _state = MutableStateFlow(KategoriUiState())
    val state: StateFlow<KategoriUiState> = _state.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val _actions = Channel<KategoriAction>(Channel.BUFFERED)
    val actions = _actions.receiveAsFlow()
    private var started = false

    fun start() { if (!started) { started = true; load() } }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.getAll()) {
                is ApiResult.Success -> _state.update {
                    it.copy(items = result.data, loading = false, error = null)
                }

                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = result.message)
                }
            }
        }
    }

    fun create(nama: String) {
        perform(KategoriAction.SAVE) { repository.create(nama) }
    }

    fun update(id: Long, nama: String) {
        perform(KategoriAction.SAVE) { repository.update(id, nama) }
    }

    fun delete(id: Long) {
        perform(KategoriAction.DELETE) { repository.delete(id) }
    }

    fun clearActionError() { _actionError.value = null }

    private fun perform(action: KategoriAction, request: suspend () -> ApiResult<Unit>) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        _actionError.value = null
        viewModelScope.launch {
            when (val result = request()) {
                is ApiResult.Success -> {
                    _state.update { it.copy(busy = false) }
                    _actions.send(action)
                    load()
                }

                is ApiResult.Failure -> {
                    _state.update { it.copy(busy = false) }
                    _actionError.value = result.message
                }
            }
        }
    }
}
