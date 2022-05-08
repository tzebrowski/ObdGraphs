package org.openobd2.core.logger.ui.preferences

import android.util.Log
import androidx.preference.ListPreference
import androidx.preference.Preference
import org.openobd2.core.logger.ApplicationContext
import org.openobd2.core.logger.MainActivity
import org.openobd2.core.logger.R

private const val LOG_KEY = "Profile"


fun PreferencesFragment.saveUserProfile() {
    val button = preferenceManager.findPreference("pref.profile.save_current") as Preference?
    button?.setOnPreferenceClickListener {
        Prefs.edit().let {
            var selectedProfile = selectedProfile()

            Log.i(LOG_KEY, "Saving user profile within $selectedProfile")

            Prefs.all.forEach { (pref, value) ->
                if (!pref.startsWith("profile_")) {

                    Log.i(LOG_KEY, "Saving user profile $selectedProfile.$pref = $value")
                    when (value) {
                        is String -> {
                            it.putString("$selectedProfile.$pref", value)
                        }
                        is Set<*> -> {
                            it.putStringSet("$selectedProfile.$pref", value as MutableSet<String>?)
                        }
                        is Int -> {
                            it.putInt("$selectedProfile.$pref", value)
                        }
                        is Boolean -> {
                            it.putBoolean("$selectedProfile.$pref", value)
                        }
                    }
                }
            }
            it
        }.apply()
        true
    }
}

private fun selectedProfile(): String {
    var selectedProfile = Prefs.getString("pref.profile.id")!!
    selectedProfile = selectedProfile.replace(" ", "_").toLowerCase()
    return selectedProfile
}

fun PreferencesFragment.registerProfileListener() {
    findPreference<ListPreference>("pref.profile.id")?.let {
        it.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                loadProfileSettings(newValue.toString())
                (ApplicationContext.get() as MainActivity).navController()
                    .navigate(R.id.navigation_preferences, null)
                true
            }
    }

}

private fun loadProfileSettings(profileId: String?) {
    val selectedProfile = profileId!!.replace(" ", "_").toLowerCase()
    Log.i(LOG_KEY, "Loading user profile: $selectedProfile")

    Prefs.edit().let {
        Prefs.all.forEach { (pref, value) ->
            if (pref.startsWith(selectedProfile)) {

                val originalPrefName = pref.substring(selectedProfile.length + 1)
                Log.i(LOG_KEY, "Loading user profile $originalPrefName = $value")

                when (value) {
                    is String -> {
                        it.putString(originalPrefName, value)
                    }
                    is Set<*> -> {
                        it.putStringSet(originalPrefName, value as MutableSet<String>?)
                    }
                    is Int -> {
                        it.putInt(originalPrefName, value)
                    }
                    is Boolean -> {
                        it.putBoolean(originalPrefName, value)
                    }
                }
            }
        }
        it
    }.apply()
}