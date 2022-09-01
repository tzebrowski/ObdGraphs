package org.obd.graphs

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log

private const val LOG_LEVEL = "Network"
const val REQUEST_PERMISSIONS_BT = "REQUEST_PERMISSIONS_BT_CONNECT"

fun findBTAdapterByName(deviceName: String): BluetoothDevice? {
    return try  {
        BluetoothAdapter.getDefaultAdapter().bondedDevices.find { it.name == deviceName }
    } catch(e: SecurityException) {
        sendBroadcastEvent(REQUEST_PERMISSIONS_BT)
        return null
    }
}

fun bluetooth(enable: Boolean) {
    Log.i(LOG_LEVEL, "Changing status of Bluetooth, enable: $enable")

    getContext()?.let {
        try {
            if (enable) {
                BluetoothAdapter.getDefaultAdapter().run {
                    enable()
                }
            } else {
                BluetoothAdapter.getDefaultAdapter().run {
                    disable()
                }
            }
        } catch (e: SecurityException) {
            sendBroadcastEvent(REQUEST_PERMISSIONS_BT)
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