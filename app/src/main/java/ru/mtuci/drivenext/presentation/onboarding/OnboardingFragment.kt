package ru.mtuci.drivenext.presentation.onboarding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import ru.mtuci.drivenext.R
import ru.mtuci.drivenext.databinding.FragmentOnboardingBinding

class OnboardingFragment : Fragment(R.layout.fragment_onboarding) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentOnboardingBinding.bind(view)
        binding.viewPager.adapter =
            OnboardingAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
    }
}