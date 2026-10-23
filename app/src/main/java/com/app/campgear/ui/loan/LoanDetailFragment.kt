package com.app.campgear.ui.loan

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
import com.app.campgear.core.StatusUi
import com.app.campgear.databinding.FragmentLoanDetailBinding
import com.app.campgear.ui.common.BaseFragment
import com.app.campgear.ui.common.confirmAction
import kotlinx.coroutines.launch

class LoanDetailFragment : BaseFragment<FragmentLoanDetailBinding>() {

    private val viewModel: LoanDetailViewModel by viewModels { viewModelFactory }
    private lateinit var adapter: LoanDetailAdapter
    private val loanId: Long by lazy { requireArguments().getLong("loanId") }
    private var lastActionError: String? = null
    private var resumedOnce = false

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentLoanDetailBinding = FragmentLoanDetailBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = LoanDetailAdapter()
        binding.recyclerItems.adapter = adapter

        binding.buttonApprove.setOnClickListener {
            confirmAction("approve", getString(R.string.loan_detail_approve), getString(R.string.loan_detail_approve_confirm))
        }
        binding.buttonReject.setOnClickListener { showRejectDialog() }
        childFragmentManager.setFragmentResultListener("reject", viewLifecycleOwner) { _, result ->
            viewModel.reject(result.getString("note")?.ifBlank { null })
        }
        binding.buttonHandover.setOnClickListener {
            confirmAction("handover", getString(R.string.loan_detail_handover), getString(R.string.loan_detail_handover_confirm))
        }
        binding.buttonReturn.setOnClickListener {
            findNavController().navigate(
                R.id.returnForm,
                Bundle().apply { putLong("loanId", loanId) }
            )
        }
        binding.buttonEditLoan.setOnClickListener {
            findNavController().navigate(
                R.id.editLoan,
                Bundle().apply { putLong("loanId", loanId) }
            )
        }
        binding.buttonDeleteLoan.setOnClickListener {
            confirmAction("delete", getString(R.string.loan_detail_delete), getString(R.string.loan_detail_delete_message))
        }
        childFragmentManager.setFragmentResultListener("confirmed_action", viewLifecycleOwner) { _, result ->
            when (result.getString("action")) { "approve" -> viewModel.approve(); "handover" -> viewModel.handover(); "delete" -> viewModel.delete() }
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
                        loan == null && state.error != null ->
                            binding.stateLayout.showError(state.error) { viewModel.load(loanId) }

                        loan == null -> binding.stateLayout.showEmpty(getString(R.string.loan_detail_empty))
                        else -> {
                            binding.stateLayout.showContent()
                            bindLoan(loan.id, loan.status, loan)
                            state.error?.let(::showError)
                        }
                    }

                    val acting = state.acting || state.loading
                    binding.buttonApprove.isEnabled = !acting
                    binding.buttonReject.isEnabled = !acting
                    binding.buttonHandover.isEnabled = !acting
                    binding.buttonDeleteLoan.isEnabled = !acting
                    binding.buttonReturn.isEnabled = !acting
                    binding.buttonEditLoan.isEnabled = !acting

                    if (state.actionError != null && state.actionError != lastActionError) {
                        showError(state.actionError)
                        viewModel.clearActionError()
                    }
                    lastActionError = state.actionError
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.actions.collect { action ->
                    val message = when (action) {
                        LoanAction.APPROVE -> R.string.loan_detail_approve_success
                        LoanAction.REJECT -> R.string.loan_detail_reject_success
                        LoanAction.HANDOVER -> R.string.loan_detail_handover_success
                        LoanAction.DELETE -> R.string.loan_detail_delete_success
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    if (action == LoanAction.DELETE) {
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    private fun bindLoan(id: Long, status: String, loan: com.app.campgear.data.PeminjamanResponse) {
        binding.detailLoanNumber.text = getString(R.string.loan_number, id)
        StatusUi.bind(binding.detailStatus, status)
        binding.detailBorrower.text = getString(
            R.string.loan_peminjam_fmt,
            loan.peminjam.orEmpty()
        )
        binding.detailStaff.text = getString(
            R.string.loan_petugas_fmt,
            loan.petugas ?: getString(R.string.symbol_dash)
        )
        binding.detailDates.text = getString(
            R.string.date_range_fmt,
            Formatters.displayDate(loan.tanggalPeminjaman),
            Formatters.displayDate(loan.tanggalPengembalian)
        )
        binding.detailTotal.text = getString(
            R.string.loan_total_fmt,
            Formatters.rupiah(loan.totalBiaya)
        )
        adapter.submitList(loan.details)
        bindTimeline(status)

        val role = appContainer.sessionManager.session.value?.role
        val isStaff = role == "Admin" || role == "Petugas"
        val isAdmin = role == "Admin"

        binding.buttonApprove.visibility = visible(isStaff && status == "diajukan")
        binding.buttonReject.visibility = visible(isStaff && status == "diajukan")
        binding.buttonHandover.visibility = visible(isStaff && status == "disetujui")
        binding.buttonReturn.visibility = visible(status == "dipinjam")
        binding.buttonEditLoan.visibility = visible(isAdmin && status == "diajukan")
        binding.buttonDeleteLoan.visibility =
            visible(isAdmin && (status == "diajukan" || status == "ditolak"))
    }

    private fun showRejectDialog() {
        com.app.campgear.ui.common.RejectDialogFragment().show(childFragmentManager, "reject_dialog")
    }

    private fun bindTimeline(status: String) {
        binding.loanTimeline.removeAllViews()
        val stages = if (status == "ditolak") listOf("diajukan", "ditolak") else listOf("diajukan", "disetujui", "dipinjam", "selesai")
        val current = stages.indexOf(status)
        stages.forEachIndexed { index, stage ->
            val row = com.app.campgear.databinding.ItemTimelineBinding.inflate(layoutInflater, binding.loanTimeline, false)
            row.timelineText.text = getString(StatusUi.labelRes(stage))
            row.timelineIcon.setImageResource(if (index <= current) R.drawable.ic_check else R.drawable.ic_monitor)
            val color = if (index <= current) StatusUi.textColor(requireContext(), stage)
                else androidx.core.content.ContextCompat.getColor(requireContext(), R.color.on_surface_variant)
            row.timelineText.setTextColor(color)
            androidx.core.widget.ImageViewCompat.setImageTintList(row.timelineIcon, android.content.res.ColorStateList.valueOf(color))
            row.timelineIcon.contentDescription = getString(StatusUi.labelRes(stage))
            binding.loanTimeline.addView(row.root)
        }
    }

    private fun visible(show: Boolean): Int = if (show) View.VISIBLE else View.GONE

    override fun onDataChanged() = viewModel.refresh()

    override fun onResume() {
        super.onResume()
        if (resumedOnce) {
            viewModel.refresh()
        }
        resumedOnce = true
    }
}
