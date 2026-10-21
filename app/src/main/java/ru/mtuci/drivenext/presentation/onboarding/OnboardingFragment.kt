package ru.mtuci.drivenext.presentation.onboarding

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import ru.mtuci.drivenext.MainActivity
import ru.mtuci.drivenext.R
import ru.mtuci.drivenext.databinding.FragmentOnboardingBinding
import ru.mtuci.drivenext.domain.StartDestination

class OnboardingFragment : Fragment(R.layout.fragment_onboarding) {

    private val viewModel: OnboardingViewModel by viewModels { OnboardingViewModel.Factory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentOnboardingBinding.bind(view)
        val lastIndex = onboardingPages.size - 1

        binding.viewPager.adapter =
            OnboardingAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateScreen(binding, position, lastIndex)
            }
        })
        updateScreen(binding, binding.viewPager.currentItem, lastIndex)

        binding.nextButton.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current < lastIndex) {
                binding.viewPager.setCurrentItem(current + 1, true)
            } else {
                viewModel.finish()
            }
        }
        binding.skipButton.setOnClickListener { viewModel.finish() }

        viewModel.finished.observe(viewLifecycleOwner) {
            (requireActivity() as MainActivity).navigateTo(StartDestination.LOGIN)
        }
    }

    private fun updateScreen(binding: FragmentOnboardingBinding, position: Int, lastIndex: Int) {
        binding.nextButton.setText(if (position == lastIndex) R.string.go else R.string.next)

        val dots = listOf(binding.dot1, binding.dot2, binding.dot3)
        val density = resources.displayMetrics.density
        dots.forEachIndexed { index, dot ->
            val active = index == position
            val color = if (active) R.color.brand_dark else R.color.dot_inactive
            dot.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), color))
            dot.layoutParams = dot.layoutParams.apply {
                width = ((if (active) 24 else 8) * density).toInt()
            }
        }
    }
}