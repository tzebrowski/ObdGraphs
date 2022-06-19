package org.obd.graphs.ui.preferences

import org.obd.metrics.ObdMetric

private const val PREFS_ID = "prefs.dash.pids.settings"

class DashboardPreferences(id: Long, position: Int) : RecycleViewPreference(id, position) {
    class Serializer : RecycleViewPreferences<DashboardPreferences>(PREFS_ID) {
        override fun metricsMapper(): MetricsMapper<DashboardPreferences> {
            return object : MetricsMapper<DashboardPreferences> {
                override fun map(m: ObdMetric, index: Int): DashboardPreferences {
                    return DashboardPreferences(m.command.pid.id, index)
                }
            }
        }

        override fun genericType(): Class<DashboardPreferences> {
            return DashboardPreferences::class.java
        }
    }
}