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

 import org.obd.graphs.bl.datalogger.PidId
 import org.obd.graphs.bl.datalogger.dataLoggerPreferences

 internal class DragRacingQueryStrategy : QueryStrategy() {
    override fun getPIDs(): MutableSet<Long> {
        return if (dataLoggerPreferences.instance.gmeExtensionsEnabled) {
            val pids =  mutableSetOf(
                PidId.EXT_VEHICLE_SPEED_PID_ID,
                PidId.EXT_ENGINE_RPM_PID_ID,
                PidId.EXT_MEASURED_INTAKE_PRESSURE_PID_ID,
                PidId.EXT_ATM_PRESSURE_PID_ID,
                PidId.EXT_AMBIENT_TEMP_PID_ID,
            )
            if (dataLoggerPreferences.instance.stnExtensionsEnabled){
                pids.add(PidId.ENGINE_TORQUE_PID_ID)
                pids.add(PidId.GAS_PID_ID)
            }
            pids.map { it.value }.toMutableSet()
        } else {
            mutableSetOf(
                PidId.VEHICLE_SPEED_PID_ID.value,
                PidId.ENGINE_RPM_PID_ID.value
            )
        }
    }
}
