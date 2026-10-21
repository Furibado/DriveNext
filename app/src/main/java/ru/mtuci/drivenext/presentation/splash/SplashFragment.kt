package ru.mtuci.drivenext.presentation.splash

import android.os.Bundle
import android.view.View
import ru.mtuci.drivenext.MainActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import ru.mtuci.drivenext.R

class SplashFragment : Fragment(R.layout.fragment_splash) {

    private val viewModel: SplashViewModel by viewModels { SplashViewModel.Factory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.destination.observe(viewLifecycleOwner) { destination ->
            (requireActivity() as MainActivity).navigateTo(destination)
        }
    }
}