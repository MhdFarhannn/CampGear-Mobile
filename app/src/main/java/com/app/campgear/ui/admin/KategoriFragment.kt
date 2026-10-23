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
import com.app.campgear.databinding.FragmentKategoriBinding
import com.app.campgear.ui.common.BaseFragment
import com.app.campgear.ui.common.confirmAction
import kotlinx.coroutines.launch

class KategoriFragment : BaseFragment<FragmentKategoriBinding>() {
    private val viewModel: KategoriViewModel by viewModels { viewModelFactory }
    override fun createBinding(inflater: LayoutInflater, container: ViewGroup?) = FragmentKategoriBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = KategoriAdapter(onEdit = { openForm(it.id) }, onDelete = { category ->
            confirmAction("delete_category", getString(R.string.kategori_delete_confirm), getString(R.string.delete_named, category.nama), category.id)
        })
        childFragmentManager.setFragmentResultListener("confirmed_action", viewLifecycleOwner) { _, result ->
            if (result.getString("action") == "delete_category") viewModel.delete(result.getLong("id"))
        }
        binding.recyclerKategori.adapter = adapter
        binding.inputCategorySearch.doAfterTextChanged { query ->
            val items = viewModel.state.value.items.filter { it.nama.contains(query.toString(), true) }
            adapter.submitList(items)
            if (items.isEmpty() && viewModel.state.value.items.isNotEmpty()) binding.stateLayout.showEmpty(getString(R.string.search_empty), {
                binding.inputCategorySearch.setText("")
            }, R.string.action_reset)
            else if (items.isNotEmpty()) binding.stateLayout.showContent()
        }
        binding.swipeRefresh.setOnRefreshListener { viewModel.load() }
        binding.buttonAddKategori.setOnClickListener { openForm(0) }
        viewModel.start()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect { state ->
                    val visible = state.items.filter { it.nama.contains(binding.inputCategorySearch.text.toString(), true) }
                    adapter.submitList(visible)
                    binding.swipeRefresh.isRefreshing = state.loading && state.items.isNotEmpty()
                    binding.kategoriProgress.visibility = if (state.busy) View.VISIBLE else View.GONE
                    binding.buttonAddKategori.isEnabled = !state.busy
                    when {
                        state.loading && state.items.isEmpty() -> binding.stateLayout.showLoading()
                        state.error != null && state.items.isEmpty() -> binding.stateLayout.showError(state.error) { viewModel.load() }
                        state.items.isEmpty() -> binding.stateLayout.showEmpty(getString(R.string.kategori_empty)) { openForm(0) }
                        visible.isEmpty() -> binding.stateLayout.showEmpty(getString(R.string.search_empty), {
                            binding.inputCategorySearch.setText("")
                        }, R.string.action_reset)
                        else -> {
                            binding.stateLayout.showContent()
                            state.error?.let(::showError)
                        }
                    }
                } }
                launch { viewModel.actionError.collect { it?.let { error -> showError(error); viewModel.clearActionError() } } }
                launch { viewModel.actions.collect { showMessage(getString(R.string.kategori_deleted)) } }
            }
        }
    }
    private fun openForm(id: Long) = findNavController().navigate(R.id.masterForm, Bundle().apply {
        putString("kind", "category"); putLong("entityId", id)
    })
    override fun onDataChanged() = viewModel.load()
}
