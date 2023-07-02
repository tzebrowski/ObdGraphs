package org.obd.graphs.aa

import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.pid.PidDefinition
import org.obd.metrics.pid.ValueType



internal data class CarMetric(
    val pid: PidDefinition,
    var value: Double?,
    var min: Double,
    var max: Double,
    var avg: Double,
    var enabled: Boolean = true
) {

    companion object {
        fun newInstance(it: ObdMetric) = CarMetric(it.command.pid, null, 0.0, 0.0, 0.0)
    }

    fun valueToString(): String {
        return if (value == null) {
            "No data"
        } else {
            return toNumber(value).toString()
        }
    }

    private fun toNumber(pid: PidDefinition, input: Number?): Number {

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
                    ValueType.DOUBLE -> value.round(2)
                    ValueType.INT -> value.toInt()
                    ValueType.SHORT -> value.toInt()
                    else -> value.round(1)
                }
            }
    }

    fun toNumber(value: Double?): Number {
        return toNumber(pid, value)
    }
}