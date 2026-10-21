package ru.mtuci.drivenext.presentation.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import ru.mtuci.drivenext.R

data class OnboardingPage(
    @DrawableRes val imageRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int
)

val onboardingPages = listOf(
    OnboardingPage(R.drawable.onboarding_1, R.string.onboarding_title_1, R.string.onboarding_desc_1),
    OnboardingPage(R.drawable.onboarding_2, R.string.onboarding_title_2, R.string.onboarding_desc_2),
    OnboardingPage(R.drawable.onboarding_3, R.string.onboarding_title_3, R.string.onboarding_desc_3)
)