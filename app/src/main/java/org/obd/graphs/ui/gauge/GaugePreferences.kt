package org.obd.graphs.ui.gauge

import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.isEnabled

data class GaugePreferences(val commandRateEnabled: Boolean)

fun getGaugePreferences(): GaugePreferences {
    val commandRateEnabled = Prefs.isEnabled("pref.gauge_display_command_rate")

    return GaugePreferences(commandRateEnabled)
}