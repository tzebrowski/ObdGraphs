package org.obd.graphs.bl.query

import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getStringSet

private const val PREFERENCE_PID_FAST = "pref.pids.generic.high"
private const val PREFERENCE_PID_SLOW = "pref.pids.generic.low"

internal class SharedQueryStrategy : QueryStrategy() {

    override fun getPIDs(): MutableSet<Long> = (fastPIDs() + slowPIDs()).toMutableSet()

    private fun fastPIDs() = Prefs.getStringSet(PREFERENCE_PID_FAST).map { s -> s.toLong() }
    private fun slowPIDs() = Prefs.getStringSet(PREFERENCE_PID_SLOW).mapNotNull {
        try {
            it.toLong()
        } catch (e: Exception) {
            null
        }
    }
}