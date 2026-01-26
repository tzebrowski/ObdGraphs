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
import org.obd.graphs.bl.datalogger.MetricsProcessor
import org.obd.graphs.bl.datalogger.dataLoggerSettings
import org.obd.graphs.getContext
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.api.model.Reply
import org.obd.metrics.api.model.ReplyObserver
import org.obd.metrics.command.obd.ObdCommand
import org.obd.metrics.pid.PidDefinition
import org.obd.metrics.pid.ValueType
import org.obd.metrics.transport.message.ConnectorResponse

private const val TAG = "GpsMetricsProcessor"

val gpsMetricsProcessor: MetricsProcessor =  GpsMetricsProcessor()

internal class GpsMetricsProcessor : MetricsProcessor {

    private val raw = object : ConnectorResponse {
        override fun at(p0: Int): Byte = "".toByte()
        override fun capacity(): Long = 0
        override fun remaining(): Int = 0
    }

    private var currentLocation: Location? = null

    private var replyObserver: ReplyObserver<Reply<*>>? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val latPid = ObdCommand(PidDefinition(9977771L, 2,"","", "Lat", "deg", "GPS Latitude", -90, 90, ValueType.DOUBLE))
    private val lonPid = ObdCommand(PidDefinition(9977772L, 2,"","", "Lon", "deg", "GPS Longitude", -180, 180, ValueType.DOUBLE))
    private val altPid = ObdCommand(PidDefinition(9977773L, 2,"","", "Alt", "m", "GPS Altitude", -100, 10000, ValueType.DOUBLE))

    override fun init(replyObserver: ReplyObserver<Reply<*>>) {
        this.replyObserver = replyObserver
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

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext()!!)
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.locations.forEach { location ->
                        currentLocation = location
                        if (Log.isLoggable(TAG, Log.VERBOSE)) {
                            Log.v(TAG, "GPS Update: ${location.latitude}, ${location.longitude}")
                        }
                        emitMetric(latPid, location.latitude)
                        emitMetric(lonPid, location.longitude)
                        emitMetric(altPid, location.altitude)
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
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun postValue(obdMetric: ObdMetric) {

        val loc = currentLocation ?: return
        if (obdMetric.command.pid.id in 9977771L..9977773L) {
            return
        }

        emitMetric(latPid, loc.latitude)
        emitMetric(lonPid, loc.longitude)
        emitMetric(altPid, loc.altitude)
    }

    private fun emitMetric(command: ObdCommand, value: Number) =
        replyObserver?.onNext(ObdMetric.builder()
            .command(command)
            .value(value)
            .raw(raw)
            .build())
}
