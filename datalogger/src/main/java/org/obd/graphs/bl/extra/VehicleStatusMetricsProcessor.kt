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
package org.obd.graphs.bl.extra

import android.util.Log
import org.obd.graphs.bl.datalogger.MetricsProcessor
import org.obd.graphs.bl.datalogger.generalPreferences
import org.obd.graphs.bl.datalogger.isVehicleStatus
import org.obd.graphs.sendBroadcastEvent
import org.obd.metrics.api.model.ObdMetric

const val EVENT_VEHICLE_STATUS_VEHICLE_RUNNING = "event.vehicle.status.vehicle.running"
const val EVENT_VEHICLE_STATUS_VEHICLE_IDLING = "event.vehicle.status.vehicle.idling"
const val EVENT_VEHICLE_STATUS_IGNITION_OFF = "event.vehicle.status.vehicle.ignition_off"
const val EVENT_VEHICLE_STATUS_IGNITION_ON = "event.vehicle.status.vehicle.ignition_on"

const val EVENT_VEHICLE_STATUS_VEHICLE_ACCELERATING = "event.vehicle.status.vehicle.accelerating"
const val EVENT_VEHICLE_STATUS_VEHICLE_DECELERATING = "event.vehicle.status.vehicle.decelerating"

val vehicleStatusMetricsProcessor: MetricsProcessor = VehicleStatusMetricsProcessor()

private const val LOG_TAG = "VehicleStatProcessor"

internal class VehicleStatusMetricsProcessor : MetricsProcessor {
    private var currentVehicleRunning = false
    private var currentEngineRunning = false
    private var currentKeyStatusOn = false
    private var currentVehicleAccelerating = false
    private var currentVehicleDecelerating = false

    override fun postValue(obdMetric: ObdMetric) {

        if ((generalPreferences.instance().vehicleStatusPanelEnabled ||
                    generalPreferences.instance().vehicleStatusDisconnectWhenOff)
            && obdMetric.isVehicleStatus()) {

            if (Log.isLoggable(LOG_TAG, Log.VERBOSE)) {
                Log.v(LOG_TAG, "Received vehicle status=${obdMetric.value}, ")
            }
            val value: Map<String, Boolean> = obdMetric.value as Map<String, Boolean>
            val engineRunning = value["engine.running"]!!
            val vehicleRunning = value["vehicle.running"]!!
            val keyStatusOn = value["key.status"]!!

            if (currentVehicleRunning != vehicleRunning || currentEngineRunning != engineRunning || currentKeyStatusOn != keyStatusOn) {
                currentVehicleRunning = vehicleRunning
                currentEngineRunning = engineRunning
                currentKeyStatusOn = keyStatusOn
                currentVehicleAccelerating = value["vehicle.accelerating"]!!
                currentVehicleDecelerating = value["vehicle.decelerating"]!!

                if (currentEngineRunning && currentVehicleRunning) {
                    sendBroadcastEvent(EVENT_VEHICLE_STATUS_VEHICLE_RUNNING)
                }

                if (currentEngineRunning && !currentVehicleRunning) {
                    sendBroadcastEvent(EVENT_VEHICLE_STATUS_VEHICLE_IDLING)
                }

                if (!currentEngineRunning && !currentVehicleRunning && !currentKeyStatusOn) {
                    sendBroadcastEvent(EVENT_VEHICLE_STATUS_IGNITION_OFF)
                }

                if (currentVehicleAccelerating) {
                    sendBroadcastEvent(EVENT_VEHICLE_STATUS_VEHICLE_ACCELERATING)
                }

                if (currentVehicleDecelerating) {
                    sendBroadcastEvent(EVENT_VEHICLE_STATUS_VEHICLE_DECELERATING)
                }

                if (!currentEngineRunning && !currentVehicleRunning && currentKeyStatusOn) {
                    sendBroadcastEvent(EVENT_VEHICLE_STATUS_IGNITION_ON)
                }
            }
        }
    }
}
