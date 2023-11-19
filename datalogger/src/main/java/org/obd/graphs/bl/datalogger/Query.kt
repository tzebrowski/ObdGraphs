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



import org.obd.graphs.bl.drag.dragRacingResultRegistry
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getStringSet

private const val PREFERENCE_PID_FAST = "pref.pids.generic.high"
private const val PREFERENCE_PID_SLOW = "pref.pids.generic.low"

class Query: java.io.Serializable {

    private val directMetrics = mutableSetOf<Long>()
    private var queryType: QueryType = QueryType.METRICS

    fun getPIDs(): MutableSet<Long> {
        return  when(queryType){
            QueryType.DIRECT_METRICS -> {
                directMetrics
            }
            QueryType.METRICS -> {
                (fastPIDs() + slowPIDs()).toMutableSet()
            }
            QueryType.DRAG_RACING -> {
                mutableSetOf(
                    dragRacingResultRegistry.getEngineRpmPID(),
                        dragRacingResultRegistry.getVehicleSpeedPID())
            }
        }
    }

    fun getQueryType(): QueryType = queryType

    fun setQueryType (queryType: QueryType){
        this.queryType = queryType
    }

    fun setDirectMetricsPIDs(newPIDs: Set<Long>){
        directMetrics.clear()
        directMetrics.addAll(newPIDs)
    }

    private fun fastPIDs() = Prefs.getStringSet(PREFERENCE_PID_FAST).map { s -> s.toLong() }
    private fun slowPIDs() = Prefs.getStringSet(PREFERENCE_PID_SLOW).mapNotNull {
        try {
            it.toLong()
        }catch (e: Exception){
            null
        }
    }
}