package org.obd.graphs

import org.obd.graphs.ui.preferences.Prefs
import org.obd.graphs.ui.preferences.isEnabled

data class PowerPreferences(
    val connectOnPower: Boolean,
    val screenOnOff: Boolean,
    val switchNetworkOffOn: Boolean,
    val startDataLoggingAfter: Long
)

fun getPowerPreferences(): PowerPreferences {

    val btOnOff = Prefs.isEnabled("pref.adapter.power.switch_network_on_off")
    val screenOnOff = Prefs.isEnabled("pref.adapter.power.screen_off")
    val connectOnPower = Prefs.isEnabled("pref.adapter.power.connect_adapter")
    val startDataLoggingAfter =
        Prefs.getString("pref.adapter.power.start_data_logging.after", "10")!!.toLong()
    return PowerPreferences(
        connectOnPower,
        screenOnOff,
        btOnOff,
        startDataLoggingAfter
    )
}