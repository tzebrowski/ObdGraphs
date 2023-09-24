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
package org.obd.graphs.aa

import android.graphics.Color
import androidx.car.app.CarContext
import org.obd.graphs.renderer.ScreenSettings
import org.obd.graphs.preferences.*
import org.obd.graphs.renderer.ColorTheme
import org.obd.graphs.renderer.DynamicSelectorMode
import org.obd.graphs.ui.common.COLOR_DYNAMIC_SELECTOR_ECO
import org.obd.graphs.ui.common.COLOR_DYNAMIC_SELECTOR_NORMAL
import org.obd.graphs.ui.common.COLOR_DYNAMIC_SELECTOR_RACE
import org.obd.graphs.ui.common.COLOR_DYNAMIC_SELECTOR_SPORT


private const val PREF_PIDS_HISTORY_ENABLED= "pref.aa.pids.history.enabled"

private const val PREF_THEME_IN_ALLERT_VALUE_COLOR= "pref.aa.theme.inAlertValueColor"
private const val PREF_DYNAMIC_SELECTOR_ENABLED= "pref.aa.theme.dynamic-selector.enabled"

private const val BACKGROUND_ENABLED= "pref.aa.theme.background.enabled"

private const val PREF_ALERT_LEGEND_ENABLED= "pref.aa.alerting.legend.enabled"
private const val PREF_ALERTING_ENABLED= "pref.aa.alerting.enabled"
private const val PREF_THEME_PROGRESS_BAR_COLOR= "pref.aa.theme.progressColor"
private const val PREF_THEME_DIVIDER_COLOR= "pref.aa.theme.dividerColor"
private const val PREF_THEME_CURR_VALUE_COLOR= "pref.aa.theme.currentValueColor"
private const val PREF_THEME_VIRTUAL_SCREEN_COLOR= "pref.aa.theme.btn.virtual-screen.color"

private const val PREF_CURRENT_VIRTUAL_SCREEN = "pref.aa.pids.vs.current"
private const val PREF_SELECTED_PIDS = "pref.aa.pids.selected"
private const val PREF_SURFACE_FRAME_RATE = "pref.aa.surface.fps"
private const val PREF_STATUS_FPS_VISIBLE = "pref.aa.status.fps.enabled"

private const val DEFAULT_ITEMS_IN_COLUMN = "1"
private const val DEFAULT_FONT_SIZE = "32"
private const val DEFAULT_FRAME_RATE = "5"

const val VIRTUAL_SCREEN_1 = "pref.aa.pids.profile_1"
const val VIRTUAL_SCREEN_2 = "pref.aa.pids.profile_2"
const val VIRTUAL_SCREEN_3 = "pref.aa.pids.profile_3"
const val VIRTUAL_SCREEN_4 = "pref.aa.pids.profile_4"

internal class CarSettings(private val carContext: CarContext) : ScreenSettings {
    override fun colorTheme(): ColorTheme {
        return ColorTheme(
            progressColor =  Prefs.getInt(PREF_THEME_PROGRESS_BAR_COLOR, COLOR_DYNAMIC_SELECTOR_SPORT),
            dividerColor =  Prefs.getInt(PREF_THEME_DIVIDER_COLOR, Color.WHITE),
            currentValueColor =  Prefs.getInt(PREF_THEME_CURR_VALUE_COLOR, Color.WHITE),
            currentValueInAlertColor = Prefs.getInt(PREF_THEME_IN_ALLERT_VALUE_COLOR, COLOR_DYNAMIC_SELECTOR_SPORT),
            actionsBtnVirtualScreensColor = Prefs.getInt(PREF_THEME_VIRTUAL_SCREEN_COLOR, Color.WHITE)
        )
    }

    override fun dynamicSelectorChangedEvent(mode: DynamicSelectorMode) {
        if (isDynamicSelectorThemeEnabled()) {
            when (mode) {
                DynamicSelectorMode.NORMAL -> Prefs.updateInt(PREF_THEME_PROGRESS_BAR_COLOR, COLOR_DYNAMIC_SELECTOR_NORMAL)
                DynamicSelectorMode.SPORT -> Prefs.updateInt(PREF_THEME_PROGRESS_BAR_COLOR, COLOR_DYNAMIC_SELECTOR_SPORT)
                DynamicSelectorMode.ECO -> Prefs.updateInt(PREF_THEME_PROGRESS_BAR_COLOR, COLOR_DYNAMIC_SELECTOR_ECO)
                DynamicSelectorMode.RACE -> Prefs.updateInt(PREF_THEME_PROGRESS_BAR_COLOR, COLOR_DYNAMIC_SELECTOR_RACE)
            }
        }
    }

    override fun applyVirtualScreen1() = applyVirtualScreen(VIRTUAL_SCREEN_1)
    override fun applyVirtualScreen2() = applyVirtualScreen(VIRTUAL_SCREEN_2)
    override fun applyVirtualScreen3() = applyVirtualScreen(VIRTUAL_SCREEN_3)
    override fun applyVirtualScreen4() = applyVirtualScreen(VIRTUAL_SCREEN_4)

    override fun getSelectedPIDs() =
        Prefs.getStringSet(PREF_SELECTED_PIDS).map { s -> s.toLong() }.toSet()

    override fun getMaxColumns(): Int = Prefs.getS("pref.aa.max_pids_in_column.${getCurrentVirtualScreenId()}", DEFAULT_ITEMS_IN_COLUMN).toInt()

    override fun getBackgroundColor(): Int =  if (carContext.isDarkMode)  Color.BLACK else Color.BLACK

    override fun isBackgroundDrawingEnabled(): Boolean  = Prefs.getBoolean(BACKGROUND_ENABLED, true)

    override fun isDynamicSelectorThemeEnabled(): Boolean =  Prefs.getBoolean(PREF_DYNAMIC_SELECTOR_ENABLED, false)

    override fun isAlertingEnabled(): Boolean = Prefs.getBoolean(PREF_ALERTING_ENABLED, false)

    override fun isAlertLegendEnabled(): Boolean = Prefs.getBoolean(PREF_ALERT_LEGEND_ENABLED, false)

    override fun isHistoryEnabled(): Boolean = Prefs.getBoolean(PREF_PIDS_HISTORY_ENABLED, true)

    override fun isFpsCounterEnabled(): Boolean  = Prefs.getBoolean(PREF_STATUS_FPS_VISIBLE, false)

    override fun getSurfaceFrameRate(): Int = Prefs.getS(PREF_SURFACE_FRAME_RATE, DEFAULT_FRAME_RATE).toInt()
    override fun getFontSize(): Int   = Prefs.getS("pref.aa.screen_font_size.${getCurrentVirtualScreenId()}", DEFAULT_FONT_SIZE).toInt()

    override fun isBreakLabelTextEnabled(): Boolean  =  Prefs.getBoolean("pref.aa.break_label.${getCurrentVirtualScreenId()}", true)

    override fun getCurrentVirtualScreen(): String = Prefs.getS(PREF_CURRENT_VIRTUAL_SCREEN, "pref.aa.pids.profile_1")

    override fun applyVirtualScreen(key: String) {
        Prefs.updateString(PREF_CURRENT_VIRTUAL_SCREEN, key)
        Prefs.updateStringSet(PREF_SELECTED_PIDS, Prefs.getStringSet(key).toList())
    }

    fun isVirtualScreenEnabled(id: Int): Boolean =  Prefs.getBoolean("pref.aa.virtual_screens.enabled.$id", true)

    private fun getCurrentVirtualScreenId(): Int = getCurrentVirtualScreen().last().digitToInt()

}