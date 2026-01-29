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
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import org.obd.graphs.Permissions
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
private const val UPDATE_INTERVAL_MS = 100L // 10Hz updates (100ms)

val gpsMetricsEmitter: MetricsProcessor = GpsMetricsEmitter()

internal class GpsMetricsEmitter : MetricsProcessor {

    private val raw = object : ConnectorResponse {
        override fun at(p0: Int): Byte = "".toByte()
        override fun capacity(): Long = 0
        override fun remaining(): Int = 0
    }

    private var replyObserver: ReplyObserver<Reply<*>>? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null

    private lateinit var latitudeCommand: ObdCommand
    private lateinit var longitudeCommand: ObdCommand
    private lateinit var altitudeCommand: ObdCommand

    override fun init(replyObserver: ReplyObserver<Reply<*>>) {
        this.replyObserver = replyObserver

        val registry = dataLogger.getPidDefinitionRegistry()
        latitudeCommand = ObdCommand(registry.findBy(Pid.GPS_LAT_PID_ID.id))
        longitudeCommand = ObdCommand(registry.findBy(Pid.GPS_LON_PID_ID.id))
        altitudeCommand = ObdCommand(registry.findBy(Pid.GPS_ALT_PID_ID.id))
    }

    @SuppressLint("MissingPermission")
    override fun onRunning(vehicleCapabilities: org.obd.metrics.api.model.VehicleCapabilities?) {
        val context = getContext()

        if (context == null) {
            Log.e(TAG, "Context is null, cannot start GPS collector")
            return
        }

        if (!dataLoggerSettings.instance().adapter.gpsCollecetingEnabled) {
            Log.i(TAG, "GPS collection disabled in settings.")
            return
        }

        if (!Permissions.hasLocationPermissions(context)) {
            Log.w(TAG, "Missing Location Permissions. GPS collector will not start.")
            return
        }

        try {
            Log.i(TAG, "Starting GPS updates with interval: ${UPDATE_INTERVAL_MS}ms (10Hz)")

            if (fusedLocationClient == null) {
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            }

            if (locationCallback == null) {
                locationCallback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        for (location in locationResult.locations) {
                            val lat = location.latitude
                            val lon = location.longitude
                            val alt = location.altitude

                            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                                Log.v(TAG, "GPS Fix: $lat, $lon")
                            }

                            if (lat == 0.0 && lon == 0.0) {
                                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                                    Log.v(TAG, "Ignoring Invalid GPS Fix (0.0, 0.0)")
                                }
                                continue
                            }

                            if (::latitudeCommand.isInitialized) emitMetric(latitudeCommand, lat)
                            if (::longitudeCommand.isInitialized) emitMetric(longitudeCommand, lon)
                            if (::altitudeCommand.isInitialized) emitMetric(altitudeCommand, alt)
                        }
                    }
                }
            }

            // High Accuracy + 100ms interval for 10Hz
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
                .setMinUpdateIntervalMillis(UPDATE_INTERVAL_MS) // Enforce strict 100ms limit
                .build()

            fusedLocationClient?.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start GPS updates", e)
        }
    }

    override fun onStopped() {
        Log.i(TAG, "Stopping GPS updates")
        try {
            if (fusedLocationClient != null && locationCallback != null) {
                fusedLocationClient?.removeLocationUpdates(locationCallback!!)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping GPS updates", e)
        }
    }

    override fun postValue(obdMetric: ObdMetric) {
        // No-op to prevent duplicate logging
    }

    private fun emitMetric(command: ObdCommand, value: Number) {
        replyObserver?.onNext(ObdMetric.builder()
            .command(command)
            .value(value)
            .raw(raw)
            .build())
    }
}
