package org.obd.graphs

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log

private const val LOG_LEVEL = "Network"

@SuppressLint("MissingPermission")
fun findBTAdapterByName(deviceName: String) =
    BluetoothAdapter.getDefaultAdapter().bondedDevices.find { it.name == deviceName }

@SuppressLint("MissingPermission")
fun bluetooth(enable: Boolean) {
    Log.i(LOG_LEVEL, "Changing status of Bluetooth, enable: $enable")

    getContext()?.let {
        if (enable) {
            BluetoothAdapter.getDefaultAdapter().run {
                enable()
            }
        } else {
            BluetoothAdapter.getDefaultAdapter().run {
                disable()
            }
        }
    }
}

fun wifi(enable: Boolean) {
    Log.i(LOG_LEVEL, "Changing status of WIFI, enable: $enable")

    getContext()?.let { it ->
        (it.getSystemService(Context.WIFI_SERVICE) as? WifiManager)?.apply {
            isWifiEnabled = enable
        }
    }
}