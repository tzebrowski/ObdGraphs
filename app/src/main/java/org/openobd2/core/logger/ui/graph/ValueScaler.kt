package org.openobd2.core.logger.ui.graph


import org.obd.metrics.ObdMetric
import org.obd.metrics.pid.PidDefinition


private const val NEW_RANGE_MIN_VAL = 0f
private const val NEW_RANGE_MAX_VAL = 3500f

class ValueScaler {

    fun scaleToPidRange(
        pid: PidDefinition,
        value: Float
    ): Float {
        return scaleToNewRange(value,NEW_RANGE_MIN_VAL, NEW_RANGE_MAX_VAL, pid.min.toFloat(), pid.max.toFloat())
    }

    fun scaleToNewRange(
        obdMetric: ObdMetric
    ): Float {
        return scaleToNewRange(obdMetric.valueToDouble().toFloat(),obdMetric.command.pid.min.toFloat(),
                obdMetric.command.pid.max.toFloat(), NEW_RANGE_MIN_VAL, NEW_RANGE_MAX_VAL)
    }

    fun scaleToNewRange(
        currentValue: Float,
        currentMin: Float,
        currentMax: Float,
        targetMin: Float,
        targetMax: Float
    ): Float {
        return (currentValue - currentMin) * (targetMax - targetMin) / (currentMax - currentMin) + targetMin
    }
}