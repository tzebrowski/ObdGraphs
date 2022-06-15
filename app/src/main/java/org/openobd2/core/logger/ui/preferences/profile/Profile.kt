package org.openobd2.core.logger.ui.preferences.profile

import android.content.SharedPreferences
import android.util.Log
import org.openobd2.core.logger.ApplicationContext
import org.openobd2.core.logger.ui.preferences.Prefs
import org.openobd2.core.logger.ui.preferences.getString
import org.openobd2.core.logger.ui.preferences.mode.*
import org.openobd2.core.logger.ui.preferences.updateToolbar
import java.util.*

private const val PROFILE_ID_PREF = "pref.profile.id"
internal const val PROFILES_PREF = "pref.profiles"
internal const val MAX_PROFILES_PREF = "pref.profile.max_profiles"
internal const val PROFILE_CURRENT_NAME_PREF = "pref.profile.current_name"

internal const val DEFAULT_PROFILE_TO_LOAD = "profile_2"
internal const val PROFILE_INSTALLATION_KEY = "prefs.installed.profiles"
internal const val PROFILE_NAME_PREFIX = "pref.profile.names"
internal const val LOG_KEY = "Profile"

fun installProfiles() {

    if (!Prefs.getBoolean(PROFILE_INSTALLATION_KEY, false)) {
        Log.e(LOG_KEY, "Installing profiles")

        Prefs.edit().let { editor ->
            arrayOf("default.properties", "giulietta.properties").forEach { fileName ->
                val prop = Properties()
                prop.load(ApplicationContext.get()!!.assets.open(fileName))
                prop.forEach { t, u ->
                    val value = u.toString()
                    val key = t.toString()

                    Log.i(LOG_KEY, "Inserting $key=$value")

                    when {
                        value.isBoolean() -> {
                            editor.putBoolean(key, value.toBoolean())
                        }
                        value.isArray() -> {
                            val v = value
                                .replace("[", "")
                                .replace("]", "")
                                .split(",")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                                .toMutableSet()
                            editor.putStringSet(key, v)
                        }
                        value.isNumeric() -> {
                            editor.putInt(key, value.toInt())
                        }
                        else -> {
                            editor.putString(key, value.replace("\"", "").replace("\"", ""))
                        }
                    }
                }
            }
            editor.putString(PROFILE_ID_PREF, DEFAULT_PROFILE_TO_LOAD)
            editor.putBoolean(PROFILE_INSTALLATION_KEY, true)
            editor.apply()
        }

        loadProfile(DEFAULT_PROFILE_TO_LOAD)
        updateToolbar()
    }
}


internal fun SharedPreferences.Editor.updatePreference(
    prefName: String,
    value: Any?
) {
    when (value) {
        is String -> {
            putString(prefName, value)
        }
        is Set<*> -> {
            putStringSet(prefName, value as MutableSet<String>?)
        }
        is Int -> {
            putInt(prefName, value)
        }
        is Boolean -> {
            putBoolean(prefName, value)
        }
    }
}

internal fun getCurrentProfile(): String = Prefs.getString(PROFILE_ID_PREF)!!

fun loadProfile(profileName: String) {
    Log.i(LOG_KEY, "Loading user preferences from the profile='$profileName'")

    resetCurrentProfile()

    Prefs.edit().let {
        Prefs.all
            .filter { (pref, _) -> pref.startsWith(profileName) }
            .filter { (pref, _) -> !pref.startsWith(PROFILE_NAME_PREFIX) }
            .filter { (pref, _) -> !pref.startsWith(PROFILE_CURRENT_NAME_PREF) }
            .filter { (pref, _) -> !pref.startsWith(PROFILE_INSTALLATION_KEY) }
            .forEach { (pref, value) ->
                pref.substring(profileName.length + 1).run {
                    Log.d(LOG_KEY, "Loading user preference $this = $value")
                    it.updatePreference(this, value)
                }
            }
        it.apply()
    }

    updateCurrentProfileValue(profileName)
}

private fun resetCurrentProfile() {

    Prefs.edit().let {
        getAvailableModes().forEach { key ->
            it.putString("$MODE_HEADER_PREFIX.$key", "")
            it.putString("$MODE_NAME_PREFIX.$key", "")
        }
        it.putString(PREF_CAN_HEADER_EDITOR, "")
        it.putString(PREF_ADAPTER_MODE_ID_EDITOR, "")

        Prefs.all
            .filter { (pref, _) -> !pref.startsWith("datalogger") }
            .filter { (pref, _) -> !pref.startsWith("profile_") }
            .filter { (pref, _) -> !pref.startsWith(PROFILE_ID_PREF) }
            .filter { (pref, _) -> !pref.startsWith(PROFILE_NAME_PREFIX) }
            .filter { (pref, _) -> !pref.startsWith(PROFILE_CURRENT_NAME_PREF) }
            .filter { (pref, _) -> !pref.startsWith(PROFILE_INSTALLATION_KEY) }
            .forEach { (pref, _) ->
                it.remove(pref)
            }
        it.apply()
    }
}

private fun updateCurrentProfileValue(profileName: String) {
    val prefName =
        Prefs.getString("$PROFILE_NAME_PREFIX.$profileName", profileName.toCamelCase())
    Log.i(LOG_KEY, "Setting $PROFILE_CURRENT_NAME_PREF=$prefName")
    Prefs.edit().putString(PROFILE_CURRENT_NAME_PREF, prefName).apply()
}


private fun String.toCamelCase() =
    split('_').joinToString(" ", transform = String::capitalize)

private fun String.isArray() = startsWith("[") || endsWith("]")
private fun String.isBoolean(): Boolean = startsWith("false") || startsWith("true")
private fun String.isNumeric(): Boolean = matches(Regex("-?\\d+"))
private fun String.toBoolean(): Boolean = startsWith("true")