package org.openobd2.core.logger.ui.graph

import android.util.Log
import org.openobd2.core.logger.ui.preferences.Prefs

data class GraphPreferences(val xAxisStartMovingAfter: Float,val xAxisMinimumShift: Float, val cacheEnabled: Boolean)

fun getGraphPreferences() : GraphPreferences{

    val xAxisStartMovingAfter =
        Prefs.getString("pref.graph.x-axis.start-moving-after.time", "20000")!!.toFloat()

    val xAxisMinimumShift = Prefs.getString("pref.graph.x-axis.minimum-shift.time", "20")!!.toFloat()

    val cacheEnabled = Prefs.getBoolean("pref.graph.cache.enabled", true)

    Log.i(
        "GRAPH",
        "Read Graph Properties from Preferences\n" +
                "xAxisStartMovingAfterProp=${xAxisStartMovingAfter}\n" +
                "xAxisMinimumShiftProp=${xAxisMinimumShift}\n" +
                "cacheEnabledProp=${cacheEnabled}\n"
    )

    return GraphPreferences(xAxisStartMovingAfter,xAxisMinimumShift,cacheEnabled)
}