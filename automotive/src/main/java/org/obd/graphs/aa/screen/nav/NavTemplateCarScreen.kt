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
package org.obd.graphs.aa.screen.nav

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.connection.CarConnection
import androidx.car.app.model.*
import androidx.car.app.navigation.NavigationManager
import androidx.car.app.navigation.NavigationManagerCallback
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.RoutingInfo
import androidx.lifecycle.LifecycleOwner
import org.obd.graphs.*
import org.obd.graphs.aa.*
import org.obd.graphs.aa.CarSettings
import org.obd.graphs.aa.screen.*
import org.obd.graphs.aa.screen.CarScreen
import org.obd.graphs.bl.collector.CarMetricsCollector
import org.obd.graphs.bl.datalogger.*
import org.obd.graphs.bl.query.QueryStrategyType
import org.obd.graphs.profile.PROFILE_CHANGED_EVENT
import org.obd.graphs.profile.PROFILE_RESET_EVENT
import org.obd.graphs.renderer.DynamicSelectorMode
import org.obd.graphs.renderer.Fps

const val SURFACE_DESTROYED_EVENT = "car.event.surface.destroyed"
const val SURFACE_AREA_CHANGED_EVENT = "car.event.surface.area_changed"
const val SURFACE_BROKEN_EVENT = "car.event.surface_broken.event"

private const val HIGH_FREQ_PID_SELECTION_CHANGED_EVENT = "pref.pids.generic.high.event.changed"
private const val LOW_FREQ_PID_SELECTION_CHANGED_EVENT = "pref.pids.generic.low.event.changed"

