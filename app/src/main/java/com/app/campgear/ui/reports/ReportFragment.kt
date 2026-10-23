package com.app.campgear.ui.reports

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.app.campgear.R
import com.app.campgear.core.Formatters
import com.app.campgear.databinding.FragmentReportsBinding
import com.app.campgear.ui.common.BaseFragment
import com.app.campgear.ui.common.bindIsoRangePicker
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import java.io.File

class ReportFragment : BaseFragment<FragmentReportsBinding>() {

    private val viewModel: ReportViewModel by viewModels { viewModelFactory }
    private val loanAdapter = ReportLoanAdapter()
    private val returnAdapter = ReportReturnAdapter()

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentReportsBinding = FragmentReportsBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerReportLoans.adapter = loanAdapter
        binding.recyclerReportReturns.adapter = returnAdapter

        binding.chipGroupReport.setOnCheckedStateChangeListener { group, _ ->
            applyType(group)
        }
        bindIsoRangePicker(binding.buttonReportRange, { viewModel.state.value.from to viewModel.state.value.to }, viewModel::setRange)
        binding.buttonLoadReport.setOnClickListener { loadReport() }
        binding.buttonShareCsv.setOnClickListener { viewModel.requestCsv() }

        observe()
    }

    private fun applyType(group: ChipGroup) {
        viewModel.selectType(
            if (group.checkedChipId == R.id.chipReportReturns) {
                ReportType.RETURNS
            } else {
                ReportType.LOANS
            }
        )
    }

    private fun loadReport() {
        val state = viewModel.state.value

        if (state.from.isBlank() || state.to.isBlank()) {
            showError(getString(R.string.report_date_error))
            return
        }
        if (state.to < state.from) {
            showError(getString(R.string.report_date_order))
            return
        }

        viewModel.load()
    }

    private fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.buttonReportRange.text = getString(R.string.date_range_fmt, Formatters.displayDate(state.from), Formatters.displayDate(state.to))
                    binding.buttonLoadReport.isEnabled = !state.loading
                    binding.buttonShareCsv.isEnabled = !state.sharing && state.data != null
                    binding.shareProgress.visibility =
                        if (state.sharing) View.VISIBLE else View.GONE
                    binding.textShareError.visibility =
                        if (state.shareError != null) View.VISIBLE else View.GONE
                    binding.textShareError.text = state.shareError

                    render(state)
                    state.csvFile?.let { file ->
                        viewModel.consumeCsv()
                        shareCsv(file)
                    }
                }
            }
        }

    }

    private fun render(state: ReportUiState) {
        when {
            state.loading -> binding.stateLayout.showLoading()
            state.error != null -> binding.stateLayout.showError(state.error) {
                viewModel.load()
            }

            state.data == null -> binding.stateLayout.showEmpty(getString(R.string.report_prompt))
            else -> {
                val data = state.data
                val empty = when (data) {
                    is ReportData.Loans -> data.value.items.isEmpty()
                    is ReportData.Returns -> data.value.items.isEmpty()
                }
                binding.stateLayout.showContent()
                binding.reportEmpty.visibility = if (empty) View.VISIBLE else View.GONE
                bindData(data)
            }
        }
    }

    private fun bindData(data: ReportData) {
        binding.reportSummaryPeriod.text = getString(
            R.string.report_period_fmt,
            Formatters.displayDate(viewModel.state.value.from),
            Formatters.displayDate(viewModel.state.value.to)
        )

        when (data) {
            is ReportData.Loans -> {
                binding.reportSummaryTitle.text = getString(R.string.report_loans)
                binding.reportSummaryTotal.text = getString(
                    R.string.report_total_biaya_fmt,
                    Formatters.rupiah(data.value.totalBiaya)
                )
                binding.reportSummaryCount.text = resources.getQuantityString(
                    R.plurals.report_count_fmt,
                    data.value.items.size,
                    data.value.items.size
                )
                loanAdapter.submitList(data.value.items)
                returnAdapter.submitList(emptyList())
                binding.recyclerReportLoans.visibility = View.VISIBLE
                binding.recyclerReportReturns.visibility = View.GONE
            }

            is ReportData.Returns -> {
                binding.reportSummaryTitle.text = getString(R.string.report_returns)
                binding.reportSummaryTotal.text = getString(
                    R.string.report_total_denda_fmt,
                    Formatters.rupiah(data.value.totalDenda)
                )
                binding.reportSummaryCount.text = resources.getQuantityString(
                    R.plurals.report_count_fmt,
                    data.value.items.size,
                    data.value.items.size
                )
                returnAdapter.submitList(data.value.items)
                loanAdapter.submitList(emptyList())
                binding.recyclerReportLoans.visibility = View.GONE
                binding.recyclerReportReturns.visibility = View.VISIBLE
            }
        }
    }

    private fun shareCsv(file: File) {
        try {
            val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = android.content.ClipData.newRawUri("CSV", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.action_share_csv)))
        } catch (e: Exception) {
            showError(getString(R.string.report_share_error))
        }
    }
}
