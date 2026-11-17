package ru.mtuci.drivenext.presentation.profile

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import ru.mtuci.drivenext.R
import ru.mtuci.drivenext.databinding.FragmentSettingsDestinationBinding

class SettingsDestinationFragment : Fragment(R.layout.fragment_settings_destination) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentSettingsDestinationBinding.bind(view)
        val connect = requireArguments().getBoolean("connect_car")
        binding.title.setText(if (connect) R.string.settings_connect_car else R.string.settings_bookings)
        binding.message.setText(R.string.feature_not_ready)
        binding.back.setOnClickListener { parentFragmentManager.popBackStack() }
    }
    companion object {
        fun create(connectCar: Boolean) = SettingsDestinationFragment().apply {
            arguments = bundleOf("connect_car" to connectCar)
        }
    }
}
