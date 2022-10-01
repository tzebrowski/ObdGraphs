package org.obd.graphs

import org.obd.graphs.preferences.Prefs

const val MODE_ID = "pref.adapter.init.mode.selected"
const val MODE_NAME_PREFIX = "pref.adapter.init.mode.id_value"
const val MODE_HEADER_PREFIX = "pref.adapter.init.mode.header_value"

const val CAN_HEADER_COUNTER_PREF = "pref.adapter.init.header.counter"
const val PREFERENCE_PAGE = "pref.init"
const val PREF_CAN_HEADER_EDITOR = "pref.adapter.init.mode.header"
const val PREF_ADAPTER_MODE_ID_EDITOR = "pref.adapter.init.mode.id"
const val MAX_MODES = 7

fun getModesAndHeaders(): Map<String, String> {
    return getAvailableModes().associate { getModeID(it) to getModeHeader(it) }
}

fun getAvailableModes() = (1..MAX_MODES).map { "mode_$it" }
fun getCurrentMode(): String = Prefs.getString(MODE_ID, "")!!
private fun getModeHeader(id: String) = Prefs.getString("$MODE_HEADER_PREFIX.$id", "")!!
private fun getModeID(id: String) = Prefs.getString("$MODE_NAME_PREFIX.$id", "")!!