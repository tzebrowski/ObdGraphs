package org.obd.graphs.preferences.profile

import android.content.Context
import android.util.AttributeSet
import androidx.preference.ListPreference
import androidx.preference.Preference.OnPreferenceChangeListener
import org.obd.graphs.activity.navigateToPreferencesScreen
import org.obd.graphs.preferences.updateToolbar

class ProfileListPreference(
    context: Context?,
    attrs: AttributeSet?
) : ListPreference(context, attrs) {

    init {

        vehicleProfile.getProfileList()
            .let {
                entries = it.values.toTypedArray()
                entryValues = it.keys.toTypedArray()
                if (it.keys.isNotEmpty()) {
                    setDefaultValue(it.keys.first())
                }
            }

        onPreferenceChangeListener =
            OnPreferenceChangeListener { _, newValue ->
                vehicleProfile.loadProfile(newValue.toString())
                updateToolbar()
                navigateToPreferencesScreen(PROFILES_PREF)
                true
            }
    }
}