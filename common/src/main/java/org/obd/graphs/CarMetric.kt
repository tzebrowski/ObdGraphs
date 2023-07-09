package org.obd.graphs

import org.obd.metrics.api.model.ObdMetric


data class CarMetric(
    var value: ObdMetric,
    var enabled: Boolean = true
) {
    companion object {
        fun newInstance(it: ObdMetric) = CarMetric(it, enabled = true)
    }
}