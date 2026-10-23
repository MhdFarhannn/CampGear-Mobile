package com.app.campgear.ui.peminjam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.campgear.data.AlatResponse
import com.app.campgear.data.ApiResult
import com.app.campgear.data.repository.AlatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EquipmentDetailState(val alat: AlatResponse? = null, val loading: Boolean = false, val error: String? = null)

class EquipmentDetailViewModel(private val repository: AlatRepository) : ViewModel() {
    private val _state = MutableStateFlow(EquipmentDetailState())
    val state = _state.asStateFlow()
    private var id = 0L
    fun initialize(alatId: Long) { if (id != alatId || (_state.value.alat == null && !_state.value.loading)) load(alatId) }
    fun load(alatId: Long = id) {
        if (_state.value.loading) return
        id = alatId
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            _state.value = when (val result = repository.getAlatById(alatId)) {
                is ApiResult.Success -> EquipmentDetailState(alat = result.data)
                is ApiResult.Failure -> _state.value.copy(loading = false, error = result.message)
            }
        }
    }
}
