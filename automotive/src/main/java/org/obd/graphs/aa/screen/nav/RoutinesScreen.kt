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
package org.obd.graphs.aa.screen.nav

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.model.*
import androidx.lifecycle.LifecycleOwner
import org.obd.graphs.aa.*
import org.obd.graphs.aa.screen.*
import org.obd.graphs.aa.screen.CarScreen
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.datalogger.*
import org.obd.graphs.bl.query.Query
import org.obd.graphs.bl.query.QueryStrategyType
import org.obd.graphs.renderer.Fps
import org.obd.metrics.pid.PIDsGroup
import org.obd.metrics.pid.PidDefinition

internal class RoutinesScreen (carContext: CarContext,
                      settings: CarSettings,
                      metricsCollector: MetricsCollector,
                      fps: Fps
) : CarScreen(carContext, settings, metricsCollector, fps) {

    private var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {

                ROUTINE_WORKFLOW_NOT_RUNNING_EVENT -> toast.show(carContext, R.string.routine_workflow_is_not_running)
                ROUTINE_UNKNOWN_STATUS_EVENT ->  toast.show(carContext, R.string.routine_unknown_error)
                ROUTINE_EXECUTION_FAILED_EVENT -> toast.show(carContext, R.string.routine_execution_failed)
                ROUTINE_EXECUTED_SUCCESSFULLY_EVENT -> toast.show(carContext, R.string.routine_executed_successfully)
                ROUTINE_EXECUTION_NO_DATA_RECEIVED_EVENT -> toast.show(carContext, R.string.routine_no_data)

                DATA_LOGGER_CONNECTING_EVENT -> {
                    try {
                        invalidate()
                    } catch (e: Exception){
                        Log.w(LOG_KEY,"Failed when received DATA_LOGGER_CONNECTING_EVENT event",e)
                    }
                }

                DATA_LOGGER_NO_NETWORK_EVENT -> toast.show(carContext, R.string.main_activity_toast_connection_no_network)
                DATA_LOGGER_ERROR_EVENT -> {
                    invalidate()
                    toast.show(carContext, R.string.main_activity_toast_connection_error)
                }

                DATA_LOGGER_STOPPED_EVENT -> {
                    try {
                        toast.show(carContext, R.string.main_activity_toast_connection_stopped)
                        invalidate()
                    } catch (e: Exception){
                        Log.w(LOG_KEY,"Failed when received DATA_LOGGER_STOPPED_EVENT event",e)
                    }
                }

                DATA_LOGGER_CONNECTED_EVENT -> {
                    try {
                        toast.show(carContext, R.string.main_activity_toast_connection_established)
                        invalidate()
                    } catch (e: Exception){
                        Log.w(LOG_KEY,"Failed when received DATA_LOGGER_ERROR_CONNECT_EVENT event",e)
                    }
                }

                DATA_LOGGER_ERROR_CONNECT_EVENT -> {
                    try {
                        invalidate()
                        toast.show(carContext, R.string.main_activity_toast_connection_connect_error)
                    }catch (e: Exception){
                        Log.w(LOG_KEY,"Failed when received DATA_LOGGER_ERROR_CONNECT_EVENT event",e)
                    }
                }

                DATA_LOGGER_ADAPTER_NOT_SET_EVENT -> {
                    try {
                        invalidate()
                        toast.show(carContext, R.string.main_activity_toast_adapter_is_not_selected)
                    }catch (e: Exception){
                        Log.w(LOG_KEY,"Failed when received DATA_LOGGER_ADAPTER_NOT_SET_EVENT event",e)
                    }
                }
            }
        }
    }

    override fun onGetTemplate(): Template  = try {
        if (dataLogger.status() == WorkflowStatus.Connecting) {
            ListTemplate.Builder()
                .setLoading(true)
                .setTitle(carContext.getString(R.string.routine_page_title))
                .setActionStrip(getHorizontalActionStrip()).build()
        } else {
            var items = ItemList.Builder()
            dataLogger.getPidDefinitionRegistry().findBy(PIDsGroup.ROUTINE).sortedBy { it.description }.forEach {
                items = items.addItem(buildRoutineListItem(it))
            }

            ListTemplate.Builder()
                .setLoading(false)
                .setTitle(carContext.getString(R.string.routine_page_title))
                .setSingleList(items.build())
                .setHeaderAction(Action.BACK)
                .setActionStrip(getHorizontalActionStrip())
                .build()
        }
    } catch (e: Exception) {
        Log.e(LOG_KEY, "Failed to build template", e)
        PaneTemplate.Builder(Pane.Builder().setLoading(true).build())
            .setHeaderAction(Action.BACK)
            .setTitle(carContext.getString(R.string.pref_aa_car_error))
            .build()
    }

    override fun onCreate(owner: LifecycleOwner) {
        carContext.registerReceiver(broadcastReceiver, IntentFilter().apply {
            addAction(ROUTINE_REJECTED_EVENT)
            addAction(ROUTINE_WORKFLOW_NOT_RUNNING_EVENT)
            addAction(ROUTINE_EXECUTION_FAILED_EVENT)
            addAction(ROUTINE_EXECUTED_SUCCESSFULLY_EVENT)
            addAction(ROUTINE_EXECUTION_NO_DATA_RECEIVED_EVENT)

            addAction(DATA_LOGGER_ADAPTER_NOT_SET_EVENT)
            addAction(DATA_LOGGER_CONNECTING_EVENT)
            addAction(DATA_LOGGER_STOPPED_EVENT)
            addAction(DATA_LOGGER_ERROR_EVENT)
            addAction(DATA_LOGGER_CONNECTED_EVENT)
            addAction(DATA_LOGGER_NO_NETWORK_EVENT)
            addAction(DATA_LOGGER_ERROR_CONNECT_EVENT)
        })
    }

    override fun onDestroy(owner: LifecycleOwner) {
        carContext.unregisterReceiver(broadcastReceiver)
    }

    private fun buildRoutineListItem(data: PidDefinition): Row = Row.Builder()
        .setOnClickListener {
            Log.i(LOG_KEY, "Executing routine ${data.description}")
            dataLogger.executeRoutine(Query.instance(QueryStrategyType.ROUTINES_QUERY).update(setOf(data.id)))
        }
        .setBrowsable(false)
        .addText(data.longDescription?:data.description)
        .setTitle(data.description)
        .build()

    private fun getHorizontalActionStrip(): ActionStrip {
        var builder = ActionStrip.Builder()

        builder = if (dataLogger.isRunning()) {
            builder.addAction(createAction(carContext, R.drawable.action_disconnect, mapColor(settings.colorTheme().actionsBtnDisconnectColor)) {
                stopDataLogging()
                toast.show(carContext, R.string.toast_connection_disconnect)
            })
        } else {
            builder.addAction(createAction(carContext, R.drawable.actions_connect, mapColor(settings.colorTheme().actionsBtnConnectColor)) {
                query.setStrategy(QueryStrategyType.ROUTINES_QUERY)
                dataLogger.start(query)

            })
        }


        builder = builder.addAction(createAction(carContext, R.drawable.action_exit, CarColor.RED) {
            try {
                stopDataLogging()
            } finally {
                Log.i(LOG_KEY, "Exiting the app. Closing the context")
                carContext.finishCarApp()
            }
        })


        return builder.build()
    }
}