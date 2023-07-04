package org.obd.graphs

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import pub.devrel.easypermissions.EasyPermissions


private const val LOG_LEVEL = "Network"
const val REQUEST_PERMISSIONS_BT = "REQUEST_PERMISSIONS_BT_CONNECT"
const val REQUEST_LOCATION_PERMISSIONS = "REQUEST_LOCATION_PERMISSION"

val network = Network()
class Network {

    var currentSSID: String? = ""
    fun bluetoothAdapter(): BluetoothAdapter? =
        (getContext()?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    fun findBluetoothAdapterByName(deviceName: String): BluetoothDevice? {
        return try {
            bluetoothAdapter()?.bondedDevices?.find { it.name == deviceName }
        } catch (e: SecurityException) {
            requestBluetoothPermissions()
            return null
        }
    }

    fun findWifiSSID(): List<String> {
        return if (EasyPermissions.hasPermissions(getContext()!!, Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION)) {
            try {
                val wifiManager = getContext()?.getSystemService(Context.WIFI_SERVICE) as WifiManager
                wifiManager.startScan()
                val ll = mutableListOf<String>()
                wifiManager.scanResults.forEach {
                    Log.d(LOG_LEVEL, "Found WIFI SSID: ${it.SSID}")
                    ll.add(it.SSID)
                }
                ll
            }catch (e: SecurityException){
                Log.e(LOG_LEVEL,"Used does not has access to ACCESS_COARSE_LOCATION permission.")
                sendBroadcastEvent(REQUEST_LOCATION_PERMISSIONS)
                emptyList()
            }
        } else {
            Log.e(LOG_LEVEL,"Used does not has access to ACCESS_COARSE_LOCATION permission.")
            sendBroadcastEvent(REQUEST_LOCATION_PERMISSIONS)
            emptyList()
        }
    }

    fun setupConnectedNetworksCallback() {
        try {
            Log.i(LOG_LEVEL, "Starting network setup")

            val wifiCallback = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
                        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                            currentSSID = readSSID(networkCapabilities)
                        }
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    object : ConnectivityManager.NetworkCallback() {
                        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                            currentSSID = readSSID(networkCapabilities)
                        }

                    }
                }
                else -> null
            }

            wifiCallback?.let {
                getContext()?.let {  contextWrapper ->
                    val request = NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .build()
                    val connectivityManager = contextWrapper.getSystemService(ConnectivityManager::class.java)
                    connectivityManager.requestNetwork(request, it)
                    connectivityManager.registerNetworkCallback(request, it)
                }
            }

            Log.i(LOG_LEVEL, "Network setup completed")

        } catch (e: Exception) {
            Log.e(LOG_LEVEL,"Failed to complete network registration",e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun readSSID(networkCapabilities: NetworkCapabilities): String? {
        val wifiInfo = networkCapabilities.transportInfo as WifiInfo?
        val ssid = wifiInfo?.ssid?.trim()?.replace("\"", "")
        if (Log.isLoggable(LOG_LEVEL,Log.VERBOSE)) {
            Log.v(LOG_LEVEL, "Wifi state changed, current WIFI SSID: $ssid, $wifiInfo")
        }
        return ssid
    }

    fun bluetooth(enable: Boolean) {
        Log.i(LOG_LEVEL, "Changing status of Bluetooth, enable: $enable")

        try {
            bluetoothAdapter()?.let {
                if (enable) {
                    it.enable()
                } else {
                    it.disable()
                }
            }
        } catch (e: SecurityException) {
            requestBluetoothPermissions()
        }
    }

    fun requestBluetoothPermissions() {
        sendBroadcastEvent(REQUEST_PERMISSIONS_BT)
    }

    fun wifi(enable: Boolean) {
        Log.i(LOG_LEVEL, "Changing status of WIFI, enable: $enable")

        getContext()?.let { it ->
            (it.getSystemService(Context.WIFI_SERVICE) as? WifiManager)?.apply {
                isWifiEnabled = enable
            }
        }
    }
}