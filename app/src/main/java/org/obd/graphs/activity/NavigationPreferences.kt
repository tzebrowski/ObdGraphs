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
package org.obd.graphs.activity

import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.isEnabled

data class NavigationPreferences(
    var hideToolbarLandscape: Boolean = true,
    var dashViewEnabled: Boolean = true,
    var giuliaViewEnabled: Boolean = true,
    var gaugeViewEnabled: Boolean = true,
    var graphViewEnabled: Boolean = true,
    var tripInfoViewEnabled: Boolean = true,
    var performanceViewEnabled: Boolean = true,
    var dragRacingViewEnabled: Boolean = true,
)

object Navigation {
    private val navigationPreferences = NavigationPreferences()

    fun getPreferences(): NavigationPreferences =
        navigationPreferences.apply {
            hideToolbarLandscape = Prefs.isEnabled("pref.toolbar.hide.landscape")
            dashViewEnabled = Prefs.isEnabled("pref.dash.view.enabled")
            performanceViewEnabled = Prefs.isEnabled("pref.performance.view.enabled")
            dragRacingViewEnabled = Prefs.isEnabled("pref.drag_racing.view.enabled")
            tripInfoViewEnabled = Prefs.isEnabled("pref.trip_info.view.enabled")

            gaugeViewEnabled = Prefs.getBoolean("pref.gauge.view.enabled", true)
            giuliaViewEnabled = Prefs.getBoolean("pref.giulia.view.enabled", true)
            graphViewEnabled = Prefs.getBoolean("pref.graph.view.enabled", true)
        }
}
