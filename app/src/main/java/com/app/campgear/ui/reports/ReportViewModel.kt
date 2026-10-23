package com.app.campgear.ui.reports

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.campgear.R
import com.app.campgear.core.AppStrings
import com.app.campgear.core.Formatters
import com.app.campgear.data.ApiResult
import com.app.campgear.data.LaporanPeminjamanResponse
import com.app.campgear.data.LaporanPengembalianResponse
import com.app.campgear.data.repository.LaporanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class ReportType { LOANS, RETURNS }
sealed interface ReportData {
    data class Loans(val value: LaporanPeminjamanResponse) : ReportData
    data class Returns(val value: LaporanPengembalianResponse) : ReportData
}
data class ReportUiState(
    val type: ReportType = ReportType.LOANS,
    val from: String = Formatters.firstDayOfMonthIso(), val to: String = Formatters.todayIso(),
    val loading: Boolean = false, val error: String? = null, val data: ReportData? = null,
    val sharing: Boolean = false, val shareError: String? = null, val csvFile: File? = null
)

class ReportViewModel(private val repository: LaporanRepository, context: Context) : ViewModel() {
    private val cacheDirectory = File(context.applicationContext.cacheDir, "reports")
    private val _state = MutableStateFlow(ReportUiState())
    val state = _state.asStateFlow()
    private var loadJob: Job? = null
    private var exportJob: Job? = null
    private var generation = 0

    fun selectType(type: ReportType) { if (type != _state.value.type) change { it.copy(type = type) } }
    fun setRange(from: String, to: String) { change { it.copy(from = from, to = to) } }

    private fun change(transform: (ReportUiState) -> ReportUiState) {
        generation++
        loadJob?.cancel()
        exportJob?.cancel()
        _state.update { transform(it).copy(data = null, loading = false, sharing = false, csvFile = null, error = null, shareError = null) }
    }

    fun load() {
        val current = _state.value
        if (!Formatters.isValidDate(current.from) || !Formatters.isValidDate(current.to) || current.to < current.from) return
        loadJob?.cancel()
        exportJob?.cancel()
        val requestGeneration = ++generation
        _state.update { it.copy(loading = true, sharing = false, csvFile = null, error = null, shareError = null) }
        loadJob = viewModelScope.launch {
            val result: ApiResult<ReportData> = when (current.type) {
                ReportType.LOANS -> when (val response = repository.getPeminjaman(current.from, current.to)) {
                    is ApiResult.Success -> ApiResult.Success(ReportData.Loans(response.data))
                    is ApiResult.Failure -> response
                }
                ReportType.RETURNS -> when (val response = repository.getPengembalian(current.from, current.to)) {
                    is ApiResult.Success -> ApiResult.Success(ReportData.Returns(response.data))
                    is ApiResult.Failure -> response
                }
            }
            if (generation != requestGeneration) return@launch
            when (result) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, data = result.data) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    fun requestCsv() {
        val current = _state.value
        if (current.sharing || current.data == null || current.loading) return
        val requestGeneration = generation
        _state.update { it.copy(sharing = true, shareError = null) }
        exportJob = viewModelScope.launch {
            val result = when (current.type) {
                ReportType.LOANS -> repository.downloadPeminjamanCsv(current.from, current.to)
                ReportType.RETURNS -> repository.downloadPengembalianCsv(current.from, current.to)
            }
            when (result) {
                is ApiResult.Failure -> if (generation == requestGeneration) _state.update { it.copy(sharing = false, shareError = result.message) }
                is ApiResult.Success -> {
                    try {
                        val file = withContext(Dispatchers.IO) {
                            cacheDirectory.mkdirs()
                            val prefix = if (current.type == ReportType.LOANS) "peminjaman" else "pengembalian"
                            File(cacheDirectory, "$prefix-${current.from}_${current.to}.csv").also { target ->
                                result.data.use { body -> body.byteStream().use { input -> target.outputStream().use(input::copyTo) } }
                            }
                        }
                        if (generation == requestGeneration) _state.update { it.copy(sharing = false, csvFile = file) }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        result.data.close()
                        throw e
                    } catch (e: Exception) {
                        result.data.close()
                        if (generation == requestGeneration) _state.update { it.copy(sharing = false, shareError = AppStrings.get(R.string.report_share_error)) }
                    }
                }
            }
        }
    }

    fun consumeCsv() { _state.update { it.copy(csvFile = null) } }
}
