package org.openobd2.core.logger.ui.preferences

import android.content.SharedPreferences

lateinit var Prefs: SharedPreferences

fun SharedPreferences.updateLongSet(key: String, list: List<Long>) {
    return edit().putStringSet(key, list.map { l -> l.toString() }.toSet()).apply()
}


fun SharedPreferences.getLongSet(key: String): Set<Long> {
    return getStringSet(key, emptySet())?.map { s -> s.toLong() }?.toSet()!!

}

fun SharedPreferences.getMode01Pids(): MutableSet<String> {
    return getStringSet("pref.pids.generic")
}

fun SharedPreferences.getMode22Pids(): MutableSet<String> {
    return getStringSet("pref.pids.mode22")
}

fun SharedPreferences.getStringSet(key: String): MutableSet<String> {
    return getStringSet(key, emptySet())!!

}

fun SharedPreferences.isEnabled(key: String): Boolean {
    return getBoolean(key, false)
}

fun SharedPreferences.isBatchEnabled(): Boolean {
    return getBoolean("pref.adapter.batch.enabled", true)

}


fun SharedPreferences.isReconnectWhenError(): Boolean {
    return getBoolean("pref.adapter.reconnect", true)
}

fun SharedPreferences.getAdapterName(): String {
    return getString("pref.adapter.id", "OBDII")!!
}

fun SharedPreferences.getCommandFreq(): Long {
    return getString("pref.adapter.command.freq", "6").toString().toLong()
}

fun SharedPreferences.getInitDelay(): Long {
    return getString("pref.adapter.init_delay", "500").toString().toLong()
}

fun SharedPreferences.getMode(): String? {
    return getString("pref.mode", "Generic mode")
}

fun SharedPreferences.getString(name: String): String? {
    return getString(name, null)
}