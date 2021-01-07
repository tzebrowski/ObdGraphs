package org.openobd2.core.logger.bl

import android.util.Log
import org.openobd2.core.command.group.AlfaMed17CommandGroup
import org.openobd2.core.command.group.Mode1CommandGroup
import org.openobd2.core.workflow.State
import org.openobd2.core.workflow.Workflow

internal class DataLogger {

    private lateinit var workflow: Workflow
    private lateinit var device: String

    init {

        Thread.currentThread().contextClassLoader
            .getResourceAsStream("generic.json").use {
                workflow = Workflow.mode1()
                    .source(it)
                    .init(Mode1CommandGroup.INIT_PROTO_DEFAULT)
                    .init(Mode1CommandGroup.SUPPORTED_PIDS)
                    .evaluationEngine("rhino")
                    .subscriber(ModelChangePublisher())
                    .state(object : State {
                        override fun starting() {
                            Log.i("DATA_LOGGER_DL", "Start collecting process for Device: $device")
                        }

                        override fun completed() {
                            Log.i(
                                "DATA_LOGGER_DL",
                                "Collecting process completed for Device: $device"
                            )
                        }

                        override fun stopping() {
                            Log.i("DATA_LOGGER_DL", "Stop collecting process for Device: $device")
                        }
                    })
                    .build()
            }
    }

    fun stop() {
        workflow.stop()
    }

    fun start(btDeviceName: String, selecetdPids: MutableSet<String> ) {
        this.device = btDeviceName
        workflow.start(BluetoothConnection(btDeviceName),selecetdPids)
    }
}