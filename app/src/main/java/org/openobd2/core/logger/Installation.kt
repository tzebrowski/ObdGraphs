package org.openobd2.core.logger

import android.util.Log
import org.openobd2.core.logger.ui.preferences.Prefs
import org.openobd2.core.logger.ui.preferences.profile.PROFILE_ID
import org.openobd2.core.logger.ui.preferences.profile.PROFILE_INSTALLATION_KEY
import org.openobd2.core.logger.ui.preferences.profile.loadProfile
import java.util.Properties

private const val DEFAULT_PROFILE_TO_LOAD = "profile_2"

internal fun MainActivity.installProfiles() {

    if (!Prefs.getBoolean(PROFILE_INSTALLATION_KEY, false)) {
        Log.e(ACTIVITY_LOGGER_TAG, "Installing profile")
        val prefsEditor = Prefs.edit()

        arrayOf("default.properties", "giulietta.properties").forEach { fileName ->
            val prop = Properties()
            prop.load(assets.open(fileName))
            prop.forEach { t, u ->
                val value = u.toString()
                val key = t.toString()

                Log.i(ACTIVITY_LOGGER_TAG, "Inserting $key=$value")

                when {
                    value.isBoolean() -> {
                        prefsEditor.putBoolean(key, value.toBoolean())
                    }
                    value.isArray() -> {
                        val v = value
                            .replace("[", "")
                            .replace("]", "")
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .toMutableSet()
                        prefsEditor.putStringSet(key,v)
                    }
                    value.isNumeric() -> {
                        prefsEditor.putInt(key, value.toInt())
                    }
                    else -> {
                        prefsEditor.putString(key, value.replace("\"", "").replace("\"", ""))
                    }
                }
            }
        }
        prefsEditor.putString(PROFILE_ID, DEFAULT_PROFILE_TO_LOAD)
        prefsEditor.putBoolean(PROFILE_INSTALLATION_KEY, true)
        prefsEditor.apply()

        loadProfile(DEFAULT_PROFILE_TO_LOAD)
        updateToolbar()
    }
}

private fun String.isArray() = startsWith("[") || endsWith("]")
private fun String.isBoolean(): Boolean = startsWith("false") || startsWith("true")
private fun String.isNumeric(): Boolean = matches(Regex("-?\\d+"))
private fun String.toBoolean(): Boolean  = startsWith("true")

private fun MainActivity.updateToolbar() {
    toggleNavigationItem(DASH_VIEW_ID, R.id.navigation_dashboard)
    toggleNavigationItem(METRICS_VIEW_ID, R.id.navigation_metrics)
    toggleNavigationItem(GRAPH_VIEW_ID, R.id.navigation_graph)
    toggleNavigationItem(GAUGE_VIEW_ID, R.id.navigation_gauge)
}
