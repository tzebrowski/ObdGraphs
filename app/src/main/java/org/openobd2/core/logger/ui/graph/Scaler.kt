package org.openobd2.core.logger.ui.graph

import org.obd.metrics.ObdMetric
import org.obd.metrics.pid.PidDefinition

class Scaler {

    private val NEW_RANGE_MIN_VAL = 0f
    private val NEW_RANGE_MAX_VAL = 2000f

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

    private fun scaleToNewRange(
        currentValue: Float,
        currentMin: Float,
        currentMax: Float,
        targetMin: Float,
        targetMax: Float
    ): Float {
        return (currentValue - currentMin) * (targetMax - targetMin) / (currentMax - currentMin) + targetMin
    }
}