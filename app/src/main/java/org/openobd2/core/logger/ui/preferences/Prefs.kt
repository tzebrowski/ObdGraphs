package org.openobd2.core.logger.ui.preferences

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import org.openobd2.core.logger.bl.LOG_KEY


class Prefs {
    companion object {

        @JvmStatic
        fun isBatchEnabled(context: Context): Boolean {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            val batchEnabled = pref.getBoolean("pref.batch.enabled", true)
            Log.v(LOG_KEY, "Batch enabled: $batchEnabled")
            return batchEnabled
        }

        @JvmStatic
        fun getMode(context: Context): String? {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            return pref.getString("pref.mode", "Generic mode")
        }
    }
}