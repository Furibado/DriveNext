package ru.mtuci.drivenext.presentation.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import ru.mtuci.drivenext.DriveNextApp

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as DriveNextApp).container.authRepository
    private val oauth = ru.mtuci.drivenext.data.GoogleOAuth(application)
    fun googleUri(): android.net.Uri = oauth.start()
    fun cancelGoogle() = oauth.clear()

    val loading = MutableLiveData(false)
    val result = MutableLiveData<Result<Unit>?>()

    fun isEmailValid(value: String) = ru.mtuci.drivenext.domain.RegistrationValidation.isEmailValid(value)

    fun signIn(email: String, password: String) {
        if (loading.value == true) return
        loading.value = true
        viewModelScope.launch {
            try {
                repository.signIn(email, password)
                result.value = Result.success(Unit)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                result.value = Result.failure(error)
            } finally {
                loading.value = false
            }
        }
    }
}
