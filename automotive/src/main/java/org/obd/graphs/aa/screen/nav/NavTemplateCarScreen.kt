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
import androidx.car.app.model.Action
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Template
import androidx.car.app.navigation.NavigationManager
import androidx.car.app.navigation.NavigationManagerCallback
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.RoutingInfo
import androidx.lifecycle.LifecycleOwner
import org.obd.graphs.*
import org.obd.graphs.aa.CarSettings
import org.obd.graphs.aa.R
import org.obd.graphs.aa.screen.*
import org.obd.graphs.aa.toast
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.datalogger.*
import org.obd.graphs.renderer.DynamicSelectorMode
import org.obd.graphs.renderer.Fps


const val SURFACE_DESTROYED_EVENT = "car.event.surface.destroyed"
const val SURFACE_AREA_CHANGED_EVENT = "car.event.surface.area_changed"
const val SURFACE_BROKEN_EVENT = "car.event.surface_broken.event"

internal class NavTemplateCarScreen(
    carContext: CarContext,
    settings: CarSettings,
    metricsCollector: MetricsCollector,
    fps: Fps
) : CarScreen(carContext, settings, metricsCollector, fps) {

    private val screenNavigator = ScreenNavigator(settings)

    private val routineScreen = RoutinesScreen(carContext, settings, metricsCollector, fps, screenNavigator = screenNavigator)
    private val surfaceScreen = SurfaceScreen(carContext, settings, metricsCollector, fps, parent = this, screenNavigator = screenNavigator)

    private var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            when (intent?.action) {

                EVENT_DYNAMIC_SELECTOR_MODE_NORMAL -> settings.dynamicSelectorChangedEvent(DynamicSelectorMode.NORMAL)
                EVENT_DYNAMIC_SELECTOR_MODE_RACE -> settings.dynamicSelectorChangedEvent(DynamicSelectorMode.RACE)
                EVENT_DYNAMIC_SELECTOR_MODE_ECO -> settings.dynamicSelectorChangedEvent(DynamicSelectorMode.ECO)
                EVENT_DYNAMIC_SELECTOR_MODE_SPORT -> settings.dynamicSelectorChangedEvent(DynamicSelectorMode.SPORT)
                AA_VIRTUAL_SCREEN_VISIBILITY_CHANGED_EVENT -> invalidate()

                AA_VIRTUAL_SCREEN_RENDERER_TOGGLE_EVENT -> {
                    surfaceScreen.toggleSurfaceRenderer(screenNavigator.nextScreenId())
                    invalidate()
                }

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

                SURFACE_DESTROYED_EVENT -> cancelRenderingTask()

                SURFACE_AREA_CHANGED_EVENT -> {
                    Log.v(LOG_KEY,"Surface area changed")
                    try {
                        invalidate()
                        submitRenderingTask()
                    }catch (e: java.lang.Exception){
                        Log.w(LOG_KEY,"Failed when received SURFACE_AREA_CHANGED_EVENT event",e)
                    }
                }

                DATA_LOGGER_CONNECTING_EVENT -> {
                    surfaceScreen.renderFrame()
                    try {
                        invalidate()
                    } catch (e: Exception){
                        Log.w(LOG_KEY,"Failed when received DATA_LOGGER_CONNECTING_EVENT event",e)
                    }
                }

                DATA_LOGGER_NO_NETWORK_EVENT -> toast.show(carContext, R.string.main_activity_toast_connection_no_network)

                DATA_LOGGER_ERROR_EVENT -> {
                    try {
                        invalidate()
                        toast.show(carContext, R.string.main_activity_toast_connection_error)
                    } catch (e: Exception){
                        Log.w(LOG_KEY,"Failed when received DATA_LOGGER_ERROR_EVENT event",e)
                    }
                }

                DATA_LOGGER_STOPPED_EVENT -> {

                    try {
                        toast.show(carContext, R.string.main_activity_toast_connection_stopped)
                        cancelRenderingTask()
                        invalidate()
                        surfaceScreen.renderFrame()
                        navigationManager().navigationEnded()
                    } catch (e: Exception){
                        Log.w(LOG_KEY,"Failed when received DATA_LOGGER_STOPPED_EVENT event",e)
                    }
                }

                DATA_LOGGER_CONNECTED_EVENT -> {
                    try {
                        toast.show(carContext, R.string.main_activity_toast_connection_established)
                        renderingThread.start()
                        fps.start()
                        invalidate()
                        navigationManager().navigationStarted()
                    }catch (e: Exception){
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

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        surfaceScreen.onCreate(owner)
        routineScreen.onCreate(owner)

        carContext.registerReceiver(broadcastReceiver, IntentFilter().apply {
            addAction(DATA_LOGGER_ADAPTER_NOT_SET_EVENT)
            addAction(DATA_LOGGER_CONNECTING_EVENT)
            addAction(DATA_LOGGER_STOPPED_EVENT)
            addAction(DATA_LOGGER_ERROR_EVENT)
            addAction(DATA_LOGGER_CONNECTED_EVENT)
            addAction(DATA_LOGGER_NO_NETWORK_EVENT)
            addAction(DATA_LOGGER_ERROR_CONNECT_EVENT)
            addAction(SURFACE_DESTROYED_EVENT)
            addAction(SURFACE_AREA_CHANGED_EVENT)
            addAction(MAIN_ACTIVITY_EVENT_DESTROYED)
            addAction(MAIN_ACTIVITY_EVENT_PAUSE)
            addAction(SURFACE_BROKEN_EVENT)

            addAction(EVENT_DYNAMIC_SELECTOR_MODE_NORMAL)
            addAction(EVENT_DYNAMIC_SELECTOR_MODE_ECO)
            addAction(EVENT_DYNAMIC_SELECTOR_MODE_SPORT)
            addAction(EVENT_DYNAMIC_SELECTOR_MODE_RACE)
            addAction(AA_VIRTUAL_SCREEN_VISIBILITY_CHANGED_EVENT)
            addAction(CarConnection.ACTION_CAR_CONNECTION_UPDATED)
            addAction(AA_VIRTUAL_SCREEN_RENDERER_TOGGLE_EVENT)
        })
    }

    override fun onCarConfigurationChanged() {
        super.onCarConfigurationChanged()
        surfaceScreen.onCarConfigurationChanged()
        routineScreen.onCarConfigurationChanged()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        lifecycle.addObserver(surfaceScreen.getLifecycleObserver())
        surfaceScreen.onResume(owner)
        routineScreen.onResume(owner)
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        lifecycle.removeObserver(surfaceScreen.getLifecycleObserver())
        surfaceScreen.onPause(owner)
        routineScreen.onPause(owner)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        lifecycle.removeObserver(surfaceScreen.getLifecycleObserver())
        surfaceScreen.onDestroy(owner)
        routineScreen.onDestroy(owner)
        carContext.unregisterReceiver(broadcastReceiver)
    }

    override fun renderAction() {
        surfaceScreen.renderFrame()
    }


    override fun onGetTemplate(): Template  = try {
            settings.initItemsSortOrder()

            if (dataLogger.status() == WorkflowStatus.Connecting) {
                NavigationTemplate.Builder()
                    .setNavigationInfo(RoutingInfo.Builder().setLoading(true).build())
                    .setActionStrip(getHorizontalActionStrip(toggleBtnColor = screenNavigator.getCurrentScreenBtnColor()))
                    .build()
            } else {
                when (screenNavigator.getCurrentScreenId()) {
                    ROUTINES_SCREEN_ID -> routineScreen.onGetTemplate()
                    else ->  surfaceScreen.onGetTemplate()
                }
            }
        } catch (e: Exception) {
            Log.e(LOG_KEY, "Failed to build template", e)
            PaneTemplate.Builder(Pane.Builder().setLoading(true).build())
                .setHeaderAction(Action.BACK)
                .setTitle(carContext.getString(R.string.pref_aa_car_error))
                .build()
        }

    init {

        lifecycle.addObserver(this)
        lifecycle.addObserver(surfaceScreen.getLifecycleObserver())

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
                        surfaceScreen.renderFrame()
                        fps.stop()
                        invalidate()
                    } catch (e: Throwable) {
                        Log.e(LOG_KEY, "Failed to stop DL threads", e)
                    }
                }
            })

        registerConnectionStateReceiver()
    }
    private fun navigationManager() = carContext.getCarService(NavigationManager::class.java)
}