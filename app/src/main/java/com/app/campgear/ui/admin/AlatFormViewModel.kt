package com.app.campgear.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.campgear.data.AlatResponse
import com.app.campgear.data.ApiResult
import com.app.campgear.data.Kategori
import com.app.campgear.data.repository.AlatRepository
import com.app.campgear.data.repository.KategoriRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import android.content.Context
import android.net.Uri

data class SelectedImage(
    val file: File,
    val mime: String,
    val displayName: String
)

data class AlatFormUiState(
    val alat: AlatResponse? = null,
    val categories: List<Kategori> = emptyList(),
    val image: SelectedImage? = null,
    val loading: Boolean = false,
    val loadError: String? = null,
    val initialized: Boolean = false,
    val nama: String = "",
    val deskripsi: String = "",
    val harga: String = "",
    val idKategori: Long? = null,
    val copyingImage: Boolean = false,
    val saving: Boolean = false,
    val saveError: String? = null,
    val completed: Boolean = false,
    val fieldErrors: Map<String, String> = emptyMap()
)

class AlatFormViewModel(
    private val alatRepository: AlatRepository,
    private val kategoriRepository: KategoriRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AlatFormUiState())
    val state: StateFlow<AlatFormUiState> = _state.asStateFlow()

    private val _saved = Channel<Unit>(Channel.BUFFERED)
    val saved = _saved.receiveAsFlow()

    fun initialize(alatId: Long) {
        if (!_state.value.initialized && !_state.value.loading) load(alatId)
    }

    fun load(alatId: Long) {
        _state.update { it.copy(loading = true, loadError = null) }
        viewModelScope.launch {
            val categories = when (val result = kategoriRepository.getAll()) {
                is ApiResult.Success -> result.data
                is ApiResult.Failure -> {
                    _state.update { it.copy(loading = false, loadError = result.message) }
                    return@launch
                }
            }

            var alat: AlatResponse? = null
            if (alatId != 0L) {
                when (val result = alatRepository.getAlatById(alatId)) {
                    is ApiResult.Success -> alat = result.data
                    is ApiResult.Failure -> {
                        _state.update { it.copy(loading = false, loadError = result.message) }
                        return@launch
                    }
                }
            }

            _state.update {
                it.copy(
                    alat = alat,
                    categories = categories,
                    loading = false,
                    loadError = null,
                    initialized = true,
                    nama = if (it.initialized) it.nama else alat?.nama.orEmpty(),
                    deskripsi = if (it.initialized) it.deskripsi else alat?.deskripsi.orEmpty(),
                    harga = if (it.initialized) it.harga else alat?.hargaSewa?.let { value ->
                        java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
                    }.orEmpty(),
                    idKategori = if (it.initialized) it.idKategori else alat?.idKategori
                )
            }
        }
    }

    fun setImage(image: SelectedImage) {
        _state.value.image?.file?.takeIf { it != image.file }?.delete()
        _state.update { it.copy(image = image) }
    }

    fun setName(value: String) {
        if (!_state.value.saving) _state.update { it.copy(nama = value, saveError = null, fieldErrors = it.fieldErrors - "nama") }
    }
    fun setDescription(value: String) { if (!_state.value.saving) _state.update { it.copy(deskripsi = value) } }
    fun setPrice(value: String) {
        if (!_state.value.saving) _state.update { it.copy(harga = value, saveError = null, fieldErrors = it.fieldErrors - "hargaSewa") }
    }
    fun setCategory(value: Long?) {
        if (!_state.value.saving) _state.update { it.copy(idKategori = value, saveError = null, fieldErrors = it.fieldErrors - "idKategori") }
    }

    fun selectImage(context: Context, uri: Uri) {
        if (_state.value.copyingImage || _state.value.saving || _state.value.completed) return
        val applicationContext = context.applicationContext
        _state.update { it.copy(copyingImage = true, saveError = null) }
        viewModelScope.launch {
            when (val result = copySelectedImage(applicationContext, uri)) {
                is ApiResult.Success -> setImage(result.data)
                is ApiResult.Failure -> _state.update { it.copy(saveError = result.message) }
            }
            _state.update { it.copy(copyingImage = false) }
        }
    }

    fun save(
        alatId: Long,
        idKategori: Long,
        nama: String,
        deskripsi: String?,
        hargaSewa: String
    ) {
        val current = _state.value
        if (current.saving || current.copyingImage || current.loading || current.completed) return

        _state.update { it.copy(saving = true, saveError = null, fieldErrors = emptyMap()) }
        viewModelScope.launch {
            val image = current.image
            val result = if (alatId == 0L) {
                alatRepository.create(
                    idKategori = idKategori,
                    nama = nama,
                    deskripsi = deskripsi,
                    hargaSewa = hargaSewa,
                    gambar = image?.file,
                    gambarMime = image?.mime
                )
            } else {
                alatRepository.update(
                    id = alatId,
                    idKategori = idKategori,
                    nama = nama,
                    deskripsi = deskripsi,
                    hargaSewa = hargaSewa,
                    gambar = image?.file,
                    gambarMime = image?.mime
                )
            }

            when (result) {
                is ApiResult.Success -> {
                    _state.update { it.copy(saving = false, completed = true, saveError = null) }
                    _saved.send(Unit)
                }

                is ApiResult.Failure -> _state.update {
                    it.copy(saving = false, saveError = result.message, fieldErrors = result.fieldErrors)
                }
            }
        }
    }

    override fun onCleared() {
        _state.value.image?.file?.delete()
    }
}
