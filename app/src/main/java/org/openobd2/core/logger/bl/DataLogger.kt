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
import org.obd.metrics.pid.PidDefinitionRegistry
import org.obd.metrics.pid.Urls
import org.obd.metrics.statistics.StatisticsRegistry
import org.openobd2.core.logger.ui.preferences.Preferences

const val NOTIFICATION_ERROR_CONNECT_BT = "data.logger.error.bt.connect"
const val NOTIFICATION_CONNECTED = "data.logger.connected"
const val NOTIFICATION_CONNECTING = "data.logger.connecting"
const val NOTIFICATION_STOPPED = "data.logger.stopped"
const val NOTIFICATION_STOPPING = "data.logger.stopping"
const val NOTIFICATION_ERROR = "data.logger.error"
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
            Log.i(LOGGER_TAG, "Start collecting process for the Device: $deviceName")
            context.sendBroadcast(Intent().apply {
                action = NOTIFICATION_CONNECTING
            })
        }

        override fun onRunning(deviceProperties: DeviceProperties) {
            Log.i(LOGGER_TAG, "We are connected to the device: $deviceProperties")

            context.sendBroadcast(Intent().apply {
                action = NOTIFICATION_CONNECTED
            })
        }

        override fun onError(msg: String, tr: Throwable?) {
            Log.e(
                LOGGER_TAG,
                "An error occurred during interaction with the device. Msg: $msg"
            )

            stop()

            if (Preferences.isReconnectWhenError(context) && "stopped" == msg) {
                Log.e(
                    LOGGER_TAG,
                    "Flag to reconnect automatically when errors occurs is turn on." +
                            " Re-establishing new connection"
                )
                start()
            } else {
                context.sendBroadcast(Intent().apply {
                    action = NOTIFICATION_ERROR
                })
            }
        }

        override fun onStopped() {

            Log.i(
                LOGGER_TAG,
                "Collecting process completed for the Device: $deviceName"
            )

            metricsAggregator.reset()

            context.sendBroadcast(Intent().apply {
                action = NOTIFICATION_STOPPED
            })
        }

        override fun onStopping() {
            Log.i(LOGGER_TAG, "Stop collecting process for the Device: $deviceName")

            context.sendBroadcast(Intent().apply {
                action = NOTIFICATION_STOPPING
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

    private lateinit var deviceName: String

    fun statistics(): StatisticsRegistry {
        return workflow().statisticsRegistry
    }

    fun getEmptyMetrics(pidIds: Set<Long>): MutableList<ObdMetric> {
        val pidRegistry: PidDefinitionRegistry = pids()
        val data: MutableList<ObdMetric> = arrayListOf()
        pidIds.forEach { s: Long? ->
            pidRegistry.findBy(s)?.apply {
                data.add(ObdMetric.builder().command(ObdCommand(this)).value(null).build())
            }
        }
        return data
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
            this.deviceName = Preferences.getAdapterName(context)

            val query = query()
            Log.i(LOGGER_TAG, "Selected pids: ${query.pids}")

            connection()?.run {
                workflow().start(this, query, adjustments())
                Log.i(LOGGER_TAG, "Start collecting process for the device: $deviceName")
            }
        }
    }

    private fun connection() : AdapterConnection? {
        try {
            return BluetoothConnection(deviceName)
        }catch (e: IllegalStateException){
            context.sendBroadcast(Intent().apply {
                action = NOTIFICATION_ERROR_CONNECT_BT
            })
        }
        return null
    }

    private fun adjustments(): Adjustments {
        return Adjustments.builder()
            .batchEnabled(Preferences.isBatchEnabled(context))
            .initDelay(Preferences.getInitDelay(context))
            .generator(
                GeneratorSpec
                    .builder()
                    .smart(true)
                    .enabled(Preferences.isEnabled(context, "pref.debug.generator.enabled"))
                    .increment(0.5).build()
            )
            .adaptiveTiming(
                AdaptiveTimeoutPolicy
                    .builder()
                    .enabled(Preferences.isEnabled(context, "pref.adapter.adaptive.enabled"))
                    .checkInterval(5000) //10s
                    .commandFrequency(Preferences.getCommandFreq(context))
                    .build()
            ).build()
    }

    private fun query(): Query {
        context.let {
            return when (Preferences.getMode(context)) {
                GENERIC_MODE -> {
                    Query.builder().pids(Preferences.getMode01Pids(context).map { s -> s.toLong() }
                        .toSet() as MutableSet<Long>).build()
                }
                else -> {
                    Query.builder().pids(Preferences.getMode22Pids(context).map { s -> s.toLong() }
                        .toSet() as MutableSet<Long>).build()

                }
            }
        }
    }

    private fun workflow(): Workflow {
        context.let {
            return when (Preferences.getMode(context)) {
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