internal class NavTemplateCarScreen(
    carContext: CarContext,
    settings: CarSettings,
    metricsCollector: CarMetricsCollector,
    fps: Fps
) : CarScreen(carContext, settings, metricsCollector, fps) {

    private val surfaceController = SurfaceController(carContext, settings, metricsCollector, fps, query)

    private var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            when (intent?.action) {

                EVENT_DYNAMIC_SELECTOR_MODE_NORMAL -> settings.dynamicSelectorChangedEvent(DynamicSelectorMode.NORMAL)
                EVENT_DYNAMIC_SELECTOR_MODE_RACE -> settings.dynamicSelectorChangedEvent(DynamicSelectorMode.RACE)
                EVENT_DYNAMIC_SELECTOR_MODE_ECO -> settings.dynamicSelectorChangedEvent(DynamicSelectorMode.ECO)
                EVENT_DYNAMIC_SELECTOR_MODE_SPORT -> settings.dynamicSelectorChangedEvent(DynamicSelectorMode.SPORT)
                AA_VIRTUAL_SCREEN_VISIBILITY_CHANGED_EVENT -> invalidate()
                AA_VIRTUAL_SCREEN_RENDERER_CHANGED_EVENT -> surfaceController.allocateSurfaceRender()

                HIGH_FREQ_PID_SELECTION_CHANGED_EVENT -> {
                    applyMetricsFilter()
                    surfaceController.renderFrame()
                }

                LOW_FREQ_PID_SELECTION_CHANGED_EVENT -> {
                    applyMetricsFilter()
                    surfaceController.renderFrame()
                }

                AA_VIRTUAL_SCREEN_RENDERER_TOGGLE_EVENT -> {
                    surfaceController.toggleSurfaceRenderer()
                    invalidate()
                }

                AA_VIRTUAL_SCREEN_REFRESH_EVENT -> surfaceController.renderFrame()

                SURFACE_BROKEN_EVENT -> {
                    Log.d(LOG_KEY, "Received event about ")
                    cancelRenderingTask()
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

                SURFACE_DESTROYED_EVENT -> {
                    cancelRenderingTask()
                }

                SURFACE_AREA_CHANGED_EVENT -> {
                    Log.v(LOG_KEY,"Surface area changed")
                    invalidate()
                    submitRenderingTask()
                }

                VIRTUAL_SCREEN_1_SETTINGS_CHANGED -> {
                    if (settings.getCurrentVirtualScreen() == VIRTUAL_SCREEN_1) {
                        settings.applyVirtualScreen1()
                    }
                    applyMetricsFilter()
                    surfaceController.renderFrame()
                }

                VIRTUAL_SCREEN_2_SETTINGS_CHANGED -> {
                    if (settings.getCurrentVirtualScreen() == VIRTUAL_SCREEN_2) {
                        settings.applyVirtualScreen2()
                    }

                    applyMetricsFilter()
                    surfaceController.renderFrame()
                }

                VIRTUAL_SCREEN_3_SETTINGS_CHANGED -> {
                    if (settings.getCurrentVirtualScreen() == VIRTUAL_SCREEN_3) {
                        settings.applyVirtualScreen3()
                    }
                    applyMetricsFilter()
                    surfaceController.renderFrame()
                }

                VIRTUAL_SCREEN_4_SETTINGS_CHANGED -> {
                    if (settings.getCurrentVirtualScreen() == VIRTUAL_SCREEN_4) {
                        settings.applyVirtualScreen4()
                    }
                    applyMetricsFilter()
                    surfaceController.renderFrame()
                }

                PROFILE_CHANGED_EVENT -> {
                    applyMetricsFilter()
                    surfaceController.allocateSurfaceRender()
                    surfaceController.renderFrame()
                }

                PROFILE_RESET_EVENT -> {
                    applyMetricsFilter()
                    surfaceController.allocateSurfaceRender()
                    surfaceController.renderFrame()
                }

                DATA_LOGGER_CONNECTING_EVENT -> {
                    surfaceController.renderFrame()
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
                    cancelRenderingTask()
                    invalidate()
                    surfaceController.renderFrame()
                    navigationManager().navigationEnded()
                }

                DATA_LOGGER_CONNECTED_EVENT -> {
                    toast.show(carContext, R.string.main_activity_toast_connection_established)
                    renderingThread.start()
                    fps.start()
                    invalidate()
                    navigationManager().navigationStarted()
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
            addAction(PROFILE_RESET_EVENT)
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
            addAction(CarConnection.ACTION_CAR_CONNECTION_UPDATED)
            addAction(AA_VIRTUAL_SCREEN_RENDERER_TOGGLE_EVENT)
            addAction(HIGH_FREQ_PID_SELECTION_CHANGED_EVENT)
            addAction(LOW_FREQ_PID_SELECTION_CHANGED_EVENT)

        })
    }

    override fun onCarConfigurationChanged() {
        surfaceController.onCarConfigurationChanged()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        lifecycle.addObserver(surfaceController)
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        lifecycle.removeObserver(surfaceController)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        lifecycle.removeObserver(surfaceController)
        carContext.unregisterReceiver(broadcastReceiver)
    }

    override fun renderAction() {
        surfaceController.renderFrame()
    }

    override fun onGetTemplate(): Template  = try {
            settings.initItemsSortOrder()

            if (dataLogger.status() == WorkflowStatus.Connecting) {
                NavigationTemplate.Builder()
                    .setNavigationInfo(RoutingInfo.Builder().setLoading(true).build())
                    .setActionStrip(getActionStrip(toggleBtnColor = surfaceController.getToggleSurfaceRendererBtnColor()))
                    .build()
            } else {
                var template = NavigationTemplate.Builder()

                if (surfaceController.isVirtualScreensEnabled()) {
                    getActionStrip()?.let {
                        template = template.setMapActionStrip(it)
                    }
                }

                template.setActionStrip(
                    getActionStrip(
                        dragMeteringEnabled = surfaceController.isDragRacingEnabled(),
                        toggleBtnColor = surfaceController.getToggleSurfaceRendererBtnColor())
                ).build()
            }
        } catch (e: Exception) {
            Log.e(LOG_KEY, "Failed to build template", e)
            PaneTemplate.Builder(Pane.Builder().setLoading(true).build())
                .setHeaderAction(Action.BACK)
                .setTitle(carContext.getString(R.string.pref_aa_car_error))
                .build()
        }
    private fun actionStripColor(key: String): CarColor =  if (settings.getCurrentVirtualScreen() == key) {
        CarColor.GREEN
    } else {
        mapColor(settings.colorTheme().actionsBtnVirtualScreensColor)
    }

    private fun getActionStrip(): ActionStrip? {

        var added  = false
        var builder = ActionStrip.Builder()

        if (settings.isVirtualScreenEnabled(1)) {
            added = true

            builder = builder.addAction(createAction(R.drawable.action_virtual_screen_1, actionStripColor(VIRTUAL_SCREEN_1)) {
                invalidate()
                settings.applyVirtualScreen1()
                applyMetricsFilter()
                surfaceController.renderFrame()
            })
        }

        if (settings.isVirtualScreenEnabled(2)) {

            added = true
            builder = builder.addAction(createAction(R.drawable.action_virtual_screen_2,  actionStripColor(VIRTUAL_SCREEN_2)) {
                invalidate()
                settings.applyVirtualScreen2()
                applyMetricsFilter()
                surfaceController.renderFrame()
            })
        }

        if (settings.isVirtualScreenEnabled(3)) {

            added = true
            builder = builder.addAction(createAction(R.drawable.action_virtual_screen_3, actionStripColor(VIRTUAL_SCREEN_3)) {
                invalidate()
                settings.applyVirtualScreen3()
                applyMetricsFilter()
                surfaceController.renderFrame()
            })
        }

        if (settings.isVirtualScreenEnabled(4)) {
            added = true

            builder = builder.addAction(createAction(R.drawable.action_virtual_screen_4,  actionStripColor(VIRTUAL_SCREEN_4)) {
                invalidate()
                settings.applyVirtualScreen4()
                applyMetricsFilter()
                surfaceController.renderFrame()
            })
        }
        return if (added) {
            builder.build()
        } else {
            null
        }
    }

    private fun applyMetricsFilter() {
        if (dataLoggerPreferences.instance.queryForEachViewStrategyEnabled) {
            Log.i(LOG_KEY, "User selection PIDs=${settings.getSelectedPIDs()}")

            metricsCollector.applyFilter(enabled = settings.getSelectedPIDs(), order = settings.getPIDsSortOrder())

            query.setStrategy(QueryStrategyType.INDIVIDUAL_QUERY_FOR_EACH_VIEW)
            query.update(metricsCollector.getMetrics().map { p-> p.source.command.pid.id }.toSet())
            dataLogger.updateQuery(query)
        } else {
            query.setStrategy(QueryStrategyType.SHARED_QUERY)
            val query = query.getPIDs()
            val selection = settings.getSelectedPIDs()
            val intersection =  selection.filter { query.contains(it) }.toSet()

            Log.i(LOG_KEY,"Query=$query,user selection=$selection, intersection=$intersection")

            metricsCollector.applyFilter(enabled = intersection, order = settings.getPIDsSortOrder())
        }
    }

    init {

        lifecycle.addObserver(this)
        lifecycle.addObserver(surfaceController)

        dataLogger.observe(this) {
            metricsCollector.append(it)
        }

        dataLogger.observe(DynamicSelectorModeEventBroadcaster())

        submitRenderingTask()

        navigationManager().setNavigationManagerCallback(
            object : NavigationManagerCallback {
                override fun onStopNavigation() {

                    try {
                        renderingThread.stop()
                        surfaceController.renderFrame()
                        fps.stop()
                        invalidate()
                    } catch (e: Throwable) {
                        Log.e(LOG_KEY, "Failed to stop DL threads", e)
                    }
                }

                override fun onAutoDriveEnabled() {

                }
            })

        registerConnectionStateReceiver()
    }

    private fun navigationManager() = carContext.getCarService(NavigationManager::class.java)
}