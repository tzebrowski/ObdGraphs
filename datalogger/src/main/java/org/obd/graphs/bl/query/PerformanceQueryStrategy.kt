package org.obd.graphs.bl.query


internal class PerformanceQueryStrategy : QueryStrategy() {

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
        namesRegistry.getGasPedalPID()
    )
    override fun getDefaults() = defaults

    override fun getPIDs() = getDefaults()
}