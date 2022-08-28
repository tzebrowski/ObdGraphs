package org.obd.graphs.bl.datalogger

import android.content.*
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import org.obd.metrics.api.Workflow
import org.obd.metrics.codec.GeneratorSpec
import org.obd.metrics.command.group.DefaultCommandGroup
import org.obd.metrics.diagnostic.Diagnostics
import org.obd.metrics.pid.PidDefinitionRegistry
import org.obd.metrics.pid.Urls
import org.obd.metrics.transport.AdapterConnection
import org.obd.graphs.findBTAdapterByName
import org.obd.graphs.getContext
import org.obd.graphs.sendBroadcastEvent
import org.obd.graphs.getModesAndHeaders
import org.obd.graphs.preferences.updateSupportedPIDsPreference
import org.obd.metrics.api.model.*
import org.obd.metrics.codec.formula.FormulaEvaluatorConfig
import java.io.File
import java.lang.IllegalArgumentException

const val WORKFLOW_RELOAD_EVENT = "data.logger.workflow.reload.event"
const val RESOURCE_LIST_CHANGED_EVENT = "data.logger.resources.changed.event"
const val PROFILE_CHANGED_EVENT = "data.logger.profile.changed.event"
const val DATA_LOGGER_ADAPTER_NOT_SET_EVENT = "data.logger.adapter.not_set"
const val DATA_LOGGER_ERROR_CONNECT_EVENT = "data.logger.error.connect"
const val DATA_LOGGER_CONNECTED_EVENT = "data.logger.connected"
const val DATA_LOGGER_CONNECTING_EVENT = "data.logger.connecting"
const val DATA_LOGGER_STOPPED_EVENT = "data.logger.stopped"
const val DATA_LOGGER_STOPPING_EVENT = "data.logger.stopping"
const val DATA_LOGGER_ERROR_EVENT = "data.logger.error"
const val DATA_LOGGER_NO_NETWORK_EVENT = "data.logger.network_error"

private const val LOGGER_TAG = "DataLogger"

class DataLogger internal constructor() {
    companion object {
        @JvmStatic
        var instance: DataLogger =
            DataLogger()
    }

