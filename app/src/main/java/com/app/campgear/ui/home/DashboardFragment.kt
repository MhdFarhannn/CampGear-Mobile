package com.app.campgear.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.app.campgear.R
import com.app.campgear.databinding.FragmentDashboardBinding
import com.app.campgear.ui.common.BaseFragment
import kotlinx.coroutines.launch

class DashboardFragment : BaseFragment<FragmentDashboardBinding>() {
    private val viewModel: DashboardViewModel by viewModels { viewModelFactory }
    override fun createBinding(inflater: LayoutInflater, container: ViewGroup?) = FragmentDashboardBinding.inflate(inflater, container, false)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val session = appContainer.sessionManager.session.value
        binding.homeGreeting.text = getString(R.string.home_greeting, session?.nama.orEmpty())
        val role = session?.role
        binding.homeSubtitle.setText(when (role) {
            "Admin" -> R.string.home_admin_subtitle
            "Petugas" -> R.string.home_staff_subtitle
            else -> R.string.home_borrower_subtitle
        })
        binding.homePrimary.setText(if (role == "Peminjam") R.string.menu_catalog else R.string.menu_admin_loans)
        binding.homePrimary.setOnClickListener {
            findNavController().navigate(when (role) { "Admin" -> R.id.adminLoans; "Petugas" -> R.id.staffRequests; else -> R.id.catalog })
        }
        binding.homeSecondary.setText(if (role == "Admin") R.string.menu_admin_alat else if (role == "Petugas") R.string.menu_staff_monitor else R.string.menu_my_loans)
        binding.homeSecondary.setOnClickListener {
            findNavController().navigate(when (role) { "Admin" -> R.id.adminAlat; "Petugas" -> R.id.staffMonitor; else -> R.id.myLoans })
        }
        binding.swipeRefresh.setOnRefreshListener { viewModel.load() }
        viewModel.load()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.swipeRefresh.isRefreshing = state.loading && state.metrics.isNotEmpty()
                    when {
                        state.loading && state.metrics.isEmpty() -> binding.stateLayout.showLoading()
                        state.error != null && state.metrics.isEmpty() -> binding.stateLayout.showError(state.error) { viewModel.load() }
                        else -> {
                            binding.stateLayout.showContent()
                            val labels = listOf(binding.metricLabel1, binding.metricLabel2, binding.metricLabel3, binding.metricLabel4)
                            val values = listOf(binding.metricValue1, binding.metricValue2, binding.metricValue3, binding.metricValue4)
                            state.metrics.forEachIndexed { index, metric ->
                                labels[index].setText(metricLabel(metric.key))
                                values[index].text = java.text.NumberFormat.getIntegerInstance().format(metric.value)
                            }
                            state.error?.let(::showError)
                        }
                    }
                }
            }
        }
    }

    private fun metricLabel(key: String) = when (key) {
        "equipment" -> R.string.metric_equipment
        "users" -> R.string.metric_users
        "returns" -> R.string.metric_returns
        "pending" -> R.string.metric_pending
        "approved" -> R.string.metric_approved
        "overdue" -> R.string.metric_overdue
        "due" -> R.string.metric_due
        "completed" -> R.string.metric_completed
        else -> R.string.metric_borrowed
    }
}
