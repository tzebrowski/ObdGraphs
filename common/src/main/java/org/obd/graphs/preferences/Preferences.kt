package org.obd.graphs.preferences

import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import org.obd.graphs.getContext

val Prefs: SharedPreferences by lazy {
   PreferenceManager.getDefaultSharedPreferences(
       getContext()!!
    )
}

fun SharedPreferences.updateBoolean(key: String, value: Boolean): SharedPreferences.Editor {
    edit().putBoolean(key, value).apply()
    return edit()
}

fun SharedPreferences.updateString(key: String, value: String?): SharedPreferences.Editor {
    edit().putString(key, value).apply()
    return edit()
}

fun SharedPreferences.updateInt(key: String, value: Int){
   edit().putInt(key, value).commit()
}

fun SharedPreferences.updateStringSet(key: String, list: List<String>) {
    return edit().putStringSet(key, list.map { l -> l }.toSet()).apply()
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