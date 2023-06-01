package org.obd.graphs.preferences.profile

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import androidx.preference.ListPreference
import androidx.preference.Preference.OnPreferenceChangeListener
import org.obd.graphs.activity.navigateToPreferencesScreen
import org.obd.graphs.preferences.updateToolbar
import org.obd.graphs.ui.common.COLOR_PHILIPPINE_GREEN
import org.obd.graphs.ui.common.colorize

class ProfileListPreference(
    context: Context?,
    attrs: AttributeSet?
) : ListPreference(context, attrs) {

    init {

        vehicleProfile.getProfileList()
            .let {
                Log.e("EEEEEEEEEEEEEEEEEEEE", "EEEEEEEEEEEEEEEEEE $it")
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

    override fun getSummary(): CharSequence {
        return super.getSummary().toString().colorize(COLOR_PHILIPPINE_GREEN, Typeface.BOLD, 1.0f)
    }
}