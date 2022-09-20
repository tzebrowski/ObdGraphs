package org.obd.graphs.preferences.supported_pids

import android.content.Context
import android.util.AttributeSet
import androidx.preference.CheckBoxPreference
import org.obd.graphs.activity.navigateToPreferencesScreen

class FilterByStablePIDsAction(
    context: Context?,
    attrs: AttributeSet?
) : CheckBoxPreference(context, attrs) {

    init {
        setOnPreferenceClickListener {
           navigateToPreferencesScreen("pref.registry")
            true
        }
    }
}