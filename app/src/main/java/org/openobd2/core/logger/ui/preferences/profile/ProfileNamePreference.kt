package org.openobd2.core.logger.ui.preferences.profile

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.preference.EditTextPreference
import androidx.preference.Preference.OnPreferenceChangeListener
import org.openobd2.core.logger.navigateToPreferencesScreen
import org.openobd2.core.logger.ui.preferences.Prefs


class ProfileNamePreference(
    context: Context?,
    attrs: AttributeSet?
) :
    EditTextPreference(context, attrs) {
    init {
        onPreferenceChangeListener = OnPreferenceChangeListener { _, newValue ->
            Log.i(LOG_KEY, "Updating profile value: ${getCurrentProfile()}=$newValue")
            Prefs.edit()
                .putString("$PROFILE_NAME_PREFIX.${getCurrentProfile()}", newValue.toString())
                .apply()
            navigateToPreferencesScreen("pref.profiles")
            true
        }
    }
}