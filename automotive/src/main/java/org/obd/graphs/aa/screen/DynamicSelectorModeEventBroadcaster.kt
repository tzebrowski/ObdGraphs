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
package org.obd.graphs.aa.screen

import android.util.Log
import org.obd.graphs.bl.datalogger.MetricsProcessor
import org.obd.graphs.bl.datalogger.isDynamicSelector
import org.obd.graphs.isNumber
import org.obd.graphs.sendBroadcastEvent
import org.obd.graphs.toInt
import org.obd.metrics.api.model.ObdMetric

const val EVENT_DYNAMIC_SELECTOR_MODE_NORMAL = "event.dynamic.selector.mode.normal"
const val EVENT_DYNAMIC_SELECTOR_MODE_ECO = "event.dynamic.selector.mode.eco"
const val EVENT_DYNAMIC_SELECTOR_MODE_SPORT = "event.dynamic.selector.mode.sport"
const val EVENT_DYNAMIC_SELECTOR_MODE_RACE = "event.dynamic.selector.mode.race"

internal val dynamicSelectorModeEventBroadcaster: MetricsProcessor = DynamicSelectorModeEventBroadcaster()

private const val LOG_TAG = "DynSelectorBroadcast"

internal class DynamicSelectorModeEventBroadcaster : MetricsProcessor {
    private var currentMode = -1

    override fun postValue(obdMetric: ObdMetric) {
        if (obdMetric.isDynamicSelector() && obdMetric.isNumber()) {
            val it = obdMetric.toInt()
            if (Log.isLoggable(LOG_TAG, Log.VERBOSE)) {
                Log.v(LOG_TAG, "Received=$it, current=$currentMode ")
            }

            if (currentMode != it) {
                if (Log.isLoggable(LOG_TAG, Log.VERBOSE)) {
                    Log.v(LOG_TAG, "Broadcasting Dynamic Selector Mode Change, new=$it")
                }

                currentMode = it
                when (it) {
                    0 -> sendBroadcastEvent(EVENT_DYNAMIC_SELECTOR_MODE_NORMAL)
                    2 -> sendBroadcastEvent(EVENT_DYNAMIC_SELECTOR_MODE_SPORT)
                    4 -> sendBroadcastEvent(EVENT_DYNAMIC_SELECTOR_MODE_ECO)
                    else -> sendBroadcastEvent(EVENT_DYNAMIC_SELECTOR_MODE_RACE)
                }
            }
        }
    }
}
