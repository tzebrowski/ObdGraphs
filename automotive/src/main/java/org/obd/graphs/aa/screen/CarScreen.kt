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
package org.obd.graphs.aa.screen

import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.connection.CarConnection
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import org.obd.graphs.AA_EDIT_PREF_SCREEN
import org.obd.graphs.AA_VIRTUAL_SCREEN_RENDERER_TOGGLE_EVENT
import org.obd.graphs.RenderingThread
import org.obd.graphs.aa.*
import org.obd.graphs.bl.collector.CarMetricsCollector
import org.obd.graphs.bl.datalogger.QueryType
import org.obd.graphs.bl.datalogger.WorkflowStatus
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.renderer.Fps
import org.obd.graphs.sendBroadcastEvent

const val VIRTUAL_SCREEN_1_SETTINGS_CHANGED = "pref.aa.pids.profile_1.event.changed"
const val VIRTUAL_SCREEN_2_SETTINGS_CHANGED = "pref.aa.pids.profile_2.event.changed"
const val VIRTUAL_SCREEN_3_SETTINGS_CHANGED = "pref.aa.pids.profile_3.event.changed"
const val VIRTUAL_SCREEN_4_SETTINGS_CHANGED = "pref.aa.pids.profile_4.event.changed"
const val LOG_KEY = "CarScreen"

internal abstract class CarScreen(
    carContext: CarContext,
    protected val settings: CarSettings,
    protected val metricsCollector: CarMetricsCollector,
    protected val fps: Fps = Fps()
) : Screen(carContext), DefaultLifecycleObserver {

    abstract fun renderAction()

    abstract fun onCarConfigurationChanged()

    protected val renderingThread: RenderingThread = RenderingThread(
        id = "CarScreenRenderingThread",
        renderAction = {
            renderAction()
        },
        perfFrameRate = {
            settings.getSurfaceFrameRate()
        }
    )

    protected fun registerConnectionStateReceiver() {
        CarConnection(carContext).type.observe(this, ::onConnectionStateUpdated)
    }

    protected fun getActionStrip(preferencesEnabled: Boolean = true, dragMeteringEnabled: Boolean = false, toggleBtnColor: Int): ActionStrip {
        var builder = ActionStrip.Builder()

        builder = if (dataLogger.isRunning()) {
            builder.addAction(createAction(R.drawable.action_disconnect, mapColor(settings.colorTheme().actionsBtnDisconnectColor)) {
                stopDataLogging()
                toast.show(carContext, R.string.toast_connection_disconnect)
            })
        } else {
            builder.addAction(createAction(R.drawable.actions_connect, mapColor(settings.colorTheme().actionsBtnConnectColor)) {
                if (dragMeteringEnabled){
                    dataLogger.start(queryType = QueryType.PERFORMANCE)
                } else {
                    dataLogger.start(queryType = QueryType.METRICS)
                }
            })
        }

        builder = builder.addAction(createAction(R.drawable.action_drag_race_screen, mapColor(toggleBtnColor)) {
            sendBroadcastEvent(AA_VIRTUAL_SCREEN_RENDERER_TOGGLE_EVENT)
        })

        if (preferencesEnabled) {
            builder = builder.addAction(createAction(R.drawable.config, CarColor.BLUE) {
                sendBroadcastEvent(AA_EDIT_PREF_SCREEN)
                toast.show(carContext, R.string.pref_aa_get_to_app_conf)
            })
        }

        builder = builder.addAction(createAction(R.drawable.action_exit, CarColor.RED) {
            try {
                stopDataLogging()
            } finally {
                Log.i(LOG_KEY, "Exiting the app. Closing the context")
                carContext.finishCarApp()
            }
        })

        return builder.build()
    }

    protected fun cancelRenderingTask() {
        renderingThread.stop()
        fps.stop()
    }

    protected fun submitRenderingTask() {
        if (!renderingThread.isRunning() && dataLogger.status() == WorkflowStatus.Connected) {
            renderingThread.start()
            fps.start()
        }
    }

    protected fun createAction(iconResId: Int, iconColorTint: CarColor, func: () -> Unit): Action =
        Action.Builder()
            .setIcon(
                CarIcon.Builder(
                    IconCompat.createWithResource(
                        carContext,
                        iconResId
                    )
                ).setTint(iconColorTint).build()
            )
            .setOnClickListener {
                func()
            }.build()

    private fun onConnectionStateUpdated(connectionState: Int) {
        when (connectionState) {
            CarConnection.CONNECTION_TYPE_PROJECTION -> {
                if (settings.isAutomaticConnectEnabled() && !dataLogger.isRunning()) {
                    dataLogger.start()
                }
            }
        }
    }

    private fun stopDataLogging() {
        Log.i(LOG_KEY, "Stopping data logging process")
        dataLogger.stop()
        cancelRenderingTask()
    }
}