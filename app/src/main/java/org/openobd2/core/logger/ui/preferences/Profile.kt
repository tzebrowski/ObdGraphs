package org.openobd2.core.logger.ui.preferences

import android.util.Log
import androidx.preference.ListPreference
import androidx.preference.Preference

private const val LOG_KEY = "Profile"

private data class Connection(
    val type: String,
    val commandFrequency: String
)


private data class Init(
    val protocol: String,
    val header01: String,
    val header22: String,
    val delay: String
)

private data class Profile(val init: Init, val connection: Connection)

private val profiles = hashMapOf(
    "Giulietta QV" to Profile(
        Init("CAN_29", "DB33F1", "DA10F1", "1000"),
        Connection("wifi", "3")
    ),
    "VW Med 17_5" to Profile(
        Init("CAN_11", "7DF", "", "2000"),
        Connection("bluetooth", "5")
    )
)

fun PreferencesFragment.registerProfileListener() {
    findPreference<ListPreference>("pref.profile.id")?.let {
        updateProfileSettings(it.value)
        it.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                updateProfileSettings(newValue.toString())
                true
            }
    }
}

private fun updateProfileSettings(profileId: String?) {
    Log.i(LOG_KEY, "Update settings based on the profile: $profileId")
    profiles[profileId]?.run {
        Prefs.edit().let {
            // init
            it.putString("pref.adapter.init.protocol", init.protocol)
            it.putString("pref.adapter.init.header22", init.header22)
            it.putString("pref.adapter.init.header01", init.header01)
            it.putString("pref.adapter.init.delay", init.delay)
            it.putString("pref.adapter.command.freq", connection.commandFrequency)
            it.putString(PREFERENCE_CONNECTION_TYPE, connection.type)
        }.apply()
    }
}