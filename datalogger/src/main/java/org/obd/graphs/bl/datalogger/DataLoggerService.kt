/**
 * Copyright 2019-2023, Tomasz Żebrowski
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package org.obd.graphs.bl.datalogger

import android.content.BroadcastReceiver
import android.content.Intent
import android.util.Log
import androidx.core.app.JobIntentService
import androidx.lifecycle.LifecycleOwner
import org.obd.graphs.getContext
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.diagnostic.Diagnostics
import org.obd.metrics.diagnostic.Histogram
import org.obd.metrics.diagnostic.Rate
import org.obd.metrics.pid.PidDefinitionRegistry
import java.util.*

private const val SCHEDULED_ACTION_START = "org.obd.graphs.logger.scheduled.START"
private const val SCHEDULED_ACTION_STOP = "org.obd.graphs.logger.scheduled.STOP"
private const val ACTION_START = "org.obd.graphs.logger.START"
private const val ACTION_START_PERFORMANCE_METERING = "org.obd.graphs.logger.START_DRAG_METERING"
private const val ACTION_STOP = "org.obd.graphs.logger.STOP"
private const val SCHEDULED_START_DELAY = "org.obd.graphs.logger.scheduled.delay"

private const val UPDATE_QUERY_METRICS = "org.obd.graphs.logger.UPDATE_QUERY_METRICS"
private const val UPDATE_QUERY_PERFORMANCE_METER = "org.obd.graphs.logger.UPDATE_QUERY_PERFORMANCE_METER"

val dataLogger = DataLoggerService()

class DataLoggerService : JobIntentService(), DataLogger {
    private val jobScheduler = DataLoggerJobScheduler()

    override fun onHandleWork(intent: Intent) {
        when (intent.action) {

            UPDATE_QUERY_METRICS -> workflowOrchestrator.updateQuery(queryType = QueryType.METRICS)

            UPDATE_QUERY_PERFORMANCE_METER -> workflowOrchestrator.updateQuery(queryType = QueryType.PERFORMANCE)

            ACTION_START_PERFORMANCE_METERING -> workflowOrchestrator.startPerformanceMetering()

            ACTION_START -> workflowOrchestrator.start()

            ACTION_STOP -> workflowOrchestrator.stop()

            SCHEDULED_ACTION_STOP ->  jobScheduler.stop()


            SCHEDULED_ACTION_START -> {
                val delay = intent.extras?.getLong(SCHEDULED_START_DELAY)
                jobScheduler.schedule(delay as Long)
            }
        }
    }

    override fun updateQuery(queryType: QueryType) {
        when (queryType){
            QueryType.METRICS ->  enqueueWork(UPDATE_QUERY_METRICS)
            QueryType.PERFORMANCE ->  enqueueWork(UPDATE_QUERY_PERFORMANCE_METER)
        }
    }

    override fun status(): WorkflowStatus  = workflowOrchestrator.status()

    override fun scheduleStart(delay: Long) {
        enqueueWork(SCHEDULED_ACTION_START) {
            it.putExtra(SCHEDULED_START_DELAY, delay)
        }
    }

    override fun scheduledStop() {
        enqueueWork(SCHEDULED_ACTION_STOP)
    }

    override fun start() {
        enqueueWork(ACTION_START)
    }

    override fun startPerformanceMetering() {
        enqueueWork(ACTION_START_PERFORMANCE_METERING)
    }

    override fun stop() {
        enqueueWork(ACTION_STOP)
    }

    override val eventsReceiver: BroadcastReceiver
        get() = workflowOrchestrator.eventsReceiver

    override fun observe(lifecycleOwner: LifecycleOwner, observer: (metric: ObdMetric) -> Unit) {
        workflowOrchestrator.observe(lifecycleOwner,observer)
    }

    override fun isRunning(): Boolean  = workflowOrchestrator.isRunning()

    override fun getDiagnostics(): Diagnostics  = workflowOrchestrator.diagnostics()

    override fun findHistogramFor(metric: ObdMetric): Histogram  = workflowOrchestrator.findHistogramFor(metric)

    override fun findRateFor(metric: ObdMetric): Optional<Rate> = workflowOrchestrator.findRateFor(metric)

    override fun getPidDefinitionRegistry(): PidDefinitionRegistry  = workflowOrchestrator.pidDefinitionRegistry()
    override fun isDTCEnabled(): Boolean  = workflowOrchestrator.isDTCEnabled()

    private fun enqueueWork(intentAction: String, func: (p: Intent) -> Unit = {}) {
        try {
            getContext()?.run {
                val intent = Intent(this, DataLoggerService::class.java)
                    .apply { action = intentAction }
                    .apply { putExtra("init", 1) }

                func(intent)
                enqueueWork(
                    this,
                    DataLoggerService::class.java,
                    1, intent
                )
            }
        } catch (e: IllegalStateException) {
            Log.e("DataLoggerService", "Failed to enqueue the work", e)
        }
    }
}