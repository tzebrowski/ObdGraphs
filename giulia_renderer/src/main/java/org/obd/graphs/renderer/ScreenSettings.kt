/**
 * Copyright 2019-2023, Tomasz Żebrowski
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
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

    fun isBreakLabelTextEnabled (): Boolean = true

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
    fun getMaxColumns(): Int
    fun isHistoryEnabled(): Boolean
    fun isFpsCounterEnabled(): Boolean
    fun getSurfaceFrameRate(): Int
    fun getFontSize(): Int
    fun getCurrentVirtualScreen(): String = ""
    fun applyVirtualScreen(key: String) {}

    fun isStatusPanelEnabled(): Boolean = true

    fun getMaxAllowedItemsInColumn(): Int = 5
}