package org.openobd2.core.logger.ui.preferences

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import org.openobd2.core.logger.bl.LOG_KEY


class Preferences {
    companion object {
        @JvmStatic
        fun getLongSet(context: Context, key: String): Set<Long> {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            return pref.getStringSet(key, emptySet())?.map { s -> s.toLong() }?.toSet()!!
        }

        @JvmStatic
        fun getMode01Pids(context: Context): MutableSet<String> {
            return getStringSet(context, "pref.pids.generic")
        }

        @JvmStatic
        fun getMode22Pids(context: Context): MutableSet<String> {
            return getStringSet(context, "pref.pids.mode22")
        }

        @JvmStatic
        fun getStringSet(context: Context, key: String): MutableSet<String> {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            return pref.getStringSet(key, emptySet())!!
        }

        @JvmStatic
        fun isEnabled(context: Context, key: String): Boolean {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            return pref.getBoolean(key, false)
        }

        @JvmStatic
        fun isBatchEnabled(context: Context): Boolean {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            val batchEnabled = pref.getBoolean("pref.adapter.batch.enabled", true)
            Log.v(LOG_KEY, "Batch enabled: $batchEnabled")
            return batchEnabled
        }


        @JvmStatic
        fun isReconnectWhenError(context: Context): Boolean {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            return pref.getBoolean("pref.adapter.reconnect", true)
        }

        @JvmStatic
        fun getAdapterName(context: Context): String {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            return pref.getString("pref.adapter.id", "OBDII")!!
        }

        @JvmStatic
        fun getCommandFreq(context: Context): Long {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            return pref.getString("pref.adapter.command.freq", "6").toString().toLong()
        }

        @JvmStatic
        fun getInitDelay(context: Context): Long {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            return pref.getString("pref.adapter.init_delay", "500").toString().toLong()
        }

        @JvmStatic
        fun getMode(context: Context): String? {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            return pref.getString("pref.mode", "Generic mode")
        }
    }
}