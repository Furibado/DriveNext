package ru.mtuci.drivenext.presentation.auth

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import ru.mtuci.drivenext.MainActivity
import ru.mtuci.drivenext.R
import ru.mtuci.drivenext.databinding.FragmentAuthChoiceBinding

class AuthChoiceFragment : Fragment(R.layout.fragment_auth_choice) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentAuthChoiceBinding.bind(view)
        binding.openLogin.setOnClickListener { (requireActivity() as MainActivity).openLogin() }
        binding.openRegistration.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, RegistrationFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}
