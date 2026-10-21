package ru.mtuci.drivenext.presentation.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ru.mtuci.drivenext.DriveNextApp
import ru.mtuci.drivenext.domain.CompleteOnboardingUseCase

class OnboardingViewModel(
    private val completeOnboarding: CompleteOnboardingUseCase
) : ViewModel() {

    private val _finished = MutableLiveData<Boolean>()
    val finished: LiveData<Boolean> = _finished

    fun finish() {
        completeOnboarding()
        _finished.value = true
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as DriveNextApp
                OnboardingViewModel(app.container.completeOnboardingUseCase)
            }
        }
    }
}