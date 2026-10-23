package com.app.campgear.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.campgear.R
import com.app.campgear.databinding.FragmentAlatManageBinding
import com.app.campgear.ui.common.BaseFragment
import com.app.campgear.ui.common.confirmAction
import kotlinx.coroutines.launch

class AlatManageFragment : BaseFragment<FragmentAlatManageBinding>() {

    private val viewModel: AlatManageViewModel by viewModels { viewModelFactory }
    private lateinit var adapter: AlatManageAdapter
    private var resumedOnce = false
    private var lastActionError: String? = null

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAlatManageBinding = FragmentAlatManageBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AlatManageAdapter(
            onEdit = { alat ->
                findNavController().navigate(
                    R.id.alatForm,
                    Bundle().apply { putLong("alatId", alat.id) }
                )
            },
            onUnits = { alat ->
                findNavController().navigate(
                    R.id.unitAlat,
                    Bundle().apply {
                        putLong("alatId", alat.id)
                        putString("alatNama", alat.nama)
                    }
                )
            },
            onDelete = { alat -> confirmDelete(alat.id, alat.nama) }
        )
        binding.recyclerAlatAdmin.adapter = adapter
        childFragmentManager.setFragmentResultListener("confirmed_action", viewLifecycleOwner) { _, result ->
            if (result.getString("action") == "delete_equipment") viewModel.delete(result.getLong("id"))
        }
        binding.recyclerAlatAdmin.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                if (dy > 0 && layoutManager.findLastVisibleItemPosition() >= adapter.itemCount - 3) {
                    viewModel.loadMore()
                }
            }
        })

        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.tilAlatSearch.setEndIconOnClickListener { binding.inputAlatSearch.setText("") }
        binding.inputAlatSearch.doAfterTextChanged {
            viewModel.setSearch(it?.toString().orEmpty())
        }
        binding.buttonAddAlat.setOnClickListener {
            findNavController().navigate(
                R.id.alatForm,
                Bundle().apply { putLong("alatId", 0L) }
            )
        }

        viewModel.start()
        observe()
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
                        emptyMessage = getString(R.string.alat_empty),
                        paginationProgress = binding.paginationProgress,
                        onRetry = { viewModel.refresh() }
                    )
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.actionError.collect { error ->
                    if (error != null && error != lastActionError) {
                        showError(error)
                        viewModel.clearActionError()
                    }
                    lastActionError = error
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.busy.collect { binding.buttonAddAlat.isEnabled = !it }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.deleted.collect {
                    Toast.makeText(
                        requireContext(),
                        R.string.alat_deleted,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun confirmDelete(id: Long, nama: String) {
        confirmAction("delete_equipment", getString(R.string.alat_delete_confirm), getString(R.string.delete_named, nama), id)
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
