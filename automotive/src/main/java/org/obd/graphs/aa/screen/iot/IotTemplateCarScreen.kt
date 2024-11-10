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
package org.obd.graphs.aa.screen.iot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.SpannableString
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.model.*
import androidx.car.app.model.Metadata
import androidx.lifecycle.LifecycleOwner
import org.obd.graphs.*
import org.obd.graphs.aa.*
import org.obd.graphs.aa.CarSettings
import org.obd.graphs.aa.mapColor
import org.obd.graphs.aa.screen.*
import org.obd.graphs.aa.screen.CarScreen
import org.obd.graphs.aa.toast
import org.obd.graphs.bl.collector.Metric
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.datalogger.*
import org.obd.graphs.bl.query.Query
import org.obd.graphs.bl.query.QueryStrategyType
import org.obd.graphs.profile.PROFILE_CHANGED_EVENT
import org.obd.graphs.renderer.DynamicSelectorMode

private const val LOG_TAG = "IotTemplateCarScreen"
internal class IotTemplateCarScreen(
    carContext: CarContext,
    settings: CarSettings,
    metricsCollector: MetricsCollector,
) : CarScreen(carContext, settings, metricsCollector) {

    private val valueDrawable = ValueDrawable(carContext)
    private val query = Query.instance(QueryStrategyType.INDIVIDUAL_QUERY_FOR_EACH_VIEW)

    private var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            when (intent?.action) {
                EVENT_DYNAMIC_SELECTOR_MODE_NORMAL -> settings.dynamicSelectorChangedEvent(DynamicSelectorMode.NORMAL)
                EVENT_DYNAMIC_SELECTOR_MODE_RACE -> settings.dynamicSelectorChangedEvent(DynamicSelectorMode.RACE)
                EVENT_DYNAMIC_SELECTOR_MODE_ECO -> settings.dynamicSelectorChangedEvent(DynamicSelectorMode.ECO)
                EVENT_DYNAMIC_SELECTOR_MODE_SPORT -> settings.dynamicSelectorChangedEvent(DynamicSelectorMode.SPORT)

                AA_VIRTUAL_SCREEN_VISIBILITY_CHANGED_EVENT -> invalidate()
                AA_VIRTUAL_SCREEN_REFRESH_EVENT -> invalidate()

                MAIN_ACTIVITY_EVENT_DESTROYED -> {
                    Log.v(LOG_TAG, "Main activity has been destroyed.")
                    invalidate()
                }
                MAIN_ACTIVITY_EVENT_PAUSE -> {
                    Log.v(LOG_TAG, "Main activity is going to the background.")
                    invalidate()
                }

                VIRTUAL_SCREEN_1_SETTINGS_CHANGED -> {
                    if (settings.getGiuliaRendererSetting().getCurrentVirtualScreen() == VIRTUAL_SCREEN_1) {
                        settings.getGiuliaRendererSetting().applyVirtualScreen1()
                        applyMetricsFilter()
                        invalidate()
                    }
                }

                VIRTUAL_SCREEN_2_SETTINGS_CHANGED -> {
                    if (settings.getGiuliaRendererSetting().getCurrentVirtualScreen() == VIRTUAL_SCREEN_2) {
                        settings.getGiuliaRendererSetting().applyVirtualScreen2()
                        applyMetricsFilter()
                        invalidate()
                    }
                }

                VIRTUAL_SCREEN_3_SETTINGS_CHANGED -> {
                    if (settings.getGiuliaRendererSetting().getCurrentVirtualScreen() == VIRTUAL_SCREEN_3) {
                        settings.getGiuliaRendererSetting().applyVirtualScreen3()
                        applyMetricsFilter()
                        invalidate()
                    }
                }

                VIRTUAL_SCREEN_4_SETTINGS_CHANGED -> {
                    if (settings.getGiuliaRendererSetting().getCurrentVirtualScreen() == VIRTUAL_SCREEN_4) {
                        settings.getGiuliaRendererSetting().applyVirtualScreen4()
                        applyMetricsFilter()
                        invalidate()
                    }
                }

                PROFILE_CHANGED_EVENT -> {
                    applyMetricsFilter()
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
        registerReceiver(carContext, broadcastReceiver) {
            it.addAction(DATA_LOGGER_ADAPTER_NOT_SET_EVENT)
            it.addAction(DATA_LOGGER_CONNECTING_EVENT)
            it.addAction(DATA_LOGGER_STOPPED_EVENT)
            it.addAction(DATA_LOGGER_ERROR_EVENT)
            it.addAction(DATA_LOGGER_CONNECTED_EVENT)
            it.addAction(DATA_LOGGER_NO_NETWORK_EVENT)
            it.addAction(DATA_LOGGER_ERROR_CONNECT_EVENT)
            it.addAction(PROFILE_CHANGED_EVENT)
            it.addAction(VIRTUAL_SCREEN_1_SETTINGS_CHANGED)
            it.addAction(VIRTUAL_SCREEN_2_SETTINGS_CHANGED)
            it.addAction(VIRTUAL_SCREEN_3_SETTINGS_CHANGED)
            it.addAction(VIRTUAL_SCREEN_4_SETTINGS_CHANGED)
            it.addAction(MAIN_ACTIVITY_EVENT_DESTROYED)
            it.addAction(MAIN_ACTIVITY_EVENT_PAUSE)

            it.addAction(EVENT_DYNAMIC_SELECTOR_MODE_NORMAL)
            it.addAction(EVENT_DYNAMIC_SELECTOR_MODE_ECO)
            it.addAction(EVENT_DYNAMIC_SELECTOR_MODE_SPORT)
            it.addAction(EVENT_DYNAMIC_SELECTOR_MODE_RACE)

            it.addAction(AA_VIRTUAL_SCREEN_RENDERER_CHANGED_EVENT)
            it.addAction(AA_VIRTUAL_SCREEN_REFRESH_EVENT)
            it.addAction(AA_VIRTUAL_SCREEN_VISIBILITY_CHANGED_EVENT)
        }
    }

    override fun actionStartDataLogging() {
        if (dataLoggerPreferences.instance.individualQueryStrategyEnabled) {
            query.setStrategy(QueryStrategyType.INDIVIDUAL_QUERY_FOR_EACH_VIEW)
            query.update(metricsCollector.getMetrics().map { p -> p.source.command.pid.id }.toSet())
        } else {
            query.setStrategy(QueryStrategyType.SHARED_QUERY)
        }
        dataLogger.start(query)
    }
    override fun renderAction() {
        invalidate()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        carContext.unregisterReceiver(broadcastReceiver)
    }

    override fun onGetTemplate(): Template =
            if (dataLogger.status() == WorkflowStatus.Connecting) {
                GridTemplate.Builder()
                    .setTitle(carContext.resources.getString(R.string.app_name))
                    .setLoading(true)
                    .setActionStrip(getHorizontalActionStrip(preferencesEnabled = false))
                    .setHeaderAction(Action.APP_ICON)
                    .build()
            } else {

                applyMetricsFilter()
                var paneBuilder = Pane.Builder()

                paneBuilder = paneBuilder.addAction(createAction(carContext,
                    R.drawable.action_virtual_screen_1,
                    mapColor(settings.getColorTheme().actionsBtnVirtualScreensColor)
                ) {

                    settings.getGiuliaRendererSetting().applyVirtualScreen1()
                    applyMetricsFilter()
                    invalidate()
                })

                paneBuilder = paneBuilder.addAction(createAction(carContext,
                    R.drawable.action_virtual_screen_2,
                    mapColor(settings.getColorTheme().actionsBtnVirtualScreensColor)
                ) {

                    settings.getGiuliaRendererSetting().applyVirtualScreen2()
                    applyMetricsFilter()
                    invalidate()
                })

                metricsCollector.getMetrics().forEach {
                    paneBuilder.addRow(
                        Row.Builder()
                        .setImage(valueDrawable.draw(it.valueToString(),settings.getColorTheme().progressColor),
                            Row.IMAGE_TYPE_LARGE
                        )
                        .setMetadata(Metadata.Builder().build())
                        .setTitle(getTitleFor(it))
                        .build())
                }

                PaneTemplate.Builder(paneBuilder.build())
                    .setTitle(carContext.resources.getString(R.string.app_name))
                    .setActionStrip(getHorizontalActionStrip(preferencesEnabled = false))
                    .build()
            }

    private fun applyMetricsFilter() {
        metricsCollector.applyFilter(settings.getGiuliaRendererSetting().selectedPIDs)

        if (dataLoggerPreferences.instance.individualQueryStrategyEnabled) {
            query.update(metricsCollector.getMetrics().map { p-> p.source.command.pid.id }.toSet())
            dataLogger.updateQuery(query)
        }
    }

    private fun getTitleFor(metric: Metric): SpannableString {
        val title = StringBuilder()
        title.append(metric.source.command.pid.description.replace("\n",""))
        title.append("\n")
        title.append("· min:${metric.toNumber(metric.min)} avg: ${metric.toNumber(metric.mean)} max: ${metric.toNumber(metric.max)}")
        return SpannableString(title)
    }

    init {
        Log.i(LOG_TAG, "IotTemplate Screen Init")
        lifecycle.addObserver(this)
        dataLogger.observe(this) {
            metricsCollector.append(it)
        }
        dataLogger.observe(dynamicSelectorModeEventBroadcaster)
        submitRenderingTask()
        registerConnectionStateReceiver()
    }
}