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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import org.obd.graphs.*
import org.obd.graphs.bl.datalogger.connectors.ConnectionManager
import org.obd.graphs.bl.query.Query
import org.obd.graphs.bl.trip.tripManager
import org.obd.graphs.profile.PROFILE_CHANGED_EVENT
import org.obd.metrics.alert.Alert
import org.obd.metrics.api.Workflow
import org.obd.metrics.api.WorkflowExecutionStatus
import org.obd.metrics.api.model.*
import org.obd.metrics.codec.formula.FormulaEvaluatorConfig
import org.obd.metrics.command.group.DefaultCommandGroup
import org.obd.metrics.command.routine.RoutineCommand
import org.obd.metrics.command.routine.RoutineExecutionStatus
import org.obd.metrics.diagnostic.Diagnostics
import org.obd.metrics.diagnostic.Histogram
import org.obd.metrics.diagnostic.Rate
import org.obd.metrics.diagnostic.RateType
import org.obd.metrics.pid.PIDsGroup
import org.obd.metrics.pid.PidDefinition
import org.obd.metrics.pid.PidDefinitionRegistry
import org.obd.metrics.pid.Urls
import org.obd.metrics.pid.ValueType
import java.util.*

private const val JS_ENGINE_NAME = "rhino"

/**
 * That's the wrapper interface on Workflow API.
 */
