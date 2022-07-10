package org.obd.graphs.ui.preferences.profile

import android.content.Context
import android.util.AttributeSet
import androidx.preference.ListPreference
import androidx.preference.Preference.OnPreferenceChangeListener
import org.obd.graphs.activity.navigateToPreferencesScreen
import org.obd.graphs.ui.preferences.Prefs
import org.obd.graphs.ui.preferences.updateToolbar


private const val DEFAULT_MAX_PROFILES = "5"

class ProfileListPreference(
    context: Context?,
    attrs: AttributeSet?
) :
    ListPreference(context, attrs) {

    init {

        generateProfileList()
            .let {
                entries = it.values.toTypedArray()
                entryValues = it.keys.toTypedArray()
                setDefaultValue(it.keys.first())
            }

        onPreferenceChangeListener =
            OnPreferenceChangeListener { _, newValue ->
                loadProfile(newValue.toString())
                updateToolbar()
                navigateToPreferencesScreen(PROFILES_PREF)
                true
            }
    }

    private fun generateProfileList() =
        (1..Prefs.getString(MAX_PROFILES_PREF, DEFAULT_MAX_PROFILES)!!.toInt())
            .associate {
                "profile_$it" to Prefs.getString(
                    "$PROFILE_NAME_PREFIX.profile_$it",
                    "Profile $it"
                )
            }
}