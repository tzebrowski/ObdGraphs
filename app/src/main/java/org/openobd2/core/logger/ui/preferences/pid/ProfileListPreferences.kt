package org.openobd2.core.logger.ui.preferences.pid

import android.content.Context
import android.util.AttributeSet
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference.OnPreferenceChangeListener
import org.openobd2.core.logger.bl.datalogger.RESOURCE_LIST_CHANGED_EVENT
import org.openobd2.core.logger.bl.datalogger.defaultPidFiles
import org.openobd2.core.logger.navigateToPreferencesScreen
import org.openobd2.core.logger.sendBroadcastEvent


class PidResourceListPreferences(
    context: Context?,
    attrs: AttributeSet?
) :
    MultiSelectListPreference(context, attrs) {

    init {

        defaultPidFiles.let {
            entries = it.values.toTypedArray()
            entryValues = it.keys.toTypedArray()
            setDefaultValue(it.keys)
        }

        onPreferenceChangeListener =
            OnPreferenceChangeListener { _, _ ->
                navigateToPreferencesScreen("pref.registry")
                sendBroadcastEvent(RESOURCE_LIST_CHANGED_EVENT)
                true
            }
    }
}