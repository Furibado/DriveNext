package ru.mtuci.drivenext.presentation.profile

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.mtuci.drivenext.DriveNextApp
import ru.mtuci.drivenext.R
import ru.mtuci.drivenext.domain.UserProfile

data class ProfileState(
    val profile: UserProfile? = null,
    val avatar: Bitmap? = null,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val message: Int? = null
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as DriveNextApp).container.profileRepository
    val state = MutableLiveData(ProfileState())
    val signedOut = MutableLiveData(false)
    private var request: Job? = null

    fun load() {
        if (state.value!!.saving) return
        request?.cancel()
        state.value = state.value!!.copy(loading = true, message = null)
        request = viewModelScope.launch {
            try {
                val profile = repository.getProfile()
                state.value = ProfileState(profile = profile, loading = true)
                val (avatar, failed) = avatar(profile)
                state.value = ProfileState(profile, avatar, message = if (failed) R.string.profile_avatar_error else null)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                state.value = state.value!!.copy(loading = false, message = R.string.profile_load_error)
            }
        }
    }

    fun uploadAvatar(uri: String) {
        val current = state.value!!
        val profile = current.profile ?: return
        if (current.loading || current.saving) return
        state.value = current.copy(saving = true, message = null)
        request = viewModelScope.launch {
            try {
                repository.updateAvatar(profile, uri)
                val updated = repository.getProfile()
                val (image, failed) = avatar(updated)
                state.value = ProfileState(updated, image,
                    message = if (failed) R.string.profile_avatar_error else R.string.profile_upload_success)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                state.value = current.copy(message = R.string.profile_upload_error)
            }
        }
    }

    private suspend fun avatar(profile: UserProfile): Pair<Bitmap?, Boolean> = try {
        val bytes = repository.getAvatar(profile)
        val image = if (bytes == null) null else withContext(Dispatchers.Default) {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            var sample = 1
            while (maxOf(options.outWidth, options.outHeight) / sample > 512) sample *= 2
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
                ?: error("Invalid avatar")
        }
        image to false
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        null to true
    }

    fun signOut() {
        if (state.value!!.saving) return
        request?.cancel()
        state.value = state.value!!.copy(saving = true)
        request = viewModelScope.launch {
            repository.signOut()
            state.value = ProfileState()
            signedOut.value = true
        }
    }
}
