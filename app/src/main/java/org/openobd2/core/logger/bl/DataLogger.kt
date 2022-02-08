package org.openobd2.core.logger.bl

import android.content.Context
import android.content.Intent
import android.util.Log
import org.obd.metrics.DeviceProperties
import org.obd.metrics.Lifecycle
import org.obd.metrics.ObdMetric
import org.obd.metrics.api.*
import org.obd.metrics.codec.GeneratorSpec
import org.obd.metrics.command.group.AlfaMed17CommandGroup
import org.obd.metrics.command.group.Mode1CommandGroup
import org.obd.metrics.command.obd.ObdCommand
import org.obd.metrics.connection.AdapterConnection
import org.obd.metrics.connection.TcpConnection
import org.obd.metrics.pid.PidDefinitionRegistry
import org.obd.metrics.pid.Urls
import org.obd.metrics.diagnostic.Diagnostics


const val DATA_LOGGER_ERROR_CONNECT_EVENT = "data.logger.error.connect"
const val DATA_LOGGER_CONNECTED_EVENT = "data.logger.connected"
const val DATA_LOGGER_CONNECTING_EVENT = "data.logger.connecting"
const val DATA_LOGGER_STOPPED_EVENT = "data.logger.stopped"
const val DATA_LOGGER_STOPPING_EVENT = "data.logger.stopping"
const val DATA_LOGGER_ERROR_EVENT = "data.logger.error"

private const val LOGGER_TAG = "DATA_LOGGER_SVC"
const val GENERIC_MODE = "Generic mode"

internal class DataLogger internal constructor() {

    companion object {
        @JvmStatic
        var INSTANCE: DataLogger =
            DataLogger()
    }

    private lateinit var context: Context

    private var metricsAggregator = MetricsAggregator()

    private var lifecycle = object : Lifecycle {
        override fun onConnecting() {
            Log.i(LOGGER_TAG, "Start collecting process")
            context.sendBroadcast(Intent().apply {
                action = DATA_LOGGER_CONNECTING_EVENT
            })
        }

        override fun onRunning(deviceProperties: DeviceProperties) {
            Log.i(LOGGER_TAG, "We are connected to the device: $deviceProperties")

            context.sendBroadcast(Intent().apply {
                action = DATA_LOGGER_CONNECTED_EVENT
            })
        }

        override fun onError(msg: String, tr: Throwable?) {
            Log.e(
                LOGGER_TAG,
                "An error occurred during interaction with the device. Msg: $msg"
            )

            stop()

            if (preferences.reconnectWhenError && "stopped" == msg) {
                Log.e(
                    LOGGER_TAG,
                    "Flag to reconnect automatically when errors occurs is turn on." +
                            " Re-establishing new connection"
                )
                start()
            } else {
                context.sendBroadcast(Intent().apply {
                    action = DATA_LOGGER_ERROR_EVENT
                })
            }
        }

        override fun onStopped() {
            Log.i(
                LOGGER_TAG,
                "Collecting process is completed."
            )

            metricsAggregator.reset()

            context.sendBroadcast(Intent().apply {
                action = DATA_LOGGER_STOPPED_EVENT
            })
        }

        override fun onStopping() {
            Log.i(LOGGER_TAG, "Stopping collecting process...")

            context.sendBroadcast(Intent().apply {
                action = DATA_LOGGER_STOPPING_EVENT
            })
        }
    }

    private var mode1: Workflow = WorkflowFactory.mode1().equationEngine("rhino")
        .pidSpec(
            PidSpec
                .builder()
                .initSequence(Mode1CommandGroup.INIT)
                .pidFile(Urls.resourceToUrl("mode01.json"))
                .pidFile(Urls.resourceToUrl("extra.json"))
                .build()
        ).observer(metricsAggregator)
        .lifecycle(lifecycle)
        .initialize()

    private var mode22: Workflow = WorkflowFactory
        .generic()
        .pidSpec(
            PidSpec
                .builder()
                .initSequence(AlfaMed17CommandGroup.CAN_INIT)
                .pidFile(Urls.resourceToUrl("alfa.json")).build()
        )
        .equationEngine("rhino")
        .observer(metricsAggregator)
        .lifecycle(lifecycle).initialize()

    val preferences by lazy { DataLoggerPreferences.instance }

    fun diagnostics(): Diagnostics {
        return workflow().diagnostics
    }

    fun getEmptyMetrics(pidIds: Set<Long>): MutableList<ObdMetric> {
        val pidRegistry: PidDefinitionRegistry = pids()
        return pidIds.map {
            ObdMetric.builder().command(ObdCommand(pidRegistry.findBy(it))).value(null).build()
        }.toMutableList()
    }

    fun pids(): PidDefinitionRegistry {
        return workflow().pidRegistry
    }

    fun stop() {
        if (::context.isInitialized){
            workflow().stop()
        }
    }

    fun init(ctx: Context) {
        this.context = ctx
    }

    fun start() {

        if (::context.isInitialized) {

            val query = query()
            Log.i(LOGGER_TAG, "Selected pids: ${query.pids}")

            connection()?.run {
                workflow().start(this, query, adjustments())
                Log.i(LOGGER_TAG, "Start collecting process")
            }
        }
    }

    private fun connection() : AdapterConnection? {

        return if (preferences.connectionType == "wifi"){
            Log.i(LOGGER_TAG, "Creating TCP connection: ${preferences.tcpHost}:${preferences.tcpPort} ...")
            TcpConnection.of(preferences.tcpHost, preferences.tcpPort)
        }else {
            try {
                val deviceName = preferences.adapterId
                Log.i(LOGGER_TAG, "Connecting Bluetooth Adapter: $deviceName ...")
                BluetoothConnection(deviceName)
            }catch (e: IllegalStateException){
                context.sendBroadcast(Intent().apply {
                    action = DATA_LOGGER_ERROR_CONNECT_EVENT
                })
                null
            }
        }
    }

    private fun adjustments(): Adjustments {
        return Adjustments.builder()
            .batchEnabled(preferences.batchEnabled)
            .initDelay(preferences.initDelay)
            .generator(
                GeneratorSpec
                    .builder()
                    .smart(true)
                    .enabled(preferences.generatorEnabled)
                    .increment(0.5).build()
            )
            .adaptiveTiming(
                AdaptiveTimeoutPolicy
                    .builder()
                    .enabled(preferences.adaptiveConnectionEnabled)
                    .checkInterval(5000) //10s
                    .commandFrequency(preferences.commandFrequency)
                    .build()
            ).build()
    }

    private fun query(): Query {
        context.let {
            return when (preferences.mode) {
                GENERIC_MODE -> {
                    Query.builder().pids(preferences.mode01Pids).build()
                }
                else -> {
                    Query.builder().pids(preferences.mode02Pids).build()
                }
            }
        }
    }

    private fun workflow(): Workflow {
        context.let {
            return when (preferences.mode) {
                GENERIC_MODE -> {
                    mode1
                }
                else -> {
                    mode22
                }
            }
        }
    }
}