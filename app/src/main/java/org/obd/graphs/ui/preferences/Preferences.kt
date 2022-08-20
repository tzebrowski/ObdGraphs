package org.obd.graphs.ui.preferences

import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import org.obd.graphs.getContext

val Prefs: SharedPreferences by lazy {
   PreferenceManager.getDefaultSharedPreferences(
       getContext()!!
    )
}

fun SharedPreferences.updateString(key: String, value: String?) {
    edit().putString(key, value).apply()
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

fun SharedPreferences.getS(name: String?, default: String): String {
    return getString(name, default)!!
}

fun SharedPreferences.getString(name: String): String? {
    return getString(name, null)
}