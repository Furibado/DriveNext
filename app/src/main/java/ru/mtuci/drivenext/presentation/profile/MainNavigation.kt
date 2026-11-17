package ru.mtuci.drivenext.presentation.profile

import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.mtuci.drivenext.MainActivity
import ru.mtuci.drivenext.R

internal fun Fragment.bindMainNavigation(navigation: BottomNavigationView, selected: Int) {
    navigation.isItemActiveIndicatorEnabled = false
    navigation.selectedItemId = selected
    navigation.setOnItemSelectedListener { item ->
        when (item.itemId) {
            R.id.nav_home -> { (requireActivity() as MainActivity).openHome(); true }
            R.id.nav_settings -> { (requireActivity() as MainActivity).openSettings(); true }
            else -> {
                MaterialAlertDialogBuilder(requireContext()).setTitle(item.title)
                    .setMessage(R.string.feature_not_ready).setPositiveButton(android.R.string.ok, null).show()
                false
            }
        }
    }
}