internal class WorkflowOrchestrator internal constructor() {
    inner class EventsReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action === PROFILE_CHANGED_EVENT) {
                Log.i(LOG_TAG, "Received profile changed event")
                updateModulesRegistry()
            }

            if (intent.action === MODULES_LIST_CHANGED_EVENT) {
                updateModulesRegistry()
                sendBroadcastEvent(WORKFLOW_RELOAD_EVENT)
            }
        }
    }

    internal val eventsReceiver = EventsReceiver()
    private val metricsObserver = MetricsObserver()

    private var lifecycle = object : Lifecycle {

        override fun onConnecting() {
            status = WorkflowStatus.Connecting
            Log.i(LOG_TAG, "Start collecting process")
            sendBroadcastEvent(DATA_LOGGER_CONNECTING_EVENT)
        }

        override fun onRoutineCompleted(routineCommand: RoutineCommand, status: RoutineExecutionStatus) {
            Log.e(LOG_TAG, "Routine: ${routineCommand.pid.description}  execution status: $status")
            val event = when (status){
                RoutineExecutionStatus.ERROR -> ROUTINE_EXECUTION_FAILED_EVENT
                RoutineExecutionStatus.NO_DATA -> ROUTINE_EXECUTION_NO_DATA_RECEIVED_EVENT
                RoutineExecutionStatus.SUCCESS -> ROUTINE_EXECUTED_SUCCESSFULLY_EVENT
            }

            sendBroadcastEvent(event)
        }

        override fun onRunning(vehicleCapabilities: VehicleCapabilities) {
            status = WorkflowStatus.Connected
            Log.i(LOG_TAG, "We are connected to the vehicle: $vehicleCapabilities")
            vehicleCapabilitiesManager.updateCapabilities(vehicleCapabilities)
            sendBroadcastEvent(DATA_LOGGER_CONNECTED_EVENT)

            // notify about DTC
            if (vehicleCapabilities.dtc.isNotEmpty()) {
                sendBroadcastEvent(DATA_LOGGER_DTC_AVAILABLE)
            }

            tripManager.startNewTrip(System.currentTimeMillis())
        }

        override fun onError(msg: String, tr: Throwable?) {
            Log.i(
                LOG_TAG,
                "An error occurred during interaction with the device. Msg: $msg"
            )

            stop()
            sendBroadcastEvent(DATA_LOGGER_ERROR_EVENT)
        }

        override fun onStopped() {
            status = WorkflowStatus.Disconnected
            Log.i(
                LOG_TAG,
                "Collecting process is completed."
            )
            sendBroadcastEvent(DATA_LOGGER_STOPPED_EVENT)
            tripManager.saveCurrentTripAsync()
        }
    }

    private val workflow: Workflow = workflow().apply {
        pidRegistry.findAll().forEach { p ->
            p.deserialize()?.let {
                p.formula = it.formula
                p.alert.lowerThreshold = it.alert.lowerThreshold
                p.alert.upperThreshold = it.alert.upperThreshold
            }
        }

        registerGPSPids(this.pidRegistry)
    }

    private var status = WorkflowStatus.Disconnected
    private val metricsProcessorsRegistry = mutableSetOf<MetricsProcessor>()
    private val adjustmentsStrategy = AdjustmentsStrategy()

    fun observe(metricsProcessor: MetricsProcessor) {
        if (metricsProcessorsRegistry.contains(metricsProcessor)){
            Log.i(LOG_TAG,"Metrics processor is already registered: $metricsProcessor")
        }else {
            Log.i(LOG_TAG,"Registering: $metricsProcessor metrics processor")
            metricsProcessorsRegistry.add(metricsProcessor)
            metricsObserver.observe(metricsProcessor)
        }
    }

    fun observe(lifecycleOwner: LifecycleOwner, observer: (metric: ObdMetric) -> Unit) =
        metricsObserver.observe(lifecycleOwner) {
            it.let {
                observer(it)
            }
        }

    fun status(): WorkflowStatus = status

    fun isRunning(): Boolean = workflow.isRunning

    fun diagnostics(): Diagnostics = workflow.diagnostics

    fun findHistogramFor(metric: ObdMetric): Histogram = workflow.diagnostics.histogram().findBy(metric.command.pid)

    fun findRateFor(metric: ObdMetric): Optional<Rate> = workflow.diagnostics.rate().findBy(RateType.MEAN, metric.command.pid)

    fun findAlertFor(metric: ObdMetric): List<Alert> = workflow.alerts.findBy(metric.command.pid)

    fun pidDefinitionRegistry(): PidDefinitionRegistry = workflow.pidRegistry

    fun stop() {

        Log.i(
            LOG_TAG, "Sending STOP to the workflow with 'graceful.stop' parameter set to " +
                    "${dataLoggerSettings.instance().adapter.gracefulStop}"
        )
        try {
            workflow.stop(dataLoggerSettings.instance().adapter.gracefulStop)
            Log.i(LOG_TAG, "After send the STOP. Workflow is running ${workflow.isRunning}")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to stop the workflow", e)
        }
    }

    fun start(query: Query) {
        currentQuery = query

        val dataLoggerQuery = org.obd.metrics.api.model.Query.builder().pids(query.getIDs()).build()
        Log.i(LOG_TAG, "Stating collecting process. Strategy: ${query.getStrategy()}. Selected PIDs: ${dataLoggerQuery.pids}")
        ConnectionManager.obtain()?.run {
            val status = workflow.start(
                this, dataLoggerQuery, init(),
                adjustmentsStrategy.findAdjustmentFor(query.getStrategy())
            )
            Log.i(LOG_TAG, "Collecting process started. Strategy: ${query.getStrategy()}. Status=$status")
        }
    }

    fun executeRoutine(query: Query) {
        currentQuery = query

        ConnectionManager.obtain()?.run {
            val dataLoggerQuery = org.obd.metrics.api.model.Query.builder().pids(query.getIDs()).build()
            Log.i(LOG_TAG, "Executing routine. Strategy: ${query.getStrategy()}. Selected PIDs: ${dataLoggerQuery.pids}")

            val status = workflow.executeRoutine(dataLoggerQuery.pids.first(), init())
            Log.i(LOG_TAG, "Routines has been completed. Strategy: ${query.getStrategy()}. Status=$status")

            when (status) {
                WorkflowExecutionStatus.REJECTED -> sendBroadcastEvent(ROUTINE_REJECTED_EVENT)
                WorkflowExecutionStatus.NOT_RUNNING -> sendBroadcastEvent(ROUTINE_WORKFLOW_NOT_RUNNING_EVENT)
                else -> sendBroadcastEvent(ROUTINE_UNKNOWN_STATUS_EVENT)
            }
        }

    }

    private lateinit var currentQuery: Query

    fun getCurrentQuery (): Query? = if (::currentQuery.isInitialized) currentQuery else null

    fun updateQuery(query: Query) {
        if (::currentQuery.isInitialized && query.getIDs() == currentQuery.getIDs()) {
            Log.w(LOG_TAG, "Received same query=${query.getIDs()}. Do not update.")
        } else {

            val dataLoggerQuery = org.obd.metrics.api.model.Query.builder().pids(query.getIDs()).build()
            val adjustments = adjustmentsStrategy.findAdjustmentFor(query.getStrategy())
            val status = workflow.updateQuery(
                dataLoggerQuery,
                init(), adjustments
            )

            Log.i(LOG_TAG, "Query update finished, strategy: ${query.getStrategy()}. Status=$status")
        }

        currentQuery = query
    }

    fun isDTCEnabled(): Boolean = workflow.pidRegistry.findBy(PIDsGroup.DTC_READ).isNotEmpty()

    private fun init(preferences: DataLoggerSettings = dataLoggerSettings.instance()) = Init.builder()
        .delayAfterInit(preferences.adapter.initDelay)
        .delayAfterReset(preferences.adapter.delayAfterReset)
        .headers(diagnosticRequestIDMapper.getMapping().map { entry ->
            Init.Header.builder().mode(entry.key).header(entry.value).build()
        }.toMutableList())
        .protocol(Init.Protocol.valueOf(preferences.adapter.initProtocol))
        .sequence(DefaultCommandGroup.INIT).build()

    private fun workflow() = Workflow.instance()
        .formulaEvaluatorConfig(FormulaEvaluatorConfig.builder().scriptEngine(JS_ENGINE_NAME).build())
        .pids(
            pids()
        )
        .observer(metricsObserver)
        .lifecycle(lifecycle)
        .lifecycle(metricsObserver)
        .initialize()

    private fun updateModulesRegistry() = runAsync {
        workflow.updatePidRegistry(
            pids()
        )

        workflow.pidRegistry.findAll().forEach { p ->
            p.deserialize()?.let {
                p.formula = it.formula
                p.alert.lowerThreshold = it.alert.lowerThreshold
                p.alert.upperThreshold = it.alert.upperThreshold
            }
        }

       registerGPSPids(workflow.pidRegistry)
    }

    private fun registerGPSPids(pidRegistry: PidDefinitionRegistry) {
        Log.d(LOG_TAG,"Registering GPS PIDs")

        pidRegistry.register(PidDefinition(Pid.GPS_LAT_PID_ID.id, 2, "", "22", "Lat", "deg", "GPS Latitude", -90, 90, ValueType.DOUBLE))
        pidRegistry.register(PidDefinition(Pid.GPS_LON_PID_ID.id, 2, "", "22", "Lon", "deg", "GPS Longitude", -180, 180, ValueType.DOUBLE))
        pidRegistry.register(PidDefinition(Pid.GPS_ALT_PID_ID.id, 2, "", "22", "Alt", "m", "GPS Altitude", -100, 10000, ValueType.DOUBLE))
    }

    private fun pids(): Pids? = Pids.builder().resources(dataLoggerSettings.instance().resources.map {
        if (modules.isExternalStorageModule(it)) {
            modules.externalModuleToURL(it)
        } else {
            Urls.resourceToUrl(it)
        }
    }.toMutableList()).build()
}
