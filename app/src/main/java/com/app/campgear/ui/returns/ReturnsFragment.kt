package com.app.campgear.ui.returns

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.campgear.R
import com.app.campgear.core.Formatters
import com.app.campgear.databinding.FragmentReturnsBinding
import com.app.campgear.ui.common.BaseFragment
import com.app.campgear.ui.common.bindIsoDatePicker
import com.app.campgear.ui.common.prepareDateInput
import kotlinx.coroutines.launch

class ReturnsFragment : BaseFragment<FragmentReturnsBinding>() {

    private val viewModel: ReturnsViewModel by viewModels { viewModelFactory }
    private lateinit var adapter: ReturnAdapter
    private var resumedOnce = false

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentReturnsBinding = FragmentReturnsBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ReturnAdapter { item ->
            findNavController().navigate(
                R.id.returnDetail,
                Bundle().apply { putLong("returnId", item.id) }
            )
        }
        binding.recyclerReturns.adapter = adapter
        binding.recyclerReturns.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                if (dy > 0 && layoutManager.findLastVisibleItemPosition() >= adapter.itemCount - 3) {
                    viewModel.loadMore()
                }
            }
        })
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.inputReturnsFrom.prepareDateInput()
        binding.inputReturnsTo.prepareDateInput()
        binding.buttonToggleReturnsFilter.setOnClickListener {
            binding.returnsFilterPanel.isVisible = !binding.returnsFilterPanel.isVisible
        }

        bindIsoDatePicker(binding.inputReturnsFrom, "returns_from", { viewModel.dates.value.from ?: Formatters.todayIso() }, viewModel::setFrom)
        bindIsoDatePicker(binding.inputReturnsTo, "returns_to", { viewModel.dates.value.to ?: Formatters.todayIso() }, viewModel::setTo)
        binding.buttonApplyReturnsFilter.setOnClickListener { applyFilter() }
        binding.buttonResetReturnsFilter.setOnClickListener {
            binding.tilReturnsFrom.error = null
            binding.tilReturnsTo.error = null
            viewModel.resetFilter()
        }

        viewModel.start()
        observe()
    }

    private fun applyFilter() {
        binding.tilReturnsFrom.error = null
        binding.tilReturnsTo.error = null
        val dates = viewModel.dates.value
        if (dates.from != null && dates.to != null && dates.to < dates.from) {
            binding.tilReturnsTo.error = getString(R.string.log_date_order)
            return
        }
        viewModel.applyFilter()
        binding.returnsFilterPanel.visibility = View.GONE
    }

    private fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.dates.collect { dates ->
                    binding.inputReturnsFrom.setText(
                        dates.from?.let { Formatters.displayDate(it) }.orEmpty()
                    )
                    binding.inputReturnsTo.setText(
                        dates.to?.let { Formatters.displayDate(it) }.orEmpty()
                    )
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    adapter.submitList(state.items)
                    renderPagedState(
                        state = state,
                        stateLayout = binding.stateLayout,
                        swipeRefresh = binding.swipeRefresh,
                        emptyMessage = getString(R.string.returns_empty),
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
