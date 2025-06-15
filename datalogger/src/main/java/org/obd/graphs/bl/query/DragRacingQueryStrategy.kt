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

import org.obd.graphs.bl.datalogger.Pid
import org.obd.graphs.bl.datalogger.dataLoggerPreferences

internal class DragRacingQueryStrategy : QueryStrategy() {
    override fun getPIDs(): MutableSet<Long> =
        (
            if (dataLoggerPreferences.instance.gmeExtensionsEnabled) {
                val pids =
                    mutableSetOf(
                        Pid.EXT_VEHICLE_SPEED_PID_ID,
                        Pid.EXT_ENGINE_SPEED_PID_ID,
                        Pid.INTAKE_PRESSURE_PID_ID,
                        Pid.ATM_PRESSURE_PID_ID,
                        Pid.AMBIENT_TEMP_PID_ID,
                    )
                if (dataLoggerPreferences.instance.stnExtensionsEnabled) {
                    pids.add(Pid.ENGINE_TORQUE_PID_ID)
                    pids.add(Pid.GAS_PID_ID)
                }
                pids
            } else {
                mutableSetOf(
                    Pid.VEHICLE_SPEED_PID_ID,
                    Pid.ENGINE_SPEED_PID_ID,
                )
            }
        ).map { it.id }.toMutableSet()
}
