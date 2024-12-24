package org.obd.graphs.bl.query

import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getLongSet

private const val DYNAMIC_QUERY_PREF_KEY = "pref.aa.dynamic.pids.selected"

internal class DynamicQueryStrategy : QueryStrategy() {

    private val defaults  = mutableSetOf(
        namesRegistry.getFuelConsumptionPID(),
        namesRegistry.getFuelLevelPID(),
        namesRegistry.getAtmPressurePID(),
        namesRegistry.getAmbientTempPID(),
        namesRegistry.getGearboxOilTempPID(),
        namesRegistry.getOilTempPID(),
        namesRegistry.getCoolantTempPID(),
        namesRegistry.getExhaustTempPID(),
        namesRegistry.getAirTempPID(),
        namesRegistry.getTotalMisfiresPID(),
        namesRegistry.getOilLevelPID(),
        namesRegistry.getTorquePID(),
        namesRegistry.getIntakePressurePID(),
        namesRegistry.getDynamicSelectorPID(),
        namesRegistry.getDistancePID(),
        namesRegistry.getBatteryVoltagePID(),
        namesRegistry.getIbsPID(),
        namesRegistry.getOilPressurePID(),
    )
    override fun getDefaults() = defaults

    override fun getPIDs() = Prefs.getLongSet(DYNAMIC_QUERY_PREF_KEY).toMutableSet()
}