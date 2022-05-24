package org.openobd2.core.logger

import android.util.Log
import org.openobd2.core.logger.ui.preferences.*
import org.openobd2.core.logger.ui.preferences.profile.loadProfile
import java.util.*

internal fun MainActivity.installProfiles() {
    if (!Prefs.getBoolean("prefs.installed.profiles", false)) {
        Log.e(ACTIVITY_LOGGER_TAG, "Installing profile")
        val prefsEditor = Prefs.edit()

        arrayOf("default.properties","giulietta.properties").forEach { fileName ->
            val prop = Properties()
            prop.load(assets.open(fileName))
            prop.forEach { t, u ->
                val value = u.toString()
                val key = t.toString()
                Log.i(ACTIVITY_LOGGER_TAG, "Inserting $key=$value")
                if (value.isBoolean()) {
                    prefsEditor.putBoolean(key, value.toBoolean())
                } else if (value.startsWith("[") || value.endsWith("]")) {
                    prefsEditor.putStringSet(key,
                        value.replace("[", "").replace("]", "").split(",").map { it.trim() }
                            .toMutableSet()
                    )
                } else if (value.isNumeric()) {
                    prefsEditor.putInt(key, value.toInt())
                } else {
                    prefsEditor.putString(key, value.replace("\"", "").replace("\"", ""))
                }
            }
        }

        prefsEditor.putString("pref.profile.id","profile_2")
        prefsEditor.apply()
        Prefs.edit().putBoolean("prefs.installed.profiles", true).apply()
        loadProfile("profile_2")

        updateToolbar()
    }
}

private fun String.isBoolean(): Boolean {
    return startsWith("false") || startsWith("true")
}
private fun String.toBoolean(): Boolean {
    return startsWith("true")
}

private fun String.isNumeric(): Boolean {
    return matches(Regex("-?\\d+"))
}

private fun MainActivity.updateToolbar() {
    toggleNavigationItem(DASH_VIEW_ID, R.id.navigation_dashboard)
    toggleNavigationItem(METRICS_VIEW_ID, R.id.navigation_metrics)
    toggleNavigationItem(GRAPH_VIEW_ID, R.id.navigation_graph)
    toggleNavigationItem(GAUGE_VIEW_ID, R.id.navigation_gauge)
}
