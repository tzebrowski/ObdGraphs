package org.obd.graphs.renderer

import android.graphics.Color
import org.obd.graphs.ui.common.COLOR_CARDINAL
import org.obd.graphs.ui.common.COLOR_DYNAMIC_SELECTOR_SPORT

enum class DynamicSelectorMode {
    NORMAL,ECO, SPORT, RACE
}

data class ColorTheme(val dividerColor: Int = Color.WHITE,
                      var progressColor: Int = COLOR_CARDINAL,
                      val statusConnectingColor: Int =  Color.YELLOW,
                      val statusConnectedColor: Int = Color.GREEN,
                      val statusDisconnectedColor: Int = COLOR_DYNAMIC_SELECTOR_SPORT,
                      val currentValueColor: Int = Color.WHITE,
                      val currentValueInAlertColor: Int = COLOR_DYNAMIC_SELECTOR_SPORT,
                      val currentProfileColor: Int = Color.WHITE,
                      val actionsBtnConnectColor: Int = Color.GREEN,
                      val actionsBtnDisconnectColor: Int = Color.RED,
                      val actionsBtnVirtualScreensColor: Int = Color.WHITE)

interface ScreenSettings {

    fun getBackgroundColor(): Int = Color.BLACK

    fun dynamicSelectorChangedEvent(mode: DynamicSelectorMode){}


    fun isBackgroundDrawingEnabled (): Boolean = true

    fun isDynamicSelectorThemeEnabled (): Boolean = false

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