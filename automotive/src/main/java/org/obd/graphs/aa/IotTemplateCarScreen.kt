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
package org.obd.graphs.aa

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.SpannableString
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.model.*
import androidx.lifecycle.LifecycleOwner
import org.obd.graphs.*
import org.obd.graphs.aa.iot.valueToIcon
import org.obd.graphs.bl.collector.CarMetric
import org.obd.graphs.bl.collector.CarMetricsCollector
import org.obd.graphs.bl.datalogger.*
import org.obd.graphs.renderer.DynamicSelectorMode


private const val LOG_KEY = "CarScreen"
private const val VIRTUAL_SCREEN_1_SETTINGS_CHANGED = "pref.aa.pids.profile_1.event.changed"
private const val VIRTUAL_SCREEN_2_SETTINGS_CHANGED = "pref.aa.pids.profile_2.event.changed"
private const val VIRTUAL_SCREEN_3_SETTINGS_CHANGED = "pref.aa.pids.profile_3.event.changed"
private const val VIRTUAL_SCREEN_4_SETTINGS_CHANGED = "pref.aa.pids.profile_4.event.changed"


internal class IotTemplateCarScreen(
    carContext: CarContext,
    settings: CarSettings,
    metricsCollector: CarMetricsCollector,
) : AbstractCarScreen(carContext, settings, metricsCollector) {


    private var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            when (intent?.action) {
                EVENT_DYNAMIC_SELECTOR_MODE_NORMAL -> settings.dynamicSelectorChangedEvent(DynamicSelectorMode.NORMAL)
                EVENT_DYNAMIC_SELECTOR_MODE_RACE -> settings.dynamicSelectorChangedEvent(DynamicSelectorMode.RACE)
                EVENT_DYNAMIC_SELECTOR_MODE_ECO -> settings.dynamicSelectorChangedEvent(DynamicSelectorMode.ECO)
                EVENT_DYNAMIC_SELECTOR_MODE_SPORT -> settings.dynamicSelectorChangedEvent(DynamicSelectorMode.SPORT)

                AA_VIRTUAL_SCREEN_VISIBILITY_CHANGED_EVENT -> invalidate()
                AA_VIRTUAL_SCREEN_REFRESH_EVENT -> invalidate()

                SURFACE_BROKEN_EVENT -> {
                    Log.d(LOG_KEY, "Received event about ")
                    renderingThread.stop()
                    carContext.finishCarApp()
                }
                MAIN_ACTIVITY_EVENT_DESTROYED -> {
                    Log.v(LOG_KEY, "Main activity has been destroyed.")
                    invalidate()
                }
                MAIN_ACTIVITY_EVENT_PAUSE -> {
                    Log.v(LOG_KEY, "Main activity is going to the background.")
                    invalidate()
                }

                SURFACE_AREA_CHANGED_EVENT -> {
                    Log.v(LOG_KEY,"Surface area changed")
                    invalidate()
                    submitRenderingTask()
                }

                VIRTUAL_SCREEN_1_SETTINGS_CHANGED -> {
                    if (settings.getCurrentVirtualScreen() == VIRTUAL_SCREEN_1) {
                        settings.applyVirtualScreen1()
                        metricsCollector.applyFilter(settings.getSelectedPIDs())
                        invalidate()
                    }
                }

                VIRTUAL_SCREEN_2_SETTINGS_CHANGED -> {
                    if (settings.getCurrentVirtualScreen() == VIRTUAL_SCREEN_2) {
                        settings.applyVirtualScreen2()
                        metricsCollector.applyFilter(settings.getSelectedPIDs())
                        invalidate()
                    }
                }

                VIRTUAL_SCREEN_3_SETTINGS_CHANGED -> {
                    if (settings.getCurrentVirtualScreen() == VIRTUAL_SCREEN_3) {
                        settings.applyVirtualScreen3()
                        metricsCollector.applyFilter(settings.getSelectedPIDs())
                        invalidate()
                    }
                }

                VIRTUAL_SCREEN_4_SETTINGS_CHANGED -> {
                    if (settings.getCurrentVirtualScreen() == VIRTUAL_SCREEN_4) {
                        settings.applyVirtualScreen4()
                        metricsCollector.applyFilter(settings.getSelectedPIDs())
                        invalidate()
                    }
                }

                PROFILE_CHANGED_EVENT -> {
                    metricsCollector.applyFilter(settings.getSelectedPIDs())
                    invalidate()
                }

                DATA_LOGGER_CONNECTING_EVENT -> {
                    invalidate()
                }

                DATA_LOGGER_NO_NETWORK_EVENT -> {
                    toast.show(carContext, R.string.main_activity_toast_connection_no_network)
                }

                DATA_LOGGER_ERROR_EVENT -> {
                    invalidate()
                    toast.show(carContext, R.string.main_activity_toast_connection_error)
                }

                DATA_LOGGER_STOPPED_EVENT -> {
                    toast.show(carContext, R.string.main_activity_toast_connection_stopped)
                    renderingThread.stop()
                    invalidate()
                }

                DATA_LOGGER_CONNECTED_EVENT -> {
                    toast.show(carContext, R.string.main_activity_toast_connection_established)
                    renderingThread.start()
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
        }
    }


    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        carContext.registerReceiver(broadcastReceiver, IntentFilter().apply {
            addAction(DATA_LOGGER_ADAPTER_NOT_SET_EVENT)
            addAction(DATA_LOGGER_CONNECTING_EVENT)
            addAction(DATA_LOGGER_STOPPED_EVENT)
            addAction(DATA_LOGGER_ERROR_EVENT)
            addAction(DATA_LOGGER_CONNECTED_EVENT)
            addAction(DATA_LOGGER_NO_NETWORK_EVENT)
            addAction(DATA_LOGGER_ERROR_CONNECT_EVENT)
            addAction(PROFILE_CHANGED_EVENT)
            addAction(VIRTUAL_SCREEN_1_SETTINGS_CHANGED)
            addAction(VIRTUAL_SCREEN_2_SETTINGS_CHANGED)
            addAction(VIRTUAL_SCREEN_3_SETTINGS_CHANGED)
            addAction(VIRTUAL_SCREEN_4_SETTINGS_CHANGED)
            addAction(SURFACE_DESTROYED_EVENT)
            addAction(SURFACE_AREA_CHANGED_EVENT)
            addAction(MAIN_ACTIVITY_EVENT_DESTROYED)
            addAction(MAIN_ACTIVITY_EVENT_PAUSE)
            addAction(SURFACE_BROKEN_EVENT)

            addAction(EVENT_DYNAMIC_SELECTOR_MODE_NORMAL)
            addAction(EVENT_DYNAMIC_SELECTOR_MODE_ECO)
            addAction(EVENT_DYNAMIC_SELECTOR_MODE_SPORT)
            addAction(EVENT_DYNAMIC_SELECTOR_MODE_RACE)

            addAction(AA_VIRTUAL_SCREEN_RENDERER_CHANGED_EVENT)
            addAction(AA_VIRTUAL_SCREEN_REFRESH_EVENT)
            addAction(AA_VIRTUAL_SCREEN_VISIBILITY_CHANGED_EVENT)
        })
    }

    override fun renderAction() {
        invalidate()
    }


    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        carContext.unregisterReceiver(broadcastReceiver)
    }

    override fun onGetTemplate(): Template  =
            if (dataLogger.status() == WorkflowStatus.Connecting) {
                GridTemplate.Builder()
                    .setTitle(carContext.resources.getString(R.string.app_name))
                    .setLoading(true)
                    .setActionStrip(getActionStrip(prefsEnabled = false))
                    .setHeaderAction(Action.APP_ICON)
                    .build()
            } else {

                metricsCollector.applyFilter(settings.getSelectedPIDs())
                var paneBuilder = Pane.Builder()

                paneBuilder = paneBuilder.addAction(createAction(R.drawable.action_virtual_screen_1,
                    mapColor(settings.colorTheme().actionsBtnVirtualScreensColor)) {

                    settings.applyVirtualScreen1()
                    metricsCollector.applyFilter(settings.getSelectedPIDs())
                    invalidate()
                })

                paneBuilder = paneBuilder.addAction(createAction(R.drawable.action_virtual_screen_2,
                    mapColor(settings.colorTheme().actionsBtnVirtualScreensColor)) {

                    settings.applyVirtualScreen2()
                    metricsCollector.applyFilter(settings.getSelectedPIDs())
                    invalidate()
                })



                metricsCollector.metrics().forEach {
                    paneBuilder.addRow(Row
                        .Builder()
                        .setImage(valueToIcon(carContext,it.valueToString()),Row.IMAGE_TYPE_LARGE)
                        .setMetadata(Metadata.Builder().build())
                        .setTitle(title(it))
                        .build())
                }

                PaneTemplate.Builder(paneBuilder.build())
                    .setTitle(carContext.resources.getString(R.string.app_name))
                    .setActionStrip(getActionStrip(prefsEnabled = false))
                    .build()
            }

    private fun title(metric: CarMetric): SpannableString {
        val title = StringBuilder()
        title.append(metric.source.command.pid.description.replace("\n",""))
        title.append("\n")
        title.append("· min:${metric.toNumber(metric.min)} avg: ${metric.toNumber(metric.mean)} max: ${metric.toNumber(metric.max)}")
        return  SpannableString(title)
    }

    init {

        lifecycle.addObserver(this)
        dataLogger.observe(this) {
            metricsCollector.append(it)
        }

        submitRenderingTask()
    }
}