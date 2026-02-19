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
package org.obd.graphs.ui.giulia

import org.obd.graphs.bl.query.Query
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getS
import org.obd.graphs.renderer.GiuliaRendererSettings
import org.obd.graphs.renderer.ScreenSettings

class GiuliaSettings(
    private val query: Query,
) : ScreenSettings {
    private val giuliaRendererSettings =
        object : GiuliaRendererSettings() {
            override fun getFontSize(): Int = giuliaVirtualScreenPreferences.getFontSize()
        }

    override fun getGiuliaRendererSetting(): GiuliaRendererSettings =
        giuliaRendererSettings.apply {
            selectedPIDs = query.getIDs()
        }

    override fun getMaxItems(): Int = Prefs.getS("pref.giulia.max_items", "6").toInt()

    override fun isBreakLabelTextEnabled(): Boolean = true

    override fun getMaxColumns(): Int = giuliaVirtualScreenPreferences.getMaxItemsInColumn()

    override fun isStatisticsEnabled(): Boolean = true

    override fun isFpsCounterEnabled(): Boolean = true

    override fun getSurfaceFrameRate(): Int = Prefs.getS("pref.giulia.fps", "5").toInt()

    override fun isStatusPanelEnabled(): Boolean = false

    override fun getMaxAllowedItemsInColumn(): Int = 8
}
