package org.obd.graphs.preferences.pid

import android.content.Context
import android.util.AttributeSet
import androidx.preference.CheckBoxPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference.OnPreferenceChangeListener
import org.obd.graphs.activity.navigateToPreferencesScreen
import org.obd.graphs.bl.datalogger.ACCESS_EXTERNAL_STORAGE_ENABLED
import org.obd.graphs.bl.datalogger.RESOURCE_LIST_CHANGED_EVENT
import org.obd.graphs.bl.datalogger.pidResources
import org.obd.graphs.sendBroadcastEvent


class PIDsResourceListPreferences(
    context: Context?,
    attrs: AttributeSet?
) :
    MultiSelectListPreference(context, attrs) {

    init {
        initialize { pidResources.getExternalPidResources(context) }

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
                initialize { pidResources.getExternalPidResources(context) { new.toString().toBoolean() } }
                true
            }
        }
    }

    private fun initialize(files: () -> MutableMap<String, String>? = { null }) {
        val mutableMap = pidResources.getDefaultPidFiles().toMutableMap().apply {
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