    private inner class EventsReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action === PROFILE_CHANGED_EVENT) {
                workflow = workflow()
            }

            if (intent.action === RESOURCE_LIST_CHANGED_EVENT) {
                workflow = workflow()
                sendBroadcastEvent(WORKFLOW_RELOAD_EVENT)
            }
        }
    }


    private val preferences by lazy { DataLoggerPreferences.instance }

    private var metricsAggregator = MetricsCollector()
    private var reconnectAttemptCount = 0
    private val broadcastReceiver = EventsReceiver()

    private var lifecycle = object : Lifecycle {
        override fun onConnecting() {
            Log.i(LOGGER_TAG, "Start collecting process")
            sendBroadcastEvent(DATA_LOGGER_CONNECTING_EVENT)
        }


        override fun onRunning(deviceProperties: DeviceProperties) {
            Log.i(LOGGER_TAG, "We are connected to the device: $deviceProperties")
            sendBroadcastEvent(DATA_LOGGER_CONNECTED_EVENT)
            updateSupportedPIDsPreference(deviceProperties.capabilities)
        }

        override fun onError(msg: String, tr: Throwable?) {
            Log.i(
                LOGGER_TAG,
                "An error occurred during interaction with the device. Msg: $msg"
            )

            stop()

            if (preferences.reconnectWhenError && reconnectAttemptCount < preferences.maxReconnectRetry) {
                Log.e(
                    LOGGER_TAG,
                    "Flag to reconnect automatically when errors occurs is turn on." +
                            " Re-establishing new connection. Reconnect attempt count=$reconnectAttemptCount"
                )
                start()
                reconnectAttemptCount++
            } else {
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

            sendBroadcastEvent(DATA_LOGGER_STOPPED_EVENT)
        }

        override fun onStopping() {
            Log.i(LOGGER_TAG, "Stopping collecting process...")
            sendBroadcastEvent(DATA_LOGGER_STOPPING_EVENT)
        }
    }

    private var workflow: Workflow = workflow()

    init {
        getContext()?.let {
            try {
                it.registerReceiver(broadcastReceiver, IntentFilter().apply {
                    addAction(RESOURCE_LIST_CHANGED_EVENT)
                    addAction(PROFILE_CHANGED_EVENT)
                })
            }catch (il : IllegalArgumentException){
                Log.e(LOGGER_TAG, "Failed to register receiver",il)
            }
        }
    }

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
        Log.i(LOGGER_TAG, "Sending STOP to workflow with graceful.stop parameter = ${preferences.gracefulStop}")
        workflow.stop(preferences.gracefulStop)
    }

    fun start() {
        connection()?.run {

            val query = query()
            Log.i(LOGGER_TAG, "Selected PID's: ${query.pids}")

            workflow.start(
                this, query, init(),
                adjustments()
            )
            Log.i(LOGGER_TAG, "Start collecting process")
        }
    }

    private fun connection() = if (preferences.connectionType == "wifi") {
        wifiConnection()
    } else {
        bluetoothConnection()
    }

    private fun bluetoothConnection(): AdapterConnection? = try {
        val deviceName = preferences.adapterId
        Log.i(LOGGER_TAG, "Connecting Bluetooth Adapter: $deviceName ...")

        if (deviceName.isEmpty()) {
            sendBroadcastEvent(DATA_LOGGER_ADAPTER_NOT_SET_EVENT)
            null
        } else {
            if (findBTAdapterByName(deviceName) == null) {
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
            "Creating TCP connection: ${preferences.tcpHost}:${preferences.tcpPort}."
        )
        WifiConnection.of(preferences.tcpHost, preferences.tcpPort)
    } catch (e: Exception) {
        Log.e(LOGGER_TAG, "Error occurred during establishing the connection $e")
        sendBroadcastEvent(DATA_LOGGER_ERROR_CONNECT_EVENT)
        null
    }

    private fun init() = Init.builder()
        .delay(preferences.initDelay)
        .headers(getModesAndHeaders().map { entry ->
            Init.Header.builder().mode(entry.key).header(entry.value).build()
        }.toMutableList())
        .fetchDeviceProperties(preferences.fetchDeviceProperties)
        .fetchSupportedPids(preferences.fetchSupportedPids)
        .protocol(Init.Protocol.valueOf(preferences.initProtocol))
        .sequence(DefaultCommandGroup.INIT).build()

    private fun adjustments() = Adjustments.builder()
        .batchEnabled(preferences.batchEnabled)
        .responseLengthEnabled(preferences.responseLengthEnabled)
        .cacheConfig(
            CacheConfig.builder()
                .resultCacheFilePath(File(getContext()?.cacheDir, "formula_cache.json").absolutePath)
                .resultCacheEnabled(preferences.resultsCacheEnabled).build()
        )
        .generator(
            GeneratorSpec
                .builder()
                .smart(true)
                .enabled(preferences.generatorEnabled)
                .increment(0.5).build()
        ).adaptiveTiming(
            AdaptiveTimeoutPolicy
                .builder()
                .enabled(preferences.adaptiveConnectionEnabled)
                .checkInterval(5000) //10s
                .commandFrequency(preferences.commandFrequency)
                .minimumTimeout(100)
                .build()
        ).build()

    private fun workflow() = Workflow.instance()
        .formulaEvaluatorConfig(FormulaEvaluatorConfig.builder().scriptEngine("rhino").build())
        .pids(
            Pids.builder().resources(
                preferences.resources.map {
                    if (isExternalStorageResource(it)) {
                        externalResourceToURL(it)
                    } else {
                        Urls.resourceToUrl(it)
                    }
                }.toMutableList()
            ).build()
        )
        .observer(metricsAggregator)
        .lifecycle(lifecycle)
        .initialize()

    private fun query() = Query.builder().pids(preferences.pids).build()
}