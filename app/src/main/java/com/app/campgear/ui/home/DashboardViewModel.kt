package com.app.campgear.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.campgear.core.SessionManager
import com.app.campgear.data.ApiResult
import com.app.campgear.data.repository.DashboardMetric
import com.app.campgear.data.repository.DashboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardState(val loading: Boolean = false, val metrics: List<DashboardMetric> = emptyList(), val error: String? = null)

class DashboardViewModel(private val repository: DashboardRepository, private val sessions: SessionManager) : ViewModel() {
    private val _state = MutableStateFlow(DashboardState())
    val state = _state.asStateFlow()
    fun load() {
        if (_state.value.loading) return
        val role = sessions.session.value?.role ?: return
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            _state.value = when (val result = repository.load(role)) {
                is ApiResult.Success -> DashboardState(metrics = result.data)
                is ApiResult.Failure -> _state.value.copy(loading = false, error = result.message)
            }
        }
    }
}
