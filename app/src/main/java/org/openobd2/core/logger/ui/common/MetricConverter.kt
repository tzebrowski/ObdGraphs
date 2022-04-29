package org.openobd2.core.logger.ui.common

import org.obd.metrics.ObdMetric
import org.obd.metrics.pid.PidDefinition
import org.openobd2.core.logger.ui.dashboard.round


fun ObdMetric.convert(value: Double): Number {
    if (value.isNaN()) {
        return 0.0
    }
    return if (command.pid.type == null) value.round(2) else
        command.pid.type.let {
            return when (command.pid.type) {
                PidDefinition.ValueType.DOUBLE -> value.round(2)
                PidDefinition.ValueType.INT -> value.toInt()
                PidDefinition.ValueType.SHORT -> value.toInt()
                else -> value.round(1)
            }
        }
}