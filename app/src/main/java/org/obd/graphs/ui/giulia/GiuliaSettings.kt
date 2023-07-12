package org.obd.graphs.ui.giulia

import org.obd.graphs.bl.datalogger.dataLoggerPreferences
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getS
import org.obd.graphs.renderer.ScreenSettings

class GiuliaSettings: ScreenSettings {
    override fun applyVirtualScreen1() {
    }

    override fun applyVirtualScreen2() {
    }

    override fun applyVirtualScreen3() {
    }

    override fun applyVirtualScreen4() {
    }

    override fun getSelectedPIDs(): Set<Long> {
        return dataLoggerPreferences.getPIDsToQuery()
    }

    override fun maxItemsInColumn(): Int = Prefs.getS("pref.giulia.max_pids_in_column","5").toInt()
    override fun isHistoryEnabled(): Boolean  = true
    override fun isFpsCounterEnabled(): Boolean  = true
    override fun getSurfaceFrameRate(): Int  = Prefs.getS("pref.giulia.fps","5").toInt()
    override fun maxFontSize(): Int = Prefs.getS("pref.giulia.screen_font_size","32").toInt()
    override fun isStatusPanelEnabled(): Boolean = false

    override fun getMaxAllowedItemsInColumn(): Int  = 14

    override fun getCurrentVirtualScreen(): String {
        return ""
    }

    override fun applyVirtualScreen(key: String) {
    }
}