package ru.mtuci.drivenext.presentation.auth

import android.os.Bundle
import android.content.Intent
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import ru.mtuci.drivenext.domain.NoConnectionException
import ru.mtuci.drivenext.domain.IncompleteRegistrationException
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.mtuci.drivenext.MainActivity
import ru.mtuci.drivenext.data.RegistrationRepository
import ru.mtuci.drivenext.R
import ru.mtuci.drivenext.databinding.FragmentLoginBinding

class LoginFragment : Fragment(R.layout.fragment_login) {
    private val model: LoginViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentLoginBinding.bind(view)

        fun updateButton() {
            val loading = model.loading.value == true
            binding.loginProgress.isVisible = loading
            binding.googleSignIn.isEnabled = !loading
            binding.openRegistration.isEnabled = !loading
            binding.signIn.isEnabled = !loading && !binding.email.text.isNullOrBlank() &&
                !binding.password.text.isNullOrEmpty()
        }

        binding.email.doAfterTextChanged {
            binding.emailLayout.error = null
            updateButton()
        }
        binding.password.doAfterTextChanged { updateButton() }
        binding.signIn.setOnClickListener {
            val email = binding.email.text.toString().trim()
            if (!model.isEmailValid(email)) {
                binding.emailLayout.error = getString(R.string.auth_invalid_email)
            } else {
                model.signIn(email, binding.password.text.toString())

            }
        }
        binding.googleSignIn.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, model.googleUri()))
            } catch (_: Exception) {
                model.cancelGoogle()
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage("Не удалось открыть вход через Google.")
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }
        binding.openRegistration.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, RegistrationFragment())
                .addToBackStack(null)
                .commit()
        }
        model.loading.observe(viewLifecycleOwner) { updateButton() }
        model.result.observe(viewLifecycleOwner) { result ->
            if (result == null) return@observe
            model.result.value = null
            result.fold(
                onSuccess = {
                    binding.password.text?.clear()
                    (requireActivity() as MainActivity).openHome()
                },
                onFailure = { error ->
                    if (error is NoConnectionException) {
                        (requireActivity() as MainActivity).showConnectionOverlay()
                    } else {
                        val message = when {
                            error is IncompleteRegistrationException ->
                                "Регистрация не завершена. Откройте регистрацию и заполните анкету с тем же email и паролем."
                            error is RegistrationRepository.ServerError && error.code == "invalid_credentials" ->
                                "Неверная электронная почта или пароль."
                            error is RegistrationRepository.ServerError && error.code == "email_not_confirmed" ->
                                "Подтвердите электронную почту перед входом."
                            else -> "Не удалось войти. Повторите попытку."
                        }
                        MaterialAlertDialogBuilder(requireContext()).setMessage(message)
                            .setPositiveButton(android.R.string.ok, null).show()
                    }
                }
            )
        }
        updateButton()
    }
}
