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
package org.obd.graphs.bl.trip

import org.obd.graphs.preferences.*

private const val VIRTUAL_SCREEN_SELECTION = "pref.graph.virtual.selected"
const val PREF_GRAPH_DIALOG = "pref.graph.pids.selected"

class TripVirtualScreenManager {
    fun getCurrentScreenId() = Prefs.getString(VIRTUAL_SCREEN_SELECTION, "1")!!

    fun updateScreenId(screenId: String) {
        Prefs.updateString(VIRTUAL_SCREEN_SELECTION, screenId)
    }

    fun getCurrentMetrics(): Set<String> =
        Prefs.getStringSet(getVirtualScreenPrefKey(), mutableSetOf())!!

    fun updateCurrentScreenMetrics(metrics: List<String>) {
        Prefs.updateStringSet(getVirtualScreenPrefKey(),metrics)
    }
    fun getVirtualScreenPrefKey(): String = "$PREF_GRAPH_DIALOG.${getCurrentScreenId()}"
}

val tripVirtualScreenManager = TripVirtualScreenManager()