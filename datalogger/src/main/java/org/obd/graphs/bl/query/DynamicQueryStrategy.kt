package org.obd.graphs.bl.query

import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getLongSet

private const val DYNAMIC_QUERY_PREF_KEY = "pref.aa.dynamic.pids.selected"

internal class DynamicQueryStrategy : QueryStrategy() {

    private val defaults  = mutableSetOf(
        namesRegistry.getAtmPressurePID(),
        namesRegistry.getAmbientTempPID(),
        namesRegistry.getGearboxOilTempPID(),
        namesRegistry.getOilTempPID(),
        namesRegistry.getCoolantTempPID(),
        namesRegistry.getExhaustTempPID(),
        namesRegistry.getAirTempPID(),
        namesRegistry.getTorquePID(),
        namesRegistry.getIntakePressurePID(),
        namesRegistry.getDynamicSelectorPID(),
        namesRegistry.getOilPressurePID(),
    )
    override fun getDefaults() = defaults

    override fun getPIDs() = Prefs.getLongSet(DYNAMIC_QUERY_PREF_KEY).toMutableSet()
}