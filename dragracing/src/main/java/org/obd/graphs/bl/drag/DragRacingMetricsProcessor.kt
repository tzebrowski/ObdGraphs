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
package org.obd.graphs.bl.drag

import android.util.Log
import org.obd.graphs.bl.datalogger.MetricsProcessor
import org.obd.graphs.bl.query.isAmbientTemp
import org.obd.graphs.bl.query.isAtmPressure
import org.obd.graphs.bl.query.isEngineRpm
import org.obd.graphs.bl.query.isVehicleSpeed
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


val dragRacingMetricsProcessor: MetricsProcessor = DragRacingMetricsProcessor(dragRacingResultRegistry)

internal class DragRacingMetricsProcessor(private val registry: DragRacingResultRegistry) : MetricsProcessor {

    private var _0ts: Long? = null
    private var _100ts: Long? = null
    private var _60ts: Long? = null
    private var result0_60: Long? = null
    private var result0_100: Long? = null
    private var result60_140: Long? = null
    private var result0_160: Long? = null
    private var result100_200: Long? = null
    private var ambientTemp: Int?  = null
    private var atmPressure: Int?  = null

    override fun onStopped() {
        registry.readyToRace(false)
        registry.enableShiftLights(false)
    }

    override fun onRunning(vehicleCapabilities: VehicleCapabilities?) {
        reset0()
    }

    override fun postValue(obdMetric: ObdMetric) {

        if (obdMetric.isEngineRpm()) {
            if (Log.isLoggable(LOG_KEY, Log.VERBOSE)) {
                Log.v(
                    LOG_KEY, "Current revLimit='${registry.getShiftLightsRevThreshold()}', " +
                            "current rev: ${obdMetric.value.toInt()}, rising: ${obdMetric.value.toInt() > registry.getShiftLightsRevThreshold()}"
                )
            }
            registry.enableShiftLights(obdMetric.value.toInt() > registry.getShiftLightsRevThreshold())
        } else if (obdMetric.isAtmPressure()) {
            atmPressure = obdMetric.value.toInt()
        } else if (obdMetric.isAmbientTemp()) {
            ambientTemp = obdMetric.value.toInt()

        } else if (obdMetric.isVehicleSpeed()) {

            if (obdMetric.value.toInt() == SPEED_0_KM_H) {
                reset0()

                if (Log.isLoggable(LOG_KEY, Log.VERBOSE)) {
                    Log.v(LOG_KEY, "Ready to measure, current speed: ${obdMetric.value}")
                }

                registry.readyToRace(true)
                _0ts = obdMetric.timestamp

            } else {
                registry.readyToRace(false)
            }


            if (isGivenSpeedReached(obdMetric, SPEED_60_KM_H - 5) && obdMetric.value.toInt() < SPEED_60_KM_H) {
                Log.i(LOG_KEY, "Reset 60-140 measurement at speed: ${obdMetric.value.toInt()}")
                result60_140 = null
                _60ts = null
            }

            if (isGivenSpeedReached(obdMetric, SPEED_60_KM_H)) {
                if (_60ts == null) {
                    _60ts = obdMetric.timestamp
                    Log.i(LOG_KEY, "Setting 60km/h ts: ${obdMetric.timestamp}")
                }

                if (result0_60 == null) {

                    _0ts?.let { _0_ts ->
                        result0_60 = obdMetric.timestamp - _0_ts
                        registry.update060(DragRacingMetric(time = result0_60!!,speed = obdMetric.value.toInt(),
                            ambientTemp = ambientTemp, atmPressure = atmPressure))
                        Log.i(LOG_KEY, "Current speed: ${obdMetric.value}. Result: 0-60 ${result0_60}ms")
                    }
                }
            }



            if (isGivenSpeedReached(obdMetric, SPEED_100_KM_H - 5) && obdMetric.value.toInt() < SPEED_100_KM_H) {
                Log.i(LOG_KEY, "Reset 100-200 measurement at speed: ${obdMetric.value.toInt()}")
                result100_200 = null
                _100ts = null
            }

            if (isGivenSpeedReached(obdMetric, SPEED_100_KM_H)) {
                if (_100ts == null) {
                    _100ts = obdMetric.timestamp
                    Log.i(LOG_KEY, "Setting 100km/h ts: ${obdMetric.timestamp}")
                }

                if (result0_100 == null) {

                    _0ts?.let { _0_ts ->
                        result0_100 = obdMetric.timestamp - _0_ts
                        registry.update0100(DragRacingMetric(time = result0_100!!,speed = obdMetric.value.toInt(),
                            ambientTemp = ambientTemp, atmPressure = atmPressure))

                        if (Log.isLoggable(LOG_KEY, Log.VERBOSE)) {
                            Log.v(LOG_KEY, "Current speed: ${obdMetric.value}. Result: 0-100 ${result0_100}ms")
                        }
                    }
                }
            }

            if (result0_160 == null && isGivenSpeedReached(obdMetric, SPEED_160_KM_H)) {
                _0ts?.let { _0_ts ->
                    result0_160 = obdMetric.timestamp - _0_ts
                    registry.update0160(DragRacingMetric(time = result0_160!!,speed =  obdMetric.value.toInt(),
                        ambientTemp = ambientTemp, atmPressure = atmPressure))
                    Log.i(LOG_KEY, "Current speed: ${obdMetric.value}. Result: 0-160 ${result0_160}ms")
                }
            }

            if (result100_200 == null && _100ts != null && isGivenSpeedReached(obdMetric, SPEED_200_KM_H)) {
                _100ts?.let { _100_ts ->
                    result100_200 = obdMetric.timestamp - _100_ts
                    registry.update100200(DragRacingMetric(time = result100_200!!,speed =  obdMetric.value.toInt(),
                        ambientTemp = ambientTemp, atmPressure = atmPressure))
                    Log.i(LOG_KEY, "Current speed: ${obdMetric.value}. Result: 100-200 ${result100_200}ms")
                }
            }

            if (result60_140 == null && _60ts != null && isGivenSpeedReached(obdMetric, SPEED_140_KM_H)) {
                _60ts?.let { _60_ts ->
                    result60_140 = obdMetric.timestamp - _60_ts
                    registry.update60140(DragRacingMetric(time = result60_140!!,speed =  obdMetric.value.toInt(),
                        ambientTemp = ambientTemp, atmPressure = atmPressure))
                    Log.i(LOG_KEY, "Current speed: ${obdMetric.value}, _60ts=${_60ts}, _140ts=${obdMetric.timestamp},  Result: 60-140 ${result60_140}ms")
                }
            }
        }
    }

    private fun isGivenSpeedReached(obdMetric: ObdMetric, givenSpeed: Int): Boolean = min(obdMetric.value.toInt(), givenSpeed) == givenSpeed

    private fun reset0() {
        _0ts = null
        _100ts = null
        _60ts = null
        result0_60 = null
        result0_100 = null
        result0_160 = null
        result60_140 = null
        result100_200 = null
    }
}