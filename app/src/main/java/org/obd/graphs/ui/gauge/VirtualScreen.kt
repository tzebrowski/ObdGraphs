package org.obd.graphs.ui.gauge

import org.obd.graphs.preferences.PREF_GAUGE_DIALOG
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.updateStringSet

const val VIRTUAL_SCREEN_SELECTION = "pref.gauge.virtual.selected"

class GaugeVirtualScreen {
    fun getCurrentVirtualScreen() = Prefs.getString(VIRTUAL_SCREEN_SELECTION, "1")!!

    fun getVirtualScreenPrefKey(): String = "$PREF_GAUGE_DIALOG.${getCurrentVirtualScreen()}"

    fun updateVirtualScreen(newList: List<String>){
        Prefs.updateStringSet(getVirtualScreenPrefKey(),newList)
    }

    fun getVirtualScreenMetrics (): Set<String> = Prefs.getStringSet(getVirtualScreenPrefKey(), mutableSetOf())!!
}

val gaugeVirtualScreen = GaugeVirtualScreen()