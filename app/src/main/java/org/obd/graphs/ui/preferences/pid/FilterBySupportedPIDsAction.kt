package org.obd.graphs.ui.preferences.pid

import android.content.Context
import android.util.AttributeSet
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import org.obd.graphs.activity.navigateToPreferencesScreen

class FilterBySupportedPIDsAction(
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