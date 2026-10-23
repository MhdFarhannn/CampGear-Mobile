package com.app.campgear.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.campgear.core.Formatters
import com.app.campgear.data.ApiResult
import com.app.campgear.data.PeminjamanItemRequest
import com.app.campgear.data.PeminjamanRequest
import com.app.campgear.data.PeminjamanResponse
import com.app.campgear.data.repository.AlatRepository
import com.app.campgear.data.repository.PeminjamanRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditLoanItem(
    val idAlat: Long,
    val nama: String,
    val hargaSewa: Double,
    val jumlah: Int
)

data class EditLoanUiState(
    val loan: PeminjamanResponse? = null,
    val items: List<EditLoanItem> = emptyList(),
    val loanDate: String = "",
    val returnDate: String = "",
    val loading: Boolean = false,
    val loadError: String? = null,
    val saving: Boolean = false,
    val saveError: String? = null,
    val completed: Boolean = false
) {
    val days: Int
        get() = Formatters.daysBetween(loanDate, returnDate)

    val total: Double
        get() = items.sumOf { it.hargaSewa * it.jumlah } * days
}

class EditLoanViewModel(
    private val repository: PeminjamanRepository,
    private val alatRepository: AlatRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EditLoanUiState())
    val state: StateFlow<EditLoanUiState> = _state.asStateFlow()

    private val _saved = Channel<Unit>(Channel.BUFFERED)
    val saved = _saved.receiveAsFlow()

    private var loanId: Long = 0
    private var loadJob: Job? = null

    fun initialize(id: Long) {
        if (loanId == id && (_state.value.loan != null || loadJob?.isActive == true)) return
        load(id)
    }

    fun load(id: Long) {
        loanId = id
        loadJob?.cancel()
        _state.update { it.copy(loading = true, loadError = null) }
        loadJob = viewModelScope.launch {
            when (val result = repository.getById(id)) {
                is ApiResult.Success -> {
                    val loan = result.data
                    if (loan.status != "diajukan") {
                        _state.update { it.copy(loading = false, loadError = com.app.campgear.core.AppStrings.get(com.app.campgear.R.string.error_loan_state_changed)) }
                        return@launch
                    }
                    val priceResults = coroutineScope {
                        loan.details.map { detail ->
                            async {
                                detail.idAlat to alatRepository.getAlatById(detail.idAlat)
                            }
                        }.awaitAll()
                    }.toMap()
                    val failure = priceResults.values.filterIsInstance<ApiResult.Failure>().firstOrNull()
                    if (failure != null) {
                        _state.update { it.copy(loading = false, loadError = failure.message) }
                        return@launch
                    }
                    val currentPrices = priceResults.mapValues { (_, result) ->
                        (result as ApiResult.Success).data.hargaSewa
                    }
                    _state.update {
                        it.copy(
                            loan = loan,
                            items = loan.details.map { detail ->
                                EditLoanItem(
                                    idAlat = detail.idAlat,
                                    nama = detail.alat.orEmpty(),
                                    hargaSewa = currentPrices[detail.idAlat]
                                        ?: detail.hargaSewaSaatTransaksi,
                                    jumlah = detail.jumlah
                                )
                            },
                            loanDate = loan.tanggalPeminjaman,
                            returnDate = loan.tanggalPengembalian,
                            loading = false,
                            loadError = null
                        )
                    }
                }

                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, loadError = result.message)
                }
            }
        }
    }

    fun setLoanDate(value: String) {
        if (_state.value.saving) return
        _state.update { it.copy(loanDate = value, saveError = null) }
    }

    fun setReturnDate(value: String) {
        if (_state.value.saving) return
        _state.update { it.copy(returnDate = value, saveError = null) }
    }

    fun increase(idAlat: Long) {
        if (_state.value.saving) return
        _state.update { current ->
            current.copy(
                items = current.items.map {
                    if (it.idAlat == idAlat && it.jumlah < MAX_QUANTITY) {
                        it.copy(jumlah = it.jumlah + 1)
                    } else {
                        it
                    }
                }
            )
        }
    }

    fun decrease(idAlat: Long) {
        if (_state.value.saving) return
        _state.update { current ->
            current.copy(
                items = current.items.map {
                    if (it.idAlat == idAlat && it.jumlah > 1) {
                        it.copy(jumlah = it.jumlah - 1)
                    } else {
                        it
                    }
                }
            )
        }
    }

    fun remove(idAlat: Long) {
        if (!_state.value.saving) _state.update { it.copy(items = it.items.filterNot { item -> item.idAlat == idAlat }) }
    }

    fun add(alat: com.app.campgear.data.AlatResponse) {
        if (_state.value.saving) return
        if (_state.value.items.any { it.idAlat == alat.id }) increase(alat.id)
        else _state.update { it.copy(items = it.items + EditLoanItem(alat.id, alat.nama, alat.hargaSewa, 1)) }
    }

    fun save() {
        val current = _state.value
        if (current.saving || current.completed || current.loading || current.loan == null || current.items.isEmpty()) return
        if (!Formatters.isValidDate(current.loanDate) || !Formatters.isValidDate(current.returnDate) ||
            current.returnDate < current.loanDate || current.items.any { it.jumlah !in 1..1000 }
        ) return

        _state.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            val request = PeminjamanRequest(
                tanggalPeminjaman = current.loanDate,
                tanggalPengembalian = current.returnDate,
                items = current.items.map { PeminjamanItemRequest(it.idAlat, it.jumlah) }
            )
            when (val result = repository.update(loanId, request)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(saving = false, completed = true, saveError = null) }
                    _saved.send(Unit)
                }

                is ApiResult.Failure -> _state.update {
                    it.copy(saving = false, saveError = result.message)
                }
            }
        }
    }

    private companion object {
        const val MAX_QUANTITY = 1000
    }
}
