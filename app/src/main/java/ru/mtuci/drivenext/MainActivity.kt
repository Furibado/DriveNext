package ru.mtuci.drivenext

import android.os.Bundle
import android.content.Intent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.mtuci.drivenext.data.GoogleOAuth
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import ru.mtuci.drivenext.domain.NoConnectionException
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import ru.mtuci.drivenext.presentation.auth.HomeFragment
import ru.mtuci.drivenext.domain.StartDestination
import ru.mtuci.drivenext.presentation.auth.AuthChoiceFragment
import ru.mtuci.drivenext.presentation.auth.LoginFragment
import ru.mtuci.drivenext.presentation.noconnection.NoConnectionFragment
import ru.mtuci.drivenext.presentation.onboarding.OnboardingFragment
import ru.mtuci.drivenext.presentation.splash.SplashFragment

class MainActivity : AppCompatActivity() {
    private val session: ru.mtuci.drivenext.presentation.auth.SessionViewModel by viewModels()
    private var pendingSessionResult: Result<Boolean>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        session.loading.observe(this) { loading ->
            findViewById<android.widget.ProgressBar>(R.id.sessionProgress).visibility =
                if (loading) android.view.View.VISIBLE else android.view.View.GONE
        }
        session.result.observe(this) { result ->
            if (result != null) {
                pendingSessionResult = result
                applySessionResult()
            }
        }
        if (handleGoogleCallback(intent)) return

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SplashFragment())
                .commit()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (handleGoogleCallback(intent)) setIntent(Intent(this, MainActivity::class.java))
    }

    private fun handleGoogleCallback(intent: Intent): Boolean {
        val uri = intent.data ?: return false
        if (intent.action != Intent.ACTION_VIEW || !GoogleOAuth.matches(uri)) return false
        session.google(uri)
        return true
    }

    private fun showGoogleError() {
        if (supportFragmentManager.findFragmentById(R.id.fragment_container) == null) {
            show(LoginFragment())
        }
        MaterialAlertDialogBuilder(this)
            .setMessage("Не удалось завершить вход через Google. Попробуйте ещё раз.")
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    fun navigateTo(destination: StartDestination) {
        when (destination) {
            StartDestination.NO_CONNECTION -> show(NoConnectionFragment())
            StartDestination.ONBOARDING -> show(OnboardingFragment())
            StartDestination.LOGIN -> session.restore()
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        applySessionResult()
    }

    private fun applySessionResult() {
        if (supportFragmentManager.isStateSaved ||
            !lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) return
        val result = pendingSessionResult ?: return
        pendingSessionResult = null
        session.result.value = null
        result.fold(
            onSuccess = { if (it) openHome() else show(AuthChoiceFragment()) },
            onFailure = { error ->
                if (error is NoConnectionException) show(NoConnectionFragment())
                else showGoogleError()
            }
        )
    }

    fun showConnectionOverlay() {
        if (supportFragmentManager.isStateSaved) return
        if (supportFragmentManager.findFragmentByTag("connection_overlay") != null) return
        val current = supportFragmentManager.findFragmentById(R.id.fragment_container) ?: return
        supportFragmentManager.beginTransaction()
            .hide(current)
            .add(R.id.fragment_container, NoConnectionFragment().apply {
                arguments = bundleOf("return_to_form" to true)
            }, "connection_overlay")
            .addToBackStack("connection_overlay")
            .commit()
    }

    fun openHome() {
        supportFragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        show(HomeFragment())
    }

    fun openSettings() {
        if (supportFragmentManager.findFragmentById(R.id.fragment_container) is ru.mtuci.drivenext.presentation.profile.SettingsFragment) return
        val existing = (0 until supportFragmentManager.backStackEntryCount).any {
            supportFragmentManager.getBackStackEntryAt(it).name == "settings"
        }
        if (existing) supportFragmentManager.popBackStackImmediate("settings", 0)
        else supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, ru.mtuci.drivenext.presentation.profile.SettingsFragment())
            .addToBackStack("settings").commit()
    }

    fun openLoginAfterLogout() {
        pendingSessionResult = null
        session.result.value = null
        supportFragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        show(LoginFragment())
    }

    fun openLogin() {
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.fragment_container, LoginFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun show(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
