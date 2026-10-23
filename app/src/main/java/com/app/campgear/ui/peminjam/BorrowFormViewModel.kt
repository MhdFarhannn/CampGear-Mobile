package com.app.campgear.ui.peminjam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.campgear.core.Formatters
import com.app.campgear.data.ApiResult
import com.app.campgear.data.PeminjamanItemRequest
import com.app.campgear.data.PeminjamanRequest
import com.app.campgear.data.repository.PeminjamanRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BorrowFormUiState(
    val loanDate: String = Formatters.todayIso(),
    val returnDate: String = Formatters.addDaysIso(Formatters.todayIso(), 2),
    val submitting: Boolean = false,
    val error: String? = null,
    val completed: Boolean = false
)

class BorrowFormViewModel(
    private val repository: PeminjamanRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BorrowFormUiState())
    val state: StateFlow<BorrowFormUiState> = _state.asStateFlow()

    private val _submitted = Channel<Unit>(Channel.BUFFERED)
    val submitted = _submitted.receiveAsFlow()

    fun setLoanDate(value: String) {
        if (_state.value.submitting || _state.value.completed) return
        _state.update { it.copy(loanDate = value, error = null) }
    }

    fun setReturnDate(value: String) {
        if (_state.value.submitting || _state.value.completed) return
        _state.update { it.copy(returnDate = value, error = null) }
    }

    fun submit(items: List<CartItem>) {
        val current = _state.value
        if (current.submitting || current.completed) return
        if (items.isEmpty() || items.any { it.jumlah !in 1..1000 || it.jumlah > it.stok } ||
            !Formatters.isValidDate(current.loanDate) || !Formatters.isValidDate(current.returnDate) || current.returnDate < current.loanDate
        ) return

        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            val request = PeminjamanRequest(
                tanggalPeminjaman = current.loanDate,
                tanggalPengembalian = current.returnDate,
                items = items.map { PeminjamanItemRequest(it.alatId, it.jumlah) }
            )
            when (val result = repository.create(request)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(submitting = false, completed = true, error = null) }
                    _submitted.send(Unit)
                }

                is ApiResult.Failure -> _state.update {
                    it.copy(submitting = false, error = result.message)
                }
            }
        }
    }

}
