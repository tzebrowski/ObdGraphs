package org.openobd2.core.logger.ui.preferences.profile

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.core.os.bundleOf
import androidx.preference.EditTextPreference
import androidx.preference.Preference.OnPreferenceChangeListener
import org.openobd2.core.logger.ApplicationContext
import org.openobd2.core.logger.MainActivity
import org.openobd2.core.logger.R
import org.openobd2.core.logger.ui.preferences.PREFERENCE_SCREEN_KEY
import org.openobd2.core.logger.ui.preferences.Prefs


class ProfileNamePreference(
    context: Context?,
    attrs: AttributeSet?
) :
    EditTextPreference(context, attrs) {
    init {
        onPreferenceChangeListener = OnPreferenceChangeListener { _, newValue ->
            Log.i(LOG_KEY, "Updating profile value: ${getCurrentProfile()}=$newValue")
            Prefs.edit().putString("$PROFILE_NAME_PRFIX.${getCurrentProfile()}", newValue.toString()).apply()
            (ApplicationContext.get() as MainActivity).navController()
                .navigate(R.id.navigation_preferences,  bundleOf(PREFERENCE_SCREEN_KEY to  "pref.profiles"))
            true
        }
    }
}