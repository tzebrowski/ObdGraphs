package org.obd.graphs.ui.common

import org.obd.metrics.ObdMetric
import org.obd.metrics.pid.PidDefinition
import org.obd.graphs.ui.dashboard.round


fun convert(pid: PidDefinition, input: Number): Number {

    if (input == null) {
        return Double.NaN
    }

    val value = input.toDouble()
    if (value.isNaN()) {
        return 0.0
    }
    return if (pid.type == null) value.round(2) else
        pid.type.let {
            return when (pid.type) {
                PidDefinition.ValueType.DOUBLE -> value.round(2)
                PidDefinition.ValueType.INT -> value.toInt()
                PidDefinition.ValueType.SHORT -> value.toInt()
                else -> value.round(1)
            }
        }
}

fun ObdMetric.convert(value: Double): Number {
    return convert(command.pid, value)
}