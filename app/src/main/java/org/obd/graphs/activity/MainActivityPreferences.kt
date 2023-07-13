package org.obd.graphs.activity

import android.util.Log
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.isEnabled

data class MainActivityPreferences(
    val hideToolbarDoubleClick: Boolean,
    val hideToolbarLandscape: Boolean,
    val showDashView : Boolean,
    val showGiuliaView: Boolean,
    val showGaugeView: Boolean,
    val showGraphView: Boolean,
    val hideToolbarConnected: Boolean
)

const val PREFS_LOGGER_TAG = "PREFS"

fun getMainActivityPreferences(): MainActivityPreferences {

    val hideToolbarDoubleClick = Prefs.isEnabled("pref.toolbar.hide.doubleclick")
    val hideToolbarLandscape = Prefs.isEnabled("pref.toolbar.hide.landscape")
    val hideToolbarConnected = Prefs.isEnabled("pref.toolbar.hide.connected")

    val showDebugView = Prefs.isEnabled("pref.debug.view.enabled")
    val showDashView = Prefs.isEnabled("pref.dash.view.enabled")
    val showGaugeView = Prefs.isEnabled("pref.gauge.view.enabled")

    val showGiuliaView = Prefs.getBoolean("pref.giulia.view.enabled", true)
    val showGraphView = Prefs.getBoolean("pref.graph.view.enabled", true)

    val prefs = MainActivityPreferences(
        hideToolbarDoubleClick =  hideToolbarDoubleClick,
        hideToolbarLandscape = hideToolbarLandscape,
        hideToolbarConnected = hideToolbarConnected,
        showGiuliaView =  showGiuliaView,
        showGraphView = showGraphView,
        showGaugeView = showGaugeView,
        showDashView = showDashView
    )

    Log.d(PREFS_LOGGER_TAG, "Loaded MainActivity preferences: $prefs")
    return prefs
}