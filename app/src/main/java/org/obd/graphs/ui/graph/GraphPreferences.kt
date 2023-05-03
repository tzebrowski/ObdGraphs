package org.obd.graphs.ui.graph

import android.util.Log
import org.obd.graphs.preferences.Prefs

data class GraphPreferences(
    val xAxisStartMovingAfter: Float,
    val xAxisMinimumShift: Float,
    val cacheEnabled: Boolean,
    val metrics: Set<Long>
)

class GraphPreferencesManager {
    fun preferences(): GraphPreferences {
        val prefixKey = "pref.graph"

        val xAxisStartMovingAfter =
            Prefs.getString("$prefixKey.x-axis.start-moving-after.time", "20000")!!.toFloat()

        val xAxisMinimumShift =
            Prefs.getString("$prefixKey.x-axis.minimum-shift.time", "20")!!.toFloat()

        val cacheEnabled = Prefs.getBoolean("$prefixKey.cache.enabled", true)

        val selectedPids = graphVirtualScreen.getVirtualScreenMetrics().map { it.toLong() }.toSet()

        Log.i(
            "GRAPH",
            "Read Graph Properties from Preferences\n" +
                    "xAxisStartMovingAfterProp=$xAxisStartMovingAfter\n" +
                    "xAxisMinimumShiftProp=$xAxisMinimumShift\n" +
                    "cacheEnabledProp=$cacheEnabled\n" +
                    "selectedPids=$selectedPids"
        )

        return GraphPreferences(
            xAxisStartMovingAfter,
            xAxisMinimumShift,
            cacheEnabled,
            selectedPids
        )
    }
}

val graphPreferencesManager = GraphPreferencesManager()