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
package org.obd.graphs.bl.query

import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getLongSet


private const val TRIP_INFO_QUERY_PREF_KEY = "pref.aa.trip_info.pids.selected"

internal class TripInfoQueryStrategy : QueryStrategy() {

    private val defaults  = mutableSetOf(
        namesRegistry.getFuelConsumptionPID(),
        namesRegistry.getFuelLevelPID(),
        namesRegistry.getAtmPressurePID(),
        namesRegistry.getAmbientTempPID(),
        namesRegistry.getGearboxOilTempPID(),
        namesRegistry.getOilTempPID(),
        namesRegistry.getCoolantTempPID(),
        namesRegistry.getExhaustTempPID(),
        namesRegistry.getAirTempPID(),
        namesRegistry.getTotalMisfiresPID(),
        namesRegistry.getOilLevelPID(),
        namesRegistry.getTorquePID(),
        namesRegistry.getIntakePressurePID(),
        namesRegistry.getDynamicSelectorPID(),
        namesRegistry.getDistancePID(),
        namesRegistry.getBatteryVoltagePID(),
        namesRegistry.getIbsPID(),
        namesRegistry.getOilPressurePID(),
    )
    override fun getDefaults() = defaults

    override fun getPIDs() = Prefs.getLongSet(TRIP_INFO_QUERY_PREF_KEY).toMutableSet()
}

