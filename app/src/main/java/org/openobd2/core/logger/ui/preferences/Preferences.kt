package org.openobd2.core.logger.ui.preferences

import android.content.SharedPreferences

lateinit var Prefs: SharedPreferences

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