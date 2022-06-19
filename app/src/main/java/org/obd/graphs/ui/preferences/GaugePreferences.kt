package org.obd.graphs.ui.preferences

import org.obd.metrics.ObdMetric

private const val PREFS_ID = "prefs.gauge.pids.settings"

class GaugePreferences(id: Long, position: Int) : RecycleViewPreference(id, position) {

    class Serializer : RecycleViewPreferences<GaugePreferences>(PREFS_ID) {
        override fun metricsMapper(): MetricsMapper<GaugePreferences> {
            return object : MetricsMapper<GaugePreferences> {
                override fun map(m: ObdMetric, index: Int): GaugePreferences {
                    return GaugePreferences(m.command.pid.id, index)
                }
            }
        }

        override fun genericType(): Class<GaugePreferences> {
            return GaugePreferences::class.java
        }
    }
}