package org.obd.graphs.bl.collector

import org.obd.graphs.round
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
        fun newInstance(source: ObdMetric, value: Number = 0, min: Double = 0.0, max: Double = 0.0, mean: Double = 0.0)
        = CarMetric(source, value = value, min = min, max = max, mean = mean, enabled = true)
    }

    fun toNumber(value: Double?): String {
        return toNumber(source.command.pid, value).toString()
    }

    fun isInAlert() : Boolean = source.isAlert


    fun valueToStringExt(): String =
         if (source.value == null) {
            "No data"
        } else {
            toNumber(source.valueToDouble())
        }

    fun toFloat(): Float =
        if (source.value == null) {
            source.command.pid.min.toFloat()
        } else {
            source.valueToDouble().toFloat()
        }

    override fun equals(other: Any?): Boolean{
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as CarMetric

        return this.source == other.source
    }

    override fun hashCode(): Int{
        return this.source.hashCode()
    }
}

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
                ValueType.DOUBLE -> value.round(2)
                ValueType.INT -> value.toInt()
                ValueType.SHORT -> value.toInt()
                else -> value.round(1)
            }
        }
}