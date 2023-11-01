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
import org.obd.metrics.api.model.Lifecycle
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.api.model.VehicleCapabilities
import kotlin.math.min


private const val SPEED_0_KM_H = 0
private const val SPEED_60_KM_H = 60
private const val SPEED_100_KM_H = 100
private const val SPEED_140_KM_H = 140
private const val SPEED_160_KM_H = 160
private const val SPEED_200_KM_H = 200

private const val LOG_KEY = "DragRaceResult"

internal class DragRacingResultsUpdater : Lifecycle {

    private var _0_ts: Long? = null
    private var _100_ts: Long? = null
    private var _60_ts: Long? = null
    private var result0_60: Long? = null
    private var result0_100: Long? = null
    private var result60_140: Long? = null
    private var result0_160: Long? = null
    private var result100_200: Long? = null

    override fun onStopped() {
        dragRacingResultRegistry.readyToRace(false)
    }

    override fun onRunning(vehicleCapabilities: VehicleCapabilities?) {
        reset()
    }

    fun postValue(obdMetric: ObdMetric) {
        if (isVehicleSpeedPID(obdMetric)) {
            if (obdMetric.value.toInt() == SPEED_0_KM_H) {
                reset()

                if (Log.isLoggable(LOG_KEY,Log.VERBOSE)) {
                    Log.v(LOG_KEY, "Ready to measure, current speed: ${obdMetric.value}")
                }

                dragRacingResultRegistry.readyToRace(true)
                _0_ts = obdMetric.timestamp

            } else {
                dragRacingResultRegistry.readyToRace(false)
            }

            if (isGivenSpeed(obdMetric, SPEED_60_KM_H)) {
                if (result0_60 == null) {
                    _60_ts = obdMetric.timestamp

                    _0_ts?.let { _0_ts ->
                        result0_60 = obdMetric.timestamp - _0_ts
                        dragRacingResultRegistry.update060(result0_60!!, obdMetric.value.toInt())
                        Log.i(LOG_KEY, "Current speed: ${obdMetric.value}. Result: 0-60 ${result0_60}ms")
                    }
                }
            }


            if (isGivenSpeed(obdMetric, SPEED_100_KM_H)) {
                if (result0_100 == null) {
                    _100_ts = obdMetric.timestamp
                    _0_ts?.let { _0_ts ->
                        result0_100 = obdMetric.timestamp - _0_ts
                        dragRacingResultRegistry.update0100(result0_100!!, obdMetric.value.toInt())

                        if (Log.isLoggable(LOG_KEY, Log.VERBOSE)) {
                            Log.v(LOG_KEY, "Current speed: ${obdMetric.value}. Result: 0-100 ${result0_100}ms")
                        }
                    }
                }
            }

            if (result0_160 == null && isGivenSpeed(obdMetric, SPEED_160_KM_H)) {
                _0_ts?.let { _0_ts ->
                    result0_160 = obdMetric.timestamp - _0_ts
                    dragRacingResultRegistry.update0160(result0_160!!, obdMetric.value.toInt())
                    Log.i(LOG_KEY, "Current speed: ${obdMetric.value}. Result: 0-160 ${result0_160}ms")
                }
            }

            if (result100_200 == null && _100_ts != null && isGivenSpeed(obdMetric, SPEED_200_KM_H)) {
                _100_ts?.let { _100_ts ->
                    result100_200 = obdMetric.timestamp - _100_ts
                    dragRacingResultRegistry.update100200(result100_200!!, obdMetric.value.toInt())
                    Log.i(LOG_KEY, "Current speed: ${obdMetric.value}. Result: 100-200 ${result100_200}ms")
                }
            }

            if (result60_140 == null && _60_ts != null && isGivenSpeed(obdMetric, SPEED_140_KM_H)) {
                _60_ts?.let { _60_ts ->
                    result60_140 = obdMetric.timestamp - _60_ts
                    dragRacingResultRegistry.update60140(result60_140!!, obdMetric.value.toInt())
                    Log.i(LOG_KEY, "Current speed: ${obdMetric.value}. Result: 60-140 ${result60_140}ms")
                }
            }

        }
    }

    private fun isGivenSpeed(obdMetric: ObdMetric, givenSpeed: Int): Boolean = min(obdMetric.value.toInt(), givenSpeed) == givenSpeed

    private fun reset() {
        _0_ts = null
        _100_ts = null
        _60_ts = null
        result0_60 = null
        result0_100 = null
        result0_160 = null
        result60_140 = null
        result100_200 = null
    }

    private fun isVehicleSpeedPID(obdMetric: ObdMetric): Boolean = obdMetric.command.pid.id == dragRacingResultRegistry.getVehicleSpeedPID()
}