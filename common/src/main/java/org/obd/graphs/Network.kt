package org.obd.graphs

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


private const val LOG_LEVEL = "Network"
const val REQUEST_PERMISSIONS_BT = "REQUEST_PERMISSIONS_BT_CONNECT"
const val REQUEST_PERMISSIONS_WIFI = "REQUEST_PERMISSIONS_WIFI"

fun bluetoothAdapter(): BluetoothAdapter? =
    (getContext()?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

fun findBluetoothAdapterByName(deviceName: String): BluetoothDevice? {
    return try  {
        bluetoothAdapter()?.bondedDevices?.find { it.name == deviceName }
    } catch(e: SecurityException) {
        requestBluetoothPermissions()
        return null
    }
}

fun findWifiSSID(): List<String>{
    val wm = getContext()?.getSystemService(Context.WIFI_SERVICE) as WifiManager
    wm.startScan()
    val ll = mutableListOf<String>()
    wm.scanResults.forEach {
        Log.i(LOG_LEVEL,"Found SSID: ${it.SSID}")
        ll.add(it.SSID)
    }
    return ll
}

var currentSSID: String? = ""

fun setupConnectedNetworksCallback() {
    Log.i(LOG_LEVEL,"setupConnectedNetworksCallback")
    val cm = getContext()!!.getSystemService(ConnectivityManager::class.java)
    val wifiCallback = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            object: ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    Log.i(LOG_LEVEL,"Wifi state changed: $networkCapabilities")
                    val wifiInfo = networkCapabilities.transportInfo as WifiInfo?
                    currentSSID = wifiInfo?.ssid?.trim()?.replace("\"","")
                }
            }
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
            object: ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    Log.i(LOG_LEVEL,"Wifi state changed: $networkCapabilities")
                    val wifiInfo = networkCapabilities.transportInfo as WifiInfo?
                    currentSSID = wifiInfo?.ssid?.trim()?.replace("\"","")
                }
            }
        }
        else -> null
    }
    try {
        wifiCallback?.let {
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            cm.requestNetwork(request, it); // For request
            cm.registerNetworkCallback(request, it)
        }
    } catch (e: SecurityException) {
        requestWifiPermissions()
    }
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

fun requestWifiPermissions() {
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