/*
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

import org.obd.graphs.bl.datalogger.Pid

// What the readiness screen polls: both monitor statuses plus the "since codes cleared" / "MIL on" counters. Fixed -
// the screen has no PID selection. Six PIDs, so a mode 01 batch fits them in one request.
val READINESS_PIDS =
    setOf(
        Pid.READINESS_SINCE_CLEAR_PID_ID.id,
        Pid.READINESS_DRIVE_CYCLE_PID_ID.id,
        Pid.DISTANCE_SINCE_CODES_CLEARED_PID_ID.id,
        Pid.TIME_SINCE_CODES_CLEARED_PID_ID.id,
        Pid.DISTANCE_MIL_ON_PID_ID.id,
        Pid.TIME_MIL_ON_PID_ID.id
    )

internal class ReadinessQueryStrategy : QueryStrategy() {
    override fun getDefaultPIDs() = READINESS_PIDS

    override fun getPIDs() = READINESS_PIDS.toMutableSet()
}
