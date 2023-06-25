package org.obd.graphs.aa

import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getS

private const val PREF_MAX_PIDS_IN_COLUMN = "pref.aa.max_pids_in_column"
private const val PREF_SCREEN_FONT_SIZE = "pref.aa.screen_font_size"
private const val DEFAULT_ITEMS_IN_COLUMN = "6"
private const val DEFAULT_FONT_SIZE= "34"

val carScreenSettings =  CarScreenSettings()

class CarScreenSettings {

    fun maxItemsInColumn(): Int =
        Prefs.getS(PREF_MAX_PIDS_IN_COLUMN, DEFAULT_ITEMS_IN_COLUMN).toInt()

    fun  maxFontSize(): Int =
        Prefs.getS(PREF_SCREEN_FONT_SIZE, DEFAULT_FONT_SIZE).toInt()
}