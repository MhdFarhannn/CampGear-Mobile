package com.app.campgear.ui.loan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.app.campgear.R
import com.app.campgear.core.Formatters
import com.app.campgear.databinding.FragmentReturnFormBinding
import com.app.campgear.ui.common.BaseFragment
import com.app.campgear.ui.common.bindIsoDatePicker
import com.app.campgear.ui.common.prepareDateInput
import com.app.campgear.ui.common.confirmAction
import kotlinx.coroutines.launch

class ReturnFormFragment : BaseFragment<FragmentReturnFormBinding>() {

    private val viewModel: ReturnFormViewModel by viewModels { viewModelFactory }
    private lateinit var adapter: ReturnUnitAdapter
    private val loanId: Long by lazy { requireArguments().getLong("loanId") }

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentReturnFormBinding = FragmentReturnFormBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ReturnUnitAdapter(
            onCondition = { unitId, kondisi -> viewModel.setUnitCondition(unitId, kondisi) },
            onNote = { unitId, note -> viewModel.setUnitNote(unitId, note) }
        )
        binding.recyclerReturnUnits.adapter = adapter
        (binding.recyclerReturnUnits.itemAnimator as? androidx.recyclerview.widget.SimpleItemAnimator)?.supportsChangeAnimations = false
        binding.inputReturnDateActual.prepareDateInput()

        bindIsoDatePicker(binding.inputReturnDateActual, "return_date", { viewModel.state.value.returnDate }, viewModel::setReturnDate)
        binding.inputReturnNote.doAfterTextChanged {
            viewModel.setNote(it?.toString().orEmpty())
        }
        binding.buttonSubmitReturn.setOnClickListener { submit() }
        childFragmentManager.setFragmentResultListener("confirmed_action", viewLifecycleOwner) { _, result ->
            if (result.getString("action") == "return") viewModel.submit()
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

                        state.unitsMissing ->
                            binding.stateLayout.showError(getString(R.string.return_unit_missing)) {
                                viewModel.load(loanId)
                            }

                        else -> {
                            binding.stateLayout.showContent()
                            bindLoan(loan.id, loan.peminjam, loan.tanggalPengembalian)
                            adapter.submitList(state.units)
                        }
                    }

                    binding.inputReturnDateActual.setText(Formatters.displayDate(state.returnDate))
                    if (!binding.inputReturnNote.hasFocus() &&
                        binding.inputReturnNote.text?.toString() != state.note
                    ) {
                        binding.inputReturnNote.setText(state.note)
                    }
                    binding.buttonSubmitReturn.isEnabled =
                        !state.submitting && !state.completed && !state.loading && state.loan != null && !state.unitsMissing
                    binding.submitReturnProgress.visibility =
                        if (state.submitting) View.VISIBLE else View.GONE
                    binding.textSubmitError.visibility =
                        if (state.submitError != null) View.VISIBLE else View.GONE
                    binding.textSubmitError.text = state.submitError
                    adapter.setBusy(state.submitting || state.completed)
                    binding.inputReturnDateActual.isEnabled = !state.submitting && !state.completed
                    binding.inputReturnNote.isEnabled = !state.submitting && !state.completed
                    binding.buttonSubmitReturn.text = getString(if (state.submitting) R.string.symbol_blank else R.string.return_submit)
                    binding.buttonSubmitReturn.icon = if (state.submitting) null else androidx.appcompat.content.res.AppCompatResources.getDrawable(requireContext(), R.drawable.ic_returns)

                    binding.textFineValue.text = Formatters.rupiah(state.estimatedFine)
                    val lateText = if (state.lateDays > 0) {
                        resources.getQuantityString(
                            R.plurals.return_estimate_late,
                            state.lateDays,
                            state.lateDays,
                            Formatters.rupiah(state.lateFine)
                        )
                    } else {
                        getString(R.string.return_on_time)
                    }
                    binding.textFineDetail.text = getString(
                        R.string.return_fine_detail,
                        lateText,
                        getString(
                            R.string.return_condition_fine_fmt,
                            Formatters.rupiah(state.conditionFine)
                        )
                    )
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.submitted.collect { receiptId ->
                    findNavController().previousBackStackEntry?.savedStateHandle?.set("refresh", true)
                    Toast.makeText(
                        requireContext(),
                        R.string.return_success,
                        Toast.LENGTH_LONG
                    ).show()
                    if (receiptId != null) {
                        findNavController().navigate(R.id.returnDetail, Bundle().apply { putLong("returnId", receiptId) },
                            androidx.navigation.NavOptions.Builder().setPopUpTo(R.id.returnForm, true).build())
                    } else findNavController().popBackStack()
                }
            }
        }
    }

    private fun bindLoan(id: Long, borrower: String?, dueDate: String) {
        binding.textReturnLoan.text = getString(R.string.loan_number, id)
        binding.textReturnBorrower.text = getString(
            R.string.loan_peminjam_fmt,
            borrower.orEmpty()
        )
        binding.textReturnDue.text = getString(
            R.string.return_due_fmt,
            Formatters.displayDate(dueDate)
        )
    }

    private fun submit() {
        binding.tilReturnDateActual.error = null
        val state = viewModel.state.value
        val loan = state.loan ?: return

        if (state.returnDate.isBlank()) {
            binding.tilReturnDateActual.error = getString(R.string.return_error_date)
            return
        }
        if (state.returnDate < loan.tanggalPeminjaman) {
            binding.tilReturnDateActual.error = getString(R.string.return_error_date_order)
            return
        }
        if (state.units.isEmpty()) {
            showError(getString(R.string.return_unit_missing))
            return
        }

        confirmAction("return", getString(R.string.return_submit), getString(R.string.return_confirm))
    }
}
