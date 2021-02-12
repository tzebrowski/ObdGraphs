package org.openobd2.core.logger.ui.preferences

import org.obd.metrics.ObdMetric

class DashPreferences(id: String, position: Int) {
    var id: String = id
    var position: Int = position

    companion object {
        class Serializer : RecycleViewPreferences<DashPreferences>("prefs.dash.pids.settings") {
            override fun metricsMapper(): MetricsMapper<DashPreferences> {
                return object : MetricsMapper<DashPreferences> {
                    override fun map(m: ObdMetric, index: Int): DashPreferences {
                        return DashPreferences(m.command.pid.id.toString(), index)
                    }
                }
            }
            override fun genericType(): Class<DashPreferences> {
                return DashPreferences::class.java
            }
        }

        @JvmStatic
        val SERIALIZER: Serializer = Serializer()
    }
}