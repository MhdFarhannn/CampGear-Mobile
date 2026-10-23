package com.app.campgear.ui.loan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.campgear.data.ApiResult
import com.app.campgear.data.PeminjamanResponse
import com.app.campgear.data.repository.PeminjamanRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LoanAction {
    APPROVE,
    REJECT,
    HANDOVER,
    DELETE
}

data class LoanDetailUiState(
    val loan: PeminjamanResponse? = null,
    val loading: Boolean = false,
    val acting: Boolean = false,
    val error: String? = null,
    val actionError: String? = null
)

class LoanDetailViewModel(
    private val repository: PeminjamanRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LoanDetailUiState())
    val state: StateFlow<LoanDetailUiState> = _state.asStateFlow()

    private val _actions = Channel<LoanAction>(Channel.BUFFERED)
    val actions = _actions.receiveAsFlow()

    private var loanId: Long = 0
    private var loadJob: Job? = null

    fun initialize(id: Long) {
        if (loanId == id && (_state.value.loan != null || loadJob?.isActive == true)) return
        load(id)
    }

    fun load(id: Long) {
        loanId = id
        loadJob?.cancel()
        _state.update { it.copy(loading = true, error = null) }
        loadJob = viewModelScope.launch {
            when (val result = repository.getById(id)) {
                is ApiResult.Success -> _state.update {
                    it.copy(loan = result.data, loading = false, error = null)
                }

                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = result.message)
                }
            }
        }
    }

    fun refresh() {
        if (loanId != 0L && !_state.value.acting && loadJob?.isActive != true) load(loanId)
    }

    fun approve() = perform(LoanAction.APPROVE) { repository.approve(loanId) }

    fun reject(note: String?) = perform(LoanAction.REJECT) { repository.reject(loanId, note) }

    fun handover() = perform(LoanAction.HANDOVER) { repository.borrow(loanId) }

    fun delete() = perform(LoanAction.DELETE) { repository.delete(loanId) }

    fun clearActionError() {
        _state.update { it.copy(actionError = null) }
    }

    private fun perform(
        action: LoanAction,
        request: suspend () -> ApiResult<Unit>
    ) {
        if (_state.value.acting || _state.value.loading || loanId <= 0) return
        _state.update { it.copy(acting = true, actionError = null) }
        viewModelScope.launch {
            when (val result = request()) {
                is ApiResult.Success -> {
                    _state.update { it.copy(acting = false) }
                    _actions.send(action)
                    if (action != LoanAction.DELETE) {
                        load(loanId)
                    }
                }

                is ApiResult.Failure -> _state.update {
                    it.copy(acting = false, actionError = result.message)
                }
            }
        }
    }
}
