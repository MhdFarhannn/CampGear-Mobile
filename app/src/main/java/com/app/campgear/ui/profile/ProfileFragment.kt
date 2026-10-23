package com.app.campgear.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.app.campgear.R
import com.app.campgear.core.Formatters
import com.app.campgear.databinding.FragmentProfileBinding
import com.app.campgear.ui.common.BaseFragment
import com.app.campgear.ui.common.confirmAction
import kotlinx.coroutines.launch

class ProfileFragment : BaseFragment<FragmentProfileBinding>() {

    private val viewModel: ProfileViewModel by viewModels { viewModelFactory }

    override fun createBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentProfileBinding =
        FragmentProfileBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.start()

        binding.buttonLogout.setOnClickListener { confirmLogout() }
        childFragmentManager.setFragmentResultListener("confirmed_action", viewLifecycleOwner) { _, result ->
            if (result.getString("action") == "logout") viewModel.logout()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    val user = state.user
                    if (user == null && state.loading) {
                        binding.stateLayout.showLoading()
                    } else if (user == null && state.error != null) {
                        binding.stateLayout.showError(state.error) { viewModel.load() }
                    } else if (user != null) {
                        binding.stateLayout.showContent()
                        binding.profileName.text = user.nama
                        binding.profileRole.text = roleLabel(user.role)
                        binding.profilePhone.text = user.noTelepon.orDash()
                        binding.profileAddress.text = user.alamat.orDash()
                        binding.profileGender.text = user.jenisKelamin.orDash()
                        binding.profileJoined.text = Formatters.displayDate(user.createdAt)
                        state.error?.let(::showError)
                    }

                    binding.buttonLogout.isEnabled = !state.loggingOut
                    binding.buttonLogout.text = getString(
                        if (state.loggingOut) R.string.action_logging_out else R.string.action_logout
                    )
                }
            }
        }
    }

    private fun confirmLogout() {
        confirmAction("logout", getString(R.string.profile_logout_confirm), getString(R.string.profile_logout_confirm_message))
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.state.value.user != null) viewModel.load()
    }

    private fun roleLabel(role: String?): String = when (role) {
        "Admin" -> getString(R.string.role_admin)
        "Petugas" -> getString(R.string.role_petugas)
        "Peminjam" -> getString(R.string.role_peminjam)
        else -> "-"
    }

    private fun String?.orDash(): String = if (isNullOrBlank()) "-" else this
}
