package org.obd.graphs.ui.preferences

import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import org.obd.graphs.ApplicationContext
import org.obd.graphs.CarApplicationContext
import java.lang.Exception

val Prefs: SharedPreferences by lazy {
    try {
        PreferenceManager.getDefaultSharedPreferences(
            ApplicationContext.get()!!
        )
    }catch (e: Exception){
        PreferenceManager.getDefaultSharedPreferences(
            CarApplicationContext.get()!!
        )
    }
}

fun SharedPreferences.updateString(key:String, value: String?) {
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