package org.openobd2.core.logger.ui.preferences

import android.content.Context
import androidx.preference.PreferenceManager


const val GENERIC_MODE = "Generic mode"
class Prefs{
    companion object {

        @JvmStatic
        fun getMode(context: Context): String? {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            return pref.getString("pref.mode", "Generic mode")
        }
    }
}