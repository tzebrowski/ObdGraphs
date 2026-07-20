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
package org.obd.graphs.bl.datalogger

import org.obd.graphs.NEW_RANGE_MAX_VAL
import org.obd.graphs.NEW_RANGE_MIN_VAL
import org.obd.graphs.USER_CUSTOM_PIDS_FILE
import org.obd.graphs.mapRange
import org.obd.graphs.toFloat
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.pid.PidDefinition

/**
 * Extension property to easily check if a PidDefinition is user-created.
 */
val PidDefinition.isUserCustom: Boolean
    get() = resourceFile == USER_CUSTOM_PIDS_FILE

fun PidDefinition.scaleToRange(value: Float): Float =
    value.mapRange(
        NEW_RANGE_MIN_VAL,
        NEW_RANGE_MAX_VAL,
        min?.toFloat() ?: 0f,
        max?.toFloat() ?: 9999f
    )

fun ObdMetric.scaleToRange(): Float =
    toFloat().mapRange(
        command.pid?.min?.toFloat() ?: 0f,
        command.pid?.max?.toFloat() ?: 9999f,
        NEW_RANGE_MIN_VAL,
        NEW_RANGE_MAX_VAL
    )

fun ObdMetric.isAtmPressure(): Boolean = command.pid.id == Pid.ATM_PRESSURE_PID_ID.id

fun ObdMetric.isAmbientTemp(): Boolean = command.pid.id == Pid.AMBIENT_TEMP_PID_ID.id

fun ObdMetric.isVehicleStatus(): Boolean = command.pid.id == Pid.VEHICLE_STATUS_PID_ID.id

fun ObdMetric.isDynamicSelector(): Boolean = command.pid.id == Pid.DYNAMIC_SELECTOR_PID_ID.id

fun ObdMetric.isVehicleSpeed(): Boolean =
    command.pid.id ==
        (if (dataLoggerSettings.instance().gmeExtensionsEnabled) Pid.EXT_VEHICLE_SPEED_PID_ID else Pid.VEHICLE_SPEED_PID_ID).id

fun ObdMetric.isEngineRpm(): Boolean =
    command.pid.id ==
        (if (dataLoggerSettings.instance().gmeExtensionsEnabled) Pid.EXT_ENGINE_SPEED_PID_ID else Pid.ENGINE_SPEED_PID_ID).id
