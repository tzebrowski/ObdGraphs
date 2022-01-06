package org.openobd2.core.logger.ui.preferences

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager

private const val LOGGER_TAG = "PREFS"

private val s1 = "pref.adapter.id"

class Preferences {
    companion object {
        @JvmStatic
        fun updateLongSet(context: Context, key: String, list: List<Long>) {
            return PreferenceManager.getDefaultSharedPreferences(context).run {
                edit().putStringSet(key, list.map { l -> l.toString() }.toSet()).apply()
            }
        }


        @JvmStatic
        fun getLongSet(context: Context, key: String): Set<Long> {
            return PreferenceManager.getDefaultSharedPreferences(context).run {
                 getStringSet(key, emptySet())?.map { s -> s.toLong() }?.toSet()!!
            }
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
            return PreferenceManager.getDefaultSharedPreferences(context).run {
                getStringSet(key, emptySet())!!
            }
        }

        @JvmStatic
        fun isEnabled(context: Context, key: String): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context).run {
                getBoolean(key, false)
            }
        }

        @JvmStatic
        fun isBatchEnabled(context: Context): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context).run {
                val batchEnabled = getBoolean("pref.adapter.batch.enabled", true)
                Log.v(LOGGER_TAG, "Batch enabled: $batchEnabled")
                batchEnabled
            }
        }


        @JvmStatic
        fun isReconnectWhenError(context: Context): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context).run{
                getBoolean("pref.adapter.reconnect", true)
            }
        }

        @JvmStatic
        fun getAdapterName(context: Context): String {
            return PreferenceManager.getDefaultSharedPreferences(context).run{
                getString("pref.adapter.id", "OBDII")!!
            }
        }

        @JvmStatic
        fun getCommandFreq(context: Context): Long {
            return PreferenceManager.getDefaultSharedPreferences(context).run{
                getString("pref.adapter.command.freq", "6").toString().toLong()
            }
        }

        @JvmStatic
        fun getInitDelay(context: Context): Long {
            return PreferenceManager.getDefaultSharedPreferences(context).run{
                getString("pref.adapter.init_delay", "500").toString().toLong()
            }
        }

        @JvmStatic
        fun getMode(context: Context): String? {
            return PreferenceManager.getDefaultSharedPreferences(context).run{
                getString("pref.mode", "Generic mode")
            }
        }


        @JvmStatic
        fun getString(context: Context, name: String): String? {
            return PreferenceManager.getDefaultSharedPreferences(context).run{
                getString(name, null)
            }
        }
    }
}