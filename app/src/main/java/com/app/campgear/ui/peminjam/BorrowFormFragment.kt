package com.app.campgear.ui.peminjam

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.app.campgear.R
import com.app.campgear.core.Formatters
import com.app.campgear.databinding.FragmentBorrowFormBinding
import com.app.campgear.ui.common.BaseFragment
import com.app.campgear.ui.common.bindIsoDatePicker
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class BorrowFormFragment : BaseFragment<FragmentBorrowFormBinding>() {
    private val viewModel: BorrowFormViewModel by viewModels { viewModelFactory }
    private val cartViewModel: CartViewModel by activityViewModels { viewModelFactory }

    override fun createBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentBorrowFormBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = CartAdapter(
            onIncrease = { if (!cartViewModel.increase(it.alatId)) showError(getString(R.string.catalog_stock_limit)) },
            onDecrease = { cartViewModel.decrease(it.alatId) },
            onRemove = { cartViewModel.remove(it.alatId) }
        )
        binding.recyclerCart.adapter = adapter
        bindIsoDatePicker(binding.inputLoanDate, "borrow_from", { viewModel.state.value.loanDate }, viewModel::setLoanDate)
        bindIsoDatePicker(binding.inputReturnDate, "borrow_to", { viewModel.state.value.returnDate }, viewModel::setReturnDate)
        binding.buttonSubmit.setOnClickListener { submit() }
        if (!viewModel.state.value.completed) cartViewModel.revalidate()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(viewModel.state, cartViewModel.items, cartViewModel.checking, cartViewModel.validated) { form, items, checking, validated ->
                    BorrowScreenState(form, items, checking, validated)
                }.collect { screen -> render(screen, adapter) }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                cartViewModel.checkError.collect { error ->
                    if (error != null) Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG)
                        .setAnchorView(binding.buttonSubmit)
                        .setAction(R.string.action_retry) { cartViewModel.revalidate() }.show()
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.submitted.collect {
                    cartViewModel.clear()
                    Toast.makeText(requireContext(), R.string.borrow_success, Toast.LENGTH_LONG).show()
                    findNavController().navigate(R.id.myLoans, null,
                        NavOptions.Builder().setPopUpTo(R.id.catalog, false).setLaunchSingleTop(true).build())
                    findNavController().currentBackStackEntry?.savedStateHandle?.set("refresh", true)
                }
            }
        }
    }

    private fun render(screen: BorrowScreenState, adapter: CartAdapter) {
        val form = screen.form
        val busy = form.submitting || screen.checking
        adapter.submitList(screen.items)
        adapter.setBusy(busy)
        if (screen.items.isEmpty() && !form.submitting) {
            binding.stateLayout.showEmpty(getString(R.string.borrow_empty), {
                findNavController().navigate(R.id.catalog)
            }, R.string.menu_catalog)
        } else binding.stateLayout.showContent()
        binding.inputLoanDate.setText(Formatters.displayDate(form.loanDate))
        binding.inputReturnDate.setText(Formatters.displayDate(form.returnDate))
        binding.inputLoanDate.isEnabled = !form.submitting
        binding.inputReturnDate.isEnabled = !form.submitting
        binding.buttonSubmit.isEnabled = !busy && !form.completed && screen.validated && screen.items.isNotEmpty()
        binding.buttonSubmit.text = getString(if (busy) R.string.symbol_blank else R.string.borrow_submit)
        binding.buttonSubmit.icon = if (busy) null else AppCompatResources.getDrawable(requireContext(), R.drawable.ic_check)
        binding.submitProgress.visibility = if (busy) View.VISIBLE else View.GONE
        binding.textFormError.text = form.error
        binding.textFormError.visibility = if (form.error != null) View.VISIBLE else View.GONE
        val days = Formatters.daysBetween(form.loanDate, form.returnDate)
        binding.textEstimatedTotal.text = Formatters.rupiah(screen.items.sumOf { it.hargaSewa * it.jumlah } * days)
        binding.textDuration.text = resources.getQuantityString(R.plurals.borrow_days, days, days)
    }

    private fun submit() {
        val form = viewModel.state.value
        if (cartViewModel.checking.value || !cartViewModel.validated.value || form.submitting || form.completed) return
        binding.tilLoanDate.error = null
        binding.tilReturnDate.error = null
        if (!Formatters.isValidDate(form.loanDate) || !Formatters.isValidDate(form.returnDate)) {
            binding.tilLoanDate.error = getString(R.string.borrow_error_dates)
            binding.tilReturnDate.error = getString(R.string.borrow_error_dates)
            return
        }
        if (form.returnDate < form.loanDate) {
            binding.tilReturnDate.error = getString(R.string.borrow_error_date_order)
            return
        }
        val items = cartViewModel.items.value
        if (items.isEmpty() || items.any { it.jumlah !in 1..1000 || it.jumlah > it.stok }) {
            showError(getString(R.string.borrow_error_items))
            return
        }
        viewModel.submit(items)
    }

    private data class BorrowScreenState(
        val form: BorrowFormUiState, val items: List<CartItem>, val checking: Boolean, val validated: Boolean
    )
}
