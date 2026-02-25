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
package org.obd.graphs.ui.performance

import org.obd.graphs.bl.query.PREF_QUERY_PERFORMANCE_BOTTOM
import org.obd.graphs.bl.query.PREF_QUERY_PERFORMANCE_TOP
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getLongSet
import org.obd.graphs.preferences.getS
import org.obd.graphs.renderer.api.BreakBoostingSettings
import org.obd.graphs.renderer.api.PerformanceScreenSettings
import org.obd.graphs.renderer.api.ScreenSettings

class PerformanceSettings : ScreenSettings {
    private val performanceScreenSettings = PerformanceScreenSettings()

    override fun getPerformanceScreenSettings() =
        performanceScreenSettings.apply {
            fontSize = Prefs.getS("pref.performance.screen_font_size", "30").toInt()
            labelCenterYPadding = Prefs.getS("pref.performance.screen_label_y_padding", "22").toFloat()
        }

    override fun isBreakLabelTextEnabled(): Boolean = true

    override fun isStatisticsEnabled(): Boolean = true

    override fun isFpsCounterEnabled(): Boolean = true

    override fun getSurfaceFrameRate(): Int = Prefs.getS("pref.performance.fps", "5").toInt()

    override fun isStatusPanelEnabled(): Boolean = false
}
