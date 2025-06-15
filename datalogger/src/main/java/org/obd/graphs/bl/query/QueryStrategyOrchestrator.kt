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
package org.obd.graphs.bl.query

import android.util.Log
import org.obd.graphs.bl.datalogger.Pid
import org.obd.graphs.bl.datalogger.dataLoggerPreferences
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getLongSet
import org.obd.graphs.runAsync

private const val LOG_KEY = "query"

internal class QueryStrategyOrchestrator : java.io.Serializable, Query {

    private val strategies: Map<QueryStrategyType, QueryStrategy> = mutableMapOf<QueryStrategyType, QueryStrategy>().apply {
        runAsync {
            this[QueryStrategyType.SHARED_QUERY] = SharedQueryStrategy()
            this[QueryStrategyType.DRAG_RACING_QUERY] = DragRacingQueryStrategy()
            this[QueryStrategyType.INDIVIDUAL_QUERY] = IndividualQueryStrategy()
            this[QueryStrategyType.ROUTINES_QUERY] = RoutinesQueryStrategy()
            this[QueryStrategyType.TRIP_INFO_QUERY] = TripInfoQueryStrategy()
            this[QueryStrategyType.PERFORMANCE_QUERY] = PerformanceQueryStrategy()
        }
    }

    private var strategy: QueryStrategyType = QueryStrategyType.SHARED_QUERY
    override fun getDefaults(): Set<Long> = strategies[strategy]?.getDefaults() ?: emptySet()

    override fun getIDs(): MutableSet<Long>  {
        val pids = strategies[strategy]?.getPIDs() ?: mutableSetOf()
        //decorate with Vehicle Status PID
        if (dataLoggerPreferences.instance.vehicleStatusPanelEnabled ||
            dataLoggerPreferences.instance.vehicleStatusDisconnectWhenOff){
            pids.add(Pid.VEHICLE_STATUS_PID_ID.id)
        }
        return pids
    }

    override fun getStrategy(): QueryStrategyType = strategy

    override fun setStrategy(queryStrategyType: QueryStrategyType): Query {
        this.strategy = queryStrategyType
        return this
    }

    override fun update(newPIDs: Set<Long>): Query {
        strategies[strategy]?.update(newPIDs)
        return this
    }

    override fun filterBy(filter: String): Set<Long> {
        val query = getIDs()
        val selection = Prefs.getLongSet(filter)
        val intersection =  selection.filter { query.contains(it) }.toSet()

        Log.i(LOG_KEY,"Individual query enabled:${isIndividualQuerySelected()}, " +
                " key:$filter, query=$query,selection=$selection, intersection=$intersection")

        return if (isIndividualQuerySelected()) {
            Log.i(LOG_KEY,"Returning selection=$selection")
            selection
        } else {
            Log.i(LOG_KEY,"Returning intersection=$intersection")
            intersection
        }
    }
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

    private fun isIndividualQuerySelected() = dataLoggerPreferences.instance.individualQueryStrategyEnabled
}
