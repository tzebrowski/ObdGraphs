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
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import org.obd.graphs.*
import org.obd.graphs.bl.datalogger.connectors.BluetoothConnection
import org.obd.graphs.bl.datalogger.connectors.UsbConnection
import org.obd.graphs.bl.datalogger.connectors.WifiConnection
import org.obd.graphs.bl.datalogger.drag.dragRaceResultRegistry
import org.obd.metrics.api.Workflow
import org.obd.metrics.api.model.*
import org.obd.metrics.codec.GeneratorPolicy
import org.obd.metrics.codec.formula.FormulaEvaluatorConfig
import org.obd.metrics.command.group.DefaultCommandGroup
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


internal val workflowOrchestrator: WorkflowOrchestrator by lazy {
    runAsync { WorkflowOrchestrator() }
}

/**
 * That's the wrapper interface on Workflow API.
 */
internal class WorkflowOrchestrator internal constructor() {
    inner class EventsReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action === PROFILE_CHANGED_EVENT) {
                Log.i(LOG_TAG, "Received profile changed event")
                updatePidRegistry()
            }

            if (intent.action === RESOURCE_LIST_CHANGED_EVENT) {
                updatePidRegistry()
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

        override fun onRunning(vehicleCapabilities: VehicleCapabilities) {
            status = WorkflowStatus.Connected
            Log.i(LOG_TAG, "We are connected to the vehicle: $vehicleCapabilities")
            vehicleCapabilitiesManager.updateCapabilities(vehicleCapabilities)
            sendBroadcastEvent(DATA_LOGGER_CONNECTED_EVENT)

            // notify about DTC
            if (vehicleCapabilities.dtc.isNotEmpty()) {
                sendBroadcastEvent(DATA_LOGGER_DTC_AVAILABLE)
            }
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
        }
    }

    private val workflow: Workflow = workflow()
    private var status = WorkflowStatus.Disconnected


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

    fun start(queryType: QueryType = QueryType.METRICS) {

        val (query, adjustments) = getSettings(queryType)
        connection()?.run {
            Log.i(LOG_TAG, "Selected PIDs: ${query.pids}")

            val status = workflow.start(
                this, query, init(),
                adjustments
            )
            Log.i(LOG_TAG, "Start collecting process ($queryType). Status=$status")
        }
    }

    fun updateQuery(queryType: QueryType) {
        getSettings(queryType).let {
            val result = workflow.updateQuery(
                it.first,
                init(), it.second)
            Log.i(LOG_TAG, "Query update result=$result")
        }
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

    private fun wifiConnection(): WifiConnection? {
        try {

            Log.i(
                LOG_TAG,
                "Creating TCP connection to: ${dataLoggerPreferences.instance.tcpHost}:${dataLoggerPreferences.instance.tcpPort}."
            )

            Log.i(LOG_TAG, "Selected WIFI SSID in preferences: ${dataLoggerPreferences.instance.wifiSSID}")
            Log.i(LOG_TAG, "Current connected WIFI SSID ${network.currentSSID}")

            if (dataLoggerPreferences.instance.wifiSSID.isEmpty()) {
                Log.d(LOG_TAG, "Target WIFI SSID is not specified in the prefs section. Connecting to the default one.")
            } else if (network.currentSSID.isNullOrBlank()) {
                sendBroadcastEvent(DATA_LOGGER_WIFI_NOT_CONNECTED)
                return null
            } else if (dataLoggerPreferences.instance.wifiSSID != network.currentSSID) {
                Log.w(
                    LOG_TAG,
                    "Preferences selected WIFI SSID ${dataLoggerPreferences.instance.wifiSSID} " +
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

    private fun init() = Init.builder()
        .delayAfterInit(dataLoggerPreferences.instance.initDelay)
        .delayAfterReset(dataLoggerPreferences.instance.delayAfterReset)
        .headers(getModesAndHeaders().map { entry ->
            Init.Header.builder().mode(entry.key).header(entry.value).build()
        }.toMutableList())
        .protocol(Init.Protocol.valueOf(dataLoggerPreferences.instance.initProtocol))
        .sequence(DefaultCommandGroup.INIT).build()

    private fun getMetricsAdjustments() = Adjustments.builder()
        .debugEnabled(dataLoggerPreferences.instance.debugLogging)
        .errorsPolicy(
            ErrorsPolicy.builder()
                .numberOfRetries(dataLoggerPreferences.instance.maxReconnectNum)
                .reconnectEnabled(dataLoggerPreferences.instance.reconnectWhenError).build()
        )
        .batchPolicy(
            BatchPolicy.builder()
                .enabled(dataLoggerPreferences.instance.batchEnabled)
                .responseLengthEnabled(dataLoggerPreferences.instance.responseLengthEnabled)
                .mode01BatchSize(dataLoggerPreferences.instance.mode01BatchSize)
                .mode22BatchSize(dataLoggerPreferences.instance.mode22BatchSize).build()
        )
        .collectRawConnectorResponseEnabled(dataLoggerPreferences.instance.dumpRawConnectorResponse)
        .stNxx(
            STNxxExtensions.builder()
                .promoteSlowGroupsEnabled(dataLoggerPreferences.instance.stnExtensionsEnabled)
                .enabled(dataLoggerPreferences.instance.stnExtensionsEnabled)
                .build()
        )
        .vehicleMetadataReadingEnabled(dataLoggerPreferences.instance.vehicleMetadataReadingEnabled)
        .vehicleCapabilitiesReadingEnabled(dataLoggerPreferences.instance.vehicleCapabilitiesReadingEnabled)
        .vehicleDtcReadingEnabled(dataLoggerPreferences.instance.vehicleDTCReadingEnabled)
        .vehicleDtcCleaningEnabled(dataLoggerPreferences.instance.vehicleDTCCleaningEnabled)
        .cachePolicy(
            CachePolicy.builder()
                .resultCacheFilePath(File(getContext()?.cacheDir, "formula_cache.json").absolutePath)
                .resultCacheEnabled(dataLoggerPreferences.instance.resultsCacheEnabled).build()
        )
        .producerPolicy(
            ProducerPolicy
                .builder()
                .conditionalSleepEnabled(dataLoggerPreferences.instance.adaptiveConnectionEnabled)
                .conditionalSleepSliceSize(10).build()
        )
        .generatorPolicy(
            GeneratorPolicy
                .builder()
                .enabled(dataLoggerPreferences.instance.generatorEnabled)
                .increment(0.5).build()
        ).adaptiveTimeoutPolicy(
            AdaptiveTimeoutPolicy
                .builder()
                .enabled(dataLoggerPreferences.instance.adaptiveConnectionEnabled)
                .checkInterval(5000)
                .commandFrequency(dataLoggerPreferences.instance.commandFrequency)
                .minimumTimeout(10)
                .build()

        ).build()

    private fun getDragRacingAdjustments() = Adjustments.builder()
        .debugEnabled(dataLoggerPreferences.instance.debugLogging)
        .errorsPolicy(
            ErrorsPolicy.builder()
                .numberOfRetries(dataLoggerPreferences.instance.maxReconnectNum)
                .reconnectEnabled(dataLoggerPreferences.instance.reconnectWhenError).build()
        )
        .batchPolicy(
            BatchPolicy.builder()
                .enabled(false).build()
        )
        .collectRawConnectorResponseEnabled(false)
        .stNxx(
            STNxxExtensions.builder()
                .enabled(false)
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
                .conditionalSleepEnabled(false)
                .build()
        )
        .generatorPolicy(
            GeneratorPolicy
                .builder()
                .enabled(dataLoggerPreferences.instance.generatorEnabled)
                .increment(0.5).build()
        ).adaptiveTimeoutPolicy(
            AdaptiveTimeoutPolicy
                .builder()
                .enabled(dataLoggerPreferences.instance.adaptiveConnectionEnabled)
                .checkInterval(5000)
                .commandFrequency(dataLoggerPreferences.instance.dragRacingCommandFrequency)
                .minimumTimeout(10)
                .build()
        ).build()

    private fun workflow() = Workflow.instance()
        .formulaEvaluatorConfig(FormulaEvaluatorConfig.builder().scriptEngine("rhino").build())
        .pids(
            Pids.builder().resources(
                getSelectedPIDsResources()
            ).build()
        )
        .observer(metricsObserver)
        .lifecycle(lifecycle)
        .lifecycle(metricsObserver)
        .initialize()

    private fun updatePidRegistry() = runAsync {
        workflow.updatePidRegistry(
            Pids.builder().resources(
                getSelectedPIDsResources()
            ).build()
        )
    }

    private fun getSelectedPIDsResources() = dataLoggerPreferences.instance.resources.map {
        if (pidResources.isExternalStorageResource(it)) {
            pidResources.externalResourceToURL(it)
        } else {
            Urls.resourceToUrl(it)
        }
    }.toMutableList()

    private fun getSettings(queryType: QueryType ): Pair<Query, Adjustments>  = when (queryType) {
        QueryType.METRICS ->
            Pair( Query.builder().pids(dataLoggerPreferences.instance.pids).build(), getMetricsAdjustments())

        QueryType.DRAG_RACING ->
            Pair( Query.builder().pid(dragRaceResultRegistry.getVehicleSpeedPID()).build(), getDragRacingAdjustments())
    }
}