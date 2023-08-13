package org.obd.graphs.ui.giulia

import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getS
import org.obd.graphs.preferences.updateString
import org.obd.graphs.preferences.updateStringSet

private const val VIRTUAL_SCREEN_SELECTION = "pref.giulia.virtual.selected"
const val PREF_GAUGE_DIALOG = "pref.giulia.pids.selected"

class GiuliaVirtualScreenPreferences {
    fun getCurrentVirtualScreen() = Prefs.getString(VIRTUAL_SCREEN_SELECTION, "1")!!

    fun getVirtualScreenPrefKey(): String = "$PREF_GAUGE_DIALOG.${getCurrentVirtualScreen()}"

    fun updateVirtualScreen(screenId: String) {
        Prefs.updateString(VIRTUAL_SCREEN_SELECTION, screenId)
    }

    fun updateVirtualScreenMetrics(newList: List<String>) {
        Prefs.updateStringSet(getVirtualScreenPrefKey(), newList)
    }

    fun getVirtualScreenMetrics(): Set<String> =
        Prefs.getStringSet(getVirtualScreenPrefKey(), mutableSetOf())!!

    fun getMaxItemsInColumn(): Int  = Prefs.getS("pref.giulia.max_pids_in_column.${getCurrentVirtualScreen()}","1").toInt()
    fun getFontSize (): Int =  Prefs.getS("pref.giulia.screen_font_size.${getCurrentVirtualScreen()}","52").toInt()
}

val giuliaVirtualScreen = GiuliaVirtualScreenPreferences()