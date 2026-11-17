package ru.mtuci.drivenext.presentation.profile

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.mtuci.drivenext.R
import ru.mtuci.drivenext.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment(R.layout.fragment_settings) {
    private val model: ProfileViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentSettingsBinding.bind(view)
        bindMainNavigation(binding.navigation.bottomNavigation, R.id.nav_settings)
        binding.openProfile.setOnClickListener {
            parentFragmentManager.beginTransaction().replace(R.id.fragment_container, ProfileFragment())
                .addToBackStack(null).commit()
        }
        binding.bookings.setOnClickListener { openDestination(false) }
        binding.connectCar.setOnClickListener { openDestination(true) }
        for ((row, title) in listOf(binding.theme to R.string.settings_theme,
            binding.notifications to R.string.settings_notifications,
            binding.help to R.string.settings_help, binding.invite to R.string.settings_invite)) {
            row.setOnClickListener {
                MaterialAlertDialogBuilder(requireContext()).setTitle(title)
                    .setMessage(R.string.feature_not_ready).setPositiveButton(android.R.string.ok, null).show()
            }
        }
        binding.retry.setOnClickListener { model.load() }
        model.state.observe(viewLifecycleOwner) { state ->
            binding.progress.isVisible = state.loading
            binding.name.text = state.profile?.name.orEmpty()
            binding.email.text = state.profile?.email.orEmpty()
            binding.error.isVisible = state.message != null
            state.message?.let { binding.error.setText(it) }
            binding.retry.isVisible = state.message != null && !state.loading
            // Отступы не меняются после построения круглой маски ShapeableImageView.
            binding.avatar.scaleType = if (state.avatar == null) android.widget.ImageView.ScaleType.CENTER_INSIDE
                else android.widget.ImageView.ScaleType.CENTER_CROP
            if (state.avatar != null) binding.avatar.setImageBitmap(state.avatar)
            else binding.avatar.setImageResource(R.drawable.settings_ic_profile)
        }
        model.load()
    }

    private fun openDestination(connectCar: Boolean) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, SettingsDestinationFragment.create(connectCar))
            .addToBackStack(null).commit()
    }
}
