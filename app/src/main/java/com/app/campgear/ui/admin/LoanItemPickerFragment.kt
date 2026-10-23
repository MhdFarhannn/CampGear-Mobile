package com.app.campgear.ui.admin

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.app.campgear.CampGearApp
import com.app.campgear.R
import com.app.campgear.databinding.DialogEquipmentPickerBinding
import com.app.campgear.ui.common.onNearEnd
import com.app.campgear.ui.peminjam.CatalogViewModel
import com.app.campgear.ui.peminjam.AlatAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class LoanItemPickerFragment : DialogFragment() {
    private var binding: DialogEquipmentPickerBinding? = null
    private val viewModel: CatalogViewModel by viewModels { (requireActivity().application as CampGearApp).container.viewModelFactory }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val ui = DialogEquipmentPickerBinding.inflate(layoutInflater)
        binding = ui
        val select: (com.app.campgear.data.AlatResponse) -> Unit = { alat ->
            parentFragmentManager.setFragmentResult("loan_item", Bundle().apply {
                putLong("id", alat.id); putString("nama", alat.nama); putDouble("harga", alat.hargaSewa)
            })
            dismiss()
        }
        val adapter = AlatAdapter(onOpen = select, onAdd = select)
        ui.recyclerPicker.adapter = adapter
        ui.recyclerPicker.onNearEnd { viewModel.loadMore() }
        ui.inputPickerSearch.doAfterTextChanged { viewModel.setSearch(it.toString()) }
        ui.inputPickerSearch.setText(savedInstanceState?.getString("query").orEmpty())
        viewModel.start()
        return MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.edit_loan_add_item)
            .setView(ui.root).setNegativeButton(R.string.action_cancel, null).create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    val ui = binding ?: return@collect
                    (ui.recyclerPicker.adapter as? AlatAdapter)?.submitList(state.items)
                    when {
                        state.loading && state.items.isEmpty() -> ui.stateLayout.showLoading()
                        state.error != null && state.items.isEmpty() -> ui.stateLayout.showError(state.error) { viewModel.refresh() }
                        state.items.isEmpty() -> ui.stateLayout.showEmpty(getString(R.string.alat_empty)) { viewModel.refresh() }
                        else -> ui.stateLayout.showContent()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        binding?.recyclerPicker?.adapter = null
        binding = null
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("query", binding?.inputPickerSearch?.text?.toString())
        super.onSaveInstanceState(outState)
    }
}
