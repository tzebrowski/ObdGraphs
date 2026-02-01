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
import android.content.Context
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import org.obd.graphs.LOCATION_IS_DISABLED
import org.obd.graphs.Permissions
import org.obd.graphs.bl.datalogger.MetricsProcessor
import org.obd.graphs.bl.datalogger.Pid
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.bl.datalogger.dataLoggerSettings
import org.obd.graphs.getContext
import org.obd.graphs.sendBroadcastEvent
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.api.model.Reply
import org.obd.metrics.api.model.ReplyObserver
import org.obd.metrics.api.model.VehicleCapabilities
import org.obd.metrics.command.obd.ObdCommand
import org.obd.metrics.transport.message.ConnectorResponse

private const val TAG = "GpsMetricsEmitter"
private const val MIN_TIME_MS = 1000L
private const val MIN_DISTANCE_M = 0.1f

val gpsMetricsEmitter: MetricsProcessor = GpsMetricsEmitter()

internal class GpsMetricsEmitter : MetricsProcessor {
    // Stateless dummy object for metrics
    private val emptyConnectorResponse =
        object : ConnectorResponse {
            override fun at(p0: Int): Byte = 0

            override fun capacity(): Long = 0

            override fun remaining(): Int = 0
        }

    private var replyObserver: ReplyObserver<Reply<*>>? = null
    private var locationManager: LocationManager? = null
    private var handlerThread: HandlerThread? = null

    // Listeners reference to allow proper unregistering
    private var locationListener: LocationListener? = null
    private var gnssCallback: GnssStatus.Callback? = null

    private lateinit var locationCommand: ObdCommand

    override fun init(replyObserver: ReplyObserver<Reply<*>>) {
        this.replyObserver = replyObserver
        val registry = dataLogger.getPidDefinitionRegistry()
        locationCommand = ObdCommand(registry.findBy(Pid.GPS_LOCATION_PID_ID.id))
    }

    @SuppressLint("MissingPermission")
    override fun onRunning(vehicleCapabilities: VehicleCapabilities?) {
        val context = getContext() ?: return

        // Guard Clauses
        if (!dataLoggerSettings.instance().adapter.gpsCollecetingEnabled) return
        if (!Permissions.hasLocationPermissions(context)) return
        if (!Permissions.isLocationEnabled(context)) {
            Log.w(TAG, "Location is disabled. Skipping")
            sendBroadcastEvent(LOCATION_IS_DISABLED)
            return
        }

        // Start Updates
        startGpsUpdates(context)
    }

    override fun onStopped() {
        stopGpsUpdates()
    }

    override fun postValue(obdMetric: ObdMetric) {
        // No-op
    }

    @SuppressLint("MissingPermission")
    private fun startGpsUpdates(context: Context) {
        try {
            // Ensure clean state before starting
            stopGpsUpdates()

            val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
            locationManager = manager

            val thread = HandlerThread("RawGpsThread").apply { start() }
            handlerThread = thread

            val listener = createLocationListener()
            locationListener = listener

            val provider =
                if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    LocationManager.GPS_PROVIDER
                } else {
                    LocationManager.NETWORK_PROVIDER
                }

            Log.i(TAG, "Starting '$provider' Provider.")
            manager.requestLocationUpdates(
                provider,
                MIN_TIME_MS,
                MIN_DISTANCE_M,
                listener,
                thread.looper,
            )

            // Register GNSS Status Callback (Android N+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val callback = createGnssCallback()
                gnssCallback = callback
                manager.registerGnssStatusCallback(callback, Handler(Looper.getMainLooper()))
                Log.i(TAG, "Registered GNSS Status Callback.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Raw GPS updates", e)
        }
    }

    private fun stopGpsUpdates() {
        try {

            Log.i(TAG, "Stopping GPS updates")

            locationListener?.let { locationManager?.removeUpdates(it) }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                gnssCallback?.let { locationManager?.unregisterGnssStatusCallback(it) }
            }

            handlerThread?.quitSafely()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping GPS updates", e)
        } finally {
            // Null out references to prevent memory leaks and reuse of dead objects
            locationListener = null
            gnssCallback = null
            handlerThread = null
            locationManager = null // Optional, but cleaner
        }
    }

    private fun createLocationListener() =
        object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "Fix: ${location.latitude},${location.longitude} Prov:${location.provider}")
                }
                processLocation(location)
            }

            override fun onProviderEnabled(provider: String) {}

            override fun onProviderDisabled(provider: String) {}

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(
                provider: String?,
                status: Int,
                extras: Bundle?,
            ) {}
        }

    private fun createGnssCallback() =
        object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(status: GnssStatus) {
                val count = status.satelliteCount
                var used = 0
                for (i in 0 until count) {
                    if (status.usedInFix(i)) used++
                }
                Log.d(TAG, "Satellite Count: $count Visible / $used Used")
            }
        }

    private fun processLocation(location: Location) {
        if (location.latitude == 0.0 && location.longitude == 0.0) return

        if (::locationCommand.isInitialized) {
            emitMetric(
                locationCommand,
                mapOf(
                    "alt" to location.altitude,
                    "acc" to location.accuracy,
                    "bear" to location.bearing,
                    "lat" to location.latitude,
                    "lon" to location.longitude,
                ),
            )
        }
    }

    private fun emitMetric(
        command: ObdCommand,
        value: Any,
    ) {
        replyObserver?.onNext(
            ObdMetric
                .builder()
                .command(command)
                .value(value)
                .raw(emptyConnectorResponse)
                .build(),
        )
    }
}
