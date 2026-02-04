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
package org.obd.graphs.bl.datalogger

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import org.obd.graphs.Permissions
import org.obd.graphs.REQUEST_NOTIFICATION_PERMISSIONS
import org.obd.graphs.bl.query.Query
import org.obd.graphs.datalogger.R
import org.obd.graphs.sendBroadcastEvent

private const val SCHEDULED_ACTION_START = "org.obd.graphs.logger.scheduled.START"
private const val SCHEDULED_ACTION_STOP = "org.obd.graphs.logger.scheduled.STOP"
private const val ACTION_START = "org.obd.graphs.logger.START"
private const val ACTION_STOP = "org.obd.graphs.logger.STOP"
private const val SCHEDULED_START_DELAY = "org.obd.graphs.logger.scheduled.delay"

private const val UPDATE_QUERY = "org.obd.graphs.logger.UPDATE_QUERY"
private const val QUERY = "org.obd.graphs.logger.QUERY"
private const val EXECUTE_ROUTINE = "org.obd.graphs.logger.EXECUTE_ROUTINE"

private const val NOTIFICATION_CHANNEL_ID = "data_logger_channel_v2"
private const val NOTIFICATION_ID = 12345

class DataLoggerService : Service() {
    private val workflowOrchestrator by lazy {
        DataLoggerRepository.workflowOrchestrator
    }

    private val jobScheduler = DataLoggerJobScheduler(this)
    private val binder = LocalBinder()

    internal inner class LocalBinder : Binder() {
        fun getService(): DataLoggerService = this@DataLoggerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        Log.i(LOG_TAG, "Destroying DataLoggerService")
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        Log.i(LOG_TAG, "Starting DataLoggerService in the Foreground Mode")

        createNotificationChannel()

        // Handle Foreground Service Types (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var serviceTypes = android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE

            if (Permissions.hasLocationPermissions(this)) {
                serviceTypes = serviceTypes or android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else {
                Log.w(LOG_TAG, "Location permission missing. Starting Service without GPS capabilities.")
            }

            startForeground(NOTIFICATION_ID, createNotification(), serviceTypes)
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }

        // Fail-fast if permissions are missing
        if (!Permissions.hasNotificationPermissions(this)) {
            Log.e(LOG_TAG, "CRITICAL: Missing required permissions. Service cannot start.")
            serviceStop()
            sendBroadcastEvent(REQUEST_NOTIFICATION_PERMISSIONS)
            return START_NOT_STICKY
        }

        val action = intent?.action
        // Because of the "Loopback" pattern, this is executed on the Main Thread,
        // effectively serializing these commands.
        when (action) {
            UPDATE_QUERY -> {
                val query = intent.extras?.get(QUERY) as? Query
                query?.let { workflowOrchestrator.updateQuery(query = it) }
            }

            ACTION_START -> {
                val query = intent.extras?.get(QUERY) as? Query
                query?.let { workflowOrchestrator.start(it) }
            }

            EXECUTE_ROUTINE -> {
                val query = intent.extras?.get(QUERY) as? Query
                query?.let { workflowOrchestrator.executeRoutine(it) }
            }

            ACTION_STOP -> {
                workflowOrchestrator.stop()
                serviceStop()
            }

            SCHEDULED_ACTION_STOP -> jobScheduler.stop()
            SCHEDULED_ACTION_START -> {
                val delay = intent.extras?.getLong(SCHEDULED_START_DELAY) ?: 0L
                val query = intent.extras?.get(QUERY) as? Query
                query?.let { jobScheduler.schedule(delay, it) }
            }
        }

        return START_STICKY
    }

    fun updateQuery(query: Query) {
        Log.i(LOG_TAG, "Updating query for strategy=${query.getStrategy()}. PIDs=${query.getIDs()}")
        if (DataLoggerRepository.isRunning()) {
            enqueueWork(UPDATE_QUERY) { it.putExtra(QUERY, query) }
        } else {
            Log.w(LOG_TAG, "No workflow is currently running. Query won't be updated.")
        }
    }

    fun scheduleStart(
        delay: Long,
        query: Query,
    ) {
        enqueueWork(SCHEDULED_ACTION_START) {
            it.putExtra(SCHEDULED_START_DELAY, delay)
            it.putExtra(QUERY, query)
        }
    }

    fun scheduledStop() {
        enqueueWork(SCHEDULED_ACTION_STOP)
    }

    fun executeRoutine(query: Query) {
        enqueueWork(EXECUTE_ROUTINE) { it.putExtra(QUERY, query) }
    }

    fun start(query: Query) {
        enqueueWork(ACTION_START) { it.putExtra(QUERY, query) }
    }

    fun stop() {
        enqueueWork(ACTION_STOP)
    }

    // --- Helper Methods ---

    private fun enqueueWork(
        intentAction: String,
        func: (p: Intent) -> Unit = {},
    ) {
        try {

            val intent =
                Intent(this, DataLoggerService::class.java).apply {
                    action = intentAction
                    putExtra("init", 1)
                }
            func(intent)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: IllegalStateException) {
            Log.e(LOG_TAG, "Failed to enqueue the work", e)
        }
    }

    private fun createNotification(): Notification {
        // Fix for NullPointerException in tests/edge cases where LaunchIntent is null
        val contentIntent =
            packageManager.getLaunchIntentForPackage(packageName)?.let {
                PendingIntent.getActivity(
                    this,
                    0,
                    it,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            }

        return NotificationCompat
            .Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Vehicle Telemetry Service")
            .setContentText("Logging OBD & GPS data in background...")
            .setSmallIcon(R.drawable.ic_mygiulia_logo)
            .apply {
                contentIntent?.let { setContentIntent(it) }
            }.setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel =
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "OBD Logger Service",
                    NotificationManager.IMPORTANCE_LOW,
                )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun serviceStop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }
}
