package org.openobd2.core.logger.ui.preferences

import org.obd.metrics.ObdMetric

class GaugePreferences(id: String, position: Int) {
    var id: String = id
    var position: Int = position

    companion object {
        class Serializer : RecycleViewPreferences<GaugePreferences>("prefs.gauge.pids.settings") {
            override fun metricsMapper(): MetricsMapper<GaugePreferences> {
                return object : MetricsMapper<GaugePreferences> {
                    override fun map(m: ObdMetric, index: Int): GaugePreferences {
                        return GaugePreferences(m.command.pid.id.toString(), index)
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