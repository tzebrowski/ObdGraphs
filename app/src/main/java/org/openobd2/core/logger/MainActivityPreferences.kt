package org.openobd2.core.logger

import android.util.Log
import org.openobd2.core.logger.ui.preferences.Prefs
import org.openobd2.core.logger.ui.preferences.isEnabled

data class MainActivityPreferences(
    val hideToolbarDoubleClick: Boolean,
    val hideToolbarLandscape: Boolean,
    val showDebugView: Boolean,
    val showDashView: Boolean,
    val showGaugeView: Boolean,
    val showMetricsView: Boolean,
    val showGraphView: Boolean,
    val hideToolbarConnected: Boolean
)

const val PREFS_LOGGER_TAG = "PREFS"

fun getMainActivityPreferences(): MainActivityPreferences {

    val hideToolbarDoubleClick = Prefs.isEnabled("pref.toolbar.hide.doubleclick")
    val hideToolbarLandscape = Prefs.isEnabled("pref.toolbar.hide.landscape")
    val showDebugView = Prefs.isEnabled("pref.debug.view.enabled")
    val showDashView = Prefs.isEnabled("pref.dash.view.enabled")
    val showGaugeView = Prefs.isEnabled("pref.gauge.view.enabled")

    val showMetricsView = Prefs.getBoolean("pref.metrics.view.enabled", true)
    val showGraphView = Prefs.getBoolean("pref.graph.view.enabled", true)
    val hideToolbarConnected = Prefs.isEnabled("pref.toolbar.hide.connected")

    val prefs = MainActivityPreferences(
        hideToolbarDoubleClick,
        hideToolbarLandscape,
        showDebugView,
        showDashView,
        showGaugeView,
        showMetricsView,
        showGraphView,
        hideToolbarConnected
    )

    Log.d(PREFS_LOGGER_TAG, "Loaded MainActivity preferences: $prefs")
    return prefs
}