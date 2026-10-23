package com.app.campgear.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.app.campgear.R
import com.app.campgear.databinding.FragmentUsersBinding
import com.app.campgear.ui.common.BaseFragment
import com.app.campgear.ui.common.onNearEnd
import com.app.campgear.ui.common.confirmAction
import kotlinx.coroutines.launch

class UsersFragment : BaseFragment<FragmentUsersBinding>() {
    private val viewModel: UsersViewModel by viewModels { viewModelFactory }
    override fun createBinding(inflater: LayoutInflater, container: ViewGroup?) = FragmentUsersBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = UserAdapter(onEdit = { openForm(it.id) }, onDelete = { user ->
            confirmAction("delete_user", getString(R.string.users_delete_confirm), getString(R.string.delete_named, user.nama), user.id)
        })
        childFragmentManager.setFragmentResultListener("confirmed_action", viewLifecycleOwner) { _, result ->
            if (result.getString("action") == "delete_user") viewModel.delete(result.getLong("id"))
        }
        binding.recyclerUsers.adapter = adapter
        binding.recyclerUsers.onNearEnd { viewModel.loadMore() }
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.inputUserSearch.doAfterTextChanged { viewModel.setSearch(it.toString()) }
        binding.buttonAddUser.setOnClickListener { openForm(0) }
        viewModel.start()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect { state ->
                    adapter.submitList(state.items)
                    renderPagedState(state, binding.stateLayout, binding.swipeRefresh, getString(R.string.users_empty), binding.paginationProgress) { viewModel.refresh() }
                } }
                launch { viewModel.busy.collect {
                    binding.usersProgress.visibility = if (it) View.VISIBLE else View.GONE
                    binding.buttonAddUser.isEnabled = !it
                } }
                launch { viewModel.actionError.collect { it?.let { error -> showError(error); viewModel.clearActionError() } } }
                launch { viewModel.actions.collect { showMessage(getString(R.string.users_deleted)) } }
            }
        }
    }

    private fun openForm(id: Long) = findNavController().navigate(R.id.masterForm, Bundle().apply {
        putString("kind", "user"); putLong("entityId", id)
    })

    override fun onDataChanged() = viewModel.refresh()
}
