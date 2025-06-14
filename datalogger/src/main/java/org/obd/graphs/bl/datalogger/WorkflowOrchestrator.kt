 /**
 * Copyright 2019-2025, Tomasz Żebrowski
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
import org.obd.graphs.bl.datalogger.connectors.BluetoothConnection
import org.obd.graphs.bl.datalogger.connectors.UsbConnection
import org.obd.graphs.bl.datalogger.connectors.WifiConnection
import org.obd.graphs.bl.query.Query
import org.obd.graphs.bl.query.QueryStrategyType
import org.obd.graphs.bl.trip.tripManager
import org.obd.graphs.profile.PROFILE_CHANGED_EVENT
import org.obd.metrics.alert.Alert
import org.obd.metrics.api.Workflow
import org.obd.metrics.api.WorkflowExecutionStatus
import org.obd.metrics.api.model.*
import org.obd.metrics.codec.GeneratorPolicy
import org.obd.metrics.codec.formula.FormulaEvaluatorConfig
import org.obd.metrics.command.group.DefaultCommandGroup
import org.obd.metrics.command.routine.RoutineCommand
import org.obd.metrics.command.routine.RoutineExecutionStatus
import org.obd.metrics.diagnostic.Diagnostics
import org.obd.metrics.diagnostic.Histogram
import org.obd.metrics.diagnostic.Rate
import org.obd.metrics.diagnostic.RateType
import org.obd.metrics.pid.PIDsGroup
import org.obd.metrics.pid.PidDefinitionRegistry
import org.obd.metrics.pid.Urls
import org.obd.metrics.transport.AdapterConnection
import java.io.File
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
    }

    private var status = WorkflowStatus.Disconnected

    private val metricsProcessorsRegistry = mutableSetOf<MetricsProcessor>()

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
                    "${dataLoggerPreferences.instance.gracefulStop}"
        )
        try {
            workflow.stop(dataLoggerPreferences.instance.gracefulStop)
            Log.i(LOG_TAG, "After send the STOP. Workflow is running ${workflow.isRunning}")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to stop the workflow", e)
        }
    }

    fun start(query: Query) {
        currentQuery = query

        val dataLoggerQuery = org.obd.metrics.api.model.Query.builder().pids(query.getIDs()).build()
        Log.i(LOG_TAG, "Stating collecting process. Strategy: ${query.getStrategy()}. Selected PIDs: ${dataLoggerQuery.pids}")

        when (query.getStrategy()) {
            QueryStrategyType.DRAG_RACING_QUERY -> {
                connection()?.run {
                    val status = workflow.start(
                        this,dataLoggerQuery, init(),
                        getDragRacingAdjustments()
                    )
                    Log.i(LOG_TAG, "Collecting process started. Strategy: ${query.getStrategy()}. Status=$status")
                }
            }
            else -> {
                connection()?.run {
                    val status = workflow.start(
                        this,dataLoggerQuery, init(),
                        getDefaultAdjustments()
                    )
                    Log.i(LOG_TAG, "Collecting process started. Strategy: ${query.getStrategy()}. Status=$status")
                }
            }
        }
    }

    fun executeRoutine(query: Query) {
        currentQuery = query

        connection()?.run {
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
        if (::currentQuery.isInitialized && query.getIDs() == currentQuery.getIDs()){
            Log.w(LOG_TAG,"Received same query=${query.getIDs()}. Do not update.")
        } else {
            queryToAdjustments(query).let {
                val dataLoggerQuery = org.obd.metrics.api.model.Query.builder().pids(query.getIDs()).build()
                val result = workflow.updateQuery(
                    dataLoggerQuery,
                    init(), it)
                Log.i(LOG_TAG, "Query=${query.getStrategy()} update result=$result")
            }
        }

        currentQuery = query
    }

    fun isDTCEnabled(): Boolean = workflow.pidRegistry.findBy(PIDsGroup.DTC_READ).isNotEmpty()

    private fun connection(): AdapterConnection? =
        when (dataLoggerPreferences.instance.connectionType) {
            "wifi" -> wifiConnection()
            "bluetooth" -> bluetoothConnection()
            "usb" -> getContext()?.let { UsbConnection.of(context = it) }
            else -> {
                null
            }
        }

    private fun bluetoothConnection(): AdapterConnection? = try {
        val deviceName = dataLoggerPreferences.instance.adapterId
        Log.i(LOG_TAG, "Connecting Bluetooth Adapter: $deviceName ...")

        if (deviceName.isEmpty()) {
            sendBroadcastEvent(DATA_LOGGER_ADAPTER_NOT_SET_EVENT)
            null
        } else {
            if (network.findBluetoothAdapterByName(deviceName) == null) {
                Log.e(LOG_TAG, "Did not find Bluetooth Adapter: $deviceName")
                sendBroadcastEvent(DATA_LOGGER_ADAPTER_NOT_SET_EVENT)
                null
            } else {
                BluetoothConnection(deviceName)
            }
        }
    } catch (e: Exception) {
        Log.e(LOG_TAG, "Error occurred during establishing the connection $e")
        sendBroadcastEvent(DATA_LOGGER_ERROR_CONNECT_EVENT)
        null
    }

    private fun wifiConnection(preferences: DataLoggerPreferences = dataLoggerPreferences.instance): WifiConnection? {
        try {
            Log.i(
                LOG_TAG,
                "Creating TCP connection to: ${preferences.tcpHost}:${preferences.tcpPort}."
            )

            Log.i(LOG_TAG, "Selected WIFI SSID in preferences: ${preferences.wifiSSID}")
            Log.i(LOG_TAG, "Current connected WIFI SSID ${network.currentSSID}")

            if (preferences.wifiSSID.isEmpty()) {
                Log.d(LOG_TAG, "Target WIFI SSID is not specified in the prefs section. Connecting to the default one.")
            } else if (network.currentSSID.isNullOrBlank()) {
                sendBroadcastEvent(DATA_LOGGER_WIFI_NOT_CONNECTED)
                return null
            } else if (preferences.wifiSSID != network.currentSSID) {
                Log.w(
                    LOG_TAG,
                    "Preferences selected WIFI SSID ${preferences.wifiSSID} " +
                            "is different than current connected ${network.currentSSID}"
                )
                sendBroadcastEvent(DATA_LOGGER_WIFI_INCORRECT)
                return null
            }
            return WifiConnection.of()

        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error occurred during establishing the connection $e")
            sendBroadcastEvent(DATA_LOGGER_ERROR_CONNECT_EVENT)
        }
        return null
    }

    private fun init(preferences: DataLoggerPreferences = dataLoggerPreferences.instance) = Init.builder()
        .delayAfterInit(preferences.initDelay)
        .delayAfterReset(preferences.delayAfterReset)
        .headers(diagnosticRequestIDMapper.getMapping().map { entry ->
            Init.Header.builder().mode(entry.key).header(entry.value).build()
        }.toMutableList())
        .protocol(Init.Protocol.valueOf(preferences.initProtocol))
        .sequence(DefaultCommandGroup.INIT).build()

    private fun getDefaultAdjustments(preferences: DataLoggerPreferences = dataLoggerPreferences.instance) = Adjustments.builder()
        .debugEnabled(preferences.debugLogging)
        .formulaExternalParams(FormulaExternalParams.builder().param("unit_tank_size", preferences.fuelTankSize).build())
        .errorsPolicy(
            ErrorsPolicy.builder()
                .numberOfRetries(preferences.maxReconnectNum)
                .reconnectEnabled(preferences.reconnectWhenError).build()
        )
        .batchPolicy(
            BatchPolicy.builder()
                .enabled(preferences.batchEnabled)
                .strictValidationEnabled(preferences.batchStricValidationEnabled)
                .responseLengthEnabled(preferences.responseLengthEnabled)
                .mode01BatchSize(preferences.mode01BatchSize)
                .otherModesBatchSize(preferences.otherModesBatchSize).build()
        )
        .collectRawConnectorResponseEnabled(preferences.dumpRawConnectorResponse)
        .stNxx(
            STNxxExtensions.builder()
                .promoteSlowGroupsEnabled(preferences.stnExtensionsEnabled)
                .promoteAllGroupsEnabled(preferences.stnExtensionsEnabled)
                .enabled(preferences.stnExtensionsEnabled)
                .build()
        )
        .vehicleMetadataReadingEnabled(preferences.vehicleMetadataReadingEnabled)
        .vehicleCapabilitiesReadingEnabled(preferences.vehicleCapabilitiesReadingEnabled)
        .vehicleDtcReadingEnabled(preferences.vehicleDTCReadingEnabled)
        .vehicleDtcCleaningEnabled(preferences.vehicleDTCCleaningEnabled)
        .cachePolicy(
            CachePolicy.builder()
                .resultCacheFilePath(File(getContext()?.cacheDir, "formula_cache.json").absolutePath)
                .resultCacheEnabled(preferences.resultsCacheEnabled).build()
        )
        .producerPolicy(
            ProducerPolicy
                .builder()
                .conditionalSleepEnabled(preferences.adaptiveConnectionEnabled)
                .conditionalSleepSliceSize(10).build()
        )
        .generatorPolicy(
            GeneratorPolicy
                .builder()
                .enabled(preferences.generatorEnabled)
                .increment(0.5).build()
        ).adaptiveTimeoutPolicy(
            AdaptiveTimeoutPolicy
                .builder()
                .enabled(preferences.adaptiveConnectionEnabled)
                .checkInterval(5000)
                .commandFrequency(preferences.commandFrequency)
                .minimumTimeout(10)
                .build()

        ).build()

    private fun getDragRacingAdjustments(preferences: DataLoggerPreferences = dataLoggerPreferences.instance): Adjustments {
        var builder = Adjustments.builder()
            .debugEnabled(preferences.debugLogging)
            .errorsPolicy(
                ErrorsPolicy.builder()
                    .numberOfRetries(preferences.maxReconnectNum)
                    .reconnectEnabled(preferences.reconnectWhenError).build()
            )
            .batchPolicy(
                BatchPolicy.builder()
                    .enabled(preferences.batchEnabled)
                    .responseLengthEnabled(preferences.responseLengthEnabled)
                    .mode01BatchSize(preferences.mode01BatchSize)
                    .otherModesBatchSize(preferences.otherModesBatchSize).build()
            )
            .collectRawConnectorResponseEnabled(false)
            .stNxx(
                STNxxExtensions.builder()
                    .enabled(dataLoggerPreferences.instance.stnExtensionsEnabled)
                    .promoteSlowGroupsEnabled(false)
                    .promoteAllGroupsEnabled(false)
                    .build()
            )
            .vehicleMetadataReadingEnabled(false)
            .vehicleCapabilitiesReadingEnabled(false)
            .vehicleDtcReadingEnabled(false)
            .vehicleDtcCleaningEnabled(false)
            .cachePolicy(
                CachePolicy.builder()
                    .resultCacheEnabled(false).build()
            )
            .producerPolicy(
                ProducerPolicy
                    .builder()
                    .pidPriority(0,0) // vehicle speed, rpm
                    .pidPriority(5,10) // atm pressure, ambient temp
                    .pidPriority(4,4) // atm pressure, ambient temp
                    .conditionalSleepEnabled(false)
                    .build()
            )
            .generatorPolicy(
                GeneratorPolicy
                    .builder()
                    .enabled(preferences.generatorEnabled)
                    .increment(0.5).build()
            ).adaptiveTimeoutPolicy(
                AdaptiveTimeoutPolicy
                    .builder()
                    .enabled(preferences.adaptiveConnectionEnabled)
                    .checkInterval(5000)
                    .commandFrequency(preferences.dragRacingCommandFrequency)
                    .minimumTimeout(10)
                    .build()
            )

        if (dataLoggerPreferences.instance.stnExtensionsEnabled){
            val overrideSettings = PidDefinitionOverride.builder().priority(0).build()
            builder = builder
                    .override(PidId.EXT_VEHICLE_SPEED_PID_ID.value,overrideSettings)
                    .override(PidId.EXT_MEASURED_INTAKE_PRESSURE_PID_ID.value,overrideSettings)
                    .override(PidId.EXT_ATM_PRESSURE_PID_ID.value,overrideSettings)
                    .override(PidId.EXT_AMBIENT_TEMP_PID_ID.value,overrideSettings)
                    .override(PidId.ENGINE_TORQUE_PID_ID.value,overrideSettings)
        }

        return builder.build()
    }

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
    }

    private fun pids(): Pids? = Pids.builder().resources(dataLoggerPreferences.instance.resources.map {
        if (modules.isExternalStorageModule(it)) {
            modules.externalModuleToURL(it)
        } else {
            Urls.resourceToUrl(it)
        }
    }.toMutableList()).build()

    private fun queryToAdjustments(query: Query): Adjustments  = when (query.getStrategy()) {
        QueryStrategyType.DRAG_RACING_QUERY ->
            getDragRacingAdjustments()
        else ->
            getDefaultAdjustments()
    }
}
