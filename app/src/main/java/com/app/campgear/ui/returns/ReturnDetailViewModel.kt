package com.app.campgear.ui.returns

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.campgear.data.ApiResult
import com.app.campgear.data.PengembalianResponse
import com.app.campgear.data.repository.PengembalianRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReturnDetailUiState(
    val data: PengembalianResponse? = null,
    val loading: Boolean = false,
    val error: String? = null
)

class ReturnDetailViewModel(
    private val repository: PengembalianRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ReturnDetailUiState())
    val state: StateFlow<ReturnDetailUiState> = _state.asStateFlow()
    private var loadedId: Long = 0

    fun initialize(id: Long) {
        if (loadedId == id && (_state.value.data != null || _state.value.loading)) return
        loadedId = id
        load(id)
    }

    fun load(id: Long) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.getById(id)) {
                is ApiResult.Success -> _state.update {
                    it.copy(data = result.data, loading = false, error = null)
                }

                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = result.message)
                }
            }
        }
    }
}
