package org.obd.graphs

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Log

private const val LOG_LEVEL = "Network"

fun bluetooth(enable: Boolean) {
    Log.i(LOG_LEVEL, "Changing status of Bluetooth, enable: $enable")

    ApplicationContext.get()?.let {
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

    ApplicationContext.get()?.let { it ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val panelIntent = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
            it.startActivityForResult(panelIntent, 0)
        } else {
            (it.getSystemService(Context.WIFI_SERVICE) as? WifiManager)?.apply {
                isWifiEnabled = enable
            }
        }
    }
}