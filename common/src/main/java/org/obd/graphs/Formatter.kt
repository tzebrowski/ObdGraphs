package org.obd.graphs

import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.pid.PidDefinition
import org.obd.metrics.pid.ValueType

private const val NO_DATA = "No data"

fun ObdMetric.format(castToInt: Boolean = false, precision: Int = 2): String = format(
    input = this.value,
    pid = null, precision = precision, castToInt = castToInt
)

fun ObdMetric.valueToFloat(): Float =
    if (this.value == null) {
        if (this.command.pid.min == null) 0f else this.command.pid.min.toFloat()
    } else {
        if (this.value is Number && !(this.value as Number).toDouble().isNaN()) {
            this.valueToDouble().toFloat()
        } else {
            0f
        }
    }

fun ObdMetric.valueToNumber(): Number? =
    if (this.value == null) {
        null
    } else {
        if (value is Number) {
            value as Number
        } else {
            null
        }
    }


fun Number.format(pid: PidDefinition, precision: Int = 2, castToInt: Boolean = false): String = format(
    input = this,
    pid = pid, precision = precision, castToInt = castToInt
)

private fun format(input: Any?, pid: PidDefinition? = null, precision: Int = 2, castToInt: Boolean = false): String =

    if (input == null) {
        NO_DATA
    } else {
        if (input is Number) {

            val number = if (pid == null || pid.type == null) {
                if (input is Number) {
                    if (castToInt) input.toInt()
                    else if (input is Double) (input as Number).toDouble().round(precision)
                    else input
                } else {
                    input
                }
            } else {
                pid.type.let {
                    when (pid.type) {
                        ValueType.DOUBLE -> input.toDouble().round(precision)
                        ValueType.INT -> input.toInt()
                        ValueType.SHORT -> input.toInt()
                        else -> input.toDouble().round(1)
                    }
                }
            }

            if (number.toDouble().isNaN()) {
                NO_DATA
            } else {
                number.toString()
            }
        } else {
            input.toString()
        }
    }