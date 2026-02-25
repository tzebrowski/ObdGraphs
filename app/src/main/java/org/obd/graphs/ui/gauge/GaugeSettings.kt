 /**
 * Copyright 2019-2026, Tomasz Żebrowski
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
package org.obd.graphs.ui.gauge

import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getS
import org.obd.graphs.preferences.getStringSet
import org.obd.graphs.preferences.isEnabled
import org.obd.graphs.renderer.api.GaugeScreenSettings
import org.obd.graphs.renderer.api.ScreenSettings
import org.obd.graphs.ui.common.COLOR_RAINBOW_INDIGO

class GaugeSettings : ScreenSettings {
    private val gaugeScreenSettings =
        object : GaugeScreenSettings() {
            override fun getFontSize(): Int = Prefs.getS("pref.gauge.font_size", "42").toInt()

            override fun getGaugeContainerColor(): Int = Prefs.getInt("pref.gauge_background_color", COLOR_RAINBOW_INDIGO)
        }

    override fun getGaugeScreenSettings(): GaugeScreenSettings =
        gaugeScreenSettings.apply {
            updateSelectedPIDs(Prefs.getStringSet(gaugeVirtualScreenPreferences.getVirtualScreenPrefKey()).map { s -> s.toLong() }.toSet())
        }

    override fun isScrollbarEnabled(): Boolean = Prefs.isEnabled("pref.gauge_scrollbar_enabled")

    override fun isAA(): Boolean = false

    override fun getMaxItems(): Int = Prefs.getS("pref.gauge.max_items", "40").toInt()

    override fun isBreakLabelTextEnabled(): Boolean = true

    override fun getMaxColumns(): Int = Prefs.getS("pref.gauge.max_columns", "2").toInt()

    override fun isStatisticsEnabled(): Boolean = true

    override fun isScaleEnabled(): Boolean = Prefs.isEnabled("pref.gauge_display_scale")

    override fun isFpsCounterEnabled(): Boolean = Prefs.isEnabled("pref.gauge_display_command_rate")

    override fun getSurfaceFrameRate(): Int = Prefs.getS("pref.gauge.fps", "5").toInt()

    override fun isStatusPanelEnabled(): Boolean = false
}
