package com.app.campgear.ui.peminjam

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.campgear.R
import com.app.campgear.databinding.FragmentLoanListBinding
import com.app.campgear.ui.common.BaseFragment
import com.app.campgear.ui.loan.LoanAdapter
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class MyLoansFragment : BaseFragment<FragmentLoanListBinding>() {

    private val viewModel: MyLoansViewModel by viewModels { viewModelFactory }
    private lateinit var adapter: LoanAdapter
    private var resumedOnce = false

    override fun createBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLoanListBinding =
        FragmentLoanListBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = LoanAdapter(showBorrower = false) { loan ->
            findNavController().navigate(
                R.id.loanDetail,
                Bundle().apply { putLong("loanId", loan.id) }
            )
        }
        binding.recyclerLoans.adapter = adapter
        binding.recyclerLoans.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                if (dy > 0 && layoutManager.findLastVisibleItemPosition() >= adapter.itemCount - 3) {
                    viewModel.loadMore()
                }
            }
        })

        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.chipGroup.check(com.app.campgear.ui.common.loanStatusChip(viewModel.selectedStatus))
        binding.chipGroup.setOnCheckedStateChangeListener { group, _ -> applyFilter(group) }

        viewModel.start()
        observe()
    }

    private fun applyFilter(group: ChipGroup) {
        val status = when (group.checkedChipId) {
            R.id.chipDiajukan -> "diajukan"
            R.id.chipDisetujui -> "disetujui"
            R.id.chipDitolak -> "ditolak"
            R.id.chipDipinjam -> "dipinjam"
            R.id.chipSelesai -> "selesai"
            else -> null
        }
        viewModel.setStatus(status)
    }

    private fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    adapter.submitList(state.items)
                    renderPagedState(
                        state = state,
                        stateLayout = binding.stateLayout,
                        swipeRefresh = binding.swipeRefresh,
                        emptyMessage = getString(R.string.my_loans_empty),
                        paginationProgress = binding.paginationProgress,
                        onRetry = { viewModel.refresh() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (resumedOnce) {
            viewModel.refresh()
        }
        resumedOnce = true
    }

    override fun onDataChanged() = viewModel.refresh()
}
