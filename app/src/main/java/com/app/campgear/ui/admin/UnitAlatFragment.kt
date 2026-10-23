package com.app.campgear.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.app.campgear.R
import com.app.campgear.databinding.FragmentUnitAlatBinding
import com.app.campgear.ui.common.BaseFragment
import com.app.campgear.ui.common.onNearEnd
import com.app.campgear.ui.common.confirmAction
import kotlinx.coroutines.launch

class UnitAlatFragment : BaseFragment<FragmentUnitAlatBinding>() {
    private val viewModel: UnitAlatViewModel by viewModels { viewModelFactory }
    private val alatId by lazy { requireArguments().getLong("alatId") }
    override fun createBinding(inflater: LayoutInflater, container: ViewGroup?) = FragmentUnitAlatBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.unitAlatTitle.text = getString(R.string.units_for_alat, requireArguments().getString("alatNama").orEmpty())
        val adapter = UnitAlatAdapter(onEdit = {
            if (it.statusKetersediaan == "dipinjam") showError(getString(R.string.units_borrowed_locked)) else openForm(it.id)
        }, onDelete = { unit ->
            if (unit.statusKetersediaan == "dipinjam") showError(getString(R.string.units_borrowed_locked))
            else confirmAction("delete_unit", getString(R.string.units_delete_confirm), getString(R.string.delete_named, unit.kodeInventaris), unit.id)
        })
        childFragmentManager.setFragmentResultListener("confirmed_action", viewLifecycleOwner) { _, result ->
            if (result.getString("action") == "delete_unit") viewModel.delete(result.getLong("id"))
        }
        binding.recyclerUnits.adapter = adapter
        binding.unitAvailabilityFilter.check(when (viewModel.selectedAvailability) {
            1L -> R.id.unitFilterAvailable
            2L -> R.id.unitFilterBorrowed
            3L -> R.id.unitFilterUnavailable
            else -> R.id.unitFilterAll
        })
        binding.unitAvailabilityFilter.setOnCheckedStateChangeListener { group, _ ->
            viewModel.setAvailability(when (group.checkedChipId) {
                R.id.unitFilterAvailable -> 1L
                R.id.unitFilterBorrowed -> 2L
                R.id.unitFilterUnavailable -> 3L
                else -> null
            })
        }
        binding.inputUnitSearch.doAfterTextChanged { query ->
            val state = viewModel.state.value
            val visible = state.items.filter { it.kodeInventaris.contains(query.toString(), true) }
            adapter.submitList(visible)
            if (visible.isEmpty() && state.items.isNotEmpty()) showSearchEmpty(state.hasMore)
            else if (state.items.isNotEmpty()) binding.stateLayout.showContent()
        }
        binding.recyclerUnits.onNearEnd { viewModel.loadMore() }
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.buttonAddUnit.setOnClickListener { openForm(0) }
        viewModel.initialize(alatId)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect { state ->
                    val visible = state.items.filter { it.kodeInventaris.contains(binding.inputUnitSearch.text.toString(), true) }
                    adapter.submitList(visible)
                    renderPagedState(state, binding.stateLayout, binding.swipeRefresh, getString(R.string.units_empty), binding.paginationProgress) { viewModel.refresh() }
                    if (visible.isEmpty() && state.items.isNotEmpty()) showSearchEmpty(state.hasMore)
                } }
                launch { viewModel.busy.collect {
                    binding.unitProgress.visibility = if (it) View.VISIBLE else View.GONE
                    binding.buttonAddUnit.isEnabled = !it
                } }
                launch { viewModel.actionError.collect { it?.let { error -> showError(error); viewModel.clearActionError() } } }
                launch { viewModel.actions.collect { showMessage(getString(R.string.units_deleted)) } }
            }
        }
    }
    private fun openForm(id: Long) = findNavController().navigate(R.id.masterForm, Bundle().apply {
        putString("kind", "unit"); putLong("entityId", id); putLong("alatId", alatId)
    })
    override fun onDataChanged() = viewModel.refresh()

    private fun showSearchEmpty(hasMore: Boolean) {
        binding.stateLayout.showEmpty(getString(R.string.search_loaded_empty), {
            if (hasMore) viewModel.loadMore() else binding.inputUnitSearch.setText("")
        }, if (hasMore) R.string.load_more else R.string.action_reset)
    }
}
