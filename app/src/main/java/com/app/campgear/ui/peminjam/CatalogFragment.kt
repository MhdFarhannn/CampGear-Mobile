package com.app.campgear.ui.peminjam

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.app.campgear.R
import com.app.campgear.databinding.FragmentCatalogBinding
import com.app.campgear.ui.common.BaseFragment
import com.app.campgear.ui.common.onNearEnd
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class CatalogFragment : BaseFragment<FragmentCatalogBinding>() {
    private val viewModel: CatalogViewModel by viewModels { viewModelFactory }
    private val cartViewModel: CartViewModel by activityViewModels { viewModelFactory }
    override fun createBinding(inflater: LayoutInflater, container: ViewGroup?) = FragmentCatalogBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = AlatAdapter(onOpen = { alat ->
            findNavController().navigate(R.id.equipmentDetail, Bundle().apply { putLong("alatId", alat.id) })
        }, onAdd = { alat ->
            if (cartViewModel.add(alat)) showMessage(getString(R.string.catalog_added, alat.nama)) else showError(getString(R.string.catalog_stock_limit))
        })
        binding.recyclerAlat.adapter = adapter
        binding.recyclerAlat.onNearEnd { viewModel.loadMore() }
        binding.inputSearch.doAfterTextChanged { viewModel.setSearch(it.toString()) }
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh(); viewModel.loadCategories() }
        binding.buttonCart.setOnClickListener { findNavController().navigate(R.id.borrowForm) }
        viewModel.initialize()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect { state ->
                    adapter.submitList(state.items)
                    renderPagedState(state, binding.stateLayout, binding.swipeRefresh, getString(R.string.catalog_empty), binding.paginationProgress) { viewModel.refresh() }
                } }
                launch { viewModel.categories.collect { state ->
                    binding.categoryChips.setOnCheckedStateChangeListener(null)
                    binding.categoryChips.removeAllViews()
                    val options = listOf(null to getString(R.string.catalog_all_categories)) + state.items.map { it.id to it.nama }
                    options.forEach { (id, name) ->
                        val chip = Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle).apply {
                            this.id = View.generateViewId()
                            text = name
                            isCheckable = true
                            tag = id
                            isChecked = id == viewModel.selectedKategoriId
                            setEnsureMinTouchTargetSize(true)
                        }
                        binding.categoryChips.addView(chip)
                    }
                    binding.categoryChips.setOnCheckedStateChangeListener { group, checked ->
                        val selected = checked.firstOrNull()?.let { group.findViewById<Chip>(it) }
                        viewModel.setKategori(selected?.tag as? Long)
                    }
                    state.error?.let { Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).setAction(R.string.action_retry) { viewModel.loadCategories() }.show() }
                } }
                launch { cartViewModel.items.collect { items ->
                    binding.buttonCart.text = getString(R.string.catalog_cart, items.sumOf { it.jumlah })
                    binding.buttonCart.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
                } }
            }
        }
    }
}
