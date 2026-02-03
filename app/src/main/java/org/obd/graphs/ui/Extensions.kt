package org.obd.graphs.ui

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import org.obd.graphs.bl.datalogger.DataLoggerConnector
import org.obd.graphs.bl.datalogger.DataLoggerService

fun Fragment.withDataLogger(onConnected: (DataLoggerService) -> Unit) {
    val connector = DataLoggerConnector(requireContext(), onConnected)
    this.lifecycle.addObserver(connector)
}

fun ComponentActivity.withDataLogger(onConnected: (DataLoggerService) -> Unit) {
    val connector = DataLoggerConnector(this, onConnected)
    this.lifecycle.addObserver(connector)
}