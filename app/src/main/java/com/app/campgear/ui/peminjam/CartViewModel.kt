package com.app.campgear.ui.peminjam

import androidx.lifecycle.ViewModel
import com.app.campgear.data.AlatResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.lifecycle.viewModelScope
import com.app.campgear.data.ApiResult
import com.app.campgear.data.repository.AlatRepository
import com.app.campgear.core.AppStrings
import com.app.campgear.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

data class CartItem(
    val alatId: Long,
    val nama: String,
    val hargaSewa: Double,
    val stok: Int,
    val jumlah: Int
)

class CartViewModel(private val repository: AlatRepository) : ViewModel() {

    private val _items = MutableStateFlow<List<CartItem>>(emptyList())
    val items: StateFlow<List<CartItem>> = _items.asStateFlow()
    private val _checking = MutableStateFlow(false)
    val checking = _checking.asStateFlow()
    private val _checkError = MutableStateFlow<String?>(null)
    val checkError = _checkError.asStateFlow()
    private val _validated = MutableStateFlow(false)
    val validated = _validated.asStateFlow()
    private var validationJob: Job? = null

    fun add(alat: AlatResponse): Boolean {
        if (_checking.value) return false
        var added = false
        _items.update { current ->
            val existing = current.firstOrNull { it.alatId == alat.id }
            if (existing == null) {
                if (alat.stokTersedia <= 0) {
                    current
                } else {
                    added = true
                    current + CartItem(alat.id, alat.nama, alat.hargaSewa, alat.stokTersedia, 1)
                }
            } else if (existing.jumlah >= alat.stokTersedia.coerceAtMost(1000)) {
                current
            } else {
                added = true
                current.map {
                    if (it.alatId == alat.id) it.copy(jumlah = it.jumlah + 1, stok = alat.stokTersedia, hargaSewa = alat.hargaSewa) else it
                }
            }
        }
        return added
    }

    fun increase(alatId: Long): Boolean {
        if (_checking.value) return false
        var increased = false
        _items.update { current ->
            current.map {
                if (it.alatId == alatId && it.jumlah < it.stok.coerceAtMost(1000)) {
                    increased = true
                    it.copy(jumlah = it.jumlah + 1)
                } else {
                    it
                }
            }
        }
        return increased
    }

    fun decrease(alatId: Long) {
        if (_checking.value) return
        _items.update { current ->
            current.mapNotNull {
                if (it.alatId == alatId) {
                    if (it.jumlah > 1) it.copy(jumlah = it.jumlah - 1) else null
                } else {
                    it
                }
            }
        }
    }

    fun remove(alatId: Long) {
        if (_checking.value) return
        _items.update { current -> current.filterNot { it.alatId == alatId } }
    }

    fun clear() {
        validationJob?.cancel()
        _checking.value = false
        _validated.value = false
        _checkError.value = null
        _items.value = emptyList()
    }

    fun revalidate() {
        if (_checking.value || _items.value.isEmpty()) return
        _checking.value = true
        _validated.value = false
        _checkError.value = null
        val snapshot = _items.value
        validationJob = viewModelScope.launch {
            val refreshed = mutableListOf<CartItem>()
            var changed = false
            for (item in snapshot) {
                when (val result = repository.getAlatById(item.alatId)) {
                    is ApiResult.Failure -> {
                        _checkError.value = result.message
                        _checking.value = false
                        return@launch
                    }
                    is ApiResult.Success -> {
                        val alat = result.data
                        val quantity = item.jumlah.coerceAtMost(alat.stokTersedia.coerceAtMost(1000))
                        if (quantity != item.jumlah || alat.hargaSewa != item.hargaSewa) changed = true
                        if (quantity > 0) refreshed += item.copy(nama = alat.nama, stok = alat.stokTersedia, hargaSewa = alat.hargaSewa, jumlah = quantity)
                    }
                }
            }
            if (_items.value == snapshot) _items.value = refreshed
            _validated.value = true
            if (changed) _checkError.value = AppStrings.get(R.string.cart_updated)
            _checking.value = false
        }
    }
}
