package org.openobd2.core.logger.ui.preferences

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import org.openobd2.core.logger.bl.LOG_KEY


class PreferencesHelper {
    companion object {

        @JvmStatic
        fun getStringSet(context: Context, key: String): MutableSet<String> {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            return pref.getStringSet(key, emptySet())!!
        }

        @JvmStatic
        fun isEnabled(context: Context, key: String): Boolean {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            return pref.getBoolean(key, true)
        }

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