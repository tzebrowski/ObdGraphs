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
import org.obd.graphs.PREF_DRAG_RACE_KEY_PREFIX
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.updateString

private const val LOG_KEY = "DragRaceRegistry"
private const val DEFAULT_SHIFT_LIGHT_THRESHOLD = 5000

internal class InMemoryDragRacingRegistry : DragRacingResultRegistry {
    private data class PreferencesIds(
        val id: String,
    ) {
        val best: String = "$PREF_DRAG_RACE_KEY_PREFIX.best.$id"
        val ambientTemp: String = "$PREF_DRAG_RACE_KEY_PREFIX.best.$id.ambient_temp"
        val atmPressure: String = "$PREF_DRAG_RACE_KEY_PREFIX.best.$id.atm_pressure"
        val last: String = "$PREF_DRAG_RACE_KEY_PREFIX.last.$id"
    }

    private val results = DragRacingResults()
    private var shiftLightThresholdValue = DEFAULT_SHIFT_LIGHT_THRESHOLD

    // Configuration Mappings
    private val ids060 = PreferencesIds("0_60")
    private val ids0100 = PreferencesIds("0_100")
    private val ids0160 = PreferencesIds("0_160")
    private val ids100200 = PreferencesIds("100_200")
    private val ids60140 = PreferencesIds("60_140")

    init {
        restoreFromPreferences()
    }

    override fun getResult(): DragRacingResults = results

    override fun getShiftLightsRevThreshold(): Int = shiftLightThresholdValue

    override fun setShiftLightsRevThreshold(newThresholdValue: Int) {
        shiftLightThresholdValue = newThresholdValue
    }

    override fun readyToRace(value: Boolean) {
        results.readyToRace = value
    }

    override fun enableShiftLights(value: Boolean) {
        results.enableShiftLights = value
    }

    // Consolidated Update Calls
    override fun update060(metric: DragRacingMetric) = updateEntry(results._0_60, ids060, metric)

    override fun update0100(metric: DragRacingMetric) = updateEntry(results._0_100, ids0100, metric)

    override fun update0160(metric: DragRacingMetric) = updateEntry(results._0_160, ids0160, metric)

    override fun update100200(metric: DragRacingMetric) = updateEntry(results._100_200, ids100200, metric)

    override fun update60140(metric: DragRacingMetric) = updateEntry(results._60_140, ids60140, metric)

    private fun restoreFromPreferences() {
        loadEntry(results._0_60, ids060)
        loadEntry(results._0_100, ids0100)
        loadEntry(results._60_140, ids60140)
        loadEntry(results._0_160, ids0160)
        loadEntry(results._100_200, ids100200)
    }

    private fun loadEntry(
        entry: DragRacingEntry,
        ids: PreferencesIds,
    ) {
        Prefs.getString(ids.best, null)?.let { entry.best = it.toLong() }
        Prefs.getString(ids.ambientTemp, null)?.let { entry.bestAmbientTemp = it.toInt() }
        Prefs.getString(ids.atmPressure, null)?.let { entry.bestAtmPressure = it.toInt() }
        Prefs.getString(ids.last, null)?.let { entry.last = it.toLong() }
    }

    private fun updateEntry(
        entry: DragRacingEntry,
        ids: PreferencesIds,
        metric: DragRacingMetric,
    ) {
        if (metric.time <= 0L) {
            Log.v(LOG_KEY, "Invalid value=${metric.time}")
            return
        }

        // Shift current to last
        if (entry.current != VALUE_NOT_SET) {
            entry.last = entry.current
            Prefs.updateString(ids.last, entry.last.toString())
        }

        // Set current
        entry.current = metric.time
        entry.currentSpeed = metric.speed

        // Update Best if applicable
        if (entry.best == VALUE_NOT_SET || metric.time < entry.best) {
            entry.best = metric.time
            Prefs.updateString(ids.best, metric.time.toString())

            metric.ambientTemp?.let {
                results.ambientTemp = it
                entry.bestAmbientTemp = it
                Prefs.updateString(ids.ambientTemp, it.toString())
            }

            metric.atmPressure?.let {
                results.atmPressure = it
                entry.bestAtmPressure = it
                Prefs.updateString(ids.atmPressure, it.toString())
            }
        }
    }
}
