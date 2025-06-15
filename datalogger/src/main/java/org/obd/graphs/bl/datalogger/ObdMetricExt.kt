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
package org.obd.graphs.bl.datalogger

import org.obd.metrics.api.model.ObdMetric

fun ObdMetric.isAtmPressure(): Boolean = command.pid.id == Pid.ATM_PRESSURE_PID_ID.id

fun ObdMetric.isAmbientTemp(): Boolean = command.pid.id == Pid.AMBIENT_TEMP_PID_ID.id

fun ObdMetric.isVehicleStatus(): Boolean = command.pid.id == Pid.VEHICLE_STATUS_PID_ID.id

fun ObdMetric.isDynamicSelector(): Boolean = command.pid.id == Pid.DYNAMIC_SELECTOR_PID_ID.id

fun ObdMetric.isVehicleSpeed(): Boolean =
    command.pid.id ==
        (if (dataLoggerPreferences.instance.gmeExtensionsEnabled) Pid.EXT_VEHICLE_SPEED_PID_ID else Pid.VEHICLE_SPEED_PID_ID).id

fun ObdMetric.isEngineRpm(): Boolean =
    command.pid.id ==
        (if (dataLoggerPreferences.instance.gmeExtensionsEnabled) Pid.EXT_ENGINE_SPEED_PID_ID else Pid.ENGINE_SPEED_PID_ID).id
