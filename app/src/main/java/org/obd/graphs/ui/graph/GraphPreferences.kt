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
package org.obd.graphs.ui.graph

import android.util.Log
import org.obd.graphs.bl.trip.tripVirtualScreenManager
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getS

data class GraphPreferences(
    val xAxisStartMovingAfter: Float,
    val xAxisMinimumShift: Float,
    val cacheEnabled: Boolean,
    val metrics: Set<Long>,
    val toggleVirtualPanel: Boolean,
)

private const val LOG_TAG = "GraphPrefs"

class GraphPreferencesReader {
    fun read(): GraphPreferences {
        val prefixKey = "pref.graph"

        val xAxisStartMovingAfter =
            Prefs.getS("$prefixKey.x-axis.start-moving-after.time", "20000").toFloat()

        val xAxisMinimumShift =
            Prefs.getS("$prefixKey.x-axis.minimum-shift.time", "20").toFloat()

        val cacheEnabled = Prefs.getBoolean("$prefixKey.cache.enabled", true)

        val metrics = tripVirtualScreenManager.getCurrentMetrics().map { it.toLong() }.toSet()

        val toggleVirtualPanel = Prefs.getBoolean("$prefixKey.toggle_virtual_screens_double_click", true)

        if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
            Log.d(
                LOG_TAG,
                "Read graph preferences\n" +
                    "xAxisStartMovingAfterProp=$xAxisStartMovingAfter\n" +
                    "xAxisMinimumShiftProp=$xAxisMinimumShift\n" +
                    "cacheEnabledProp=$cacheEnabled\n" +
                    "toggleVirtualPanel=$toggleVirtualPanel\n" +
                    "metrics=$metrics\n",
            )
        }

        return GraphPreferences(
            xAxisStartMovingAfter,
            xAxisMinimumShift,
            cacheEnabled,
            metrics,
            toggleVirtualPanel = toggleVirtualPanel,
        )
    }
}

val graphPreferencesReader = GraphPreferencesReader()
