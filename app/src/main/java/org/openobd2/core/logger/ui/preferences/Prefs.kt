package org.openobd2.core.logger.ui.preferences

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import org.openobd2.core.logger.bl.LOG_KEY


const val GENERIC_MODE = "Generic mode"

class Prefs {
    companion object {

        @JvmStatic
        fun isBatchEnabled(context: Context): Boolean {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            val bratchEnabled = pref.getBoolean("pref.batch.enabled", true)
            Log.i(LOG_KEY, "Batch enabled: $bratchEnabled")
            return bratchEnabled
        }

        @JvmStatic
        fun getMode(context: Context): String? {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            val selectedMode = pref.getString("pref.mode", "Generic mode")
            Log.i(LOG_KEY, "Selected OBD mode: $selectedMode")
            return selectedMode
        }
    }
}