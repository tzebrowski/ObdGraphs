package org.obd.graphs.preferences.profile

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference

class ProfileResetPreferenceAction(
    context: Context?,
    attrs: AttributeSet?
) : Preference(context, attrs) {

    init {
        setOnPreferenceClickListener {
            vehicleProfile.reset()
            true
        }
    }
}