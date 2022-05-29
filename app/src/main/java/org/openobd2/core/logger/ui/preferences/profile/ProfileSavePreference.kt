package org.openobd2.core.logger.ui.preferences.profile

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.preference.Preference
import org.openobd2.core.logger.ui.preferences.Prefs

class ProfileSavePreference(
    context: Context?,
    attrs: AttributeSet?
) : Preference(context, attrs) {

    init {
        setOnPreferenceClickListener {
            Prefs.edit().let {
                val profileName = getCurrentProfile()
                Log.i(LOG_KEY, "Saving user preference to profile='$profileName'")
                Prefs.all
                    .filter { (pref, _) -> !pref.startsWith("profile_") }
                    .filter { (pref, _) -> !pref.startsWith(PROFILE_NAME_PREFIX) }
                    .filter { (pref, _) -> !pref.startsWith(PROFILE_CURRENT_NAME_ID) }
                    .filter { (pref, _) -> !pref.startsWith(PROFILE_INSTALLATION_KEY) }
                    .filter { (pref, _) -> !pref.startsWith(PROFILE_INSTALLATION_KEY) }
                    .forEach { (pref, value) ->
                        Log.v(LOG_KEY, "'$profileName.$pref'=$value")
                        it.updatePreference("$profileName.$pref", value)
                    }
                it.apply()
            }
            true
        }
    }
}