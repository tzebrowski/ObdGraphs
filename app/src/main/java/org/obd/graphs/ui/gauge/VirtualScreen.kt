package org.obd.graphs.ui.gauge

import org.obd.graphs.preferences.PREF_GAUGE_DIALOG
import org.obd.graphs.preferences.Prefs

const val VIRTUAL_SCREEN_SELECTION = "pref.gauge.virtual.selected"
fun getCurrentVirtualScreen() = Prefs.getString(VIRTUAL_SCREEN_SELECTION, "1")!!
fun getVirtualScreenMetrics(): String = "$PREF_GAUGE_DIALOG.${getCurrentVirtualScreen()}"

