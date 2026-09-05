package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NetworkMonitor(context: Context) {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isOnline = MutableStateFlow(checkCurrentConnectivity())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var onNetworkAvailableListener: (() -> Unit)? = null

    private var callback: ConnectivityManager.NetworkCallback? = null

    init {
        registerNetworkCallback()
    }

    fun setOnNetworkAvailableListener(listener: () -> Unit) {
        this.onNetworkAvailableListener = listener
    }

    fun stopMonitoring() {
        callback?.let {
            try {
                connectivityManager?.unregisterNetworkCallback(it)
            } catch (_: Exception) {}
            callback = null
        }
    }

    private fun checkCurrentConnectivity(): Boolean {
        val cm = connectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun registerNetworkCallback() {
        val cm = connectivityManager ?: return
        val builder = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val wasOffline = !_isOnline.value
                _isOnline.value = true
                if (wasOffline) {
                    CoroutineScope(Dispatchers.Main).launch {
                        onNetworkAvailableListener?.invoke()
                    }
                }
            }

            override fun onLost(network: Network) {
                _isOnline.value = checkCurrentConnectivity()
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                val wasOffline = !_isOnline.value
                _isOnline.value = hasInternet
                if (wasOffline && hasInternet) {
                    CoroutineScope(Dispatchers.Main).launch {
                        onNetworkAvailableListener?.invoke()
                    }
                }
            }
        }
        callback = cb
        try {
            cm.registerNetworkCallback(builder.build(), cb)
        } catch (_: Exception) {}
    }
}
