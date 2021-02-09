package org.openobd2.core.logger.ui.preferences

import org.obd.metrics.ObdMetric

class GaugePreferences(query: String, position: Int) {
    var query: String = query
    var position: Int = position

    companion object {
        class Serializer : RecycleViewPreferences<GaugePreferences>("prefs.gauge.pids.settings") {
            override fun metricsMapper(): MetricsMapper<GaugePreferences> {
                return object : MetricsMapper<GaugePreferences> {
                    override fun map(m: ObdMetric, index: Int): GaugePreferences {
                        return GaugePreferences(m.command.query, index)
                    }
                }
            }

            override fun genericType(): Class<GaugePreferences> {
               return GaugePreferences::class.java
            }
        }

        @JvmStatic
        val SERIALIZER: Serializer = Serializer()
    }
}