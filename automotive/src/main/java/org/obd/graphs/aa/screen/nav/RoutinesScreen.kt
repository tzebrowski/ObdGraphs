 /**
 * Copyright 2019-2025, Tomasz Żebrowski
 *
 * <p>Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.obd.graphs.aa.screen.nav

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.model.*
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.LifecycleOwner
import org.obd.graphs.aa.CarSettings
import org.obd.graphs.aa.R
import org.obd.graphs.aa.mapColor
import org.obd.graphs.aa.screen.CarScreen
import org.obd.graphs.aa.screen.createAction
import org.obd.graphs.aa.toast
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.datalogger.*
import org.obd.graphs.bl.query.Query
import org.obd.graphs.bl.query.QueryStrategyType
import org.obd.graphs.registerReceiver
import org.obd.graphs.renderer.Fps
import org.obd.graphs.renderer.Identity
import org.obd.metrics.pid.PIDsGroup
import org.obd.metrics.pid.PidDefinition


private enum class RoutineScreen(private val code: Int): Identity {
    ROUTINES(222);

    override fun id(): Int = this.code
}

private const val LOG_TAG = "RoutinesScreen"

internal class RoutinesScreen(
    carContext: CarContext,
    settings: CarSettings,
    metricsCollector: MetricsCollector,
    fps: Fps
) : CarScreen(carContext, settings, metricsCollector, fps) {
    private var routineExecuting = false
    private var routineId = -1L
    private var routineExecutionSuccessfully = false
    private val query: Query = Query.instance(QueryStrategyType.ROUTINES_QUERY)

    private var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.i(LOG_TAG, "Received event=${intent?.action}")
            try {
                when (intent?.action) {
                    ROUTINE_WORKFLOW_NOT_RUNNING_EVENT -> {
                        toast.show(carContext, R.string.routine_workflow_is_not_running)
                        routineExecuting = false
                        routineExecutionSuccessfully = false
                        invalidate()
                    }
                    ROUTINE_UNKNOWN_STATUS_EVENT -> {
                        toast.show(carContext, R.string.routine_unknown_error)
                        routineExecuting = false
                        routineExecutionSuccessfully = false
                        invalidate()
                    }
                    ROUTINE_EXECUTION_FAILED_EVENT -> {
                        routineExecuting = false
                        routineExecutionSuccessfully = false
                        invalidate()
                        toast.show(carContext, R.string.routine_execution_failed)
                    }

                    ROUTINE_EXECUTED_SUCCESSFULLY_EVENT -> {
                        toast.show(carContext, R.string.routine_executed_successfully)
                        routineExecuting = false
                        routineExecutionSuccessfully = true
                        invalidate()
                    }

                    ROUTINE_EXECUTION_NO_DATA_RECEIVED_EVENT -> {
                        routineExecuting = false
                        routineExecutionSuccessfully = false
                        invalidate()
                        toast.show(carContext, R.string.routine_no_data)
                    }

                    DATA_LOGGER_CONNECTING_EVENT -> invalidate()

                    DATA_LOGGER_NO_NETWORK_EVENT -> toast.show(carContext, R.string.main_activity_toast_connection_no_network)
                    DATA_LOGGER_ERROR_EVENT -> {
                        invalidate()
                        toast.show(carContext, R.string.main_activity_toast_connection_error)
                    }

                    DATA_LOGGER_STOPPED_EVENT -> {
                        toast.show(carContext, R.string.main_activity_toast_connection_stopped)
                        invalidate()
                    }

                    DATA_LOGGER_CONNECTED_EVENT -> {
                        toast.show(carContext, R.string.main_activity_toast_connection_established)
                        invalidate()
                    }

                    DATA_LOGGER_ERROR_CONNECT_EVENT -> {
                        invalidate()
                        toast.show(carContext, R.string.main_activity_toast_connection_connect_error)
                    }

                    DATA_LOGGER_ADAPTER_NOT_SET_EVENT -> {
                        invalidate()
                        toast.show(carContext, R.string.main_activity_toast_adapter_is_not_selected)
                    }
                }
            } catch (e: Exception) {
                Log.w(LOG_TAG, "Failed event ${intent?.action} processing", e)
            }
        }
    }

    override fun getFeatureDescription(): List<FeatureDescription>  = mutableListOf<FeatureDescription>().apply {
        if (settings.getRoutinesScreenSettings().viewEnabled) {
            add(FeatureDescription( RoutineScreen.ROUTINES,
                R.drawable.action_features,
                carContext.getString(R.string.available_features_routine_screen_title)))
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        Log.i(LOG_TAG, "RoutinesScreen onResume")
        registerReceiver(carContext, broadcastReceiver) {
            it.addAction(ROUTINE_REJECTED_EVENT)
            it.addAction(ROUTINE_WORKFLOW_NOT_RUNNING_EVENT)
            it.addAction(ROUTINE_EXECUTION_FAILED_EVENT)
            it.addAction(ROUTINE_EXECUTED_SUCCESSFULLY_EVENT)
            it.addAction(ROUTINE_EXECUTION_NO_DATA_RECEIVED_EVENT)
            it.addAction(DATA_LOGGER_ADAPTER_NOT_SET_EVENT)
            it.addAction(DATA_LOGGER_CONNECTING_EVENT)
            it.addAction(DATA_LOGGER_STOPPED_EVENT)
            it.addAction(DATA_LOGGER_ERROR_EVENT)
            it.addAction(DATA_LOGGER_CONNECTED_EVENT)
            it.addAction(DATA_LOGGER_NO_NETWORK_EVENT)
            it.addAction(DATA_LOGGER_ERROR_CONNECT_EVENT)
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        Log.i(LOG_TAG, "RoutinesScreen onPause")
        carContext.unregisterReceiver(broadcastReceiver)
    }

    override fun actionStartDataLogging(){
        dataLogger.start(query)
    }


    override fun onGetTemplate(): Template = try {
        if (routineExecuting) {
            ListTemplate.Builder()
                .setLoading(true)
                .setTitle(carContext.getString(R.string.routine_execution_start))
                .setActionStrip(getHorizontalActionStrip()).build()
        } else if (dataLogger.status() == WorkflowStatus.Connecting) {
            ListTemplate.Builder()
                .setLoading(true)
                .setTitle(carContext.getString(R.string.routine_page_connecting))
                .setActionStrip(getHorizontalActionStrip()).build()
        } else {
            var items = ItemList.Builder()
            dataLogger.getPidDefinitionRegistry().findBy(PIDsGroup.ROUTINE)
                .sortedBy { it.description }
                .sortedBy { it.id != routineId }
                .forEach {
                    items = items.addItem(buildItem(it))
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
        Log.e(LOG_TAG, "Failed to build template", e)
        PaneTemplate.Builder(Pane.Builder().setLoading(true).build())
            .setHeaderAction(Action.BACK)
            .setTitle(carContext.getString(R.string.pref_aa_car_error))
            .build()
    }

    private fun buildItem(data: PidDefinition): Row {
        var rowBuilder = Row.Builder()
            .setOnClickListener {
                Log.i(LOG_TAG, "Executing routine ${data.description}")

                if (dataLogger.isRunning()) {
                    routineExecuting = true
                    routineId = data.id
                } else {
                    routineId = -1L
                }
                invalidate()
                dataLogger.executeRoutine(Query.instance(QueryStrategyType.ROUTINES_QUERY).update(setOf(data.id)))
            }
            .setBrowsable(false)

            .addText(data.longDescription ?: data.description)
            .setTitle(data.description)

        if (data.id == routineId) {
            rowBuilder = if (routineExecutionSuccessfully) {
                rowBuilder.setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(
                            carContext, android.R.drawable.ic_input_add
                        )
                    ).build()
                )
            } else {
                rowBuilder.setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(
                            carContext, android.R.drawable.ic_delete
                        )
                    ).build()
                )
            }
        }
        return rowBuilder.build()
    }

    private fun getHorizontalActionStrip(): ActionStrip {
        var builder = ActionStrip.Builder()

        builder = if (dataLogger.isRunning()) {
            builder.addAction(
                createAction(
                    carContext,
                    R.drawable.action_disconnect,
                    mapColor(settings.getColorTheme().actionsBtnDisconnectColor)
                ) {
                    actionStopDataLogging()
                    toast.show(carContext, R.string.toast_connection_disconnect)
                })
        } else {
            builder.addAction(createAction(carContext, R.drawable.actions_connect, mapColor(settings.getColorTheme().actionsBtnConnectColor)) {
                dataLogger.start(query)
            })
        }

        builder = builder.addAction(createAction(carContext, R.drawable.action_exit, CarColor.RED) {
            try {
                actionStopDataLogging()
            } finally {
                Log.i(LOG_TAG, "Exiting the app. Closing the context")
                carContext.finishCarApp()
            }
        })

        return builder.build()
    }
}