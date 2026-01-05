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
import org.obd.graphs.preferences.updateString

private const val VIRTUAL_SCREEN_SELECTION = "pref.gauge.virtual.selected"
const val PREF_GAUGE_DIALOG = "pref.gauge.pids.selected"

class GaugeVirtualScreen {
    fun getCurrentVirtualScreen() = Prefs.getString(VIRTUAL_SCREEN_SELECTION, "1")!!

    fun getVirtualScreenPrefKey(screenId: String = getCurrentVirtualScreen()): String = "$PREF_GAUGE_DIALOG.$screenId"

    fun updateVirtualScreen(screenId: String) {
        Prefs.updateString(VIRTUAL_SCREEN_SELECTION, screenId)
    }
}

val gaugeVirtualScreen = GaugeVirtualScreen()
