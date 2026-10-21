package ru.mtuci.drivenext.presentation.splash

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.mtuci.drivenext.DriveNextApp
import ru.mtuci.drivenext.domain.GetStartDestinationUseCase
import ru.mtuci.drivenext.domain.StartDestination

class SplashViewModel(
    private val getStartDestination: GetStartDestinationUseCase
) : ViewModel() {

    private val _destination = MutableLiveData<StartDestination>()
    val destination: LiveData<StartDestination> = _destination

    init {
        viewModelScope.launch {
            delay(SPLASH_DELAY_MS)
            _destination.value = getStartDestination()
        }
    }

    companion object {
        private const val SPLASH_DELAY_MS = 2500L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as DriveNextApp
                SplashViewModel(app.container.getStartDestinationUseCase)
            }
        }
    }
}