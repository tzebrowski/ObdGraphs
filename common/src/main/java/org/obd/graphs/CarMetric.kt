package org.obd.graphs

import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.pid.PidDefinition
import org.obd.metrics.pid.ValueType

data class CarMetric(
    var source: ObdMetric,
    var value: Number,
    var min: Double,
    var max: Double,
    var mean: Double,
    var enabled: Boolean = true
) {
    companion object {
        fun newInstance(source: ObdMetric) = CarMetric(source, value = 0, min = 0.0, max = 0.0, mean = 0.0, enabled = true)
    }

    fun toNumber(value: Double?): String {
        return toNumber(source.command.pid, value).toString()
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
}