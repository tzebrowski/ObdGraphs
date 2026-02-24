package org.obd.graphs.bl.datalogger

import android.content.Context
import android.os.Build
import android.util.Log
import org.obd.graphs.Network

object AutoConnect {
    fun setup(context: Context) {

        val autoConnect = dataLoggerSettings.instance().adapter.autoConnectEnabled

        Log.i("AutoConnect", "Auto connect is enabled=$autoConnect")

        if (autoConnect && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Network.startBackgroundBleScanForMac(
                context,
                dataLoggerSettings.instance().adapter.deviceAddress.uppercase()
            )
        }
    }
}