package org.openobd2.core.logger.ui.preferences.pid

import android.content.Context
import android.util.AttributeSet

import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference.OnPreferenceChangeListener
import org.openobd2.core.logger.bl.datalogger.RESOURCE_LIST_CHANGED_EVENT
import org.openobd2.core.logger.navigateToPreferencesScreen
import org.openobd2.core.logger.sendBroadcastEvent

class PidResourceListPreferences(
    context: Context?,
    attrs: AttributeSet?
) :
    MultiSelectListPreference(context, attrs) {

    init {
        val files = mapOf(
            "alfa.json" to "Giulietta QV",
            "mode01.json" to "Mode 01",
            "mode01_3.json" to "Mode 01.3",
            "extra.json" to "Extra"
        )

        entries = files.values.toTypedArray()
        entryValues = files.keys.toTypedArray()
        setDefaultValue(files.keys)

        onPreferenceChangeListener =
            OnPreferenceChangeListener { _, _ ->
                navigateToPreferencesScreen("pref.registry")
                sendBroadcastEvent(RESOURCE_LIST_CHANGED_EVENT)
                true
            }
    }
}