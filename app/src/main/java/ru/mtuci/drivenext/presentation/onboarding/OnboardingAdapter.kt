package ru.mtuci.drivenext.presentation.onboarding

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class OnboardingAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int = onboardingPages.size

    override fun createFragment(position: Int): Fragment =
        OnboardingPageFragment.newInstance(onboardingPages[position])
}