package org.openobd2.core.logger.ui.preferences

import org.obd.metrics.ObdMetric

class DashPreferences(var id: Long, var position: Int) {

    companion object {
        class Serializer : RecycleViewPreferences<DashPreferences>("prefs.dash.pids.settings") {
            override fun metricsMapper(): MetricsMapper<DashPreferences> {
                return object : MetricsMapper<DashPreferences> {
                    override fun map(m: ObdMetric, index: Int): DashPreferences {
                        return DashPreferences(m.command.pid.id, index)
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