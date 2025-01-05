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
const val EVENT_VEHICLE_STATUS_VEHICLE_ACCELERATING = "event.vehicle.status.vehicle.accelerating"
const val EVENT_VEHICLE_STATUS_VEHICLE_DECELERATING = "event.vehicle.status.vehicle.decelerating"

val vehicleStatusEventBroadcaster:MetricsProcessor  = VehicleStatusEventBroadcaster()

private const val LOG_TAG = "VehicleStatBroad"

internal class VehicleStatusEventBroadcaster: MetricsProcessor {
    private var currVehicleRunning = false
    private var currEngineRunning = false
    private var currKeyStatusOn = false
    private var currVehicleAccelerating = false
    private var currVehicleDecelerating = false


    override fun postValue(obdMetric: ObdMetric) {

        if (dataLoggerPreferences.instance.vehicleStatusReading && obdMetric.isVehicleStatus()) {

            if (Log.isLoggable(LOG_TAG, Log.VERBOSE)) {
                Log.v(LOG_TAG, "Received=${obdMetric.value}, ")
            }
            val value: Map<String, Boolean> = obdMetric.value as Map<String, Boolean>
            val engineRunning = value["engine.running"]!!
            val vehicleRunning = value["vehicle.running"]!!
            val keyStatusOn = value["key.status"]!!

            if (currVehicleRunning != vehicleRunning || currEngineRunning != engineRunning || currKeyStatusOn != keyStatusOn) {
                currVehicleRunning = vehicleRunning
                currEngineRunning = engineRunning
                currKeyStatusOn = keyStatusOn
                currVehicleAccelerating = value["vehicle.accelerating"]!!
                currVehicleDecelerating = value["vehicle.decelerating"]!!

                if (currEngineRunning && currVehicleRunning){
                    sendBroadcastEvent(EVENT_VEHICLE_STATUS_VEHICLE_MOVING)
                }else if (currEngineRunning && !currVehicleRunning){
                    sendBroadcastEvent(EVENT_VEHICLE_STATUS_VEHICLE_IDLING)
                }else if (!currEngineRunning && !currVehicleRunning && !currKeyStatusOn){
                    sendBroadcastEvent(EVENT_VEHICLE_STATUS_IGNITION_OFF)
                }else if (currVehicleAccelerating){
                    sendBroadcastEvent(EVENT_VEHICLE_STATUS_VEHICLE_ACCELERATING)
                }else if (currVehicleDecelerating){
                    sendBroadcastEvent(EVENT_VEHICLE_STATUS_VEHICLE_DECELERATING)
                }
            }
        }
    }
}