package org.openobd2.core.logger.ui.gauge

import org.openobd2.core.logger.ui.preferences.Prefs
import org.openobd2.core.logger.ui.preferences.isEnabled

data class GaugePreferences(val commandRateEnabled: Boolean)

fun getGaugePreferences(): GaugePreferences {
    val commandRateEnabled = Prefs.isEnabled("pref.gauge_display_command_rate")

    return GaugePreferences(commandRateEnabled)
}