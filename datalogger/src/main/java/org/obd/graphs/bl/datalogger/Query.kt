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

enum class QueryStrategyType {
    DRAG_RACING_QUERY, SHARED_QUERY, INDIVIDUAL_QUERY_FOR_EACH_VIEW
}

private const val PREFERENCE_PID_FAST = "pref.pids.generic.high"
private const val PREFERENCE_PID_SLOW = "pref.pids.generic.low"

class QueryStrategy(private val pids: MutableSet<Long> = mutableSetOf()) : java.io.Serializable {
    fun update(newPIDs: Set<Long>) {
        pids.clear()
        pids.addAll(newPIDs)
    }

    fun getPIDs(): MutableSet<Long> = pids
}

class Query : java.io.Serializable {

    private val strategies: Map<QueryStrategyType, QueryStrategy> = mutableMapOf<QueryStrategyType, QueryStrategy>().apply {
        this[QueryStrategyType.SHARED_QUERY] = QueryStrategy((fastPIDs() + slowPIDs()).toMutableSet())
        this[QueryStrategyType.DRAG_RACING_QUERY] =
            QueryStrategy(mutableSetOf(dragRacingResultRegistry.getEngineRpmPID(), dragRacingResultRegistry.getVehicleSpeedPID()))
        this[QueryStrategyType.INDIVIDUAL_QUERY_FOR_EACH_VIEW] = QueryStrategy()
    }

    private var strategy: QueryStrategyType = QueryStrategyType.SHARED_QUERY

    fun getPIDs(): MutableSet<Long> = strategies[strategy]?.getPIDs() ?: mutableSetOf()

    fun getStrategy(): QueryStrategyType = strategy

    fun setStrategy(queryStrategyType: QueryStrategyType) {
        this.strategy = queryStrategyType
    }

    fun update(newPIDs: Set<Long>) {
        strategies[strategy]?.update(newPIDs)
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