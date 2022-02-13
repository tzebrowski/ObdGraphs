package org.openobd2.core.logger

import org.openobd2.core.logger.ui.preferences.Prefs
import org.openobd2.core.logger.ui.preferences.isEnabled


data class MainActivityPreferences(val hideToolbarDoubleClick: Boolean,
                                   val hideToolbarLandscape: Boolean,
                                   val showDebugView: Boolean,
                                   val showDashView: Boolean,
                                   val showGaugeView: Boolean,
                                   val showMetricsView: Boolean,
                                   val showGraphView: Boolean
)

fun getMainActivityPreferences(): MainActivityPreferences {

    val hideToolbarDoubleClick = Prefs.isEnabled("pref.toolbar.hide.doubleclick")
    val hideToolbarLandscape = Prefs.isEnabled("pref.toolbar.hide.landscape")
    val showDebugView = Prefs.isEnabled("pref.debug.view.enabled")
    val showDashView = Prefs.isEnabled("pref.dash.view.enabled")
    val showGaugeView = Prefs.isEnabled("pref.gauge.view.enabled")

    val showMetricsView = Prefs.isEnabled("pref.metrics.view.enabled")
    val showGraphView = Prefs.isEnabled("pref.graph.view.enabled")

    return MainActivityPreferences(hideToolbarDoubleClick,
        hideToolbarLandscape,
        showDebugView,
        showDashView,
        showGaugeView,
        showMetricsView,
        showGraphView)
}