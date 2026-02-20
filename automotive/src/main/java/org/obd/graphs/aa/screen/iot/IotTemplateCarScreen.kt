 /**
 * Copyright 2019-2026, Tomasz Żebrowski
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
package org.obd.graphs.aa.screen.iot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.SpannableString
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.model.Action
import androidx.car.app.model.GridTemplate
import androidx.car.app.model.Metadata
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.LifecycleOwner
import org.obd.graphs.AA_VIRTUAL_SCREEN_RENDERER_CHANGED_EVENT
import org.obd.graphs.MAIN_ACTIVITY_EVENT_DESTROYED
import org.obd.graphs.MAIN_ACTIVITY_EVENT_PAUSE
import org.obd.graphs.SCREEN_REFRESH_EVENT
import org.obd.graphs.aa.CarSettings
import org.obd.graphs.aa.R
import org.obd.graphs.aa.mapColor
import org.obd.graphs.aa.screen.CarScreen
import org.obd.graphs.aa.screen.EVENT_DYNAMIC_SELECTOR_MODE_ECO
import org.obd.graphs.aa.screen.EVENT_DYNAMIC_SELECTOR_MODE_NORMAL
import org.obd.graphs.aa.screen.EVENT_DYNAMIC_SELECTOR_MODE_RACE
import org.obd.graphs.aa.screen.EVENT_DYNAMIC_SELECTOR_MODE_SPORT
import org.obd.graphs.aa.screen.GIULIA_VIRTUAL_SCREEN_1_SETTINGS_CHANGED
import org.obd.graphs.aa.screen.GIULIA_VIRTUAL_SCREEN_2_SETTINGS_CHANGED
import org.obd.graphs.aa.screen.GIULIA_VIRTUAL_SCREEN_3_SETTINGS_CHANGED
import org.obd.graphs.aa.screen.GIULIA_VIRTUAL_SCREEN_4_SETTINGS_CHANGED
import org.obd.graphs.aa.screen.createAction
import org.obd.graphs.aa.screen.dynamicSelectorModeEventBroadcaster
import org.obd.graphs.aa.screen.withDataLogger
import org.obd.graphs.aa.toast
import org.obd.graphs.bl.collector.Metric
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.datalogger.DATA_LOGGER_ADAPTER_NOT_SET_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_CONNECTED_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_CONNECTING_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_ERROR_CONNECT_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_ERROR_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_NO_NETWORK_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_STOPPED_EVENT
import org.obd.graphs.bl.datalogger.DataLoggerRepository
import org.obd.graphs.bl.datalogger.WorkflowStatus
import org.obd.graphs.bl.datalogger.dataLoggerSettings
import org.obd.graphs.bl.query.Query
import org.obd.graphs.bl.query.QueryStrategyType
import org.obd.graphs.format
import org.obd.graphs.profile.PROFILE_CHANGED_EVENT
import org.obd.graphs.registerReceiver
import org.obd.graphs.renderer.api.DynamicSelectorMode

private const val LOG_TAG = "IotTemplateCarScreen"

internal class IotTemplateCarScreen(
    carContext: CarContext,
    settings: CarSettings,
    metricsCollector: MetricsCollector,
) : CarScreen(carContext, settings, metricsCollector) {
    private val valueDrawable = ValueDrawable(carContext)
    private val query = Query.instance(QueryStrategyType.INDIVIDUAL_QUERY)

    private var broadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                when (intent?.action) {
                    EVENT_DYNAMIC_SELECTOR_MODE_NORMAL -> settings.dynamicSelectorChangedEvent(DynamicSelectorMode.NORMAL)
                    EVENT_DYNAMIC_SELECTOR_MODE_RACE -> settings.dynamicSelectorChangedEvent(DynamicSelectorMode.RACE)
                    EVENT_DYNAMIC_SELECTOR_MODE_ECO -> settings.dynamicSelectorChangedEvent(DynamicSelectorMode.ECO)
                    EVENT_DYNAMIC_SELECTOR_MODE_SPORT -> settings.dynamicSelectorChangedEvent(DynamicSelectorMode.SPORT)

                    SCREEN_REFRESH_EVENT -> invalidate()

                    MAIN_ACTIVITY_EVENT_DESTROYED -> {
                        Log.v(LOG_TAG, "Main activity has been destroyed.")
                        invalidate()
                    }

                    MAIN_ACTIVITY_EVENT_PAUSE -> {
                        Log.v(LOG_TAG, "Main activity is going to the background.")
                        invalidate()
                    }

                    GIULIA_VIRTUAL_SCREEN_1_SETTINGS_CHANGED -> {
                        if (settings.getGiuliaRendererSetting().getVirtualScreen() == 1) {
                            settings.getGiuliaRendererSetting().setVirtualScreen(1)
                            applyMetricsFilter()
                            invalidate()
                        }
                    }

                    GIULIA_VIRTUAL_SCREEN_2_SETTINGS_CHANGED -> {
                        if (settings.getGiuliaRendererSetting().getVirtualScreen() == 2) {
                            settings.getGiuliaRendererSetting().setVirtualScreen(2)
                            applyMetricsFilter()
                            invalidate()
                        }
                    }

                    GIULIA_VIRTUAL_SCREEN_3_SETTINGS_CHANGED -> {
                        if (settings.getGiuliaRendererSetting().getVirtualScreen() == 3) {
                            settings.getGiuliaRendererSetting().setVirtualScreen(3)
                            applyMetricsFilter()
                            invalidate()
                        }
                    }

                    GIULIA_VIRTUAL_SCREEN_4_SETTINGS_CHANGED -> {
                        if (settings.getGiuliaRendererSetting().getVirtualScreen() == 4) {
                            settings.getGiuliaRendererSetting().setVirtualScreen(4)
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
                        metricsCollector.reset()
                    }

                    DATA_LOGGER_NO_NETWORK_EVENT -> {
                        toast.show(carContext, org.obd.graphs.commons.R.string.main_activity_toast_connection_no_network)
                    }

                    DATA_LOGGER_ERROR_EVENT -> {
                        invalidate()
                        toast.show(carContext, org.obd.graphs.commons.R.string.main_activity_toast_connection_error)
                    }

                    DATA_LOGGER_STOPPED_EVENT -> {
                        toast.show(carContext, org.obd.graphs.commons.R.string.main_activity_toast_connection_stopped)
                        renderingThread.stop()
                        invalidate()
                    }

                    DATA_LOGGER_CONNECTED_EVENT -> {
                        toast.show(carContext, org.obd.graphs.commons.R.string.main_activity_toast_connection_established)
                        renderingThread.start()
                        invalidate()
                    }

                    DATA_LOGGER_ERROR_CONNECT_EVENT -> {
                        invalidate()
                        toast.show(carContext, org.obd.graphs.commons.R.string.main_activity_toast_connection_connect_error)
                    }

                    DATA_LOGGER_ADAPTER_NOT_SET_EVENT -> {
                        invalidate()
                        toast.show(carContext, org.obd.graphs.commons.R.string.main_activity_toast_adapter_is_not_selected)
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
            it.addAction(GIULIA_VIRTUAL_SCREEN_1_SETTINGS_CHANGED)
            it.addAction(GIULIA_VIRTUAL_SCREEN_2_SETTINGS_CHANGED)
            it.addAction(GIULIA_VIRTUAL_SCREEN_3_SETTINGS_CHANGED)
            it.addAction(GIULIA_VIRTUAL_SCREEN_4_SETTINGS_CHANGED)
            it.addAction(MAIN_ACTIVITY_EVENT_DESTROYED)
            it.addAction(MAIN_ACTIVITY_EVENT_PAUSE)

            it.addAction(EVENT_DYNAMIC_SELECTOR_MODE_NORMAL)
            it.addAction(EVENT_DYNAMIC_SELECTOR_MODE_ECO)
            it.addAction(EVENT_DYNAMIC_SELECTOR_MODE_SPORT)
            it.addAction(EVENT_DYNAMIC_SELECTOR_MODE_RACE)

            it.addAction(AA_VIRTUAL_SCREEN_RENDERER_CHANGED_EVENT)
            it.addAction(SCREEN_REFRESH_EVENT)
        }
    }

    override fun startDataLogging() {
        if (dataLoggerSettings.instance().adapter.individualQueryStrategyEnabled) {
            query.setStrategy(QueryStrategyType.INDIVIDUAL_QUERY)
            query.update(metricsCollector.getMetrics().map { p -> p.source.command.pid.id }.toSet())
        } else {
            query.setStrategy(QueryStrategyType.SHARED_QUERY)
        }

        withDataLogger {
            start(query)
        }
    }

    override fun renderAction() {
        invalidate()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        carContext.unregisterReceiver(broadcastReceiver)
    }

    override fun onGetTemplate(): Template =
        if (DataLoggerRepository.status() == WorkflowStatus.Connecting) {
            GridTemplate
                .Builder()
                .setTitle(carContext.resources.getString(R.string.app_name))
                .setLoading(true)
                .setActionStrip(getHorizontalActionStrip(preferencesEnabled = false))
                .setHeaderAction(Action.APP_ICON)
                .build()
        } else {
            applyMetricsFilter()
            var paneBuilder = Pane.Builder()

            paneBuilder =
                paneBuilder.addAction(
                    createAction(
                        carContext,
                        R.drawable.action_virtual_screen_1,
                        mapColor(settings.getColorTheme().actionsBtnVirtualScreensColor),
                    ) {
                        settings.getGiuliaRendererSetting().setVirtualScreen(1)
                        applyMetricsFilter()
                        invalidate()
                    },
                )

            paneBuilder =
                paneBuilder.addAction(
                    createAction(
                        carContext,
                        R.drawable.action_virtual_screen_2,
                        mapColor(settings.getColorTheme().actionsBtnVirtualScreensColor),
                    ) {
                        settings.getGiuliaRendererSetting().setVirtualScreen(2)
                        applyMetricsFilter()
                        invalidate()
                    },
                )

            metricsCollector.getMetrics().forEach {
                paneBuilder.addRow(
                    Row
                        .Builder()
                        .setImage(
                            valueDrawable.draw(it.source.format(castToInt = false), settings.getColorTheme().progressColor),
                            Row.IMAGE_TYPE_LARGE,
                        ).setMetadata(Metadata.Builder().build())
                        .setTitle(getTitleFor(it))
                        .build(),
                )
            }

            PaneTemplate
                .Builder(paneBuilder.build())
                .setTitle(carContext.resources.getString(R.string.app_name))
                .setActionStrip(getHorizontalActionStrip(preferencesEnabled = false))
                .build()
        }

    private fun applyMetricsFilter() {
        metricsCollector.applyFilter(settings.getGiuliaRendererSetting().selectedPIDs)

        if (dataLoggerSettings.instance().adapter.individualQueryStrategyEnabled) {
            query.update(metricsCollector.getMetrics().map { p -> p.source.command.pid.id }.toSet())
            withDataLogger {
                updateQuery(query)
            }
        }
    }

    private fun getTitleFor(metric: Metric): SpannableString {
        val title = StringBuilder()
        title.append(
            metric.source.command.pid.description
                .replace("\n", ""),
        )
        title.append("\n")
        val pid = metric.pid
        title.append("· min:${metric.min.format(pid)} avg: ${metric.mean.format(pid)} max: ${metric.max.format(pid)}")
        return SpannableString(title)
    }

    init {
        Log.i(LOG_TAG, "IotTemplate Screen Init")
        lifecycle.addObserver(this)
        DataLoggerRepository.observe(this) {
            metricsCollector.append(it)
        }
        DataLoggerRepository.observe(dynamicSelectorModeEventBroadcaster)
        submitRenderingTask()
    }
}
