package org.obd.graphs.bl.datalogger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import org.obd.graphs.findBluetoothAdapterByName
import org.obd.graphs.getContext
import org.obd.graphs.getModesAndHeaders
import org.obd.graphs.preferences.vehicleCapabilitiesManager
import org.obd.graphs.sendBroadcastEvent
import org.obd.metrics.api.Workflow
import org.obd.metrics.api.model.*
import org.obd.metrics.codec.GeneratorPolicy
import org.obd.metrics.codec.formula.FormulaEvaluatorConfig
import org.obd.metrics.command.group.DefaultCommandGroup
import org.obd.metrics.diagnostic.Diagnostics
import org.obd.metrics.pid.PIDsGroup
import org.obd.metrics.pid.PidDefinitionRegistry
import org.obd.metrics.pid.Urls
import org.obd.metrics.transport.AdapterConnection
import java.io.File

const val WORKFLOW_RELOAD_EVENT = "data.logger.workflow.reload.event"
const val RESOURCE_LIST_CHANGED_EVENT = "data.logger.resources.changed.event"
const val PROFILE_CHANGED_EVENT = "data.logger.profile.changed.event"
const val DATA_LOGGER_ADAPTER_NOT_SET_EVENT = "data.logger.adapter.not_set"
const val DATA_LOGGER_ERROR_CONNECT_EVENT = "data.logger.error.connect"
const val DATA_LOGGER_CONNECTED_EVENT = "data.logger.connected"
const val DATA_LOGGER_DTC_AVAILABLE = "data.logger.dtc.available"
const val DATA_LOGGER_CONNECTING_EVENT = "data.logger.connecting"
const val DATA_LOGGER_STOPPED_EVENT = "data.logger.stopped"
const val DATA_LOGGER_STOPPING_EVENT = "data.logger.stopping"
const val DATA_LOGGER_ERROR_EVENT = "data.logger.error"
const val DATA_LOGGER_NO_NETWORK_EVENT = "data.logger.network_error"

private const val LOGGER_TAG = "DataLogger"

var dataLogger: DataLogger = DataLogger()

/**
 * That's the wrapper interface on Workflow API.
 */
class DataLogger internal constructor() {

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
    private var metricsAggregator = MetricsCollector()
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

    fun diagnostics(): Diagnostics {
        return workflow.diagnostics
    }

    fun pidDefinitionRegistry(): PidDefinitionRegistry {
        return workflow.pidRegistry
    }

    fun stop() {
        Log.i(LOGGER_TAG, "Sending STOP to the workflow with 'graceful.stop' parameter set to " +
                "${dataLoggerPreferences.instance.gracefulStop}")
        workflow.stop(dataLoggerPreferences.instance.gracefulStop)
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

    private fun connection() = if (dataLoggerPreferences.instance.connectionType == "wifi") {
        wifiConnection()
    } else {
        bluetoothConnection()
    }

    private fun bluetoothConnection(): AdapterConnection? = try {
        val deviceName = dataLoggerPreferences.instance.adapterId
        Log.i(LOGGER_TAG, "Connecting Bluetooth Adapter: $deviceName ...")

        if (deviceName.isEmpty()) {
            sendBroadcastEvent(DATA_LOGGER_ADAPTER_NOT_SET_EVENT)
            null
        } else {
            if (findBluetoothAdapterByName(deviceName) == null) {
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

    private fun wifiConnection() = try {
        Log.i(
            LOGGER_TAG,
            "Creating TCP connection: ${dataLoggerPreferences.instance.tcpHost}:${dataLoggerPreferences.instance.tcpPort}."
        )
        WifiConnection.of()
    } catch (e: Exception) {
        Log.e(LOGGER_TAG, "Error occurred during establishing the connection $e")
        sendBroadcastEvent(DATA_LOGGER_ERROR_CONNECT_EVENT)
        null
    }

    private fun init() = Init.builder()
        .delay(dataLoggerPreferences.instance.initDelay)
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
                .minimumTimeout(100)
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
        if (isExternalStorageResource(it)) {
            externalResourceToURL(it)
        } else {
            Urls.resourceToUrl(it)
        }
    }.toMutableList()

    private fun query() = Query.builder().pids(dataLoggerPreferences.instance.pids).build()
}