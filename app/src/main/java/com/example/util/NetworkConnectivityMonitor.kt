package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class NetworkConnectivityMonitor(context: Context) {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun isCurrentlyOnline(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
    }

    val isOnline: Flow<Boolean> = callbackFlow {
        trySend(isCurrentlyOnline())

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(isCurrentlyOnline())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val online = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                trySend(online)
            }

            override fun onUnavailable() {
                trySend(false)
            }
        }

        val registered = runCatching {
            connectivityManager.registerDefaultNetworkCallback(callback)
            true
        }.getOrDefault(false)

        awaitClose {
            if (registered) {
                runCatching { connectivityManager.unregisterNetworkCallback(callback) }
            }
        }
    }.distinctUntilChanged()
}
