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
package org.obd.graphs.bl.drag

import android.util.Log
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.updateString

private const val LOG_KEY = "DragRaceRegistry"
private const val PERF_0_60_BEST = "pref.drag_race.best.0_60"
private const val PERF_0_100_BEST = "pref.drag_race.best.0_100"
private const val PERF_0_160_BEST = "pref.drag_race.best.0_160"
private const val PERF_100_200_BEST = "pref.drag_race.best.100_200"
private const val PERF_60_140_BEST = "pref.drag_race.best.60_140"
private const val PERF_0_60_LAST = "pref.drag_race.last.0_60"
private const val PERF_0_100_LAST = "pref.drag_race.last.0_100"
private const val PERF_0_160_LAST = "pref.drag_race.last.0_160"
private const val PERF_60_140_LAST = "pref.drag_race.last.60_140"
private const val PERF_100_200_LAST = "pref.drag_race.last.100_200"

internal class InMemoryDragRacingRegistry : DragRacingResultRegistry {

    private val results = DragRacingResults()

    init {

        Prefs.getString(PERF_0_60_BEST, null)?.let {
            results._0_60.best = it.toLong()
        }
        Prefs.getString(PERF_0_100_BEST, null)?.let {
            results._0_100.best = it.toLong()
        }
        Prefs.getString(PERF_0_160_BEST, null)?.let {
            results._0_160.best = it.toLong()
        }

        Prefs.getString(PERF_100_200_BEST, null)?.let {
            results._100_200.best = it.toLong()
        }

        Prefs.getString(PERF_0_60_LAST, null)?.let {
            results._0_60.last = it.toLong()
        }
        Prefs.getString(PERF_0_100_LAST, null)?.let {
            results._0_100.last = it.toLong()
        }

        Prefs.getString(PERF_0_160_LAST, null)?.let {
            results._0_160.last = it.toLong()
        }

        Prefs.getString(PERF_100_200_LAST, null)?.let {
            results._100_200.last = it.toLong()
        }
    }

    override fun readyToRace(value: Boolean) {
        results.readyToRace = value
    }

    override fun enableShiftLights(value: Boolean) {
        results.enableShiftLights  = value
    }

    override fun update60140(time: Long, speed: Int) {
        if (time <= 0L) {
            Log.v(LOG_KEY, "Invalid value")
        } else {
            results._60_140.last = if (results._60_140.last == VALUE_NOT_SET) {
                time
            } else {
                results._60_140.current
            }

            Prefs.updateString(PERF_60_140_LAST, results._60_140.last.toString())

            results._60_140.current = time
            results._60_140.currentSpeed = speed

            if (results._60_140.best > time || results._60_140.best == VALUE_NOT_SET) {
                results._60_140.best = time
                Prefs.updateString(PERF_60_140_BEST, time.toString())
            }
        }
    }

    override fun update0100(time: Long, speed: Int) {
        if (time <= 0L) {
            Log.v(LOG_KEY, "Invalid value")
        } else {
            results._0_100.last = if (results._0_100.last == VALUE_NOT_SET) {
                time
            } else {
                results._0_100.current
            }

            Prefs.updateString(PERF_0_100_LAST, results._0_100.last.toString())

            results._0_100.current = time
            results._0_100.currentSpeed = speed

            if (results._0_100.best > time || results._0_100.best == VALUE_NOT_SET) {
                results._0_100.best = time
                Prefs.updateString(PERF_0_100_BEST, time.toString())
            }
        }
    }

    override fun update060(time: Long, speed: Int) {
        if (time <= 0L) {
            Log.v(LOG_KEY, "Invalid value")
        } else {
            results._0_60.last = if (results._0_60.last == VALUE_NOT_SET) {
                time
            } else {
                results._0_60.current
            }

            Prefs.updateString(PERF_0_60_LAST, results._0_60.last.toString())

            results._0_60.current = time
            results._0_60.currentSpeed = speed

            if (results._0_60.best > time || results._0_60.best == VALUE_NOT_SET) {
                results._0_60.best = time
                Prefs.updateString(PERF_0_60_BEST, time.toString())
            }
        }
    }

    override fun update0160(time: Long, speed: Int) {
        if (time <= 0L) {
            Log.v(LOG_KEY, "Invalid value")
        } else {
            results._0_160.last = if (results._0_160.last == VALUE_NOT_SET) {
                time
            } else {
                results._0_160.current
            }

            Prefs.updateString(PERF_0_160_LAST, results._0_160.last.toString())

            results._0_160.current = time
            results._0_160.currentSpeed = speed

            if (results._0_160.best > time || results._0_160.best == VALUE_NOT_SET) {
                results._0_160.best = time
                Prefs.updateString(PERF_0_160_BEST, time.toString())
            }
        }
    }

    override fun update100200(time: Long, speed: Int) {
        if (time <= 0L) {
            Log.v(LOG_KEY, "Invalid value")
        } else {

            results._100_200.last = if (results._100_200.last == VALUE_NOT_SET) {
                time
            } else {
                results._100_200.current
            }

            Prefs.updateString(PERF_100_200_LAST, results._100_200.last.toString())

            results._100_200.current = time
            results._100_200.currentSpeed = speed

            if (results._100_200.best > time || results._100_200.best == VALUE_NOT_SET) {
                results._100_200.best = time
                Prefs.updateString(PERF_100_200_BEST, time.toString())
            }
        }
    }

    override fun getResult(): DragRacingResults = results
}