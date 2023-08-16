package org.obd.graphs.renderer

import android.graphics.Color
import org.obd.graphs.ui.common.COLOR_CARDINAL

data class ColorTheme(val dividerColor: Int = Color.WHITE,
                      val progressColor: Int = COLOR_CARDINAL,
                      val statusConnectedColor: Int = Color.GREEN,
                      val statusDisconnectedColor: Int = Color.YELLOW,
                      val currentValueColor: Int = Color.WHITE,
                      val currentValueInAlertColor: Int = Color.RED,
                      val currentProfileColor: Int = Color.YELLOW,
                      val actionsBtnConnectColor: Int = Color.GREEN,
                      val actionsBtnDisconnectColor: Int = Color.BLUE,
                      val actionsBtnVirtualScreensColor: Int = Color.YELLOW)

interface ScreenSettings {

    fun isAlertLegendEnabled (): Boolean = false
    fun isAlertingEnabled (): Boolean = false

    fun colorTheme(): ColorTheme = ColorTheme()

    fun applyVirtualScreen1() {}
    fun applyVirtualScreen2() {}
    fun applyVirtualScreen3() {}
    fun applyVirtualScreen4() {}
    fun getSelectedPIDs(): Set<Long> = emptySet()
    fun getMaxItemsInColumn(): Int
    fun isHistoryEnabled(): Boolean
    fun isFpsCounterEnabled(): Boolean
    fun getSurfaceFrameRate(): Int
    fun getMaxFontSize(): Int
    fun getCurrentVirtualScreen(): String = ""
    fun applyVirtualScreen(key: String) {}

    fun isStatusPanelEnabled(): Boolean = true

    fun getMaxAllowedItemsInColumn(): Int = 5
}