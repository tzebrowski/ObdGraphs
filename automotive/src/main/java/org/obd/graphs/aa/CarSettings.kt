/**
 * Copyright 2019-2024, Tomasz Żebrowski
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
import android.util.Log
import androidx.car.app.CarContext
import org.obd.graphs.PREF_DYNAMIC_SELECTOR_ENABLED
import org.obd.graphs.ViewPreferencesSerializer
import org.obd.graphs.preferences.*
import org.obd.graphs.renderer.*
import org.obd.graphs.runAsync
import org.obd.graphs.ui.common.COLOR_DYNAMIC_SELECTOR_ECO
import org.obd.graphs.ui.common.COLOR_DYNAMIC_SELECTOR_NORMAL
import org.obd.graphs.ui.common.COLOR_DYNAMIC_SELECTOR_RACE
import org.obd.graphs.ui.common.COLOR_DYNAMIC_SELECTOR_SPORT

private const val LAST_USER_SCREEN = "pref.aa.screen.last_used"

private const val PREF_PIDS_HISTORY_ENABLED = "pref.aa.pids.history.enabled"

private const val PREF_THEME_IN_ALLERT_VALUE_COLOR = "pref.aa.theme.inAlertValueColor"

private const val BACKGROUND_ENABLED = "pref.aa.theme.background.enabled"

private const val PREF_ALERT_LEGEND_ENABLED = "pref.aa.alerting.legend.enabled"
private const val PREF_ALERTING_ENABLED = "pref.aa.alerting.enabled"
private const val PREF_THEME_PROGRESS_BAR_COLOR = "pref.aa.theme.progressColor"
private const val PREF_THEME_DIVIDER_COLOR = "pref.aa.theme.dividerColor"
private const val PREF_THEME_CURR_VALUE_COLOR = "pref.aa.theme.currentValueColor"
private const val PREF_THEME_VIRTUAL_SCREEN_COLOR = "pref.aa.theme.btn.virtual-screen.color"

private const val PREF_SURFACE_FRAME_RATE = "pref.aa.surface.fps"
private const val PREF_STATUS_FPS_VISIBLE = "pref.aa.status.fps.enabled"

private const val DEFAULT_ITEMS_IN_COLUMN = "1"
private const val DEFAULT_FONT_SIZE = "32"
private const val DEFAULT_FRAME_RATE = "5"


enum class ScreenTemplateType {
    NAV, IOT
}

private data class DataPrefs(val virtualScreenPrefixKey:String, val currentVirtualScreenKey: String, val selectedPIDsKey: String)

private const val LOG_TAG = "CAR_SETTINGS"

class CarSettings(private val carContext: CarContext) : ScreenSettings {
    private var itemsSortOrder: Map<Long, Int>? = emptyMap()
    private val dragRacingScreenSettings = DragRacingScreenSettings()
    private val colorTheme = ColorTheme()

    private val gaugeRendererSettings = object: GaugeRendererSettings(){
        val dataPrefs = DataPrefs(
            virtualScreenPrefixKey="pref.aa.gauge.pids.profile_",
            currentVirtualScreenKey = "pref.aa.gauge.pids.vs.current",
            selectedPIDsKey = "pref.aa.gauge.pids.selected")

        override fun setVirtualScreen(id: Int) = setVirtualScreenById(dataPrefs=dataPrefs, screenId=id)
        override fun getVirtualScreen(): Int =  getCurrentVirtualScreenId(dataPrefs)
        override fun isPIDsSortOrderEnabled(): Boolean = Prefs.getBoolean("pref.aa.virtual_screens.sort_order.enabled", false)
        override fun getPIDsSortOrder(): Map<Long, Int>? = if (isPIDsSortOrderEnabled()) itemsSortOrder else null
    }

    private val giuliaRendererSettings = object:  GiuliaRendererSettings(){
        val dataPrefs = DataPrefs(
            virtualScreenPrefixKey="pref.aa.pids.profile_",
            currentVirtualScreenKey = "pref.aa.pids.vs.current",
            selectedPIDsKey = "pref.aa.pids.selected")

        override fun setVirtualScreen(id: Int) = setVirtualScreenById(screenId=id, dataPrefs=dataPrefs)
        override fun getVirtualScreen(): Int =  getCurrentVirtualScreenId(dataPrefs)
        override fun isPIDsSortOrderEnabled(): Boolean = Prefs.getBoolean("pref.aa.virtual_screens.sort_order.enabled", false)
        override fun getPIDsSortOrder(): Map<Long, Int>? = if (isPIDsSortOrderEnabled()) itemsSortOrder else null
    }

    private val tripInfoScreenSettings = TripInfoScreenSettings()
    private val routinesScreenSettings = RoutinesScreenSettings()

    init {
        copyGiuliaSettings()
    }

    override fun handleProfileChanged() {
        copyGiuliaSettings()
    }

    override fun getDragRacingScreenSettings(): DragRacingScreenSettings = dragRacingScreenSettings.apply {
        metricsFrequencyReadEnabled = Prefs.getBoolean("pref.aa.drag_race.debug.display_frequency", true)
        vehicleSpeedDisplayDebugEnabled = Prefs.getBoolean("pref.aa.drag_race.debug.vehicle_speed_measurement", false)
        displayMetricsEnabled = Prefs.getBoolean("pref.aa.drag_race.vehicle_speed.enabled", true)
        shiftLightsEnabled = Prefs.getBoolean("pref.aa.drag_race.shift_lights.enabled", false)
        shiftLightsRevThreshold = Prefs.getS("pref.aa.drag_race.shift_lights.rev_value", "5000").toInt()
        contextInfoEnabled = Prefs.getBoolean("pref.profile.2_0_GME_extension.enabled", false)
        fontSize = Prefs.getS("pref.aa.drag_race.font_size", "30").toInt()
    }

    override fun getRoutinesScreenSettings(): RoutinesScreenSettings  = routinesScreenSettings.apply {
        viewEnabled = Prefs.getBoolean("pref.aa.routines.enabled", true)
    }

    override fun getTripInfoScreenSettings(): TripInfoScreenSettings = tripInfoScreenSettings.apply {
        fontSize = Prefs.getS("pref.aa.trip_info.font_size", "24").toInt()
        viewEnabled = Prefs.getBoolean("pref.aa.trip_info.enabled", true)
    }

    override fun getColorTheme(): ColorTheme = colorTheme.apply {
        progressColor = Prefs.getInt(PREF_THEME_PROGRESS_BAR_COLOR, COLOR_DYNAMIC_SELECTOR_SPORT)
        dividerColor = Prefs.getInt(PREF_THEME_DIVIDER_COLOR, Color.WHITE)
        currentValueColor = Prefs.getInt(PREF_THEME_CURR_VALUE_COLOR, Color.WHITE)
        currentValueInAlertColor = Prefs.getInt(PREF_THEME_IN_ALLERT_VALUE_COLOR, COLOR_DYNAMIC_SELECTOR_SPORT)
        actionsBtnVirtualScreensColor = Prefs.getInt(PREF_THEME_VIRTUAL_SCREEN_COLOR, Color.WHITE)
    }

    override fun dynamicSelectorChangedEvent(mode: DynamicSelectorMode) {
        runAsync {
            if (isDynamicSelectorThemeEnabled()) {
                when (mode) {
                    DynamicSelectorMode.NORMAL -> Prefs.updateInt(PREF_THEME_PROGRESS_BAR_COLOR, COLOR_DYNAMIC_SELECTOR_NORMAL)
                    DynamicSelectorMode.SPORT -> Prefs.updateInt(PREF_THEME_PROGRESS_BAR_COLOR, COLOR_DYNAMIC_SELECTOR_SPORT)
                    DynamicSelectorMode.ECO -> Prefs.updateInt(PREF_THEME_PROGRESS_BAR_COLOR, COLOR_DYNAMIC_SELECTOR_ECO)
                    DynamicSelectorMode.RACE -> Prefs.updateInt(PREF_THEME_PROGRESS_BAR_COLOR, COLOR_DYNAMIC_SELECTOR_RACE)
                }
            }
        }
    }

    fun isAutomaticConnectEnabled(): Boolean = Prefs.getBoolean("pref.aa.connection.auto.enabled", false)

    fun isLoadLastVisitedScreenEnabled(): Boolean = Prefs.getBoolean("pref.aa.screen.load_last_visited.enabled", false)

    fun getLastVisitedScreen(): Identity = SurfaceRendererType.fromInt(Prefs.getInt(LAST_USER_SCREEN, 0))

    fun setLastVisitedScreen(identity: Identity){
        if (identity is SurfaceRendererType) {
            Prefs.updateInt(LAST_USER_SCREEN, identity.id())
        }
    }

    override fun getGaugeRendererSetting(): GaugeRendererSettings = gaugeRendererSettings.apply {
        gaugeProgressBarType =  GaugeProgressBarType.valueOf(Prefs.getS("pref.aa.virtual_screens.screen.gauge.progress_type", GaugeProgressBarType.LONG.name))
        topOffset = Prefs.getS("pref.aa.virtual_screens.gauge.top_offset.${getCurrentVirtualScreenId(dataPrefs = this.dataPrefs)}","0").toInt()
        selectedPIDs = Prefs.getStringSet(dataPrefs.selectedPIDsKey).map { s -> s.toLong() }.toSet()
    }

    override fun getGiuliaRendererSetting(): GiuliaRendererSettings = giuliaRendererSettings.apply {
        selectedPIDs = Prefs.getStringSet(dataPrefs.selectedPIDsKey).map { s -> s.toLong() }.toSet()
    }

    override fun getMaxItems(): Int  =  Prefs.getS("pref.aa.virtual_screens.screen.max_items","6").toInt()

    override fun isStatusPanelEnabled(): Boolean = Prefs.getBoolean("pref.aa.virtual_screens.status_panel.enabled", true)

    override fun isScaleEnabled(): Boolean = Prefs.getBoolean("pref.aa.virtual_screens.scale.enabled", true)

    override fun getHeightPixels(): Int = carContext.resources.displayMetrics.heightPixels
    override fun getWidthPixels(): Int = carContext.resources.displayMetrics.widthPixels

    override fun getMaxColumns(): Int =
        Prefs.getS("pref.aa.max_pids_in_column.${getCurrentVirtualScreenId(giuliaRendererSettings.dataPrefs)}", DEFAULT_ITEMS_IN_COLUMN).toInt()

    override fun getBackgroundColor(): Int = if (carContext.isDarkMode) Color.BLACK else Color.BLACK

    override fun isBackgroundDrawingEnabled(): Boolean = Prefs.getBoolean(BACKGROUND_ENABLED, true)

    override fun isDynamicSelectorThemeEnabled(): Boolean = Prefs.getBoolean(PREF_DYNAMIC_SELECTOR_ENABLED, false)

    override fun isAlertingEnabled(): Boolean = Prefs.getBoolean(PREF_ALERTING_ENABLED, false)

    override fun isAlertLegendEnabled(): Boolean = Prefs.getBoolean(PREF_ALERT_LEGEND_ENABLED, false)

    override fun isStatisticsEnabled(): Boolean = Prefs.getBoolean(PREF_PIDS_HISTORY_ENABLED, true)

    override fun isFpsCounterEnabled(): Boolean = Prefs.getBoolean(PREF_STATUS_FPS_VISIBLE, false)

    override fun getSurfaceFrameRate(): Int = Prefs.getS(PREF_SURFACE_FRAME_RATE, DEFAULT_FRAME_RATE).toInt()
    override fun getFontSize(): Int = Prefs.getS("pref.aa.screen_font_size.${getCurrentVirtualScreenId(giuliaRendererSettings.dataPrefs)}", DEFAULT_FONT_SIZE).toInt()

    override fun isBreakLabelTextEnabled(): Boolean = Prefs.getBoolean("pref.aa.break_label.${getCurrentVirtualScreenId(giuliaRendererSettings.dataPrefs)}", true)

    private fun setVirtualScreenById( screenId: Int, dataPrefs: DataPrefs) {
        val value = "${dataPrefs.virtualScreenPrefixKey}${screenId}"
        Prefs.updateString(dataPrefs.currentVirtualScreenKey, value)
        Prefs.updateStringSet(dataPrefs.selectedPIDsKey, Prefs.getStringSet(value).toList())
        itemsSortOrder = loadItemsSortOrder(value)
    }

    fun initItemsSortOrder() {
        itemsSortOrder = loadItemsSortOrder(getCurrentVirtualScreen(giuliaRendererSettings.dataPrefs))
    }

    fun isVirtualScreenEnabled(id: Int): Boolean = Prefs.getBoolean("pref.aa.virtual_screens.enabled.$id", true)

    fun getScreenTemplate(): ScreenTemplateType = ScreenTemplateType.NAV

    private fun getCurrentVirtualScreenId(dataPrefs: DataPrefs): Int = getCurrentVirtualScreen(dataPrefs).last().digitToInt()
    private fun getCurrentVirtualScreen(dataPrefs: DataPrefs): String = Prefs.getS(dataPrefs.currentVirtualScreenKey, "pref.aa.pids.profile_1")

    private fun loadItemsSortOrder(key: String) = ViewPreferencesSerializer("${key}.view.settings").getItemsSortOrder()

    private fun copyGiuliaSettings() {
        try{
            val gauge = gaugeRendererSettings.dataPrefs
            if (!Prefs.contains(gauge.selectedPIDsKey)) {
                Log.i(LOG_TAG, "No Gauge settings found. Copy Giulia Settings...")
                val giulia = giuliaRendererSettings.dataPrefs
                val list = Prefs.getStringSet(giulia.selectedPIDsKey).toList()

                (1..4).forEach {
                    val list = Prefs.getStringSet("${giulia.virtualScreenPrefixKey}$it").toList()
                    Log.i(LOG_TAG, "Giulia virtual screen $it=$list")
                    Prefs.updateStringSet("${gauge.virtualScreenPrefixKey}$it", list)
                }

                Log.i(LOG_TAG, "Updating Gauge Selected PIDs $list")
                Prefs.updateStringSet(gauge.selectedPIDsKey, list)
            }
        }catch (e: Exception){
            Log.e(LOG_TAG, "Failed to set copy Giulia settings",e)
        }

    }
}