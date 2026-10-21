package ru.mtuci.drivenext.domain

interface ConnectionRepository {
    fun isInternetAvailable(): Boolean
}