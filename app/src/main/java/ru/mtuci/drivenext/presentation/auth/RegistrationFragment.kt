package ru.mtuci.drivenext.presentation.auth

import android.net.Uri
import android.os.Bundle
import ru.mtuci.drivenext.domain.RegistrationValidation
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import ru.mtuci.drivenext.domain.RegistrationData
import ru.mtuci.drivenext.domain.NoConnectionException
import ru.mtuci.drivenext.DriveNextApp
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import ru.mtuci.drivenext.data.RegistrationRepository
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.mtuci.drivenext.MainActivity
import ru.mtuci.drivenext.R
import ru.mtuci.drivenext.databinding.FragmentRegistrationBinding
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class RegistrationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as DriveNextApp).container.authRepository
    fun isEmailValid(value: String) = RegistrationValidation.isEmailValid(value)
    fun dateForStorage(value: String, format: String) = RegistrationValidation.dateForStorage(value, format)
    fun passwordsMatch() = RegistrationValidation.passwordsMatch(password, confirmation)
    var password = ""
    var confirmation = ""
    val result = MutableLiveData<Result<Unit>?>()
    val loading = MutableLiveData(false)

    internal fun submit(email: String, profile: RegistrationData, photos: Map<String, String>) {
        if (loading.value == true) return
        val submittedPassword = password
        loading.value = true
        viewModelScope.launch {
            try {
                repository.register(email, submittedPassword, profile, photos)
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

class RegistrationFragment : Fragment(R.layout.fragment_registration) {
    private enum class Photo { AVATAR, LICENSE, PASSPORT }

    private val form: RegistrationViewModel by viewModels()
    private var binding: FragmentRegistrationBinding? = null
    private var step = 0
    private var photoTarget = Photo.AVATAR
    private var pendingCameraUri: Uri? = null
    private val photos = mutableMapOf<Photo, Uri>()

    private val gallery = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            runCatching {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            acceptPhoto(uri)
        }
    }
    private val camera = registerForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (taken && uri != null) acceptPhoto(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        step = savedInstanceState?.getInt("step") ?: 0
        photoTarget = Photo.valueOf(savedInstanceState?.getString("target") ?: Photo.AVATAR.name)
        pendingCameraUri = savedInstanceState?.getString("camera")?.toUri()
        Photo.entries.forEach { photo ->
            savedInstanceState?.getString(photo.name)?.let { photos[photo] = it.toUri() }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("step", step)
        outState.putString("target", photoTarget.name)
        outState.putString("camera", pendingCameraUri?.toString())
        photos.forEach { (photo, uri) -> outState.putString(photo.name, uri.toString()) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentRegistrationBinding.bind(view)
        binding = b
        b.password.setText(form.password)
        b.confirmation.setText(form.confirmation)
        b.password.doAfterTextChanged { form.password = it.toString(); render() }
        b.confirmation.doAfterTextChanged { form.confirmation = it.toString(); b.confirmation.error = null; render() }
        listOf(b.email, b.surname, b.givenName, b.licenseNumber, b.birthDate, b.issueDate)
            .forEach { input -> input.doAfterTextChanged { input.error = null; render() } }
        b.consent.setOnCheckedChangeListener { _, _ -> render() }
        b.gender.setOnCheckedChangeListener { _, _ -> render() }
        b.back.setOnClickListener { goBack() }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goBack()
        })
        setupDatePicker(b.birthDate, "birth_date", "MM/dd/uuuu", "Дата рождения")
        setupDatePicker(b.issueDate, "issue_date", "dd/MM/uuuu", "Дата выдачи")
        b.avatar.setOnClickListener { choosePhoto(Photo.AVATAR) }
        b.avatarPreview.setOnClickListener { choosePhoto(Photo.AVATAR) }
        b.license.setOnClickListener { choosePhoto(Photo.LICENSE) }
        b.passport.setOnClickListener { choosePhoto(Photo.PASSPORT) }
        b.next.setOnClickListener { advance(b) }
        form.loading.observe(viewLifecycleOwner) { render() }
        form.result.observe(viewLifecycleOwner) { result ->
            if (result == null) return@observe
            form.result.value = null
            result.fold(
                onSuccess = {
                    form.password = ""
                    form.confirmation = ""
                    b.password.text?.clear()
                    b.confirmation.text?.clear()
                    step = 3
                    b.root.smoothScrollTo(0, 0)
                    render()
                },
                onFailure = { error ->
                    if (error is NoConnectionException) {
                        (requireActivity() as MainActivity).showConnectionOverlay()
                        return@fold
                    }
                    val message = when (error) {
                        is RegistrationRepository.ConfirmationRequired ->
                            "Подтвердите электронную почту и снова нажмите «Далее»."
                        is RegistrationRepository.ProfileExists ->
                            "Профиль уже создан. Войдите в свой аккаунт."
                        is RegistrationRepository.ServerError -> when (error.code) {
                            "weak_password" -> "Выберите более надёжный пароль."
                            "user_already_exists", "email_exists" -> "Этот email уже зарегистрирован."
                            else -> "Supabase отклонил запрос (код ${error.status})."
                        }
                        else -> "Не удалось завершить регистрацию. Проверьте подключение и выбранные фотографии."
                    }
                    MaterialAlertDialogBuilder(requireContext())
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
            )
        }
        render()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        render()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun goBack() {
        if (form.loading.value == true) return
        if (step == 3) (requireActivity() as MainActivity).openHome()
        else if (step == 0) parentFragmentManager.popBackStack()
        else {
            step--
            render()
        }
    }

    private fun advance(b: FragmentRegistrationBinding) {
        if (step == 0) {
            if (!form.isEmailValid(b.email.text.toString())) {
                b.email.error = getString(R.string.auth_invalid_email)
                return
            }
            if (!form.passwordsMatch()) {
                b.confirmation.error = "Пароли не совпадают"
                return
            }
        }
        if (step == 3) {
            (requireActivity() as MainActivity).openHome()
            return
        }
        if (step == 2) {
            fun date(text: CharSequence?, format: String): String =
                form.dateForStorage(text.toString(), format)
            val profile = RegistrationData(
                surname = b.surname.text.toString().trim(),
                givenName = b.givenName.text.toString().trim(),
                patronymic = b.patronymic.text.toString().trim().ifBlank { null },
                birthDate = date(b.birthDate.text, "MM/dd/uuuu"),
                gender = if (b.gender.checkedRadioButtonId == R.id.male) "male" else "female",
                licenseNumber = b.licenseNumber.text.toString(),
                licenseIssueDate = date(b.issueDate.text, "dd/MM/uuuu")
            )
            form.submit(
                b.email.text.toString().trim(),
                profile,
                photos.mapKeys { it.key.name.lowercase() }.mapValues { it.value.toString() }
            )
            return
        }
        step++
        b.root.clearFocus()
        b.root.smoothScrollTo(0, 0)
        render()
    }

    private fun render() {
        val b = binding ?: return
        b.account.isVisible = step == 0
        b.profile.isVisible = step == 1
        b.documents.isVisible = step == 2
        b.documentsSpacer.isVisible = step != 3
        b.success.isVisible = step == 3
        b.title.text = if (step == 3) "Пользуйтесь с удовольствием" else "Создать аккаунт"
        b.back.isVisible = step != 3
        b.next.text = when (step) {
            3 -> "Далее"
            else -> "Далее"
        }
        val loading = form.loading.value == true
        b.registrationProgress.isVisible = loading
        b.back.isEnabled = !loading
        b.avatar.isEnabled = !loading
        b.avatarPreview.isEnabled = !loading
        b.license.isEnabled = !loading
        b.passport.isEnabled = !loading
        b.licenseNumber.isEnabled = !loading
        b.issueDate.isEnabled = !loading
        b.next.isEnabled = !loading && when (step) {
            0 -> !b.email.text.isNullOrBlank() && form.password.isNotEmpty() &&
                form.confirmation.isNotEmpty() && b.consent.isChecked
            1 -> !b.surname.text.isNullOrBlank() && !b.givenName.text.isNullOrBlank() &&
                !b.birthDate.text.isNullOrBlank() && b.gender.checkedRadioButtonId != View.NO_ID
            2 -> b.licenseNumber.text?.length == 10 && !b.issueDate.text.isNullOrBlank() &&
                Photo.LICENSE in photos && Photo.PASSPORT in photos
            else -> true
        }
        b.validationHint.text = when {
            step == 0 && !b.consent.isChecked ->
                "Необходимо согласиться с условиями обслуживания и политикой конфиденциальности."
            step == 1 && !b.next.isEnabled && !loading ->
                "Пожалуйста, заполните все обязательные поля."
            step == 2 && (Photo.LICENSE !in photos || Photo.PASSPORT !in photos) ->
                "Пожалуйста, загрузите все необходимые фото."
            step == 2 && !b.next.isEnabled && !loading ->
                "Пожалуйста, заполните все обязательные поля."
            else -> ""
        }
        b.validationHint.isVisible = b.validationHint.text.isNotEmpty()
        b.license.text = if (Photo.LICENSE in photos) "Фото выбрано" else "Загрузить фото"
        b.passport.text = if (Photo.PASSPORT in photos) "Фото выбрано" else "Загрузить фото"
    }

    private fun setupDatePicker(input: android.widget.EditText, tag: String, format: String, title: String) {
        fun listen(picker: MaterialDatePicker<Long>) {
            picker.addOnPositiveButtonClickListener { millis ->
                val date = Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).toLocalDate()
                input.setText(date.format(DateTimeFormatter.ofPattern(format)))
            }
        }
        @Suppress("UNCHECKED_CAST")
        (childFragmentManager.findFragmentByTag(tag) as? MaterialDatePicker<Long>)?.let { listen(it) }
        input.setOnClickListener {
            if (childFragmentManager.findFragmentByTag(tag) != null) return@setOnClickListener
            val constraints = CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now())
                .build()
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(title)
                .setCalendarConstraints(constraints)
                .build()
            listen(picker)
            picker.showNow(childFragmentManager, tag)
        }
    }

    private fun choosePhoto(photo: Photo) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Источник фотографии")
            .setItems(arrayOf("Галерея", "Камера")) { _, index ->
                photoTarget = photo
                if (index == 0) gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                else takePhoto()
            }
            .show()
    }

    private fun takePhoto() {
        try {
            val directory = File(requireContext().cacheDir, "registration").apply { mkdirs() }
            val file = File.createTempFile("photo_", ".jpg", directory)
            val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.registration", file)
            pendingCameraUri = uri
            camera.launch(uri)
        } catch (_: android.content.ActivityNotFoundException) {
            pendingCameraUri = null
            MaterialAlertDialogBuilder(requireContext())
                .setMessage("Камера недоступна")
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    private fun acceptPhoto(uri: Uri) {
        photos[photoTarget] = uri
        showAvatar()
        render()
    }

    override fun onStart() {
        super.onStart()
        showAvatar()
    }

    private fun showAvatar() {
        photos[Photo.AVATAR]?.let { binding?.avatarPreview?.setImageURI(it) }
    }
}
