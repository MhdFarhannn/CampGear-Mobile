package com.app.campgear.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.app.campgear.R
import com.app.campgear.core.MasterOptions
import com.app.campgear.databinding.FragmentMasterFormBinding
import com.app.campgear.ui.common.BaseFragment
import kotlinx.coroutines.launch

class MasterFormFragment : BaseFragment<FragmentMasterFormBinding>() {
    private val viewModel: MasterFormViewModel by viewModels { viewModelFactory }
    override fun createBinding(inflater: LayoutInflater, container: ViewGroup?) = FragmentMasterFormBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val user = binding.userFields
        val category = binding.categoryFields
        val unit = binding.unitFields
        user.root.visibility = if (viewModel.kind == "user") View.VISIBLE else View.GONE
        category.root.visibility = if (viewModel.kind == "category") View.VISIBLE else View.GONE
        unit.root.visibility = if (viewModel.kind == "unit") View.VISIBLE else View.GONE
        unit.tilUnitAvailability.visibility = if (viewModel.entityId != 0L) View.VISIBLE else View.GONE
        binding.formTitle.setText(when (viewModel.kind) {
            "category" -> if (viewModel.entityId == 0L) R.string.kategori_add_title else R.string.kategori_edit_title
            "unit" -> if (viewModel.entityId == 0L) R.string.units_add_title else R.string.units_edit_title
            else -> if (viewModel.entityId == 0L) R.string.users_add_title else R.string.users_edit_title
        })
        user.tilUserPassword.hint = getString(if (viewModel.entityId == 0L) R.string.users_password_create_hint else R.string.users_password_hint)
        if (viewModel.kind == "user") {
            user.inputUserName.doAfterTextChanged { viewModel.edit { draft -> draft.copy(name = it.toString()) } }
            user.inputUserPhone.doAfterTextChanged { viewModel.edit { draft -> draft.copy(phone = it.toString()) } }
            user.inputUserAddress.doAfterTextChanged { viewModel.edit { draft -> draft.copy(address = it.toString()) } }
            user.inputUserPassword.doAfterTextChanged { viewModel.edit { draft -> draft.copy(password = it.toString()) } }
        }
        if (viewModel.kind == "category") category.inputKategoriName.doAfterTextChanged { viewModel.edit { draft -> draft.copy(name = it.toString()) } }
        if (viewModel.kind == "unit") unit.inputUnitCode.doAfterTextChanged { viewModel.edit { draft -> draft.copy(code = it.toString()) } }
        user.dropdownUserRole.setAdapter(ArrayAdapter(requireContext(), R.layout.item_dropdown, MasterOptions.roles.map { getString(it.labelRes) }))
        user.dropdownUserRole.setOnItemClickListener { _, _, index, _ -> viewModel.edit { it.copy(roleId = MasterOptions.roles[index].id) } }
        val genders = listOf(getString(R.string.users_gender_none)) + MasterOptions.genders.map { getString(it.labelRes) }
        user.dropdownUserGender.setAdapter(ArrayAdapter(requireContext(), R.layout.item_dropdown, genders))
        user.dropdownUserGender.setOnItemClickListener { _, _, index, _ -> viewModel.edit { it.copy(genderId = MasterOptions.genders.getOrNull(index - 1)?.id) } }
        unit.dropdownUnitStatus.setAdapter(ArrayAdapter(requireContext(), R.layout.item_dropdown, MasterOptions.statusBarang.map { getString(it.labelRes) }))
        unit.dropdownUnitStatus.setOnItemClickListener { _, _, index, _ -> viewModel.edit { it.copy(conditionId = MasterOptions.statusBarang[index].id) } }
        val availability = MasterOptions.statusKetersediaan.filter { it.id != 2L }
        unit.dropdownUnitAvailability.setAdapter(ArrayAdapter(requireContext(), R.layout.item_dropdown, availability.map { getString(it.labelRes) }))
        unit.dropdownUnitAvailability.setOnItemClickListener { _, _, index, _ -> viewModel.edit { it.copy(availabilityId = availability[index].id) } }
        binding.buttonSaveMaster.setOnClickListener { viewModel.save() }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when {
                        state.loading -> binding.stateLayout.showLoading()
                        state.loadError != null -> binding.stateLayout.showError(state.loadError) { viewModel.load() }
                        else -> binding.stateLayout.showContent()
                    }
                    val draft = state.draft
                    fun bindText(input: android.widget.TextView, text: String) {
                        if (!input.hasFocus() && input.text.toString() != text) input.text = text
                    }
                    when (viewModel.kind) {
                        "user" -> {
                            bindText(user.inputUserName, draft.name)
                            bindText(user.inputUserPhone, draft.phone)
                            bindText(user.inputUserAddress, draft.address)
                            bindText(user.inputUserPassword, draft.password)
                            user.dropdownUserRole.setText(MasterOptions.roles.firstOrNull { it.id == draft.roleId }?.let { getString(it.labelRes) }.orEmpty(), false)
                            user.dropdownUserGender.setText(MasterOptions.genders.firstOrNull { it.id == draft.genderId }?.let { getString(it.labelRes) } ?: genders.first(), false)
                        }
                        "category" -> bindText(category.inputKategoriName, draft.name)
                        "unit" -> {
                            bindText(unit.inputUnitCode, draft.code)
                            unit.dropdownUnitStatus.setText(MasterOptions.statusBarang.firstOrNull { it.id == draft.conditionId }?.let { getString(it.labelRes) }.orEmpty(), false)
                            unit.dropdownUnitAvailability.setText(availability.firstOrNull { it.id == draft.availabilityId }?.let { getString(it.labelRes) }.orEmpty(), false)
                        }
                    }
                    user.tilUserName.error = state.errors["nama"]
                    category.tilKategoriName.error = state.errors["nama"]
                    user.tilUserPhone.error = state.errors["noTelepon"]
                    user.tilUserPassword.error = state.errors["password"]
                    user.tilUserRole.error = state.errors["idRole"]
                    user.tilUserGender.error = state.errors["idJenisKelamin"]
                    unit.tilUnitCode.error = state.errors["kodeInventaris"]
                    unit.tilUnitStatus.error = state.errors["idStatusBarang"]
                    unit.tilUnitAvailability.error = state.errors["idStatusKetersediaan"]
                    binding.textMasterError.text = state.saveError
                    binding.textMasterError.visibility = if (state.saveError != null) View.VISIBLE else View.GONE
                    binding.masterProgress.visibility = if (state.saving) View.VISIBLE else View.GONE
                    binding.buttonSaveMaster.isEnabled = !state.saving && !state.loading && state.loadError == null
                    binding.buttonSaveMaster.text = getString(if (state.saving) R.string.symbol_blank else R.string.action_save)
                    binding.buttonSaveMaster.icon = if (state.saving) null else androidx.appcompat.content.res.AppCompatResources.getDrawable(requireContext(), R.drawable.ic_check)
                    setFieldsEnabled(binding.stateLayout, !state.saving)
                    if (state.saved) {
                        viewModel.consumeSaved()
                        if (viewModel.kind == "user" && viewModel.entityId == appContainer.sessionManager.session.value?.userId) {
                            android.widget.Toast.makeText(requireContext(), R.string.account_updated_relogin, android.widget.Toast.LENGTH_LONG).show()
                            appContainer.sessionManager.clear()
                        } else {
                            findNavController().previousBackStackEntry?.savedStateHandle?.set("refresh", true)
                            findNavController().popBackStack()
                        }
                    }
                }
            }
        }
    }

    private fun setFieldsEnabled(view: View, enabled: Boolean) {
        if (view is android.widget.EditText) view.isEnabled = enabled
        if (view is ViewGroup) for (index in 0 until view.childCount) setFieldsEnabled(view.getChildAt(index), enabled)
    }
}
