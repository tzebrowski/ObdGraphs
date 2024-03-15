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
package org.obd.graphs.activity

import android.util.Log
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.isEnabled
import org.obd.graphs.runAsync

data class MainActivityPreferences(
    val hideToolbarDoubleClick: Boolean,
    val hideToolbarLandscape: Boolean,
    val showDashView : Boolean,
    val showGiuliaView: Boolean,
    val showGaugeView: Boolean,
    val showGraphView: Boolean,
    val hideToolbarConnected: Boolean
)

const val PREFS_LOGGER_TAG = "PREFS"

fun getMainActivityPreferences(): MainActivityPreferences  = runAsync {
    val hideToolbarDoubleClick = Prefs.isEnabled("pref.toolbar.hide.doubleclick")
    val hideToolbarLandscape = Prefs.isEnabled("pref.toolbar.hide.landscape")
    val hideToolbarConnected = Prefs.isEnabled("pref.toolbar.hide.connected")
    val showDashView = Prefs.isEnabled("pref.dash.view.enabled")
    val showGaugeView = Prefs.isEnabled("pref.gauge.view.enabled")

    val showGiuliaView = Prefs.getBoolean("pref.giulia.view.enabled", true)
    val showGraphView = Prefs.getBoolean("pref.graph.view.enabled", true)

    val prefs = MainActivityPreferences(
        hideToolbarDoubleClick =  hideToolbarDoubleClick,
        hideToolbarLandscape = hideToolbarLandscape,
        hideToolbarConnected = hideToolbarConnected,
        showGiuliaView =  showGiuliaView,
        showGraphView = showGraphView,
        showGaugeView = showGaugeView,
        showDashView = showDashView
    )

    Log.d(PREFS_LOGGER_TAG, "Loaded MainActivity preferences: $prefs")
    prefs
}