package org.openobd2.core.logger.ui.preferences.profile

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.preference.EditTextPreference
import org.openobd2.core.logger.ui.preferences.Prefs


class ProfileNamePreference(
    context: Context?,
    attrs: AttributeSet?
) :
    EditTextPreference(context, attrs) {

    override fun setText(text: String?) {
        super.setText(text)
        Log.i(LOG_KEY,"Updating profile value: ${getCurrentProfile()}=$text")
        Prefs.edit().putString("pref.profile.names.${getCurrentProfile()}",text).apply()
    }
}