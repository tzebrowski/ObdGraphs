package org.obd.graphs.ui.common

import org.obd.graphs.ui.dashboard.round
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.pid.PidDefinition

fun toNumber(pid: PidDefinition, input: Number?): Number {

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

fun ObdMetric.toNumber(value: Double?): Number {
    return toNumber(command.pid, value)
}

fun ObdMetric.valueToStringExt(): String {
    return if (value == null) {
        "No data"
    } else {
        return toNumber(valueToDouble()).toString()
    }
}

fun ObdMetric.toFloat(): Float {
    return if (value == null) {
        command.pid.min.toFloat()
    } else {
        valueToDouble().toFloat()
    }
 }