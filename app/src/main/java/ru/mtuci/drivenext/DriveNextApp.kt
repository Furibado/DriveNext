package ru.mtuci.drivenext

import android.app.Application

class DriveNextApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}