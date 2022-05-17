package org.openobd2.core.logger.ui.preferences

import android.content.SharedPreferences
import android.util.Log
import androidx.preference.ListPreference
import androidx.preference.Preference
import org.openobd2.core.logger.ApplicationContext
import org.openobd2.core.logger.MainActivity
import org.openobd2.core.logger.R
import java.util.*

private const val LOG_KEY = "Profile"
private const val PREF_PROFILE_ID = "pref.profile.id"

fun PreferencesFragment.registerSaveUserPreferences() {
    (preferenceManager.findPreference("pref.profile.save_current") as Preference?)
        ?.setOnPreferenceClickListener {
            Prefs.edit().let {
                val profileName = selectedProfile()
                Log.i(LOG_KEY, "Saving user preference to profile='$profileName'")
                Prefs.all
                    .filter { (pref, _) -> !pref.startsWith("profile_") }
                    .forEach { (pref, value) ->
                        Log.d(LOG_KEY, "User preference '$profileName.$pref'=$value")
                        it.updatePreference("$profileName.$pref", value)
                    }
                it.apply()
            }
            true
        }
}

fun PreferencesFragment.registerProfileListener() {
    findPreference<ListPreference>(PREF_PROFILE_ID)?.let {
        it.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                loadProfile(newValue.toString())
                (ApplicationContext.get() as MainActivity).navController()
                    .navigate(R.id.navigation_preferences, null)
                updateToolbar()
                true
            }
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

private fun profileToId(profileId: String) =
    profileId.replace(" ", "_").toLowerCase(Locale.getDefault())

private fun selectedProfile(): String = profileToId(Prefs.getString(PREF_PROFILE_ID)!!)

private fun loadProfile(profileId: String) {
    val selectedProfile = profileToId(profileId)
    Log.i(LOG_KEY, "Loading user preferences from the profile='$selectedProfile'")

    Prefs.edit().let {
        Prefs.all
            .filter { (pref, _) -> pref.startsWith(selectedProfile) }
            .forEach { (pref, value) ->
                pref.substring(selectedProfile.length + 1).run {
                    Log.d(LOG_KEY, "Loading user preference $this = $value")
                    it.updatePreference(this, value)
                }
            }
        it.apply()
    }
}
