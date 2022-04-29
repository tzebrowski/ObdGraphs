package org.openobd2.core.logger.ui.dashboard

import org.openobd2.core.logger.ui.preferences.Prefs
import org.openobd2.core.logger.ui.preferences.getLongSet
import org.openobd2.core.logger.ui.preferences.isEnabled
import org.openobd2.core.logger.ui.preferences.updateLongSet


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

    val gaugeVisible = Prefs.getBoolean("pref.dash.gauge.view.visible", false)
    val dashboardVisible = Prefs.getBoolean("pref.dash.dash.view.visible", false)
    val visiblePids = Prefs.getLongSet("pref.dash.pids.selected")

    val colors = Prefs.isEnabled("pref.dash.top.values.red.color")
    val blink = Prefs.isEnabled("pref.dash.top.values.blink")

    return DashboardPreferences(
        swipeToDelete, gaugeVisible,
        dashboardVisible, visiblePids, colors, blink
    )
}