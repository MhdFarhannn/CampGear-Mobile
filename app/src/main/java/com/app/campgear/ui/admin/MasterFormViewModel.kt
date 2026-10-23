package com.app.campgear.ui.admin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.campgear.R
import com.app.campgear.core.AppStrings
import com.app.campgear.data.ApiResult
import com.app.campgear.data.UserRequest
import com.app.campgear.data.repository.KategoriRepository
import com.app.campgear.data.repository.UnitAlatRepository
import com.app.campgear.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.Serializable

data class MasterDraft(
    val name: String = "", val phone: String = "", val address: String = "", val password: String = "",
    val roleId: Long = 3, val genderId: Long? = null, val code: String = "",
    val conditionId: Long = 1, val availabilityId: Long = 1
) : Serializable

data class MasterFormState(
    val draft: MasterDraft = MasterDraft(), val loading: Boolean = false, val loadError: String? = null,
    val saving: Boolean = false, val saveError: String? = null,
    val errors: Map<String, String> = emptyMap(), val saved: Boolean = false
)

class MasterFormViewModel(
    private val users: UserRepository,
    private val categories: KategoriRepository,
    private val units: UnitAlatRepository,
    private val savedState: SavedStateHandle
) : ViewModel() {
    val kind = savedState.get<String>("kind") ?: "user"
    val entityId = savedState.get<Long>("entityId") ?: 0L
    private val alatId = savedState.get<Long>("alatId") ?: 0L
    private val _state = MutableStateFlow(MasterFormState(draft = savedState["draft"] ?: MasterDraft()))
    val state = _state.asStateFlow()
    private var locked = false

    init { load() }

    fun load() {
        if (_state.value.loading || _state.value.saving || entityId == 0L) return
        _state.update { it.copy(loading = true, loadError = null) }
        viewModelScope.launch {
            val result: ApiResult<MasterDraft> = when (kind) {
                "user" -> when (val response = users.getById(entityId)) {
                    is ApiResult.Failure -> response
                    is ApiResult.Success -> response.data.let {
                        ApiResult.Success(MasterDraft(name = it.nama, phone = it.noTelepon.orEmpty(), address = it.alamat.orEmpty(), roleId = it.idRole, genderId = it.idJenisKelamin))
                    }
                }
                "category" -> when (val response = categories.getById(entityId)) {
                    is ApiResult.Failure -> response
                    is ApiResult.Success -> ApiResult.Success(MasterDraft(name = response.data.nama))
                }
                "unit" -> when (val response = units.getById(entityId)) {
                    is ApiResult.Failure -> response
                    is ApiResult.Success -> response.data.let {
                        locked = it.statusKetersediaan == "dipinjam"
                        ApiResult.Success(MasterDraft(code = it.kodeInventaris, conditionId = it.idStatusBarang, availabilityId = it.idStatusKetersediaan))
                    }
                }
                else -> ApiResult.Failure(AppStrings.get(R.string.state_bad_response))
            }
            when (result) {
                is ApiResult.Success -> {
                    val draft = savedState.get<MasterDraft>("draft") ?: result.data
                    _state.update { it.copy(draft = draft, loading = false, loadError = if (locked) AppStrings.get(R.string.units_borrowed_locked) else null) }
                }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, loadError = result.message) }
            }
        }
    }

    fun edit(transform: (MasterDraft) -> MasterDraft) {
        if (_state.value.saving || _state.value.loading) return
        val draft = transform(_state.value.draft)
        if (draft == _state.value.draft) return
        _state.update { it.copy(draft = draft, errors = emptyMap(), saveError = null) }
        // Kata sandi bertahan di memori VM saat rotasi, tidak dimasukkan saved-state.
        savedState["draft"] = draft.copy(password = "")
    }

    fun save() {
        val current = _state.value
        if (current.saving || current.loading || current.saved || locked) return
        val draft = current.draft
        val errors = validate(draft)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            return
        }
        _state.update { it.copy(saving = true, saveError = null, errors = emptyMap()) }
        viewModelScope.launch {
            val result = when (kind) {
                "user" -> {
                    val request = UserRequest(draft.roleId, draft.genderId, draft.name.trim(), draft.password.ifBlank { null }, draft.phone.trim(), draft.address.trim().ifBlank { null })
                    if (entityId == 0L) users.create(request) else users.update(entityId, request)
                }
                "category" -> if (entityId == 0L) categories.create(draft.name.trim()) else categories.update(entityId, draft.name.trim())
                "unit" -> if (entityId == 0L) units.create(alatId, draft.code.trim(), draft.conditionId)
                    else units.update(entityId, draft.code.trim(), draft.conditionId, draft.availabilityId)
                else -> ApiResult.Failure(AppStrings.get(R.string.state_bad_response))
            }
            when (result) {
                is ApiResult.Success -> _state.update { it.copy(saving = false, saved = true) }
                is ApiResult.Failure -> _state.update {
                    val fields = result.fieldErrors.ifEmpty {
                        when {
                            kind == "user" && result.status == 409 -> mapOf("noTelepon" to result.message)
                            kind == "category" && result.status == 409 -> mapOf("nama" to result.message)
                            kind == "unit" && result.status == 409 -> mapOf("kodeInventaris" to result.message)
                            else -> emptyMap()
                        }
                    }
                    it.copy(saving = false, saveError = result.message, errors = fields)
                }
            }
        }
    }

    fun consumeSaved() { _state.update { it.copy(saved = false) } }

    private fun validate(draft: MasterDraft): Map<String, String> = buildMap {
        if (kind != "unit" && (draft.name.isBlank() || draft.name.trim().length > 100)) put("nama", AppStrings.get(R.string.validation_name_length))
        if (kind == "user") {
            val normalized = draft.phone.replace(" ", "").replace("-", "")
            if (normalized.isBlank() || draft.phone.length > 20 || !Regex("\\+?\\d+").matches(normalized)) put("noTelepon", AppStrings.get(R.string.validation_phone))
            if ((entityId == 0L && draft.password.isBlank()) || draft.password.length > 100) put("password", AppStrings.get(R.string.validation_password))
            if (draft.roleId !in 1L..3L) put("idRole", AppStrings.get(R.string.state_bad_response))
        }
        if (kind == "unit") {
            if (draft.code.isBlank() || draft.code.trim().length > 50) put("kodeInventaris", AppStrings.get(R.string.validation_unit_code))
            if (draft.conditionId !in 1L..3L) put("idStatusBarang", AppStrings.get(R.string.state_bad_response))
            if (draft.availabilityId !in setOf(1L, 3L)) put("idStatusKetersediaan", AppStrings.get(R.string.state_bad_response))
            if (entityId == 0L && alatId <= 0L) put("idAlat", AppStrings.get(R.string.state_not_found))
        }
    }
}
