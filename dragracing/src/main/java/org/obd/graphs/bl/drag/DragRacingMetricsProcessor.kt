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
package org.obd.graphs.bl.drag

import android.util.Log
import org.obd.graphs.bl.datalogger.*
import org.obd.graphs.isNumber
import org.obd.graphs.toInt
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.api.model.VehicleCapabilities

private const val LOG_KEY = "DragRaceResult"

// Allows simple instantiation if needed, or Dependency Injection
val dragRacingMetricsProcessor: MetricsProcessor by lazy {
    DragRacingMetricsProcessor(DragRacingService.registry)
}

internal class DragRacingMetricsProcessor(private val registry: DragRacingResultRegistry) : MetricsProcessor {

    // Definition of a specific race segment (e.g. 0-100)
    private data class RaceDefinition(
        val startSpeed: Int,
        val endSpeed: Int,
        val updateRegistry: (DragRacingMetric) -> Unit
    )

    // Configuration of all supported races
    private val raceConfiguration = listOf(
        RaceDefinition(0, 60) { registry.update060(it) },
        RaceDefinition(0, 100) { registry.update0100(it) },
        RaceDefinition(0, 160) { registry.update0160(it) },
        RaceDefinition(60, 140) { registry.update60140(it) },
        RaceDefinition(100, 200) { registry.update100200(it) }
    )

    // Dynamic state
    private val startTimestamps = mutableMapOf<Int, Long>()
    private val completedRaces = mutableSetOf<RaceDefinition>()

    // Environment state
    private var ambientTemperature: Int? = null
    private var atmosphericPressure: Int? = null

    override fun onStopped() {
        registry.readyToRace(false)
        registry.enableShiftLights(false)
        resetState()
    }

    override fun onRunning(vehicleCapabilities: VehicleCapabilities?) {
        resetState()
    }

    override fun postValue(obdMetric: ObdMetric) {
        if (!obdMetric.isNumber()) return

        val intValue = obdMetric.toInt()

        when {
            obdMetric.isEngineRpm() -> handleRpm(intValue)
            obdMetric.isAtmPressure() -> atmosphericPressure = intValue
            obdMetric.isAmbientTemp() -> ambientTemperature = intValue
            obdMetric.isVehicleSpeed() -> handleSpeed(obdMetric, intValue)
        }
    }

    private fun handleRpm(rpm: Int) {
        val threshold = registry.getShiftLightsRevThreshold()
        registry.enableShiftLights(rpm > threshold)
        if (Log.isLoggable(LOG_KEY, Log.VERBOSE)) {
            Log.v(LOG_KEY, "RPM: $rpm, Threshold: $threshold")
        }
    }

    private fun handleSpeed(metric: ObdMetric, speed: Int) {
        // 1. Reset logic: If stopped, reset everything.
        if (speed == 0) {
            resetState()
            startTimestamps[0] = metric.timestamp
            registry.readyToRace(true)
            if (Log.isLoggable(LOG_KEY, Log.VERBOSE)) {
                Log.v(LOG_KEY, "Speed 0 detected, ready to race.")
            }
            return
        } else {
            registry.readyToRace(false)
        }

        // 2. Track Starts (Flying starts like 60-140 or 100-200)
        // If we drop slightly below a start threshold (e.g. 55 for a 60 start), reset that start time.
        // If we cross the start threshold, record the time.
        raceConfiguration.map { it.startSpeed }.distinct().filter { it > 0 }.forEach { startSpeed ->
            if (speed >= startSpeed && !startTimestamps.containsKey(startSpeed)) {
                startTimestamps[startSpeed] = metric.timestamp
                Log.i(LOG_KEY, "Recorded start timestamp for speed $startSpeed")
            }

            // Hysteresis reset: if we drop 5km/h below start speed, cancel that start
            if (speed < (startSpeed - 5)) {
                if (startTimestamps.remove(startSpeed) != null) {
                    Log.i(LOG_KEY, "Speed dropped below ${startSpeed - 5}, resetting start time for $startSpeed")
                    // Clear completions dependent on this start speed
                    completedRaces.removeAll { it.startSpeed == startSpeed }
                }
            }
        }

        // 3. Check Finishes
        raceConfiguration.forEach { race ->
            if (completedRaces.contains(race)) return@forEach

            val startTime = startTimestamps[race.startSpeed] ?: return@forEach

            if (speed >= race.endSpeed) {
                val duration = metric.timestamp - startTime
                val resultMetric = DragRacingMetric(
                    time = duration,
                    speed = speed,
                    ambientTemp = ambientTemperature,
                    atmPressure = atmosphericPressure
                )

                race.updateRegistry(resultMetric)
                completedRaces.add(race)

                Log.i(LOG_KEY, "Race Finished: ${race.startSpeed}-${race.endSpeed} in ${duration}ms")
            }
        }
    }

    private fun resetState() {
        startTimestamps.clear()
        completedRaces.clear()
    }
}
