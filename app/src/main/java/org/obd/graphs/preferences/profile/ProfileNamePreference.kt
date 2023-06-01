package org.obd.graphs.preferences.profile

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.preference.EditTextPreference
import androidx.preference.Preference.OnPreferenceChangeListener
import org.obd.graphs.activity.navigateToPreferencesScreen
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.profile.PROFILE_NAME_PREFIX

class ProfileNamePreference(
    context: Context?,
    attrs: AttributeSet?
):
    EditTextPreference(context, attrs) {
    init {
        onPreferenceChangeListener = OnPreferenceChangeListener { _, newValue ->

            Log.d(LOG_KEY, "Updating profile value: ${vehicleProfile.getCurrentProfile()}=$newValue")

            Prefs.edit()
                .putString("$PROFILE_NAME_PREFIX.${vehicleProfile.getCurrentProfile()}", newValue.toString())
                .apply()
            navigateToPreferencesScreen(PROFILES_PREF)
            true
        }
    }
}