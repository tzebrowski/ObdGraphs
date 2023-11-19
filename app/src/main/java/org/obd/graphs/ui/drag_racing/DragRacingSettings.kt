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
package org.obd.graphs.ui.drag_racing

import org.obd.graphs.bl.query.Query
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getS
import org.obd.graphs.renderer.DragRacingSettings
import org.obd.graphs.renderer.ScreenSettings

class DragRacingSettings(private val query: Query): ScreenSettings {

    private val dragRacingSettings = DragRacingSettings()

    override fun getDragRacingSettings(): DragRacingSettings  = dragRacingSettings.apply {
        vehicleSpeedFrequencyReadEnabled = Prefs.getBoolean("pref.drag_racing.debug.display_frequency", true)
        vehicleSpeedDisplayDebugEnabled = Prefs.getBoolean("pref.drag_racing.debug.vehicle_speed_measurement", false)
        vehicleSpeedEnabled = Prefs.getBoolean("pref.drag_racing.vehicle_speed.enabled", true)
        shiftLightsEnabled = Prefs.getBoolean("pref.drag_racing.shift_lights.enabled", false)
        shiftLightsRevThreshold = Prefs.getS("pref.drag_racing.shift_lights.rev_value", "5000").toInt()
    }

    override fun getSelectedPIDs(): Set<Long> {
        return query.getPIDs()
    }

    override fun isBreakLabelTextEnabled(): Boolean = false

    override fun isHistoryEnabled(): Boolean  = true
    override fun isFpsCounterEnabled(): Boolean  = true
    override fun getSurfaceFrameRate(): Int  = Prefs.getS("pref.drag_racing.fps","5").toInt()
    override fun getFontSize(): Int =  Prefs.getS("pref.drag_racing.screen_font_size","30").toInt()

    override fun isStatusPanelEnabled(): Boolean = false

    override fun getMaxAllowedItemsInColumn(): Int  = 8
}