package org.obd.graphs.ui.preferences.pid

import org.obd.graphs.ui.preferences.Prefs

const val ECU_SUPPORTED_PIDS = "datalogger.supported.pids"

internal fun updateSupportedPIDsPreference(value: Set<String>) {
    Prefs.edit().putStringSet(ECU_SUPPORTED_PIDS, value).apply()
}
internal fun  getECUSupportedPIDs(): MutableSet<String> {
    return Prefs.getStringSet(ECU_SUPPORTED_PIDS, emptySet())!!
}