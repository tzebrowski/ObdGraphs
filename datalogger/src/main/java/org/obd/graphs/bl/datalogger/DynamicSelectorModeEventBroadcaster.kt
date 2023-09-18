/**
 * Copyright 2019-2023, Tomasz Żebrowski
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
package org.obd.graphs.bl.datalogger

import android.util.Log
import org.obd.graphs.datalogger.BuildConfig
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.runAsync
import org.obd.graphs.sendBroadcastEvent
import org.obd.metrics.api.model.Lifecycle
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.api.model.VehicleCapabilities
import org.obd.metrics.command.obd.ObdCommand


private const val LOG_KEY = "DynamicSelectorModeEventEmitter"
private class FakeMetricsBroadcaster (private val emitter: DynamicSelectorModeEventBroadcaster) {

    private val values = listOf(2,2,2,2,0,0,0,0,0,4,4,4,4,4,4,2,2,2,2,0,0,0,0,0,4,4,4,4,4,4,2,2,2,2,0,0,0,0,0,4,4,4,4,4,4)
    private var broadcasterLaunched:Boolean = false

    fun onStopped() {
        broadcasterLaunched = false
    }
    fun onRunning() {
        if (!broadcasterLaunched){
            broadcasterLaunched = true
            val pid = dataLogger.getPidDefinitionRegistry().findBy(DYNAMIC_SELECTOR_PID_ID)
            var cnt = 0

            runAsync {
                while (broadcasterLaunched){
                    Thread.sleep(3000)
                    if (cnt == values.size){
                        cnt = 0
                    }
                    val metric = ObdMetric.builder().value(values[cnt]).command(ObdCommand(pid)).build()
                    emitter.postValue(metric)
                    cnt++
                }
            }
        }
    }
}

private const val DYNAMIC_SELECTOR_PID_ID = 7036L
internal class DynamicSelectorModeEventBroadcaster: Lifecycle {
    private var currentMode = -1
    private val fakeEventsBroadcaster = FakeMetricsBroadcaster(this)
    
    override fun onStopped() {
        if (isBroadcastingFakeMetricsEnabled()){
            fakeEventsBroadcaster.onStopped()
        }
    }

    override fun onRunning(vehicleCapabilities: VehicleCapabilities?) {
        if (isBroadcastingFakeMetricsEnabled()) {
            fakeEventsBroadcaster.onRunning()
        }
    }

    fun postValue(obdMetric: ObdMetric) {
        if (isDynamicSelectorPID(obdMetric)) {
            Log.d(LOG_KEY,"Received=${obdMetric.value.toInt()}, current=${currentMode} ")
            if (currentMode != obdMetric.value) {
                Log.d(LOG_KEY,"Broadcasting Dynamic Selector Mode Change, new=${obdMetric.value.toInt()}")

                currentMode = obdMetric.value.toInt()
                when (obdMetric.value.toInt()) {
                    0 -> sendBroadcastEvent(EVENT_DYNAMIC_SELECTOR_MODE_NORMAL)
                    2 -> sendBroadcastEvent(EVENT_DYNAMIC_SELECTOR_MODE_SPORT)
                    4 -> sendBroadcastEvent(EVENT_DYNAMIC_SELECTOR_MODE_ECO)
                    else -> sendBroadcastEvent(EVENT_DYNAMIC_SELECTOR_MODE_RACE)
                }
            }
        }
    }

    private fun isBroadcastingFakeMetricsEnabled(): Boolean = BuildConfig.DEBUG &&
        Prefs.getBoolean("pref.debug.generator.broadcast_fake_metrics", false)

    private fun isDynamicSelectorPID(obdMetric: ObdMetric): Boolean =  obdMetric.command.pid.id ==  DYNAMIC_SELECTOR_PID_ID
}