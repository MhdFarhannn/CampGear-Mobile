package com.app.campgear.ui.peminjam

import androidx.lifecycle.viewModelScope
import com.app.campgear.core.PagedViewModel
import com.app.campgear.data.AlatResponse
import com.app.campgear.data.ApiResult
import com.app.campgear.data.Kategori
import com.app.campgear.data.PagedResponse
import com.app.campgear.data.repository.AlatRepository
import com.app.campgear.data.repository.KategoriRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CategoryUiState(
    val items: List<Kategori> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

class CatalogViewModel(
    private val alatRepository: AlatRepository,
    private val kategoriRepository: KategoriRepository
) : PagedViewModel<AlatResponse>() {

    private var search: String? = null
    private var idKategori: Long? = null
    private var searchJob: Job? = null
    private var categoriesLoaded = false

    private val _categories = MutableStateFlow(CategoryUiState())
    val categories: StateFlow<CategoryUiState> = _categories.asStateFlow()

    val selectedKategoriId: Long?
        get() = idKategori

    fun initialize() {
        start()
        if (!categoriesLoaded) {
            categoriesLoaded = true
            loadCategories()
        }
    }

    protected override suspend fun fetchPage(page: Int): ApiResult<PagedResponse<AlatResponse>> =
        alatRepository.getAlat(page, search, idKategori)

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

    fun setKategori(kategoriId: Long?) {
        if (idKategori == kategoriId) return
        idKategori = kategoriId
        reload()
    }

    fun loadCategories() {
        _categories.value = CategoryUiState(loading = true)
        viewModelScope.launch {
            when (val result = kategoriRepository.getAll()) {
                is ApiResult.Success -> _categories.value =
                    CategoryUiState(items = result.data, loading = false)

                is ApiResult.Failure -> _categories.value =
                    CategoryUiState(loading = false, error = result.message)
            }
        }
    }
}
