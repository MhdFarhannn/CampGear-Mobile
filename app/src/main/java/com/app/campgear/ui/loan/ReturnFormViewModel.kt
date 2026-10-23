package com.app.campgear.ui.loan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.campgear.core.Formatters
import com.app.campgear.core.MasterOptions
import com.app.campgear.data.ApiResult
import com.app.campgear.data.PeminjamanResponse
import com.app.campgear.data.PengembalianRequest
import com.app.campgear.data.PengembalianUnitRequest
import com.app.campgear.data.repository.PeminjamanRepository
import com.app.campgear.data.repository.PengembalianRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReturnUnitInput(
    val idUnitAlat: Long,
    val kodeInventaris: String,
    val alat: String,
    val kondisi: String = "baik",
    val catatan: String = ""
)

data class ReturnFormUiState(
    val loan: PeminjamanResponse? = null,
    val units: List<ReturnUnitInput> = emptyList(),
    val loading: Boolean = false,
    val loadError: String? = null,
    val returnDate: String = Formatters.todayIso(),
    val note: String = "",
    val submitting: Boolean = false,
    val submitError: String? = null,
    val unitsMissing: Boolean = false,
    val completed: Boolean = false
) {
    val lateDays: Int
        get() = Formatters.lateDays(loan?.tanggalPengembalian, returnDate)

    val lateFine: Double
        get() = lateDays * MasterOptions.LATE_FINE_PER_DAY

    val conditionFine: Double
        get() = units.sumOf { MasterOptions.conditionFine(it.kondisi) }

    val estimatedFine: Double
        get() = lateFine + conditionFine
}

class ReturnFormViewModel(
    private val peminjamanRepository: PeminjamanRepository,
    private val pengembalianRepository: PengembalianRepository,
    private val sessions: com.app.campgear.core.SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(ReturnFormUiState())
    val state: StateFlow<ReturnFormUiState> = _state.asStateFlow()

    private val _submitted = Channel<Long?>(Channel.BUFFERED)
    val submitted = _submitted.receiveAsFlow()

    private var loanId: Long = 0
    private var initialized = false
    private var loadJob: Job? = null

    fun initialize(id: Long) {
        if (loanId == id && (initialized || loadJob?.isActive == true)) return
        load(id)
    }

    fun load(id: Long) {
        loanId = id
        loadJob?.cancel()
        _state.update { it.copy(loading = true, loadError = null) }
        loadJob = viewModelScope.launch {
            when (val result = peminjamanRepository.getById(id)) {
                is ApiResult.Success -> {
                    val loan = result.data
                    val draft = _state.value.units.associateBy { it.idUnitAlat }
                    val units = loan.details.flatMap { detail ->
                        detail.idUnitAlat.mapIndexed { index, unitId ->
                            draft[unitId] ?: ReturnUnitInput(
                                idUnitAlat = unitId,
                                kodeInventaris = detail.kodeInventaris.getOrNull(index).orEmpty(),
                                alat = detail.alat.orEmpty()
                            )
                        }
                    }
                    val defaultDate = if (!initialized) {
                        val today = Formatters.todayIso()
                        if (today < loan.tanggalPeminjaman) loan.tanggalPeminjaman else today
                    } else {
                        _state.value.returnDate
                    }
                    initialized = true
                    _state.update {
                        it.copy(
                            loan = loan,
                            units = units,
                            loading = false,
                            loadError = null,
                            returnDate = defaultDate,
                            unitsMissing = units.isEmpty() || loan.status != "dipinjam" ||
                                units.size != loan.details.sumOf { detail -> detail.jumlah } ||
                                units.distinctBy { unit -> unit.idUnitAlat }.size != units.size
                        )
                    }
                }

                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, loadError = result.message)
                }
            }
        }
    }

    fun setReturnDate(value: String) {
        if (_state.value.submitting || _state.value.completed) return
        _state.update { it.copy(returnDate = value, submitError = null) }
    }

    fun setNote(value: String) {
        if (_state.value.submitting || _state.value.completed) return
        _state.update { it.copy(note = value) }
    }

    fun setUnitCondition(unitId: Long, kondisi: String) {
        if (_state.value.submitting || kondisi !in setOf("baik", "rusak_ringan", "rusak_berat", "hilang")) return
        _state.update { current ->
            current.copy(
                units = current.units.map {
                    if (it.idUnitAlat == unitId) it.copy(kondisi = kondisi) else it
                }
            )
        }
    }

    fun setUnitNote(unitId: Long, note: String) {
        if (_state.value.submitting) return
        _state.update { current ->
            current.copy(
                units = current.units.map {
                    if (it.idUnitAlat == unitId) it.copy(catatan = note) else it
                }
            )
        }
    }

    fun submit() {
        val current = _state.value
        if (current.submitting || current.completed || current.unitsMissing || current.units.isEmpty() || current.loan == null) return
        if (!Formatters.isValidDate(current.returnDate) || current.returnDate < current.loan.tanggalPeminjaman) return

        _state.update { it.copy(submitting = true, submitError = null) }
        viewModelScope.launch {
            val request = PengembalianRequest(
                idPeminjaman = loanId,
                tanggalKembali = current.returnDate,
                catatan = current.note.trim().ifBlank { null },
                units = current.units.map {
                    PengembalianUnitRequest(
                        idUnitAlat = it.idUnitAlat,
                        statusKondisi = it.kondisi,
                        catatan = it.catatan.trim().ifBlank { null }
                    )
                }
            )
            when (val result = pengembalianRepository.create(request)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(completed = true, submitError = null) }
                    val receipt = if (sessions.session.value?.role in setOf("Admin", "Petugas")) {
                        when (val response = pengembalianRepository.findByLoan(loanId, current.returnDate)) {
                            is ApiResult.Success -> response.data?.id
                            is ApiResult.Failure -> null
                        }
                    } else null
                    _state.update { it.copy(submitting = false) }
                    _submitted.send(receipt)
                }

                is ApiResult.Failure -> _state.update {
                    it.copy(submitting = false, submitError = result.message)
                }
            }
        }
    }
}
