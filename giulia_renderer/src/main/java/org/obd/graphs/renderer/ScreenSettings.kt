package org.obd.graphs.renderer

interface ScreenSettings {
    fun applyVirtualScreen1()
    fun applyVirtualScreen2()
    fun applyVirtualScreen3()
    fun applyVirtualScreen4()
    fun getSelectedPIDs(): Set<Long>
    fun maxItemsInColumn(): Int
    fun isHistoryEnabled(): Boolean
    fun isFpsCounterEnabled(): Boolean
    fun getSurfaceFrameRate(): Int
    fun maxFontSize(): Int
    fun getCurrentVirtualScreen(): String
    fun applyVirtualScreen(key: String)

    fun isStatusPanelEnabled (): Boolean = true

    fun getMaxAllowedItemsInColumn(): Int = 5
}