package org.obd.graphs.ui.graph

import android.util.Log
import org.obd.graphs.preferences.Prefs

data class GraphPreferences(
    val xAxisStartMovingAfter: Float,
    val xAxisMinimumShift: Float,
    val cacheEnabled: Boolean,
    val metrics: Set<Long>,
    val toggleVirtualPanel: Boolean
)

class GraphPreferencesReader {
    fun read(): GraphPreferences {
        val prefixKey = "pref.graph"

        val xAxisStartMovingAfter =
            Prefs.getString("$prefixKey.x-axis.start-moving-after.time", "20000")!!.toFloat()

        val xAxisMinimumShift =
            Prefs.getString("$prefixKey.x-axis.minimum-shift.time", "20")!!.toFloat()

        val cacheEnabled = Prefs.getBoolean("$prefixKey.cache.enabled", true)

        val metrics = graphVirtualScreen.getVirtualScreenMetrics().map { it.toLong() }.toSet()

        val toggleVirtualPanel = Prefs.getBoolean("$prefixKey.toggle_virtual_screens_double_click", true)

        Log.d(
            GRAPH_LOGGER_TAG,
            "Read graph preferences\n" +
                    "xAxisStartMovingAfterProp=$xAxisStartMovingAfter\n" +
                    "xAxisMinimumShiftProp=$xAxisMinimumShift\n" +
                    "cacheEnabledProp=$cacheEnabled\n" +
                    "toggleVirtualPanel=$toggleVirtualPanel\n" +
                    "metrics=$metrics\n"
        )


        return GraphPreferences(
            xAxisStartMovingAfter,
            xAxisMinimumShift,
            cacheEnabled,
            metrics,
            toggleVirtualPanel = toggleVirtualPanel
        )
    }
}

val graphPreferencesReader = GraphPreferencesReader()