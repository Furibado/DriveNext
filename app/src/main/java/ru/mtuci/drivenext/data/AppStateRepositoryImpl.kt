package ru.mtuci.drivenext.data

import android.content.Context
import ru.mtuci.drivenext.domain.AppStateRepository

class AppStateRepositoryImpl(context: Context) : AppStateRepository {

    private val prefs = context.getSharedPreferences("drivenext_prefs", Context.MODE_PRIVATE)

    override fun isOnboardingCompleted(): Boolean =
        prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    override fun setOnboardingCompleted() {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
    }

    private companion object {
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }
}