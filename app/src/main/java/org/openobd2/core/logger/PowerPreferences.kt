package org.openobd2.core.logger

import org.openobd2.core.logger.ui.preferences.Prefs
import org.openobd2.core.logger.ui.preferences.isEnabled

data class PowerPreferences(
    val connectOnPower: Boolean,
    val screenOnOff: Boolean,
    val switchNetworkOffOn: Boolean
)

private const val ADAPTER_CONNECT_PREFERENCE_KEY = "pref.adapter.power.connect_adapter"
private const val SCREEN_ON_OFF_PREFERENCE_KEY = "pref.adapter.power.screen_off"
private const val BT_ON_OFF_PREFERENCE_KEY = "pref.adapter.power.switch_network_on_off"

fun getPowerPreferences(): PowerPreferences {

    val btOnOff = Prefs.isEnabled(BT_ON_OFF_PREFERENCE_KEY)
    val screenOnOff = Prefs.isEnabled(SCREEN_ON_OFF_PREFERENCE_KEY)
    val connectOnPower = Prefs.isEnabled(ADAPTER_CONNECT_PREFERENCE_KEY)
    return PowerPreferences(connectOnPower, screenOnOff, btOnOff)
}