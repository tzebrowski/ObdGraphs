package org.openobd2.core.logger.ui.preferences.profile

import android.content.Context
import android.util.AttributeSet
import androidx.preference.ListPreference
import androidx.preference.Preference.OnPreferenceChangeListener
import org.openobd2.core.logger.navigateToPreferencesScreen
import org.openobd2.core.logger.ui.preferences.Prefs
import org.openobd2.core.logger.ui.preferences.updateToolbar


class ProfileListPreferences(
    context: Context?,
    attrs: AttributeSet?
) :
    ListPreference(context, attrs) {

    init {
        (1..Prefs.getString(MAX_PROFILES_PREF, "5")!!.toInt())
            .associate {
                "profile_$it" to Prefs.getString(
                    "$PROFILE_NAME_PREFIX.profile_$it",
                    "Profile $it"
                )
            }
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
}