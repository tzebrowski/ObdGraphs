package org.obd.graphs.aa

import org.obd.graphs.ui.preferences.Prefs
import org.obd.graphs.ui.preferences.getStringSet


const val LOG_KEY = "AndroidAuto"

internal fun aaPIDs() =
    Prefs.getStringSet("pref.aa.pids.selected").map { s -> s.toLong() }.toSet()

