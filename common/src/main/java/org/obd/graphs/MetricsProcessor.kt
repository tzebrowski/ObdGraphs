package org.obd.graphs

import org.obd.metrics.api.model.Lifecycle
import org.obd.metrics.api.model.ObdMetric

interface MetricsProcessor : Lifecycle {
    fun postValue(obdMetric: ObdMetric)
}
