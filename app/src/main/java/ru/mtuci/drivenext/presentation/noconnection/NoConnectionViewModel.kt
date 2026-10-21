package ru.mtuci.drivenext.presentation.noconnection

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ru.mtuci.drivenext.DriveNextApp
import ru.mtuci.drivenext.domain.GetStartDestinationUseCase
import ru.mtuci.drivenext.domain.StartDestination

class NoConnectionViewModel(
    private val getStartDestination: GetStartDestinationUseCase
) : ViewModel() {

    private val _destination = MutableLiveData<StartDestination>()
    val destination: LiveData<StartDestination> = _destination

    fun retry() {
        val result = getStartDestination()
        // Если интернета всё ещё нет, ничего не меняем: экран остаётся на месте
        if (result != StartDestination.NO_CONNECTION) {
            _destination.value = result
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as DriveNextApp
                NoConnectionViewModel(app.container.getStartDestinationUseCase)
            }
        }
    }
}