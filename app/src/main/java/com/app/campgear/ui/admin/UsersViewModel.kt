package com.app.campgear.ui.admin

import androidx.lifecycle.viewModelScope
import com.app.campgear.core.PagedViewModel
import com.app.campgear.data.ApiResult
import com.app.campgear.data.PagedResponse
import com.app.campgear.data.UserResponse
import com.app.campgear.data.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class UserAction {
    DELETE
}

class UsersViewModel(
    private val repository: UserRepository
) : PagedViewModel<UserResponse>() {

    private var search: String? = null
    private var searchJob: Job? = null

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _actions = Channel<UserAction>(Channel.BUFFERED)
    val actions = _actions.receiveAsFlow()

    protected override suspend fun fetchPage(page: Int): ApiResult<PagedResponse<UserResponse>> =
        repository.getUsers(page, search)

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
        perform(UserAction.DELETE) { repository.delete(id) }
    }

    fun clearActionError() {
        _actionError.value = null
    }

    private fun perform(action: UserAction, request: suspend () -> ApiResult<Unit>) {
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
