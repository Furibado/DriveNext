package ru.mtuci.drivenext.domain

interface AppStateRepository {
    fun isOnboardingCompleted(): Boolean
    fun setOnboardingCompleted()
}