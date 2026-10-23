package com.app.campgear.ui.auth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.app.campgear.R
import com.app.campgear.databinding.FragmentLoginBinding
import com.app.campgear.ui.common.BaseFragment
import kotlinx.coroutines.launch

class LoginFragment : BaseFragment<FragmentLoginBinding>() {

    private val viewModel: LoginViewModel by viewModels { viewModelFactory }

    override fun createBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLoginBinding =
        FragmentLoginBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.inputPhone.doAfterTextChanged {
            binding.tilPhone.error = null
            viewModel.clearError()
        }
        binding.inputPassword.doAfterTextChanged {
            binding.tilPassword.error = null
            viewModel.clearError()
        }

        binding.inputPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit()
                true
            } else {
                false
            }
        }

        binding.buttonLogin.setOnClickListener { submit() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.buttonLogin.isEnabled = !state.loading
                    binding.buttonLogin.text = getString(
                        if (state.loading) R.string.symbol_blank else R.string.action_login
                    )
                    binding.buttonLogin.icon = if (state.loading) null else androidx.appcompat.content.res.AppCompatResources.getDrawable(requireContext(), R.drawable.ic_arrow_forward)
                    binding.inputPhone.isEnabled = !state.loading
                    binding.inputPassword.isEnabled = !state.loading
                    binding.loginProgress.visibility =
                        if (state.loading) View.VISIBLE else View.GONE

                    if (state.error != null) {
                        binding.textLoginError.text = state.error
                        binding.textLoginError.visibility = View.VISIBLE
                        if (state.error == getString(R.string.login_invalid_credentials)) binding.tilPassword.error = state.error
                    } else {
                        binding.textLoginError.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun submit() {
        val phone = binding.inputPhone.text?.toString()?.trim().orEmpty()
        val password = binding.inputPassword.text?.toString().orEmpty()

        binding.tilPhone.error = null
        binding.tilPassword.error = null

        if (phone.isEmpty() || password.isEmpty()) {
            if (phone.isEmpty()) binding.tilPhone.error = getString(R.string.login_error_empty)
            if (password.isEmpty()) binding.tilPassword.error = getString(R.string.login_error_empty)
            return
        }

        viewModel.login(phone, password)
    }
}
