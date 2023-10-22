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
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.updateString

private const val LOG_KEY = "DragRaceRegistry"
private const val PERF_0_60_BEST = "pref.drag_race.best.0_60"
private const val PERF_0_100_BEST = "pref.drag_race.best.0_100"
private const val PERF_0_160_BEST = "pref.drag_race.best.0_160"
private const val PERF_100_200_BEST = "pref.drag_race.best.100_200"


private const val PERF_0_60_LAST = "pref.drag_race.last.0_60"
private const val PERF_0_100_LAST = "pref.drag_race.last.0_100"
private const val PERF_0_160_LAST = "pref.drag_race.last.0_160"
private const val PERF_100_200_LAST = "pref.drag_race.last.100_200"


internal class DragRaceResultRegistryImpl : DragRaceResultRegistry {

    private val results = DragRaceResults()

    init {

        Prefs.getString(PERF_0_60_BEST, null)?.let {
            results.best._0_60ms = it.toLong()
        }
        Prefs.getString(PERF_0_100_BEST, null)?.let {
            results.best._0_100ms = it.toLong()
        }
        Prefs.getString(PERF_0_160_BEST, null)?.let {
            results.best._0_160ms = it.toLong()
        }

        Prefs.getString(PERF_100_200_BEST, null)?.let {
            results.best._100_200ms = it.toLong()
        }

        Prefs.getString(PERF_0_60_LAST, null)?.let {
            results.last._0_60ms = it.toLong()
        }
        Prefs.getString(PERF_0_100_LAST, null)?.let {
            results.last._0_100ms = it.toLong()
        }

        Prefs.getString(PERF_0_160_LAST, null)?.let {
            results.last._0_160ms = it.toLong()
        }

        Prefs.getString(PERF_100_200_LAST, null)?.let {
            results.last._100_200ms = it.toLong()
        }
    }

    override fun readyToRace(value: Boolean) {
        results.readyToRace = value
    }

    override fun update0100(time: Long, speed: Int) {
        if (time == 0L) {
            Log.v(LOG_KEY, "Invalid value")
        } else {
            results.last._0_100ms = if (results.last._0_100ms == VALUE_NOT_SET) {
                time
            } else {
                results.current._0_100ms
            }

            Prefs.updateString(PERF_0_100_LAST, results.last._0_100ms.toString())

            results.current._0_100ms = time
            results.current._0_100speed = speed

            if (results.best._0_100ms > time || results.best._0_100ms == VALUE_NOT_SET) {
                results.best._0_100ms = time
                Prefs.updateString(PERF_0_100_BEST, time.toString())
            }
        }
    }

    override fun update060(time: Long, speed: Int) {
        if (time == 0L) {
            Log.v(LOG_KEY, "Invalid value")
        } else {
            results.last._0_60ms = if (results.last._0_60ms == VALUE_NOT_SET) {
                time
            } else {
                results.current._0_60ms
            }

            Prefs.updateString(PERF_0_60_LAST, results.last._0_60ms.toString())

            results.current._0_60ms = time
            results.current._0_60speed = speed

            if (results.best._0_60ms > time || results.best._0_60ms == VALUE_NOT_SET) {
                results.best._0_60ms = time
                Prefs.updateString(PERF_0_60_BEST, time.toString())
            }
        }
    }

    override fun update0160(time: Long, speed: Int) {
        if (time == 0L) {
            Log.v(LOG_KEY, "Invalid value")
        } else {
            results.last._0_160ms = if (results.last._0_160ms == VALUE_NOT_SET) {
                time
            } else {
                results.current._0_160ms
            }

            Prefs.updateString(PERF_0_160_LAST, results.last._0_160ms.toString())

            results.current._0_160ms = time
            results.current._0_160speed = speed

            if (results.best._0_160ms > time || results.best._0_160ms == VALUE_NOT_SET) {
                results.best._0_160ms = time
                Prefs.updateString(PERF_0_160_BEST, time.toString())
            }
        }
    }

    override fun update100200(time: Long, speed: Int) {
        if (time == 0L) {
            Log.v(LOG_KEY, "Invalid value")
        } else {

            results.last._100_200ms = if (results.last._100_200ms == VALUE_NOT_SET) {
                time
            } else {
                results.current._100_200ms
            }

            Prefs.updateString(PERF_100_200_LAST, results.last._100_200ms.toString())

            results.current._100_200ms = time
            results.current._100_200speed = speed

            if (results.best._100_200ms > time || results.best._100_200ms == VALUE_NOT_SET) {
                results.best._100_200ms = time
                Prefs.updateString(PERF_100_200_BEST, time.toString())
            }
        }
    }

    override fun getResult(): DragRaceResults = results
}