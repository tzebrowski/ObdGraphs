package org.openobd2.core.logger.ui.preferences.pid

import android.content.Context
import android.util.AttributeSet
import androidx.preference.CheckBoxPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference.OnPreferenceChangeListener
import org.openobd2.core.logger.bl.datalogger.ACCESS_EXTERNAL_STORAGE_ENABLED
import org.openobd2.core.logger.bl.datalogger.RESOURCE_LIST_CHANGED_EVENT
import org.openobd2.core.logger.bl.datalogger.defaultPidFiles
import org.openobd2.core.logger.bl.datalogger.getExternalPidResources
import org.openobd2.core.logger.navigateToPreferencesScreen
import org.openobd2.core.logger.sendBroadcastEvent


class PidResourceListPreferences(
    context: Context?,
    attrs: AttributeSet?
) :
    MultiSelectListPreference(context, attrs) {

    init {
        initialize() { getExternalPidResources(context) }

        onPreferenceChangeListener =
            OnPreferenceChangeListener { _, _ ->
                navigateToPreferencesScreen("pref.registry")
                sendBroadcastEvent(RESOURCE_LIST_CHANGED_EVENT)
                true
            }
    }

    override fun onAttached() {
        super.onAttached()

        findPreferenceInHierarchy<CheckBoxPreference>(ACCESS_EXTERNAL_STORAGE_ENABLED)?.run {
            onPreferenceChangeListener = OnPreferenceChangeListener { _, new ->
                initialize() { getExternalPidResources(context) { new.toString().toBoolean() } }
                true
            }
        }
    }

    private fun initialize(files: () -> MutableMap<String, String>? = { null }) {
        val mutableMap = defaultPidFiles.toMutableMap().apply {
            files()?.let {
                putAll(it)
            }
        }
        mutableMap.let {
            entries = it.values.toTypedArray()
            entryValues = it.keys.toTypedArray()
            setDefaultValue(it.keys)
        }
    }
}