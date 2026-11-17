package ru.mtuci.drivenext.presentation.profile

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import ru.mtuci.drivenext.MainActivity
import ru.mtuci.drivenext.R
import ru.mtuci.drivenext.databinding.FragmentProfileBinding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ProfileFragment : Fragment(R.layout.fragment_profile) {
    private val model: ProfileViewModel by viewModels()
    private val chooseAvatar = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { model.uploadAvatar(it.toString()) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentProfileBinding.bind(view)
        bindMainNavigation(binding.navigation.bottomNavigation, R.id.nav_settings)
        binding.back.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.changeAvatar.setOnClickListener { chooseAvatar.launch("image/*") }
        binding.logout.setOnClickListener { model.signOut() }
        binding.retry.setOnClickListener { model.load() }
        model.signedOut.observe(viewLifecycleOwner) { if (it) showLogin() }
        model.state.observe(viewLifecycleOwner) { state ->
            binding.progress.isVisible = state.loading || state.saving
            binding.changeAvatar.isEnabled = state.profile != null && !state.loading && !state.saving
            binding.logout.isEnabled = !state.saving
            binding.back.isEnabled = !state.saving
            val menu = binding.navigation.bottomNavigation.menu
            for (index in 0 until menu.size()) menu.getItem(index).isEnabled = !state.saving
            binding.error.isVisible = state.message != null
            state.message?.let { binding.error.setText(it) }
            binding.retry.isVisible = state.message != null && state.message != R.string.profile_upload_success && !state.loading && !state.saving
            binding.name.text = state.profile?.name.orEmpty()
            binding.email.text = state.profile?.email.orEmpty()
            binding.gender.setText(when (state.profile?.gender) {
                "male" -> R.string.profile_male
                "female" -> R.string.profile_female
                else -> R.string.profile_unspecified
            })
            binding.googleEmail.text = state.profile?.googleEmail?.takeIf { it.isNotBlank() }
                ?: getString(R.string.profile_google_not_connected)
            binding.joined.text = state.profile?.createdAt?.let { raw ->
                runCatching {
                    val date = Instant.parse(raw).atZone(ZoneId.systemDefault())
                    getString(R.string.profile_joined, DateTimeFormatter.ofPattern("LLLL yyyy", Locale.forLanguageTag("ru")).format(date))
                }.getOrDefault("")
            }.orEmpty()
            // Отступы не меняются после построения круглой маски ShapeableImageView.
            binding.avatar.scaleType = if (state.avatar == null) android.widget.ImageView.ScaleType.CENTER_INSIDE
                else android.widget.ImageView.ScaleType.CENTER_CROP
            if (state.avatar != null) binding.avatar.setImageBitmap(state.avatar)
            else binding.avatar.setImageResource(R.drawable.settings_ic_profile)
        }
        model.load()
    }

    override fun onResume() {
        super.onResume()
        if (model.signedOut.value == true) showLogin()
    }

    private fun showLogin() {
        if (!parentFragmentManager.isStateSaved) (requireActivity() as MainActivity).openLoginAfterLogout()
    }
}
