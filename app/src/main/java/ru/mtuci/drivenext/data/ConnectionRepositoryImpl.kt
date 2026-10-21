package ru.mtuci.drivenext.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import ru.mtuci.drivenext.domain.ConnectionRepository

class ConnectionRepositoryImpl(private val context: Context) : ConnectionRepository {

    override fun isInternetAvailable(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}