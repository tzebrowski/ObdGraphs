 /**
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
package org.obd.graphs.bl.gps

import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import org.obd.graphs.bl.datalogger.LOG_TAG
import org.obd.graphs.bl.datalogger.MetricsProcessor
import org.obd.graphs.bl.datalogger.Pid
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.bl.datalogger.dataLoggerSettings
import org.obd.graphs.getContext
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.api.model.Reply
import org.obd.metrics.api.model.ReplyObserver
import org.obd.metrics.command.obd.ObdCommand
import org.obd.metrics.transport.message.ConnectorResponse

private const val TAG = "GpsMetricsEmitter"
private const val MIN_EMISSION_INTERVAL = 1000L // Limit updates to 10Hz max

val gpsMetricsEmitter: MetricsProcessor = GpsMetricsEmitter()

internal class GpsMetricsEmitter : MetricsProcessor {

    private val raw = object : ConnectorResponse {
        override fun at(p0: Int): Byte = "".toByte()
        override fun capacity(): Long = 0
        override fun remaining(): Int = 0
    }

    private var currentLocation: Location? = null
    // Rate Limiter State
    private var lastEmissionTime: Long = 0L

    private var replyObserver: ReplyObserver<Reply<*>>? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private lateinit var latitudeCommand: ObdCommand
    private lateinit var longitudeCommand: ObdCommand
    private lateinit var altitudeCommand: ObdCommand

    override fun init(replyObserver: ReplyObserver<Reply<*>>) {
        this.replyObserver = replyObserver
        latitudeCommand = ObdCommand(dataLogger.getPidDefinitionRegistry().findBy(Pid.GPS_LAT_PID_ID.id))
        longitudeCommand = ObdCommand(dataLogger.getPidDefinitionRegistry().findBy(Pid.GPS_LON_PID_ID.id))
        altitudeCommand = ObdCommand(dataLogger.getPidDefinitionRegistry().findBy(Pid.GPS_ALT_PID_ID.id))
    }

    @SuppressLint("MissingPermission")
    override fun onRunning(vehicleCapabilities: org.obd.metrics.api.model.VehicleCapabilities?) {
        try {
            if (!dataLoggerSettings.instance().adapter.gpsCollecetingEnabled){
                Log.i(TAG,"GPS collector won't be registered")
                currentLocation = null
                return
            }

            Log.i(TAG, "Starting GPS updates")

            // Initialize client here to ensure context is valid
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext()!!)
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.locations.forEach { location ->
                        currentLocation = location
                        if (Log.isLoggable(TAG, Log.VERBOSE)) {
                            Log.v(TAG, "GPS Update: ${location.latitude}, ${location.longitude}")
                        }
                        // Direct GPS updates (from hardware) are always emitted immediately
                        emitMetric(latitudeCommand, location.latitude)
                        emitMetric(longitudeCommand, location.longitude)
                        emitMetric(altitudeCommand, location.altitude)
                    }
                }
            }

            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500)
                .setMinUpdateIntervalMillis(500)
                .build()

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start GPS", e)
        }
    }

    override fun onStopped() {
        Log.i(TAG, "Stopping GPS updates")
        // Check if initialized to avoid crash if onRunning failed
        if (::fusedLocationClient.isInitialized && ::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    override fun postValue(obdMetric: ObdMetric) {
        val loc = currentLocation ?: return

        // Guard: Don't process our own GPS metrics (infinite loop prevention)
        if (obdMetric.command.pid.id in 9977771L..9977773L) {
            return
        }

        // Rate Limiter: Only re-emit GPS data if enough time has passed
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastEmissionTime < MIN_EMISSION_INTERVAL) {
            return
        }
        lastEmissionTime = currentTime

        // Re-emit the last known location
        emitMetric(latitudeCommand, loc.latitude)
        emitMetric(longitudeCommand, loc.longitude)
        emitMetric(altitudeCommand, loc.altitude)
    }

    private fun emitMetric(command: ObdCommand, value: Number) =
        replyObserver?.onNext(ObdMetric.builder()
            .command(command)
            .value(value)
            .raw(raw)
            .build())
}
