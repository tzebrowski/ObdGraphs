package org.obd.graphs.ui.giulia

import org.obd.graphs.bl.datalogger.dataLoggerPreferences
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getS
import org.obd.graphs.renderer.ScreenSettings

class GiuliaSettings: ScreenSettings {
    override fun getSelectedPIDs(): Set<Long> {
        return dataLoggerPreferences.getPIDsToQuery()
    }

    override fun getMaxColumns(): Int = giuliaVirtualScreen.getMaxItemsInColumn()
    override fun isHistoryEnabled(): Boolean  = true
    override fun isFpsCounterEnabled(): Boolean  = true
    override fun getSurfaceFrameRate(): Int  = Prefs.getS("pref.giulia.fps","5").toInt()
    override fun getFontSize(): Int = giuliaVirtualScreen.getFontSize()
    override fun isStatusPanelEnabled(): Boolean = false

    override fun getMaxAllowedItemsInColumn(): Int  = 8
}