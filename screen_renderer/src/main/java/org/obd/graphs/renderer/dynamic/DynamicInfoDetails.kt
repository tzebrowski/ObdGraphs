package org.obd.graphs.renderer.dynamic

import org.obd.graphs.bl.collector.Metric

data class DynamicInfoDetails(
    var ambientTemp: Metric? = null,
    var atmPressure: Metric? = null,
    var oilTemp: Metric? = null,
    var coolantTemp: Metric? = null,
    var airTemp: Metric? = null,
    var exhaustTemp: Metric? = null,
    var gearboxOilTemp: Metric? = null,
    var intakePressure: Metric? = null,
    var torque: Metric? = null,
    var oilPressure: Metric? = null,
)