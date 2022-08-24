package org.obd.graphs

import org.obd.metrics.api.model.ObdMetric

const val NEW_RANGE_MIN_VAL = 0f
const val NEW_RANGE_MAX_VAL = 3500f

class ValueScaler {
    fun scaleToNewRange(
        obdMetric: ObdMetric
    ): Float {
        return scaleToNewRange(
            obdMetric.valueToDouble().toFloat(), obdMetric.command.pid.min.toFloat(),
            obdMetric.command.pid.max.toFloat(), NEW_RANGE_MIN_VAL, NEW_RANGE_MAX_VAL
        )
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