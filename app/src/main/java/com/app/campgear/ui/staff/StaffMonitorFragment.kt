package com.app.campgear.ui.staff

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
import com.app.campgear.databinding.FragmentMonitorBinding
import com.app.campgear.ui.common.BaseFragment
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class StaffMonitorFragment : BaseFragment<FragmentMonitorBinding>() {

    private val viewModel: StaffMonitorViewModel by viewModels { viewModelFactory }
    private lateinit var adapter: MonitorAdapter
    private var resumedOnce = false

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMonitorBinding = FragmentMonitorBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MonitorAdapter { loan ->
            findNavController().navigate(
                R.id.loanDetail,
                Bundle().apply { putLong("loanId", loan.id) }
            )
        }
        binding.recyclerMonitor.adapter = adapter
        binding.recyclerMonitor.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                if (dy > 0 && layoutManager.findLastVisibleItemPosition() >= adapter.itemCount - 3) {
                    viewModel.loadMore()
                }
            }
        })

        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.chipGroupMonitor.check(when (viewModel.selectedStatus) {
            "jatuh_tempo" -> R.id.chipMonitorDue
            "terlambat" -> R.id.chipMonitorLate
            else -> R.id.chipMonitorAll
        })
        binding.chipGroupMonitor.setOnCheckedStateChangeListener { group, _ ->
            applyFilter(group)
        }

        viewModel.start()
        observe()
    }

    private fun applyFilter(group: ChipGroup) {
        val status = when (group.checkedChipId) {
            R.id.chipMonitorDue -> "jatuh_tempo"
            R.id.chipMonitorLate -> "terlambat"
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
                        emptyMessage = getString(R.string.monitor_empty),
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
}
