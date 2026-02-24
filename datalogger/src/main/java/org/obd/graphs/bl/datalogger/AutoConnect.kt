package org.obd.graphs.bl.datalogger

import android.content.Context
import android.os.Build
import android.util.Log
import org.obd.graphs.Network

object AutoConnect {
    fun setup(context: Context) {

        val autoConnect = dataLoggerSettings.instance().adapter.autoConnectEnabled
        val macAddress = dataLoggerSettings.instance().adapter.deviceAddress.uppercase()
        Log.i("AutoConnect", "Auto connect is enabled=$autoConnect. MacAddress of device=$macAddress")

        if (autoConnect && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && macAddress.isNotEmpty()) {
            Network.startBackgroundBleScanForMac(
                context,
                macAddress
            )
        }
    }
}