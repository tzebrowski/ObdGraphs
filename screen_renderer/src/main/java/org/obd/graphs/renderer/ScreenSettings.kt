 /**
 * Copyright 2019-2025, Tomasz Żebrowski
 *
 * <p>Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.obd.graphs.renderer

import android.graphics.Color
import org.obd.graphs.getContext
import org.obd.graphs.ui.common.COLOR_CARDINAL
import org.obd.graphs.ui.common.COLOR_DYNAMIC_SELECTOR_SPORT

const val DEFAULT_FONT_SIZE = "32"

enum class GaugeProgressBarType {
    LONG, SHORT
}

enum class DynamicSelectorMode {
    NORMAL, ECO, SPORT, RACE
}

data class ColorTheme(
    var dividerColor: Int = Color.WHITE,
    var progressColor: Int = COLOR_CARDINAL,
    var statusConnectingColor: Int = Color.YELLOW,
    var statusConnectedColor: Int = Color.GREEN,
    var statusDisconnectedColor: Int = COLOR_DYNAMIC_SELECTOR_SPORT,
    var currentValueColor: Int = Color.WHITE,
    var currentValueInAlertColor: Int = COLOR_DYNAMIC_SELECTOR_SPORT,
    var currentProfileColor: Int = Color.WHITE,
    var actionsBtnConnectColor: Int = Color.GREEN,
    var actionsBtnDisconnectColor: Int = Color.RED,
    var actionsBtnVirtualScreensColor: Int = Color.WHITE
)


open class GaugeRendererSettings (
    var gaugeProgressBarType: GaugeProgressBarType = GaugeProgressBarType.LONG,
    var topOffset:Int = 0,
    var selectedPIDs: Set<Long> = emptySet()
){

    open fun getVirtualScreen(): Int = 0
    open fun isPIDsSortOrderEnabled(): Boolean = false
    open  fun getPIDsSortOrder(): Map<Long, Int>? = emptyMap()

    open fun setVirtualScreen(id: Int) {}
    open fun getFontSize(): Int =  DEFAULT_FONT_SIZE.toInt()
}

open class GiuliaRendererSettings (var selectedPIDs: Set<Long>  = emptySet()){

    open fun isPIDsSortOrderEnabled(): Boolean = false
    open  fun getPIDsSortOrder(): Map<Long, Int>? = emptyMap()

    open fun getVirtualScreen(): Int = 0

    open fun setVirtualScreen(id: Int) {}
    open fun getFontSize(): Int =  DEFAULT_FONT_SIZE.toInt()
}


data class DragRacingScreenSettings(
    var shiftLightsRevThreshold: Int = 5000,
    var shiftLightsEnabled: Boolean = true,
    var displayMetricsEnabled: Boolean = true,
    var metricsFrequencyReadEnabled: Boolean = true,
    var vehicleSpeedDisplayDebugEnabled: Boolean = true,
    var contextInfoEnabled: Boolean = false,
    var fontSize: Int = 32,
    var selectedPIDs: Set<Long>  = emptySet()
)


data class TripInfoScreenSettings(
    var fontSize: Int = 24,
    var viewEnabled: Boolean = true
)

data class DynamicScreenSettings(
    var fontSize: Int = 24,
    var viewEnabled: Boolean = true
)

data class RoutinesScreenSettings(
    var viewEnabled: Boolean = true
)

interface ScreenSettings {

    fun handleProfileChanged(){}

    fun getRoutinesScreenSettings(): RoutinesScreenSettings = RoutinesScreenSettings()

    fun getDragRacingScreenSettings(): DragRacingScreenSettings = DragRacingScreenSettings()

    fun getTripInfoScreenSettings(): TripInfoScreenSettings = TripInfoScreenSettings()

    fun getDynamicScreenSettings(): DynamicScreenSettings = DynamicScreenSettings()

    fun getMaxItems (): Int = 6

    fun getGaugeRendererSetting(): GaugeRendererSettings = GaugeRendererSettings()

    fun getGiuliaRendererSetting(): GiuliaRendererSettings = GiuliaRendererSettings()

    fun isScaleEnabled(): Boolean = true

    fun getHeightPixels(): Int = getContext()!!.resources.displayMetrics.heightPixels

    fun getWidthPixels(): Int = getContext()!!.resources.displayMetrics.widthPixels

    fun isProgressGradientEnabled(): Boolean = false

    fun getBackgroundColor(): Int = Color.BLACK

    fun dynamicSelectorChangedEvent(mode: DynamicSelectorMode) {}

    fun isBreakLabelTextEnabled(): Boolean = true

    fun isBackgroundDrawingEnabled(): Boolean = true

    fun isDynamicSelectorThemeEnabled(): Boolean = false

    fun isAlertLegendEnabled(): Boolean = false
    fun isAlertingEnabled(): Boolean = false

    fun getColorTheme(): ColorTheme = ColorTheme()

    fun getMaxColumns(): Int = 1
    fun isStatisticsEnabled(): Boolean
    fun isFpsCounterEnabled(): Boolean
    fun getSurfaceFrameRate(): Int

    fun isStatusPanelEnabled(): Boolean = true

    fun getMaxAllowedItemsInColumn(): Int = 5
}
