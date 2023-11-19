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

enum class QueryStrategy {
    DRAG_RACING_QUERY, SHARED_QUERY, INDIVIDUAL_QUERY_FOR_EACH_VIEW
}

private const val PREFERENCE_PID_FAST = "pref.pids.generic.high"
private const val PREFERENCE_PID_SLOW = "pref.pids.generic.low"

class Query : java.io.Serializable {

    private val individualViewPIDs = mutableSetOf<Long>()
    private var strategy: QueryStrategy = QueryStrategy.SHARED_QUERY

    fun getPIDs(): MutableSet<Long> {
        return when (strategy) {
            QueryStrategy.INDIVIDUAL_QUERY_FOR_EACH_VIEW -> {
                individualViewPIDs
            }
            QueryStrategy.SHARED_QUERY -> {
                (fastPIDs() + slowPIDs()).toMutableSet()
            }
            QueryStrategy.DRAG_RACING_QUERY -> {
                mutableSetOf(
                    dragRacingResultRegistry.getEngineRpmPID(),
                    dragRacingResultRegistry.getVehicleSpeedPID()
                )
            }
        }
    }

    fun getStrategy(): QueryStrategy = strategy

    fun setStrategy(queryStrategy: QueryStrategy) {
        this.strategy = queryStrategy
    }

    fun setIndividualViewPIDs(newPIDs: Set<Long>) {
        individualViewPIDs.clear()
        individualViewPIDs.addAll(newPIDs)
    }

    private fun fastPIDs() = Prefs.getStringSet(PREFERENCE_PID_FAST).map { s -> s.toLong() }
    private fun slowPIDs() = Prefs.getStringSet(PREFERENCE_PID_SLOW).mapNotNull {
        try {
            it.toLong()
        } catch (e: Exception) {
            null
        }
    }
}