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
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getLongSet

private const val PERFORMANCE_QUERY_PREF_KEY = "pref.aa.performance.pids.selected"
const val PREF_QUERY_PERFORMANCE_TOP = "pref.query.performance.top"
const val PREF_QUERY_PERFORMANCE_BOTTOM = "pref.query.performance.bottom"

const val PREF_QUERY_PERFORMANCE_BRAKE_BOOSTING_GAS =
    "pref.query.performance.break_boosting.gas_pid"
const val PREF_QUERY_PERFORMANCE_BRAKE_BOOSTING_TORQUE =
    "pref.query.performance.break_boosting.torque_pid"

internal class PerformanceQueryStrategy : QueryStrategy() {
    override fun getDefaultPIDs() =
        Prefs.getLongSet(PREF_QUERY_PERFORMANCE_TOP) +
            Prefs.getLongSet(PREF_QUERY_PERFORMANCE_BOTTOM) +
            Prefs.getInt(PREF_QUERY_PERFORMANCE_BRAKE_BOOSTING_GAS, -1).toLong() +
            Prefs.getInt(PREF_QUERY_PERFORMANCE_BRAKE_BOOSTING_TORQUE, -1).toLong()

    override fun getPIDs() = Prefs.getLongSet(PERFORMANCE_QUERY_PREF_KEY)

    init {
        Log.i("PerformanceQueryStrategy", "Read defaults=${getDefaultPIDs()}")
    }
}
