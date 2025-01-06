package org.obd.graphs.bl.extra

import android.util.Log
import org.obd.graphs.bl.datalogger.MetricsProcessor
import org.obd.graphs.bl.datalogger.dataLoggerPreferences
import org.obd.graphs.bl.query.isVehicleStatus
import org.obd.graphs.sendBroadcastEvent
import org.obd.metrics.api.model.ObdMetric

const val EVENT_VEHICLE_STATUS_VEHICLE_MOVING = "event.vehicle.status.vehicle.moving"
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

        if ((dataLoggerPreferences.instance.vehicleStatusPanelEnabled ||
                    dataLoggerPreferences.instance.vehicleStatusDisconnectWhenOff)
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
                    sendBroadcastEvent(EVENT_VEHICLE_STATUS_VEHICLE_MOVING)
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