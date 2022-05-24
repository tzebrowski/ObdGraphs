package org.openobd2.core.logger.ui.preferences.profile

import android.content.Context
import android.util.AttributeSet
import androidx.preference.ListPreference
import org.openobd2.core.logger.ui.preferences.Prefs

private const val profile_1_id = "profile_1"
private const val profile_2_id = "profile_2"
private const val profile_3_id = "profile_3"
private const val profile_4_id = "profile_4"
private const val profile_5_id = "profile_5"

class ProfileListPreferences(
    context: Context?,
    attrs: AttributeSet?
) :
    ListPreference(context, attrs) {

    init {
        val map = linkedMapOf(
            profile_1_id to Prefs.getString("$PROFILE_NAME_PREFIX.$profile_1_id", "Profile 1"),
            profile_2_id to Prefs.getString("$PROFILE_NAME_PREFIX.$profile_2_id", "Profile 2"),
            profile_3_id to Prefs.getString("$PROFILE_NAME_PREFIX.$profile_3_id", "Profile 3"),
            profile_4_id to Prefs.getString("$PROFILE_NAME_PREFIX.$profile_4_id", "Profile 4"),
            profile_5_id to Prefs.getString("$PROFILE_NAME_PREFIX.$profile_5_id", "Profile 5")
        )
        setDefaultValue(profile_1_id)

        entries = map.values.toTypedArray()
        entryValues = map.keys.toTypedArray()
    }
}