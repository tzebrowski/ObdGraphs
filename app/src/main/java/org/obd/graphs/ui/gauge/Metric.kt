package org.obd.graphs.ui.gauge

import org.obd.metrics.api.model.ObdMetric

internal data class CarMetric(
    var value: ObdMetric,
    var enabled: Boolean = true
) {

    companion object {
        fun newInstance(it: ObdMetric) = CarMetric(it, enabled = true)
    }
}