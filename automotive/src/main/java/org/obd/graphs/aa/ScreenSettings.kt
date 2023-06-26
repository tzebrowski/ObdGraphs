package org.obd.graphs.aa

import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getS
import org.obd.graphs.preferences.getStringSet
import org.obd.graphs.preferences.updateStringSet

private const val PREF_SELECTED_PIDS = "pref.aa.pids.selected"
private const val PREF_MAX_PIDS_IN_COLUMN = "pref.aa.max_pids_in_column"
private const val PREF_SCREEN_FONT_SIZE = "pref.aa.screen_font_size"
private const val DEFAULT_ITEMS_IN_COLUMN = "6"
private const val DEFAULT_FONT_SIZE= "34"

val carScreenSettings =  CarScreenSettings()

class CarScreenSettings {

    fun applyVirtualScreen1() = applyVirtualScreen("pref.aa.pids.profile_1")
    fun applyVirtualScreen2() = applyVirtualScreen("pref.aa.pids.profile_2")
    fun applyVirtualScreen3() = applyVirtualScreen("pref.aa.pids.profile_3")
    fun applyVirtualScreen4() = applyVirtualScreen("pref.aa.pids.profile_4")

    fun getSelectedPIDs() =
        Prefs.getStringSet(PREF_SELECTED_PIDS).map { s -> s.toLong() }.toSet()

    fun maxItemsInColumn(): Int {
        return when (getSelectedPIDs().size){
            1 -> 1
            2 -> 1
            3 -> 1
            4 -> 1
            else -> Prefs.getS(PREF_MAX_PIDS_IN_COLUMN, DEFAULT_ITEMS_IN_COLUMN).toInt()
        }
    }

    fun  maxFontSize(): Int =
        Prefs.getS(PREF_SCREEN_FONT_SIZE, DEFAULT_FONT_SIZE).toInt()

    private fun applyVirtualScreen(key: String) =  Prefs.updateStringSet(PREF_SELECTED_PIDS, Prefs.getStringSet(key).toList())
}