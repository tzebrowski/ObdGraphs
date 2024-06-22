/**
 * Copyright 2019-2024, Tomasz Żebrowski
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
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.lifecycle.DefaultLifecycleObserver
import org.obd.graphs.AA_EDIT_PREF_SCREEN
import org.obd.graphs.RenderingThread
import org.obd.graphs.aa.*
import org.obd.graphs.aa.mapColor
import org.obd.graphs.aa.screen.nav.CHANGE_SCREEN_EVENT
import org.obd.graphs.aa.screen.nav.DRAG_RACING_SCREEN_ID
import org.obd.graphs.aa.screen.nav.GIULIA_SCREEN_ID
import org.obd.graphs.aa.screen.nav.ROUTINES_SCREEN_ID
import org.obd.graphs.aa.toast
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.datalogger.WorkflowStatus
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.bl.datalogger.dataLoggerPreferences
import org.obd.graphs.bl.query.Query
import org.obd.graphs.bl.query.QueryStrategyType
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
    protected val metricsCollector: MetricsCollector,
    protected val fps: Fps = Fps()
    ) : Screen(carContext), DefaultLifecycleObserver {

    protected val query = Query.instance()

    protected open fun renderAction() {}

    open fun onCarConfigurationChanged(){}

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

    protected open fun getHorizontalActionStrip(
        preferencesEnabled: Boolean = true,
        exitEnabled: Boolean = true,
        featureListsEnabledSetting: Boolean = true,
        screenId: Int = 0
    ): ActionStrip {
        var builder = ActionStrip.Builder()

        builder = if (dataLogger.isRunning()) {
            builder.addAction(createAction(carContext, R.drawable.action_disconnect, mapColor(settings.getColorTheme().actionsBtnDisconnectColor)) {
                stopDataLogging()
                toast.show(carContext, R.string.toast_connection_disconnect)
            })
        } else {
            builder.addAction(createAction(carContext, R.drawable.actions_connect, mapColor(settings.getColorTheme().actionsBtnConnectColor)) {
                when (screenId) {
                    GIULIA_SCREEN_ID -> {
                        if (dataLoggerPreferences.instance.individualQueryStrategyEnabled) {
                            query.setStrategy(QueryStrategyType.INDIVIDUAL_QUERY_FOR_EACH_VIEW)
                            query.update(metricsCollector.getMetrics().map { p -> p.source.command.pid.id }.toSet())
                        } else {
                            query.setStrategy(QueryStrategyType.SHARED_QUERY)
                        }
                        dataLogger.start(query)
                    }
                    DRAG_RACING_SCREEN_ID -> {
                        query.setStrategy(QueryStrategyType.DRAG_RACING_QUERY)
                        dataLogger.start(query)
                    }
                    ROUTINES_SCREEN_ID -> {
                        query.setStrategy(QueryStrategyType.ROUTINES_QUERY)
                        dataLogger.start(query)
                    }
                }
            })
        }

        if (featureListsEnabledSetting) {
            builder = builder.addAction(createAction(carContext, android.R.drawable.ic_dialog_dialer, CarColor.BLUE) {
                sendBroadcastEvent(CHANGE_SCREEN_EVENT)
            })
        }

        if (preferencesEnabled) {
            builder = builder.addAction(createAction(carContext, R.drawable.config, CarColor.BLUE) {
                sendBroadcastEvent(AA_EDIT_PREF_SCREEN)
                toast.show(carContext, R.string.pref_aa_get_to_app_conf)
            })
        }

        if (exitEnabled) {
            builder = builder.addAction(createAction(carContext, R.drawable.action_exit, CarColor.RED) {
                try {
                    stopDataLogging()
                } finally {
                    Log.i(LOG_KEY, "Exiting the app. Closing the context")
                    carContext.finishCarApp()
                }
            })
        }

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


    private fun onConnectionStateUpdated(connectionState: Int) {
        when (connectionState) {
            CarConnection.CONNECTION_TYPE_PROJECTION -> {
                if (settings.isAutomaticConnectEnabled() && !dataLogger.isRunning()) {
                    query.setStrategy(QueryStrategyType.SHARED_QUERY)
                    dataLogger.start(query)
                }
            }
        }
    }

    protected fun stopDataLogging() {
        Log.i(LOG_KEY, "Stopping data logging process")
        dataLogger.stop()
        cancelRenderingTask()
    }
}