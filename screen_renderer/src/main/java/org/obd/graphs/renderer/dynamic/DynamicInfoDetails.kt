package org.obd.graphs.renderer.dynamic

import org.obd.graphs.bl.collector.Metric

data class DynamicInfoDetails(
    var ambientTemp: Metric? = null,
    var atmPressure: Metric? = null,
    var totalMisfires: Metric? = null,
    var fuellevel: Metric? = null,
    var fuelConsumption: Metric? = null,
    var oilTemp: Metric? = null,
    var coolantTemp: Metric? = null,
    var airTemp: Metric? = null,
    var exhaustTemp: Metric? = null,
    var gearboxOilTemp: Metric? = null,
    var oilLevel: Metric? = null,
    var intakePressure: Metric? = null,
    var torque: Metric? = null,
    var distance: Metric? = null,
    var ibs: Metric? = null,
    var batteryVoltage: Metric? = null,
    var oilPressure: Metric? = null,
)