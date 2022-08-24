package org.obd.graphs.preferences

const val ECU_SUPPORTED_PIDS = "pref.datalogger.supported.pids"

internal fun updateSupportedPIDsPreference(value: Set<String>) {
    Prefs.edit().putStringSet(ECU_SUPPORTED_PIDS, value).apply()
}
fun  getECUSupportedPIDs(): MutableSet<String> {
    return Prefs.getStringSet(ECU_SUPPORTED_PIDS, emptySet())!!
}