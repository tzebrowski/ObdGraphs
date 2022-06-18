package org.obd.graphs.ui.dashboard

import org.obd.graphs.ui.preferences.Prefs
import org.obd.graphs.ui.preferences.getLongSet
import org.obd.graphs.ui.preferences.isEnabled
import org.obd.graphs.ui.preferences.updateLongSet


data class DashboardPreferences(
    val swipeToDeleteEnabled: Boolean,
    val gaugeViewVisible: Boolean,
    val dashboardViewVisible: Boolean,
    val dashboardVisiblePids: Set<Long>,
    val colorsEnabled: Boolean,
    val blinkEnabled: Boolean
)

fun updateDashboardPids(list: List<Long>) {
    Prefs.updateLongSet(
        "pref.dash.pids.selected",
        list
    )
}

fun getDashboardPreferences(): DashboardPreferences {

    val swipeToDelete =
        Prefs.getBoolean("pref.dash.swipe.to.delete", false)

    val tilesVisible = Prefs.getBoolean("pref.dash.gauge.view.visible", false)
    val dashboardVisible = Prefs.getBoolean("pref.dash.dash.view.visible", false)
    val visiblePids = Prefs.getLongSet("pref.dash.pids.selected")

    val colors = Prefs.isEnabled("pref.dash.top.values.red.color")
    val blink = Prefs.isEnabled("pref.dash.top.values.blink")

    return DashboardPreferences(
        swipeToDelete, tilesVisible,
        dashboardVisible, visiblePids, colors, blink
    )
}