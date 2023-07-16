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
import org.obd.metrics.api.Workflow
import org.obd.metrics.api.model.*
import org.obd.metrics.codec.GeneratorPolicy
import org.obd.metrics.codec.formula.FormulaEvaluatorConfig
import org.obd.metrics.command.group.DefaultCommandGroup
import org.obd.metrics.diagnostic.Diagnostics
import org.obd.metrics.diagnostic.Histogram
import org.obd.metrics.pid.PIDsGroup
import org.obd.metrics.pid.PidDefinitionRegistry
import org.obd.metrics.pid.Urls
import org.obd.metrics.transport.AdapterConnection
import java.io.File

internal val workflowOrchestrator = WorkflowOrchestrator()

/**
 * That's the wrapper interface on Workflow API.
 */
internal class WorkflowOrchestrator internal constructor() {
    inner class EventsReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action === PROFILE_CHANGED_EVENT) {
                Log.i(LOGGER_TAG,"Received profile changed event")
                workflow = workflow()
            }

            if (intent.action === RESOURCE_LIST_CHANGED_EVENT) {
                workflow = workflow()
                sendBroadcastEvent(WORKFLOW_RELOAD_EVENT)
            }
        }
    }

    val eventsReceiver = EventsReceiver()

    private var metricsAggregator = MetricsObserver()
    private var reconnectAttemptCount = 0
    private var reconnecting = false

    private var lifecycle = object : Lifecycle {
        override fun onConnecting() {
            Log.i(LOGGER_TAG, "Start collecting process")
            if (!reconnecting) {
                sendBroadcastEvent(DATA_LOGGER_CONNECTING_EVENT)
            }
        }

        override fun onRunning(vehicleCapabilities: VehicleCapabilities) {
            Log.i(LOGGER_TAG, "We are connected to the vehicle: $vehicleCapabilities")
            vehicleCapabilitiesManager.updateCapabilities(vehicleCapabilities)
            sendBroadcastEvent(DATA_LOGGER_CONNECTED_EVENT)

            // notify about DTC
            if (vehicleCapabilities.dtc.isNotEmpty()){
                sendBroadcastEvent(DATA_LOGGER_DTC_AVAILABLE)
            }
        }

        override fun onError(msg: String, tr: Throwable?) {
            Log.i(
                LOGGER_TAG,
                "An error occurred during interaction with the device. Msg: $msg"
            )

            if (dataLoggerPreferences.instance.reconnectWhenError && reconnectAttemptCount < dataLoggerPreferences.instance.maxReconnectRetry) {

                if (dataLoggerPreferences.instance.reconnectSilent) {
                    reconnecting = true
                }

                Log.e(
                    LOGGER_TAG,
                    "Flag to reconnect automatically when errors occurs is turn on." +
                            " Re-establishing new connection. Reconnect attempt count=$reconnectAttemptCount"
                )

                stop()
                start()
                reconnectAttemptCount++
            } else {
                reconnecting = false
                stop()
                reconnectAttemptCount = 0
                sendBroadcastEvent(DATA_LOGGER_ERROR_EVENT)
            }
        }

        override fun onStopped() {
            Log.i(
                LOGGER_TAG,
                "Collecting process is completed."
            )

            metricsAggregator.reset()
            if (!reconnecting) {
                sendBroadcastEvent(DATA_LOGGER_STOPPED_EVENT)
            }
        }

        override fun onStopping() {
            Log.i(LOGGER_TAG, "Stopping collecting process...")
            if (!reconnecting) {
                sendBroadcastEvent(DATA_LOGGER_STOPPING_EVENT)
            }
        }
    }

    private var workflow: Workflow = workflow()

    fun observe(lifecycleOwner: LifecycleOwner, observer: (metric: ObdMetric) -> Unit) {
        metricsAggregator.metrics.observe(lifecycleOwner){
            it?.let {
                observer(it)
            }
        }
    }

    fun isRunning(): Boolean  =  workflow.isRunning

    fun diagnostics(): Diagnostics = workflow.diagnostics

    fun findHistogramFor(metric: ObdMetric): Histogram = workflow.diagnostics.histogram().findBy(metric.command.pid)

    fun pidDefinitionRegistry(): PidDefinitionRegistry  = workflow.pidRegistry

    fun stop() {
        Log.i(LOGGER_TAG, "Sending STOP to the workflow with 'graceful.stop' parameter set to " +
                "${dataLoggerPreferences.instance.gracefulStop}")
        try {
            workflow.stop(dataLoggerPreferences.instance.gracefulStop)
        }catch (e: Exception){
            Log.e(LOGGER_TAG, "Failed to stop the workflow", e)
        }
    }

    fun start() {
        connection()?.run {
            val query = query()
            Log.i(LOGGER_TAG, "Selected PIDs: ${query.pids}")

            workflow.start(
                this, query, init(),
                adjustments()
            )
            Log.i(LOGGER_TAG, "Start collecting process")
        }
    }

    fun isDTCEnabled(): Boolean =  workflow.pidRegistry.findBy(PIDsGroup.DTC_READ).isNotEmpty()

    private fun connection():AdapterConnection? =
         when (dataLoggerPreferences.instance.connectionType){
            "wifi" -> wifiConnection()
            "bluetooth" -> bluetoothConnection()
            "usb" -> getContext()?.let { UsbConnection.of(context = it) }
            else -> {
                null
            }
        }

    private fun bluetoothConnection(): AdapterConnection? = try {
        val deviceName = dataLoggerPreferences.instance.adapterId
        Log.i(LOGGER_TAG, "Connecting Bluetooth Adapter: $deviceName ...")

        if (deviceName.isEmpty()) {
            sendBroadcastEvent(DATA_LOGGER_ADAPTER_NOT_SET_EVENT)
            null
        } else {
            if (network.findBluetoothAdapterByName(deviceName) == null) {
                Log.e(LOGGER_TAG, "Did not find Bluetooth Adapter: $deviceName")
                sendBroadcastEvent(DATA_LOGGER_ADAPTER_NOT_SET_EVENT)
                null
            } else {
                BluetoothConnection(deviceName)
            }
        }
    } catch (e: Exception) {
        Log.e(LOGGER_TAG, "Error occurred during establishing the connection $e")
        sendBroadcastEvent(DATA_LOGGER_ERROR_CONNECT_EVENT)
        null
    }

    private fun wifiConnection(): WifiConnection? {
        try {

            Log.i(
                LOGGER_TAG,
                "Creating TCP connection to: ${dataLoggerPreferences.instance.tcpHost}:${dataLoggerPreferences.instance.tcpPort}."
            )

            Log.i(LOGGER_TAG, "Selected WIFI SSID in preferences: ${dataLoggerPreferences.instance.wifiSSID}")
            Log.i(LOGGER_TAG, "Current connected WIFI SSID ${network.currentSSID}")

            if (dataLoggerPreferences.instance.wifiSSID.isEmpty()) {
                Log.d(LOGGER_TAG, "Target WIFI SSID is not specified in the prefs section. Connecting to the default one.")
            } else if (network.currentSSID.isNullOrBlank()) {
                sendBroadcastEvent(DATA_LOGGER_WIFI_NOT_CONNECTED)
                return null
            }  else if (dataLoggerPreferences.instance.wifiSSID != network.currentSSID) {
                Log.w(
                    LOGGER_TAG,
                    "Preferences selected WIFI SSID ${dataLoggerPreferences.instance.wifiSSID} " +
                            "is different than current connected ${network.currentSSID}"
                )
                sendBroadcastEvent(DATA_LOGGER_WIFI_INCORRECT)
                return null
            }
            return WifiConnection.of()

        } catch (e: Exception) {
            Log.e(LOGGER_TAG, "Error occurred during establishing the connection $e")
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

    private fun adjustments() = Adjustments.builder()
        .batchEnabled(dataLoggerPreferences.instance.batchEnabled)
        .collectRawConnectorResponseEnabled(dataLoggerPreferences.instance.dumpRawConnectorResponse)
        .stNxx(STNxxExtensions.builder()
            .promoteSlowGroupsEnabled(dataLoggerPreferences.instance.stnExtensionsEnabled)
            .enabled(dataLoggerPreferences.instance.stnExtensionsEnabled)
            .build())
        .responseLengthEnabled(dataLoggerPreferences.instance.responseLengthEnabled)
        .vehicleMetadataReadingEnabled(dataLoggerPreferences.instance.vehicleMetadataReadingEnabled)
        .vehicleCapabilitiesReadingEnabled(dataLoggerPreferences.instance.vehicleCapabilitiesReadingEnabled)
        .vehicleDtcReadingEnabled(dataLoggerPreferences.instance.vehicleDTCReadingEnabled)
        .vehicleDtcCleaningEnabled(dataLoggerPreferences.instance.vehicleDTCCleaningEnabled)
        .cacheConfig(
            CachePolicy.builder()
                .resultCacheFilePath(File(getContext()?.cacheDir, "formula_cache.json").absolutePath)
                .resultCacheEnabled(dataLoggerPreferences.instance.resultsCacheEnabled).build()
        )
        .producerPolicy(ProducerPolicy
            .builder()
            .conditionalSleepEnabled(dataLoggerPreferences.instance.adaptiveConnectionEnabled)
            .conditionalSleepSliceSize(10).build())
        .generator(
            GeneratorPolicy
                .builder()
                .enabled(dataLoggerPreferences.instance.generatorEnabled)
                .increment(0.5).build()
        ).adaptiveTiming(
            AdaptiveTimeoutPolicy
                .builder()
                .enabled(dataLoggerPreferences.instance.adaptiveConnectionEnabled)
                .checkInterval(5000)
                .commandFrequency(dataLoggerPreferences.instance.commandFrequency)
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
        .observer(metricsAggregator)
        .lifecycle(lifecycle)
        .initialize()

    private fun getSelectedPIDsResources() = dataLoggerPreferences.instance.resources.map {
        if (pidResources.isExternalStorageResource(it)) {
            pidResources.externalResourceToURL(it)
        } else {
            Urls.resourceToUrl(it)
        }
    }.toMutableList()

    private fun query() = Query.builder().pids(dataLoggerPreferences.instance.pids).build()
}