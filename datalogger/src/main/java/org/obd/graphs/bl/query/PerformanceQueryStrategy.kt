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

 import org.obd.graphs.preferences.Prefs
 import org.obd.graphs.preferences.getLongSet

 private const val PERFORMANCE_QUERY_PREF_KEY = "pref.aa.performance.pids.selected"

 internal class PerformanceQueryStrategy : QueryStrategy() {

    private val defaults  = mutableSetOf(
        namesRegistry.getAtmPressurePID(),
        namesRegistry.getAmbientTempPID(),
        namesRegistry.getGearboxOilTempPID(),
        namesRegistry.getOilTempPID(),
        namesRegistry.getCoolantTempPID(),
        namesRegistry.getExhaustTempPID(),
        namesRegistry.getPostICAirTempPID(),
        namesRegistry.getTorquePID(),
        namesRegistry.getIntakePressurePID(),
        namesRegistry.getDynamicSelectorPID(),
        namesRegistry.getGasPedalPID(),
        namesRegistry.getWcaTempPID(),
        namesRegistry.getPreICAirPID(),
        namesRegistry.getVehicleSpeedPID(),
        namesRegistry.getGearEngagedPID()
    )
    override fun getDefaults() = defaults

    override fun getPIDs() = Prefs.getLongSet(PERFORMANCE_QUERY_PREF_KEY).toMutableSet()
}
