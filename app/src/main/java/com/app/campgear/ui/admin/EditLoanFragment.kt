package com.app.campgear.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.app.campgear.R
import com.app.campgear.core.Formatters
import com.app.campgear.databinding.FragmentEditLoanBinding
import com.app.campgear.ui.common.BaseFragment
import com.app.campgear.ui.common.bindIsoDatePicker
import com.app.campgear.ui.common.prepareDateInput
import kotlinx.coroutines.launch

class EditLoanFragment : BaseFragment<FragmentEditLoanBinding>() {

    private val viewModel: EditLoanViewModel by viewModels { viewModelFactory }
    private lateinit var adapter: EditLoanAdapter
    private val loanId: Long by lazy { requireArguments().getLong("loanId") }

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentEditLoanBinding = FragmentEditLoanBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = EditLoanAdapter(
            onIncrease = {
                viewModel.increase(it.idAlat)
            },
            onDecrease = {
                viewModel.decrease(it.idAlat)
            },
            onRemove = { viewModel.remove(it.idAlat) }
        )
        binding.recyclerEditItems.adapter = adapter
        binding.inputEditLoanDate.prepareDateInput()
        binding.inputEditReturnDate.prepareDateInput()

        bindIsoDatePicker(binding.inputEditLoanDate, "edit_from", { viewModel.state.value.loanDate }, viewModel::setLoanDate)
        bindIsoDatePicker(binding.inputEditReturnDate, "edit_to", { viewModel.state.value.returnDate }, viewModel::setReturnDate)
        binding.buttonSaveEdit.setOnClickListener { save() }
        binding.buttonAddEditItem.setOnClickListener { LoanItemPickerFragment().show(childFragmentManager, "item_picker") }
        childFragmentManager.setFragmentResultListener("loan_item", viewLifecycleOwner) { _, result ->
            viewModel.add(com.app.campgear.data.AlatResponse(id = result.getLong("id"), nama = result.getString("nama").orEmpty(), hargaSewa = result.getDouble("harga")))
        }

        viewModel.initialize(loanId)
        observe()
    }

    private fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    val loan = state.loan
                    when {
                        loan == null && state.loading -> binding.stateLayout.showLoading()
                        loan == null && state.loadError != null ->
                            binding.stateLayout.showError(state.loadError) { viewModel.load(loanId) }

                        loan == null ->
                            binding.stateLayout.showEmpty(getString(R.string.loan_detail_empty))

                        else -> binding.stateLayout.showContent()
                    }

                    if (loan != null) {
                        binding.editLoanNumber.text = getString(R.string.loan_number, loan.id)
                    }
                    binding.inputEditLoanDate.setText(Formatters.displayDate(state.loanDate))
                    binding.inputEditReturnDate.setText(Formatters.displayDate(state.returnDate))
                    adapter.submitList(state.items)
                    binding.editLoanTotal.text = Formatters.rupiah(state.total)
                    binding.editLoanDays.text = resources.getQuantityString(
                        R.plurals.borrow_days,
                        state.days,
                        state.days
                    )
                    binding.buttonSaveEdit.isEnabled = !state.saving && !state.completed && !state.loading && state.loan != null && state.items.isNotEmpty()
                    binding.editLoanProgress.visibility =
                        if (state.saving) View.VISIBLE else View.GONE
                    binding.textEditLoanError.visibility =
                        if (state.saveError != null) View.VISIBLE else View.GONE
                    binding.textEditLoanError.text = state.saveError
                    binding.buttonSaveEdit.text = getString(if (state.saving) R.string.symbol_blank else R.string.edit_loan_save)
                    binding.buttonSaveEdit.icon = if (state.saving) null else androidx.appcompat.content.res.AppCompatResources.getDrawable(requireContext(), R.drawable.ic_check)
                    adapter.setBusy(state.saving || state.completed)
                    binding.buttonAddEditItem.isEnabled = !state.saving && !state.completed && !state.loading
                    binding.inputEditLoanDate.isEnabled = !state.saving && !state.completed
                    binding.inputEditReturnDate.isEnabled = !state.saving && !state.completed
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.saved.collect {
                    findNavController().previousBackStackEntry?.savedStateHandle?.set("refresh", true)
                    Toast.makeText(
                        requireContext(),
                        R.string.edit_loan_success,
                        Toast.LENGTH_LONG
                    ).show()
                    findNavController().popBackStack()
                }
            }
        }
    }

    private fun save() {
        binding.tilEditLoanDate.error = null
        binding.tilEditReturnDate.error = null
        val state = viewModel.state.value

        if (state.loanDate.isBlank() || state.returnDate.isBlank()) {
            binding.tilEditLoanDate.error = getString(R.string.edit_loan_validation_date)
            binding.tilEditReturnDate.error = getString(R.string.edit_loan_validation_date)
            return
        }
        if (state.returnDate < state.loanDate) {
            binding.tilEditReturnDate.error = getString(R.string.edit_loan_validation_date)
            return
        }
        if (state.items.isEmpty() || state.items.any { it.jumlah < 1 }) {
            showError(getString(R.string.edit_loan_validation_quantity))
            return
        }

        viewModel.save()
    }
}
