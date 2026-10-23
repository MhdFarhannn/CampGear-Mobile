package com.app.campgear.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.campgear.data.ApiResult
import com.app.campgear.data.UserResponse
import com.app.campgear.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: UserResponse? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val loggingOut: Boolean = false
)

class ProfileViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private var started = false

    fun start() {
        if (started) return
        started = true
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.me()) {
                is ApiResult.Success -> _state.update {
                    it.copy(user = result.data, loading = false, error = null)
                }

                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = result.message)
                }
            }
        }
    }

    fun logout() {
        if (_state.value.loggingOut) return
        _state.update { it.copy(loggingOut = true) }
        viewModelScope.launch {
            repository.logout()
            _state.update { it.copy(loggingOut = false) }
        }
    }
}
