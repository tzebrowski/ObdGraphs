package org.openobd2.core.logger.ui.preferences.mode

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.preference.EditTextPreference
import androidx.preference.Preference.OnPreferenceChangeListener
import org.openobd2.core.logger.navigateToPreferencesScreen
import org.openobd2.core.logger.ui.preferences.Prefs


class ModeNamePreference(
    context: Context?,
    attrs: AttributeSet?
) :
    EditTextPreference(context, attrs) {
    private val preferenceChangeListener = OnPreferenceChangeListener { _, newValue ->
        Log.i(LOG_KEY, "Updating mode name: ${getCurrentMode()}=$newValue")
        Prefs.edit()
            .putString("$MODE_NAME_PREFIX.${getCurrentMode()}", newValue.toString())
            .apply()
        navigateToPreferencesScreen(PREFERENCE_PAGE)
        true
    }

    init {
        onPreferenceChangeListener = preferenceChangeListener
    }
}