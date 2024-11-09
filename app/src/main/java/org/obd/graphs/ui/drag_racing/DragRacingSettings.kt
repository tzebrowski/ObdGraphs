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
package org.obd.graphs.ui.drag_racing

import org.obd.graphs.bl.query.Query
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getS
import org.obd.graphs.renderer.DragRacingScreenSettings
import org.obd.graphs.renderer.ScreenSettings

class DragRacingSettings(private val query: Query): ScreenSettings {

    private val dragRacingScreenSettings = DragRacingScreenSettings()

    override fun getDragRacingScreenSettings(): DragRacingScreenSettings  = dragRacingScreenSettings.apply {
        metricsFrequencyReadEnabled = Prefs.getBoolean("pref.drag_racing.debug.display_frequency", true)
        vehicleSpeedDisplayDebugEnabled = Prefs.getBoolean("pref.drag_racing.debug.vehicle_speed_measurement", false)
        displayMetricsEnabled = Prefs.getBoolean("pref.drag_racing.vehicle_speed.enabled", true)
        shiftLightsEnabled = Prefs.getBoolean("pref.drag_racing.shift_lights.enabled", false)
        shiftLightsRevThreshold = Prefs.getS("pref.drag_racing.shift_lights.rev_value", "5000").toInt()
        fontSize = Prefs.getS("pref.drag_racing.screen_font_size","30").toInt()
        selectedPIDs = query.getIDs()
    }

    override fun isBreakLabelTextEnabled(): Boolean = false

    override fun isStatisticsEnabled(): Boolean  = true
    override fun isFpsCounterEnabled(): Boolean  = true
    override fun getSurfaceFrameRate(): Int  = Prefs.getS("pref.drag_racing.fps","5").toInt()

    override fun isStatusPanelEnabled(): Boolean = false

    override fun getMaxAllowedItemsInColumn(): Int  = 8
}