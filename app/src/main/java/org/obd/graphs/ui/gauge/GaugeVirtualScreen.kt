package org.obd.graphs.ui.gauge

import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.updateString

private const val VIRTUAL_SCREEN_SELECTION = "pref.gauge.virtual.selected"
const val PREF_GAUGE_DIALOG = "pref.gauge.pids.selected"

class GaugeVirtualScreen {
    fun getCurrentVirtualScreen() = Prefs.getString(VIRTUAL_SCREEN_SELECTION, "1")!!

    fun getVirtualScreenPrefKey(): String = "$PREF_GAUGE_DIALOG.${getCurrentVirtualScreen()}"

    fun updateVirtualScreen(screenId: String) {
        Prefs.updateString(VIRTUAL_SCREEN_SELECTION, screenId)
    }
}

val gaugeVirtualScreen = GaugeVirtualScreen()