package org.obd.graphs.ui.preferences

import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import org.obd.graphs.ApplicationContext
import org.obd.graphs.R

val Prefs: SharedPreferences by lazy {
    PreferenceManager.setDefaultValues(ApplicationContext.get()!!, R.xml.preferences, false)
    PreferenceManager.getDefaultSharedPreferences(
        ApplicationContext.get()!!
    )
}

private const val ECU_SUPPORTED_PIDS = "datalogger.supported.pids"

fun SharedPreferences.getECUSupportedPids(): MutableSet<String> {
    return getStringSet(ECU_SUPPORTED_PIDS, emptySet())!!
}

fun SharedPreferences.updatePIDSupportedByECU(list: Set<String>) {
    edit().putStringSet(ECU_SUPPORTED_PIDS, list).apply()
}

fun SharedPreferences.updateLongSet(key: String, list: List<Long>) {
    return edit().putStringSet(key, list.map { l -> l.toString() }.toSet()).apply()
}

fun SharedPreferences.getLongSet(key: String): Set<Long> {
    return getStringSet(key, emptySet())?.map { s -> s.toLong() }?.toSet()!!
}

fun SharedPreferences.getStringSet(key: String): MutableSet<String> {
    return getStringSet(key, emptySet())!!
}

fun SharedPreferences.isEnabled(key: String): Boolean {
    return getBoolean(key, false)
}

fun SharedPreferences.getString(name: String): String? {
    return getString(name, null)
}