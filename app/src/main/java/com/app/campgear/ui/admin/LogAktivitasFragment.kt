package com.app.campgear.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.campgear.R
import com.app.campgear.core.Formatters
import com.app.campgear.data.UserResponse
import com.app.campgear.databinding.FragmentLogAktivitasBinding
import com.app.campgear.ui.common.BaseFragment
import com.app.campgear.ui.common.bindIsoDatePicker
import com.app.campgear.ui.common.prepareDateInput
import kotlinx.coroutines.launch

class LogAktivitasFragment : BaseFragment<FragmentLogAktivitasBinding>() {

    private val viewModel: LogAktivitasViewModel by viewModels { viewModelFactory }
    private val adapter = LogAktivitasAdapter()
    private var userOptions: List<UserResponse> = emptyList()

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentLogAktivitasBinding =
        FragmentLogAktivitasBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerLog.adapter = adapter
        binding.recyclerLog.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                if (dy > 0 && layoutManager.findLastVisibleItemPosition() >= adapter.itemCount - 3) {
                    viewModel.loadMore()
                }
            }
        })

        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.inputLogFrom.prepareDateInput()
        binding.inputLogTo.prepareDateInput()
        binding.buttonToggleLogFilter.setOnClickListener {
            binding.logFilterPanel.isVisible = !binding.logFilterPanel.isVisible
        }
        binding.inputLogUser.setOnItemClickListener { _, _, position, _ ->
            viewModel.setUser(userOptions.getOrNull(position - 1)?.id)
        }
        bindIsoDatePicker(binding.inputLogFrom, "log_from", { viewModel.filter.value.from ?: Formatters.todayIso() }, viewModel::setFrom)
        bindIsoDatePicker(binding.inputLogTo, "log_to", { viewModel.filter.value.to ?: Formatters.todayIso() }, viewModel::setTo)
        binding.buttonApplyLogFilter.setOnClickListener { applyFilter() }
        binding.buttonResetLogFilter.setOnClickListener {
            binding.tilLogFrom.error = null
            binding.tilLogTo.error = null
            viewModel.resetFilter()
        }

        viewModel.start()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userError.collect { error ->
                    if (error != null) com.google.android.material.snackbar.Snackbar.make(binding.root, error, com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                        .setAction(R.string.action_retry) { viewModel.loadUsers() }.show()
                }
            }
        }
        observe()
    }

    private fun applyFilter() {
        binding.tilLogFrom.error = null
        binding.tilLogTo.error = null
        val filter = viewModel.filter.value
        if (filter.from != null && filter.to != null && filter.to < filter.from) {
            binding.tilLogTo.error = getString(R.string.log_date_order)
            return
        }
        viewModel.applyFilter()
        binding.logFilterPanel.visibility = View.GONE
    }

    private fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.users.collect { users ->
                    userOptions = users
                    val labels = buildList {
                        add(getString(R.string.log_filter_all_users))
                        users.forEach { add(it.nama) }
                    }
                    binding.inputLogUser.setAdapter(
                        ArrayAdapter(requireContext(), R.layout.item_dropdown, labels)
                    )
                    binding.inputLogUser.setText(users.firstOrNull { it.id == viewModel.filter.value.idUser }?.nama ?: getString(R.string.log_filter_all_users), false)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.filter.collect { filter ->
                    binding.inputLogFrom.setText(
                        filter.from?.let { Formatters.displayDate(it) }.orEmpty()
                    )
                    binding.inputLogTo.setText(
                        filter.to?.let { Formatters.displayDate(it) }.orEmpty()
                    )
                    val label = userOptions.firstOrNull { it.id == filter.idUser }?.nama
                        ?: getString(R.string.log_filter_all_users)
                    if (binding.inputLogUser.text?.toString() != label) {
                        binding.inputLogUser.setText(label, false)
                    }
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
                        emptyMessage = getString(R.string.log_empty),
                        paginationProgress = binding.paginationProgress,
                        onRetry = { viewModel.refresh() }
                    )
                }
            }
        }
    }
}
