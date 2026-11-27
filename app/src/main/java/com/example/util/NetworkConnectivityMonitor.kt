package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged

class NetworkConnectivityMonitor(context: Context) {

    private val connectivityManager: ConnectivityManager? = runCatching {
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    }.onFailure { err ->
        Log.w(TAG, "ConnectivityManager unavailable", err)
    }.getOrNull()

    fun isCurrentlyOnline(): Boolean {
        return runCatching {
            val cm = connectivityManager ?: return@runCatching false
            val activeNetwork = cm.activeNetwork ?: return@runCatching false
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return@runCatching false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        }.onFailure { err ->
            Log.w(TAG, "Safe fallback in isCurrentlyOnline()", err)
        }.getOrDefault(false)
    }

    val isOnline: Flow<Boolean> = callbackFlow {
        trySend(isCurrentlyOnline())

        val cm = connectivityManager
        if (cm == null) {
            close()
            return@callbackFlow
        }

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
                val online = runCatching {
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                }.getOrDefault(false)
                trySend(online)
            }

            override fun onUnavailable() {
                trySend(false)
            }
        }

        val registered = runCatching {
            cm.registerDefaultNetworkCallback(callback)
            true
        }.onFailure { err ->
            Log.w(TAG, "Could not register default network callback", err)
        }.getOrDefault(false)

        awaitClose {
            if (registered) {
                runCatching { cm.unregisterNetworkCallback(callback) }
            }
        }
    }
        .catch { err ->
            Log.w(TAG, "Network flow error caught safely", err)
            emit(false)
        }
        .distinctUntilChanged()

    companion object {
        private const val TAG = "NetworkMonitor"
    }
}
