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
package org.obd.graphs.ui.dashboard

import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getLongSet
import org.obd.graphs.preferences.isEnabled

data class DashboardPreferences(
    val swipeToDeleteEnabled: Boolean,
    val dragAndDropEnabled: Boolean,
    val dashboardSelectedMetrics: Pair<String, Set<Long>>,
    val gaugeSelectedMetrics: Pair<String, Set<Long>>,
    val colorsEnabled: Boolean,
    val blinkEnabled: Boolean,
)

private const val SELECTED_DASHBOARD_METRICS = "pref.dash.pids.selected"
private const val SELECTED_GAUGE_METRICS = "pref.dash.gauge_pids.selected"

fun getDashboardPreferences(): DashboardPreferences {
    val swipeToDelete =
        Prefs.getBoolean("pref.dash.swipe.to.delete", false)

    val dragAndDropEnabled = Prefs.getBoolean("pref.dash.enable_drag", false)
    val dashboardSelectedMetrics = Prefs.getLongSet(SELECTED_DASHBOARD_METRICS)
    val gaugeSelectedMetrics = Prefs.getLongSet("pref.dash.gauge_pids.selected")

    val colors = Prefs.isEnabled("pref.dash.top.values.red.color")
    val blink = Prefs.isEnabled("pref.dash.top.values.blink")

    return DashboardPreferences(
        swipeToDeleteEnabled = swipeToDelete,
        dashboardSelectedMetrics = Pair(SELECTED_DASHBOARD_METRICS, dashboardSelectedMetrics),
        gaugeSelectedMetrics = Pair(SELECTED_GAUGE_METRICS, gaugeSelectedMetrics),
        colorsEnabled = colors,
        blinkEnabled = blink,
        dragAndDropEnabled = dragAndDropEnabled,
    )
}
