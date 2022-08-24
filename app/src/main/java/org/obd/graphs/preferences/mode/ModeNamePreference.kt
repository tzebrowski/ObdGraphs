package org.obd.graphs.preferences.mode

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.preference.EditTextPreference
import androidx.preference.Preference.OnPreferenceChangeListener
import org.obd.graphs.MODE_NAME_PREFIX
import org.obd.graphs.PREFERENCE_PAGE
import org.obd.graphs.activity.navigateToPreferencesScreen
import org.obd.graphs.getCurrentMode
import org.obd.graphs.preferences.Prefs


class ModeNamePreference(
    context: Context?,
    attrs: AttributeSet?
) :
    EditTextPreference(context, attrs) {
    private val preferenceChangeListener = OnPreferenceChangeListener { _, newValue ->
        Log.i(MODE_LOG_KEY, "Updating mode name: ${getCurrentMode()}=$newValue")
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