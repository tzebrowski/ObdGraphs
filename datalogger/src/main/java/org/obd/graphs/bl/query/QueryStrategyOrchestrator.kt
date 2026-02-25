 /**
 * Copyright 2019-2026, Tomasz Żebrowski
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
package org.obd.graphs.bl.query

import android.util.Log
import org.obd.graphs.bl.datalogger.Pid
import org.obd.graphs.bl.datalogger.dataLoggerSettings
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getLongSet

private const val TAG = "QueryOrchestrator"

internal class QueryStrategyOrchestrator :
    java.io.Serializable,
    Query {
    private val strategies: Map<QueryStrategyType, QueryStrategy> = mapOf(
        QueryStrategyType.SHARED_QUERY to SharedQueryStrategy(),
        QueryStrategyType.DRAG_RACING_QUERY to DragRacingQueryStrategy(),
        QueryStrategyType.INDIVIDUAL_QUERY to IndividualQueryStrategy(),
        QueryStrategyType.ROUTINES_QUERY to RoutinesQueryStrategy(),
        QueryStrategyType.TRIP_INFO_QUERY to TripInfoQueryStrategy(),
        QueryStrategyType.PERFORMANCE_QUERY to PerformanceQueryStrategy()
    )

    private var strategy: QueryStrategyType = QueryStrategyType.SHARED_QUERY

    override fun getDefaultPIDs(): Set<Long> = strategies[strategy]?.getDefaultPIDs() ?: emptySet()

    override fun getIDs(): MutableSet<Long> {
        val pids = strategies[strategy]?.getPIDs() ?: mutableSetOf()

        if (dataLoggerSettings.instance().vehicleStatusPanelEnabled ||
            dataLoggerSettings.instance().vehicleStatusDisconnectWhenOff
        ) {
            pids.add(Pid.VEHICLE_STATUS_PID_ID.id)
        }
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "${currentThread()} Gets PIDs '$pids' for current strategy $strategy")
        }

        return pids.filter { it != Pid.GPS_LOCATION_PID_ID.id }.toMutableSet()
    }

    override fun getStrategy(): QueryStrategyType = strategy

    override fun setStrategy(queryStrategyType: QueryStrategyType): Query {
        if (Log.isLoggable(TAG,Log.DEBUG)) {
            Log.d(TAG, "${currentThread()} Sets new strategy $queryStrategyType")
        }
        this.strategy = queryStrategyType
        return this
    }

    override fun update(newPIDs: Set<Long>): Query {
        if (Log.isLoggable(TAG,Log.DEBUG)) {
            Log.d(TAG, "${currentThread()} Updating query  for $strategy. New PIDs $newPIDs")
        }

        strategies[strategy]?.update(newPIDs)
        return this
    }

    override fun filterBy(filter: String): Set<Long> {
        val query = getIDs()
        val selection = Prefs.getLongSet(filter)
        val intersection = selection.intersect(query)

        if (Log.isLoggable(TAG,Log.DEBUG)) {
            Log.d(
                TAG,
                "${currentThread()} Individual query enabled:${isIndividualQuerySelected()}, " +
                        " key:$filter, query=$query,selection=$selection, intersection=$intersection",
            )
        }

        return if (isIndividualQuerySelected()) {
            if (Log.isLoggable(TAG,Log.DEBUG)) {
                Log.d(TAG, "${currentThread()} Returning selection=$selection")
            }

            selection
        } else {
            Log.i(TAG, "${currentThread()} Returning intersection=$intersection")
            intersection
        }
    }

    private fun currentThread() = "[${Thread.currentThread().id}, ${hashCode()}]"

    override fun apply(filter: String): Query =
        if (isIndividualQuerySelected()) {
            setStrategy(QueryStrategyType.INDIVIDUAL_QUERY)
                .update(filterBy(filter))
        } else {
            setStrategy(QueryStrategyType.SHARED_QUERY)
        }

    override fun apply(filter: Set<Long>): Query =
        if (isIndividualQuerySelected()) {
            setStrategy(QueryStrategyType.INDIVIDUAL_QUERY)
                .update(filter)
        } else {
            setStrategy(QueryStrategyType.SHARED_QUERY)
        }

    private fun isIndividualQuerySelected() = dataLoggerSettings.instance().adapter.individualQueryStrategyEnabled
}
