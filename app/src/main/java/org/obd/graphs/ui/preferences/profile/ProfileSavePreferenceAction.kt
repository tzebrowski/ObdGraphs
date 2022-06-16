package org.obd.graphs.ui.preferences.profile

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference

class ProfileSavePreferenceAction(
    context: Context?,
    attrs: AttributeSet?
) : Preference(context, attrs) {

    init {
        setOnPreferenceClickListener {
            saveCurrentProfile()
            true
        }
    }
}