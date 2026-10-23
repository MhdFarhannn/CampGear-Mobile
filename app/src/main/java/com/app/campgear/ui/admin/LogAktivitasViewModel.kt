package com.app.campgear.ui.admin

import androidx.lifecycle.viewModelScope
import com.app.campgear.core.PagedViewModel
import com.app.campgear.data.ApiResult
import com.app.campgear.data.LogAktivitas
import com.app.campgear.data.PagedResponse
import com.app.campgear.data.UserResponse
import com.app.campgear.data.repository.LogRepository
import com.app.campgear.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LogFilter(
    val idUser: Long? = null,
    val from: String? = null,
    val to: String? = null
)

class LogAktivitasViewModel(
    private val repository: LogRepository,
    private val userRepository: UserRepository
) : PagedViewModel<LogAktivitas>() {

    private val _filter = MutableStateFlow(LogFilter())
    val filter: StateFlow<LogFilter> = _filter.asStateFlow()
    private var appliedFilter = LogFilter()
    private val _userError = MutableStateFlow<String?>(null)
    val userError: StateFlow<String?> = _userError.asStateFlow()

    private val _users = MutableStateFlow<List<UserResponse>>(emptyList())
    val users: StateFlow<List<UserResponse>> = _users.asStateFlow()

    init {
        loadUsers()
    }

    fun loadUsers() {
        _userError.value = null
        viewModelScope.launch {
            when (val result = userRepository.getAllForPicker()) {
                is ApiResult.Success -> _users.value = result.data
                is ApiResult.Failure -> _userError.value = result.message
            }
        }
    }

    protected override suspend fun fetchPage(page: Int): ApiResult<PagedResponse<LogAktivitas>> =
        repository.getAll(page, appliedFilter.idUser, appliedFilter.from, appliedFilter.to)

    fun setUser(idUser: Long?) {
        _filter.update { it.copy(idUser = idUser) }
    }

    fun setFrom(value: String?) {
        _filter.update { it.copy(from = value) }
    }

    fun setTo(value: String?) {
        _filter.update { it.copy(to = value) }
    }

    fun applyFilter() {
        val next = _filter.value
        if (next.from != null && next.to != null && next.to < next.from) return
        appliedFilter = next
        reload()
    }

    fun resetFilter() {
        _filter.value = LogFilter()
        appliedFilter = LogFilter()
        reload()
    }
}
