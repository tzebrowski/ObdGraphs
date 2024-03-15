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
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.updateString

private const val LOG_KEY = "DragRaceRegistry"
private const val DEFAULT_SHIFT_LIGHT_THRESHOLD = 5000

internal class InMemoryDragRacingRegistry : DragRacingResultRegistry {

    private data class PreferencesIds(
        val id: String,
        val best: String = "pref.drag_race.best.${id}",
        val ambientTemp: String = "pref.drag_race.best.${id}.ambient_temp",
        val atmPressure: String = "pref.drag_race.best.${id}.atm_pressure",
        val last: String = "pref.drag_race.last.${id}"
    )

    private val results = DragRacingResults()
    private val ids060 = PreferencesIds(id = "0_60")
    private val ids0100 = PreferencesIds(id = "0_100")
    private val ids0160 = PreferencesIds(id = "0_160")
    private val ids100200 = PreferencesIds(id = "100_200")
    private val ids60140 = PreferencesIds(id = "60_140")

    init {
        readPreferencesByIds(results._0_60, ids060)
        readPreferencesByIds(results._0_100, ids0100)
        readPreferencesByIds(results._60_140, ids60140)
        readPreferencesByIds(results._0_160, ids0160)
        readPreferencesByIds(results._100_200, ids100200)
    }

    private var shiftLightThresholdValue = DEFAULT_SHIFT_LIGHT_THRESHOLD
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

    override fun update60140(metric: DragRacingMetric) {
        updateEntry(results._60_140, ids60140, metric)
    }

    override fun update0100(metric: DragRacingMetric) {
        updateEntry(results._0_100, ids0100, metric)
    }

    override fun update060(metric: DragRacingMetric) {
        updateEntry(results._0_60, ids060, metric)
    }

    override fun update0160(metric: DragRacingMetric) {
        updateEntry(results._0_160, ids0160, metric)
    }

    override fun update100200(metric: DragRacingMetric) {
        updateEntry(results._100_200, ids100200, metric)
    }

    override fun getResult(): DragRacingResults = results

    private fun readPreferencesByIds(entry: DragRacingEntry, id: PreferencesIds) {
        Prefs.getString(id.best, null)?.let {
            entry.best = it.toLong()
        }

        Prefs.getString(id.ambientTemp, null)?.let {
            entry.bestAmbientTemp = it.toInt()
        }

        Prefs.getString(id.atmPressure, null)?.let {
            entry.bestAtmPressure = it.toInt()
        }

        Prefs.getString(id.last, null)?.let {
            entry.last = it.toLong()
        }
    }

    private fun updateEntry(entry: DragRacingEntry, id: PreferencesIds, metric: DragRacingMetric) {
        val time = metric.time
        val speed = metric.speed

        if (time <= 0L) {
            Log.v(LOG_KEY, "Invalid value")
        } else {
            entry.last = if (entry.last == VALUE_NOT_SET) {
                time
            } else {
                entry.current
            }

            Prefs.updateString(id.last, entry.last.toString())

            entry.current = time
            entry.currentSpeed = speed

            if (entry.best > time || entry.best == VALUE_NOT_SET) {

                entry.best = metric.time

                metric.ambientTemp?.let {
                    results.ambientTemp = it
                    entry.bestAmbientTemp = it
                    Prefs.updateString(id.ambientTemp, it.toString())
                }

                metric.atmPressure?.let {
                    results.atmPressure = it
                    entry.bestAtmPressure = it
                    Prefs.updateString(id.atmPressure, it.toString())
                }

                Prefs.updateString(id.best, metric.time.toString())
            }
        }
    }
}