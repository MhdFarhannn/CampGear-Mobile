package com.app.campgear.ui.returns

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.app.campgear.R
import com.app.campgear.core.Formatters
import com.app.campgear.databinding.FragmentReturnDetailBinding
import com.app.campgear.ui.common.BaseFragment
import kotlinx.coroutines.launch

class ReturnDetailFragment : BaseFragment<FragmentReturnDetailBinding>() {

    private val viewModel: ReturnDetailViewModel by viewModels { viewModelFactory }
    private lateinit var adapter: ReturnDetailAdapter
    private val returnId: Long by lazy { requireArguments().getLong("returnId") }

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentReturnDetailBinding = FragmentReturnDetailBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ReturnDetailAdapter()
        binding.recyclerReturnUnits.adapter = adapter

        viewModel.initialize(returnId)
        observe()
    }

    private fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    val data = state.data
                    when {
                        data == null && state.loading -> binding.stateLayout.showLoading()
                        data == null && state.error != null ->
                            binding.stateLayout.showError(state.error) {
                                viewModel.load(returnId)
                            }

                        data == null ->
                            binding.stateLayout.showEmpty(getString(R.string.label_no_data))

                        else -> {
                            binding.stateLayout.showContent()
                            bind(data)
                        }
                    }
                }
            }
        }
    }

    private fun bind(data: com.app.campgear.data.PengembalianResponse) {
        binding.detailReturnNumber.text = getString(R.string.return_number, data.id)
        binding.detailReturnBorrower.text = getString(
            R.string.loan_peminjam_fmt,
            data.peminjam.orEmpty()
        )
        binding.detailReturnDate.text = getString(
            R.string.return_date_fmt,
            Formatters.displayDate(data.tanggalKembali)
        )
        binding.detailReturnLate.text = resources.getQuantityString(
            R.plurals.return_late_days_fmt,
            data.keterlambatanPengembalian,
            data.keterlambatanPengembalian
        )
        binding.detailReturnFine.text = getString(
            R.string.return_total_fine_fmt,
            Formatters.rupiah(data.denda)
        )
        binding.detailLateFine.text = getString(R.string.late_fine_value, Formatters.rupiah(data.lateFine))
        binding.detailConditionFine.text = getString(R.string.return_condition_fine_fmt, Formatters.rupiah(data.conditionFine))
        binding.detailReturnNote.visibility =
            if (data.catatan.isNullOrBlank()) View.GONE else View.VISIBLE
        binding.detailReturnNote.text = getString(
            R.string.return_note_fmt,
            data.catatan.orEmpty()
        )
        adapter.submitList(data.details)
    }
}
