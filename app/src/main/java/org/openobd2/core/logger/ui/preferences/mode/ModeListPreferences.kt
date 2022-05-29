package org.openobd2.core.logger.ui.preferences.mode

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.preference.ListPreference
import androidx.preference.Preference.OnPreferenceChangeListener
import org.openobd2.core.logger.navigateToPreferencesScreen
import org.openobd2.core.logger.ui.preferences.Prefs

class ModeListPreferences(
    context: Context?,
    attrs: AttributeSet?
) :
    ListPreference(context, attrs) {

    private val preferenceChangeListener = OnPreferenceChangeListener { _, newValue ->
        val modeId = Prefs.getString("$MODE_NAME_PREFIX.$newValue", "")
        val modeHeader = Prefs.getString("$MODE_HEADER_PREFIX.$newValue", "")

        Log.i(LOG_KEY, "Updating mode $modeId=$modeHeader")

        Prefs.edit().run {
            putString(PREF_ADAPTER_MODE_ID_EDITOR, modeId)
            putString(PREF_CAN_HEADER_EDITOR, modeHeader)
            apply()
        }

        navigateToPreferencesScreen(PREFERENCE_PAGE)
        true
    }

    init {

        onPreferenceChangeListener = preferenceChangeListener

        getAvailableModes().associateWith { Prefs.getString("$MODE_NAME_PREFIX.$it", "") }.let {
            setDefaultValue(it.keys.first())
            entries = it.values.toTypedArray()
            entryValues = it.keys.toTypedArray()
        }
    }
}