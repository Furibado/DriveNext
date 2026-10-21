package ru.mtuci.drivenext

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import ru.mtuci.drivenext.presentation.splash.SplashFragment
import android.widget.Toast
import androidx.fragment.app.Fragment
import ru.mtuci.drivenext.domain.StartDestination
import ru.mtuci.drivenext.presentation.noconnection.NoConnectionFragment
import ru.mtuci.drivenext.presentation.onboarding.OnboardingFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen() // обязательно ДО super.onCreate и setContentView
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SplashFragment())
                .commit()
        }
    }
    fun navigateTo(destination: StartDestination) {
        when (destination) {
            StartDestination.NO_CONNECTION -> show(NoConnectionFragment())
            StartDestination.ONBOARDING -> show(OnboardingFragment())
            StartDestination.LOGIN ->
                Toast.makeText(this, destination.name, Toast.LENGTH_LONG).show() // временно
        }
    }

    private fun show(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}