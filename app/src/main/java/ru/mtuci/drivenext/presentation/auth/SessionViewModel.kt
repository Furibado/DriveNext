package ru.mtuci.drivenext.presentation.auth

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import ru.mtuci.drivenext.DriveNextApp
import ru.mtuci.drivenext.data.GoogleOAuth
import ru.mtuci.drivenext.domain.IncompleteRegistrationException

class SessionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as DriveNextApp).container.authRepository
    private val oauth = GoogleOAuth(application)
    val loading = MutableLiveData(false)
    val result = MutableLiveData<Result<Boolean>?>()
    private var running = false

    fun restore() = execute { 
        try {
            repository.restoreSession()
        } catch (_: IncompleteRegistrationException) {
            false
        }
    }

    fun google(uri: Uri) = execute {
        val code = uri.getQueryParameter("code") ?: error("Missing OAuth code")
        val verifier = oauth.verifier() ?: error("Missing OAuth verifier")
        repository.exchangeGoogleCode(code, verifier)
        oauth.clear()
        true
    }

    private fun execute(operation: suspend () -> Boolean) {
        if (running) return
        running = true
        loading.value = true
        viewModelScope.launch {
            try {
                result.value = Result.success(operation())
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                result.value = Result.failure(error)
            } finally {
                running = false
                loading.value = false
            }
        }
    }
}
