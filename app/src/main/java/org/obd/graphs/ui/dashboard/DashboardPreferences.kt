package org.obd.graphs.ui.dashboard

import org.obd.graphs.ui.preferences.Prefs
import org.obd.graphs.ui.preferences.getLongSet
import org.obd.graphs.ui.preferences.isEnabled


data class DashboardPreferences(
    val swipeToDeleteEnabled: Boolean,
    val gaugeViewVisible: Boolean,
    val dashboardViewVisible: Boolean,
    val dragAndDropEnabled: Boolean,
    val dashboardSelectedMetrics: Pair<String,Set<Long>>,
    val gaugeSelectedMetrics: Pair<String,Set<Long>>,
    val colorsEnabled: Boolean,
    val blinkEnabled: Boolean
)

private const val SELECTED_DASHBOARD_METRICS = "pref.dash.pids.selected"
private const val SELECTED_GAUGE_METRICS = "pref.dash.gauge_pids.selected"


fun getDashboardPreferences(): DashboardPreferences {

    val swipeToDelete =
        Prefs.getBoolean("pref.dash.swipe.to.delete", false)

    val dragAndDropEnabled =  Prefs.getBoolean("pref.dash.enable_drag", false)
    val gaugeViewVisible = Prefs.getBoolean("pref.dash.gauge.view.visible", false)
    val dashboardVisible = Prefs.getBoolean("pref.dash.dash.view.visible", false)
    val dashboardSelectedMetrics = Prefs.getLongSet(SELECTED_DASHBOARD_METRICS)
    val gaugeSelectedMetrics =  Prefs.getLongSet("pref.dash.gauge_pids.selected")

    val colors = Prefs.isEnabled("pref.dash.top.values.red.color")
    val blink = Prefs.isEnabled("pref.dash.top.values.blink")

    return DashboardPreferences(
        swipeToDeleteEnabled = swipeToDelete,
        gaugeViewVisible = gaugeViewVisible,
        dashboardViewVisible = dashboardVisible,
        dashboardSelectedMetrics = Pair(SELECTED_DASHBOARD_METRICS,dashboardSelectedMetrics),
        gaugeSelectedMetrics = Pair(SELECTED_GAUGE_METRICS,gaugeSelectedMetrics),
        colorsEnabled = colors,
        blinkEnabled = blink,
        dragAndDropEnabled = dragAndDropEnabled
    )
}