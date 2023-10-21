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
package org.obd.graphs.bl.datalogger.drag

import android.util.Log
import org.obd.graphs.bl.datalogger.VEHICLE_SPEED_PID_ID
import org.obd.metrics.api.model.Lifecycle
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.api.model.VehicleCapabilities
import kotlin.math.min

private const val SPEED_0_KM_H = 0
private const val SPEED_100_KM_H = 100
private const val SPEED_160_KM_H = 160
private const val SPEED_200_KM_H = 200

private const val LOG_KEY = "DragRaceResultBroadcaster"

internal class DragRaceResultBroadcaster : Lifecycle {

    private var _0_ts: Long? = null
    private var _100_ts: Long? = null
    private var result0_100: Long? = null
    private var result0_160: Long? = null
    private var result100_200: Long? = null

    override fun onStopped() {
    }

    override fun onRunning(vehicleCapabilities: VehicleCapabilities?) {
        reset()
    }

    fun postValue(obdMetric: ObdMetric) {
        if (isVehicleSpeedPID(obdMetric)) {
            if (obdMetric.value.toInt() == SPEED_0_KM_H) {
                reset()
                Log.i(LOG_KEY, "Ready to measure, current speed: ${obdMetric.value}")
                _0_ts = obdMetric.timestamp
            }

            if (result0_100 == null && min(obdMetric.value.toInt(), SPEED_100_KM_H) == SPEED_100_KM_H) {
                _100_ts = obdMetric.timestamp
                _0_ts?.let { _0_ts ->
                    result0_100 = obdMetric.timestamp - _0_ts
                    dragRaceRegistry.update0100(result0_100!!, obdMetric.value.toInt())
                    Log.i(LOG_KEY, "Current speed: ${obdMetric.value}. Result: 0-100 ${result0_100}ms")
                }
            }

            if (result0_160 == null && min(obdMetric.value.toInt(), SPEED_160_KM_H) == SPEED_160_KM_H) {
                _0_ts?.let { _0_ts ->
                    result0_160 = obdMetric.timestamp - _0_ts
                    dragRaceRegistry.update0160(result0_160!!, obdMetric.value.toInt())
                    Log.i(LOG_KEY, "Current speed: ${obdMetric.value}. Result: 0-160 ${result0_160}ms")
                }
            }

            if (result100_200 == null && _100_ts != null && min(obdMetric.value.toInt(), SPEED_200_KM_H) == SPEED_200_KM_H) {
                _100_ts?.let { _100_ts ->
                    result100_200 = obdMetric.timestamp - _100_ts
                    dragRaceRegistry.update100200(result100_200!!, obdMetric.value.toInt())
                    Log.i(LOG_KEY, "Current speed: ${obdMetric.value}. Result: 100-200 ${result100_200}ms")
                }
            }
        }
    }

    private fun reset() {
        _0_ts = null
        _100_ts = null
        result0_100 = null
        result0_160 = null
        result100_200 = null
    }

    private fun isVehicleSpeedPID(obdMetric: ObdMetric): Boolean = obdMetric.command.pid.id == VEHICLE_SPEED_PID_ID
}