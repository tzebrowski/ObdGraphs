package org.obd.graphs.ui.preferences.mode

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.preference.ListPreference
import androidx.preference.Preference.OnPreferenceChangeListener
import org.obd.graphs.navigateToPreferencesScreen
import org.obd.graphs.ui.preferences.Prefs

class HeaderListPreferences(
    context: Context?,
    attrs: AttributeSet?
) :
    ListPreference(context, attrs) {

    private val preferenceChangeListener = OnPreferenceChangeListener { _, newValue ->
        Log.i(LOG_KEY, "Updating mode name: ${getCurrentMode()}=$newValue")
        Prefs.edit()
            .putString("$MODE_HEADER_PREFIX.${getCurrentMode()}", newValue.toString())
            .apply()
        navigateToPreferencesScreen(PREFERENCE_PAGE)
        true
    }

    init {
        onPreferenceChangeListener = preferenceChangeListener

        linkedMapOf(
            "" to "",
            "DA10F1" to "DA10F1",
            "DB33F1" to "DB33F1",
            "7DF" to "7DF"
        ).apply {
            Prefs.getInt(CAN_HEADER_COUNTER_PREF, 0).let { it ->
                Log.d(LOG_KEY, "Number of custom CAN headers available: $it")
                if (it > 0) {
                    (1..it).forEach {
                        Prefs.getString("pref.adapter.init.header.$it", "")?.let { header ->
                            this[header] = header
                        }
                    }
                }
            }
        }.let {
            setDefaultValue(it.keys.first())
            entries = it.values.toTypedArray()
            entryValues = it.keys.toTypedArray()
        }
    }
}