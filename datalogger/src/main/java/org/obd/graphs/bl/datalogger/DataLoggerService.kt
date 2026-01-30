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
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleOwner
import org.obd.graphs.Permissions
import org.obd.graphs.REQUEST_NOTIFICATION_PERMISSIONS
import org.obd.graphs.bl.query.Query
import org.obd.graphs.datalogger.R
import org.obd.graphs.getContext
import org.obd.graphs.runAsync
import org.obd.graphs.sendBroadcastEvent
import org.obd.metrics.alert.Alert
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.diagnostic.Diagnostics
import org.obd.metrics.diagnostic.Histogram
import org.obd.metrics.diagnostic.Rate
import org.obd.metrics.pid.PidDefinitionRegistry
import java.util.Optional


private const val SCHEDULED_ACTION_START = "org.obd.graphs.logger.scheduled.START"
private const val SCHEDULED_ACTION_STOP = "org.obd.graphs.logger.scheduled.STOP"
private const val ACTION_START = "org.obd.graphs.logger.START"
private const val ACTION_STOP = "org.obd.graphs.logger.STOP"
private const val SCHEDULED_START_DELAY = "org.obd.graphs.logger.scheduled.delay"

private const val UPDATE_QUERY = "org.obd.graphs.logger.UPDATE_QUERY"
private const val QUERY = "org.obd.graphs.logger.QUERY"
private const val EXECUTE_ROUTINE = "org.obd.graphs.logger.EXECUTE_ROUTINE"

private const val NOTIFICATION_CHANNEL_ID = "data_logger_channel"
private const val NOTIFICATION_ID = 12345

private var _workflowOrchestrator: WorkflowOrchestrator? = null

internal val workflowOrchestrator: WorkflowOrchestrator
    get() {
        if (_workflowOrchestrator == null) {
            Log.e(LOG_TAG, "Asynchronously loading WorkflowOrchestrator")
            _workflowOrchestrator = runAsync { WorkflowOrchestrator() }
        }
        return _workflowOrchestrator!!
    }

@VisibleForTesting
internal fun setWorkflowOrchestrator(mock: WorkflowOrchestrator) {
    _workflowOrchestrator = mock
}

val dataLogger: DataLogger = DataLoggerService()

internal class DataLoggerService : Service(), DataLogger {

    private val jobScheduler = DataLoggerJobScheduler()

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): DataLoggerService = this@DataLoggerService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(LOG_TAG, "Destroying DataLoggerService")
        workflowOrchestrator.stop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(LOG_TAG, "Starting DataLoggerService in Foreground Mode")

        createNotificationChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                createNotification(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }

        if (!Permissions.hasNotificationPermissions(getContext()!!)) {
            Log.e(LOG_TAG, "CRITICAL: Missing required permissions. Service cannot start.")

            serviceStop() // Stop immediately to avoid crash
            sendBroadcastEvent(REQUEST_NOTIFICATION_PERMISSIONS)
            return START_NOT_STICKY
        }

        val action = intent?.action

        when (action) {

            UPDATE_QUERY -> {
                val query = intent.extras?.get(QUERY) as Query
                workflowOrchestrator.updateQuery(query = query)
            }

            ACTION_START -> {
                val query = intent.extras?.get(QUERY) as Query
                workflowOrchestrator.start(query)
            }

            EXECUTE_ROUTINE -> {
                val query = intent.extras?.get(QUERY) as Query
                workflowOrchestrator.executeRoutine(query)
            }

            ACTION_STOP -> {
                workflowOrchestrator.stop()
                serviceStop()
            }

            SCHEDULED_ACTION_STOP -> jobScheduler.stop()

            SCHEDULED_ACTION_START -> {
                val delay = intent.extras?.getLong(SCHEDULED_START_DELAY)
                val query = intent.extras?.get(QUERY) as Query
                jobScheduler.schedule(delay as Long, query)
            }
        }

        return START_STICKY
    }


    override fun updateQuery(query: Query) {
        Log.i(LOG_TAG, "Updating query for strategy=${query.getStrategy()}. PIDs=${query.getIDs()}")
        if (isRunning()) {
            enqueueWork(UPDATE_QUERY) {
                it.putExtra(QUERY, query)
            }
        } else {
            Log.w(LOG_TAG, "No workflow is currently running. Query won't be updated.")
        }
    }

    override fun status(): WorkflowStatus = workflowOrchestrator.status()

    override fun scheduleStart(delay: Long, query: Query) {
        enqueueWork(SCHEDULED_ACTION_START) {
            it.putExtra(SCHEDULED_START_DELAY, delay)
            it.putExtra(QUERY, query)
        }
    }

    override fun scheduledStop() {
        enqueueWork(SCHEDULED_ACTION_STOP)
    }

    override fun executeRoutine(query: Query) {
        enqueueWork(EXECUTE_ROUTINE) {
            it.putExtra(QUERY, query)
        }
    }

    override fun start(query: Query) {
        enqueueWork(ACTION_START) {
            it.putExtra(QUERY, query)
        }
    }

    override fun stop() {
        enqueueWork(ACTION_STOP)
    }

    override val eventsReceiver: BroadcastReceiver
        get() = workflowOrchestrator.eventsReceiver

    override fun observe(lifecycleOwner: LifecycleOwner, observer: (metric: ObdMetric) -> Unit) {
        workflowOrchestrator.observe(lifecycleOwner, observer)
    }

    override fun observe(metricsProcessor: MetricsProcessor): DataLogger {
        workflowOrchestrator.observe(metricsProcessor)
        return this
    }

    override fun getCurrentQuery(): Query? = workflowOrchestrator.getCurrentQuery()
    override fun findAlertFor(metric: ObdMetric): List<Alert> = workflowOrchestrator.findAlertFor(metric)

    override fun isRunning(): Boolean = workflowOrchestrator.isRunning()

    override fun getDiagnostics(): Diagnostics = workflowOrchestrator.diagnostics()

    override fun findHistogramFor(metric: ObdMetric): Histogram = workflowOrchestrator.findHistogramFor(metric)

    override fun findRateFor(metric: ObdMetric): Optional<Rate> = workflowOrchestrator.findRateFor(metric)

    override fun getPidDefinitionRegistry(): PidDefinitionRegistry = workflowOrchestrator.pidDefinitionRegistry()
    override fun isDTCEnabled(): Boolean = workflowOrchestrator.isDTCEnabled()

    private fun enqueueWork(intentAction: String, func: (p: Intent) -> Unit = {}) {
        try {
            getContext()?.run {
                val intent = Intent(this, DataLoggerService::class.java)
                    .apply { action = intentAction }
                    .apply { putExtra("init", 1) }

                func(intent)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }
        } catch (e: IllegalStateException) {
            Log.e(LOG_TAG, "Failed to enqueue the work", e)
        }
    }

    // Create an Intent that opens your main Activity when the notification is clicked
    // Replace 'MainActivity::class.java' with your actual main activity
    // val notificationIntent = Intent(this, MainActivity::class.java)
    // val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
    private fun createNotification(): Notification =
        NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("OBD Data Logging")
            .setContentText("Connected to vehicle...")
            .setSmallIcon(R.drawable.ic_mygiulia_logo)
            // .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "OBD Logger Service",
                NotificationManager.IMPORTANCE_LOW // Low importance to avoid annoying sounds
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
