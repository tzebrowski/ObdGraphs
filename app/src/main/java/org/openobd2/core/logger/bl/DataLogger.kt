package org.openobd2.core.logger.bl

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.preference.PreferenceManager
import org.obd.metrics.StatusObserver
import org.obd.metrics.command.group.AlfaMed17CommandGroup
import org.obd.metrics.pid.PidRegistry
import org.obd.metrics.statistics.StatisticsAccumulator
import org.obd.metrics.workflow.EcuSpecific
import org.obd.metrics.workflow.Workflow
import org.openobd2.core.logger.ui.preferences.Prefs

const val NOTIFICATION_CONNECTED = "data.logger.connected"
const val NOTIFICATION_CONNECTING = "data.logger.connecting"
const val NOTIFICATION_STOPPED = "data.logger.stopped"
const val NOTIFICATION_STOPPING = "data.logger.stopping"
const val NOTIFICATION_ERROR = "data.logger.error"
const val LOG_KEY = "DATA_LOGGER_DL"
const val GENERIC_MODE = "Generic mode"


class DataLogger {

    private var modelUpdate = ModelChangePublisher()
    private lateinit var context: Context
    private var statusObserver = object : StatusObserver {
        override fun onConnecting() {
            Log.i(LOG_KEY, "Start collecting process for the Device: $device")
            modelUpdate.data.clear()
            context.sendBroadcast(Intent().apply {
                action = NOTIFICATION_CONNECTING
            })
        }

        override fun onConnected() {
            Log.i(LOG_KEY, "We are connected to the device: $device")
            context.sendBroadcast(Intent().apply {
                action = NOTIFICATION_CONNECTED
            })
        }

        override fun onError(msg: String, tr: Throwable?) {
            Log.e(
                LOG_KEY,
                "An error occurred during interaction with the device. Msg: $msg"
            )

            mode1.stop()
            mode22.stop()

            context.sendBroadcast(Intent().apply {
                action = NOTIFICATION_ERROR
            })
        }

        override fun onStopped() {
            Log.i(
                LOG_KEY,
                "Collecting process completed for the Device: $device"
            )

            context.sendBroadcast(Intent().apply {
                action = NOTIFICATION_STOPPED
            })
        }

        override fun onStopping() {
            Log.i(LOG_KEY, "Stop collecting process for the Device: $device")

            context.sendBroadcast(Intent().apply {
                action = NOTIFICATION_STOPPING
            })
        }
    }

    private var mode1: Workflow =
        Workflow.mode1().equationEngine("rhino").metricsObserver(modelUpdate)
            .statusObserver(statusObserver).build()

    private var mode22: Workflow = Workflow
        .generic()
        .ecuSpecific(
            EcuSpecific
                .builder()
                .initSequence(AlfaMed17CommandGroup.CAN_INIT)
                .pidFile("alfa.json").build()
        )
        .equationEngine("rhino")
        .metricsObserver(modelUpdate)
        .statusObserver(statusObserver).build()

    private lateinit var device: String

    fun statistics(): StatisticsAccumulator {
        return workflow().statistics
    }

    fun pids(): PidRegistry {
        return workflow().pids
    }

    fun stop() {
        workflow().stop()
    }

    fun init(ctx: Context) {
        this.context = ctx
    }

    fun start() {

        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        var adapterName = pref.getString("pref.adapter.id", "OBDII")
        this.device = adapterName.toString()

        when (Prefs.getMode(context)) {
            GENERIC_MODE -> {
                var selectedPids = pref.getStringSet("pref.pids.generic", emptySet())
                Log.i(LOG_KEY, "Generic mode, selected pids: $selectedPids")
                mode1.connection(BluetoothConnection(device.toString())).filter(selectedPids)
                    .batchEnabled(Prefs.isBatchEnabled(context)).start()
            }

            else -> {
                var selectedPids = pref.getStringSet("pref.pids.mode22", emptySet())

                Log.i(LOG_KEY, "Mode 22, selected pids: $selectedPids")
                mode22.connection(
                    BluetoothConnection(device.toString())
                ).filter(selectedPids).batchEnabled(Prefs.isBatchEnabled(context)).start()
            }
        }

        Log.i(LOG_KEY, "Start collecting process for device $adapterName")
    }

    private fun workflow(): Workflow {
        context?.let {

            return when (Prefs.getMode(context)) {
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