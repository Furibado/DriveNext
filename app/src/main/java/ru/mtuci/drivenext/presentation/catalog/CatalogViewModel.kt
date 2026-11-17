package ru.mtuci.drivenext.presentation.catalog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import ru.mtuci.drivenext.DriveNextApp
import ru.mtuci.drivenext.domain.Car

data class CatalogState(
    val query: String = "",
    val results: Boolean = false,
    val loading: Boolean = true,
    val cars: List<Car> = emptyList(),
    val failed: Boolean = false
)

class CatalogViewModel(application: Application, private val saved: SavedStateHandle) : AndroidViewModel(application) {
    private val repository = (application as DriveNextApp).container.carRepository
    val state = MutableLiveData(CatalogState(saved["query"] ?: "", saved["results"] ?: false))
    private var request: Job? = null

    init { load() }

    fun search(query: String, immediately: Boolean = false) {
        val current = state.value!!
        if (query == current.query && !immediately) return
        saved["query"] = query
        saved["results"] = query.isNotBlank()
        state.value = CatalogState(query, query.isNotBlank())
        load(if (immediately) 0 else 450)
    }

    fun home() {
        saved["query"] = ""
        saved["results"] = false
        state.value = CatalogState()
        load()
    }

    fun load(debounceMillis: Long = 0) {
        request?.cancel()
        val current = state.value!!
        state.value = current.copy(loading = true, cars = emptyList(), failed = false)
        request = viewModelScope.launch {
            try {
                delay(debounceMillis)
                val cars = repository.getCars(current.query)
                ensureActive()
                state.value = current.copy(loading = false, cars = cars, failed = false)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                state.value = current.copy(loading = false, cars = emptyList(), failed = true)
            }
        }
    }
}
