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
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.car.app.navigation.NavigationManager
import androidx.car.app.navigation.NavigationManagerCallback
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.RoutingInfo
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.obd.graphs.*
import org.obd.graphs.bl.collector.CarMetricsCollector
import org.obd.graphs.bl.datalogger.*
import org.obd.graphs.renderer.DynamicSelectorMode
import org.obd.graphs.renderer.Fps


private const val LOG_KEY = "CarScreen"
private const val VIRTUAL_SCREEN_1_SETTINGS_CHANGED = "pref.aa.pids.profile_1.event.changed"
private const val VIRTUAL_SCREEN_2_SETTINGS_CHANGED = "pref.aa.pids.profile_2.event.changed"
private const val VIRTUAL_SCREEN_3_SETTINGS_CHANGED = "pref.aa.pids.profile_3.event.changed"
private const val VIRTUAL_SCREEN_4_SETTINGS_CHANGED = "pref.aa.pids.profile_4.event.changed"
const val SURFACE_DESTROYED_EVENT = "car.event.surface.destroyed"
const val SURFACE_AREA_CHANGED_EVENT = "car.event.surface.area_changed"
const val SURFACE_BROKEN_EVENT = "car.event.surface_broken.event"


internal class CarScreen(
    carContext: CarContext,
    private val surfaceController: SurfaceController,
    private val settings: CarSettings,
    private val metricsCollector: CarMetricsCollector,
    private val fps: Fps
) :
    Screen(carContext),
    DefaultLifecycleObserver {

    private val renderingThread: RenderingThread = RenderingThread(
        id = "AARenderingThread",
        renderAction = {
            surfaceController.renderFrame()
        },
        perfFrameRate = {
            settings.getSurfaceFrameRate()
        }
    )

    private var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            when (intent?.action) {
                EVENT_DYNAMIC_SELECTOR_MODE_NORMAL -> settings.dynamicSelectorChangedEvent(DynamicSelectorMode.NORMAL)
                EVENT_DYNAMIC_SELECTOR_MODE_RACE -> settings.dynamicSelectorChangedEvent(DynamicSelectorMode.RACE)
                EVENT_DYNAMIC_SELECTOR_MODE_ECO -> settings.dynamicSelectorChangedEvent(DynamicSelectorMode.ECO)
                EVENT_DYNAMIC_SELECTOR_MODE_SPORT -> settings.dynamicSelectorChangedEvent(DynamicSelectorMode.SPORT)

                AA_VIRTUAL_SCREEN_VISIBILITY_CHANGED_EVENT -> invalidate()

                AA_SCREEN_RENDERER_CHANGED_EVENT -> surfaceController.updateScreenRender()

                SURFACE_BROKEN_EVENT -> {
                    Log.d(LOG_KEY, "Received event about ")
                    renderingThread.stop()
                    fps.stop()
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
                    renderingThread.stop()
                    fps.stop()
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
                        surfaceController.renderFrame()
                    }
                }

                VIRTUAL_SCREEN_2_SETTINGS_CHANGED -> {
                    if (settings.getCurrentVirtualScreen() == VIRTUAL_SCREEN_2) {
                        settings.applyVirtualScreen2()
                        metricsCollector.applyFilter(settings.getSelectedPIDs())
                        surfaceController.renderFrame()
                    }
                }

                VIRTUAL_SCREEN_3_SETTINGS_CHANGED -> {
                    if (settings.getCurrentVirtualScreen() == VIRTUAL_SCREEN_3) {
                        settings.applyVirtualScreen3()
                        metricsCollector.applyFilter(settings.getSelectedPIDs())
                        surfaceController.renderFrame()
                    }
                }

                VIRTUAL_SCREEN_4_SETTINGS_CHANGED -> {
                    if (settings.getCurrentVirtualScreen() == VIRTUAL_SCREEN_4) {
                        settings.applyVirtualScreen4()
                        metricsCollector.applyFilter(settings.getSelectedPIDs())
                        surfaceController.renderFrame()
                    }
                }

                PROFILE_CHANGED_EVENT -> {
                    metricsCollector.applyFilter(settings.getSelectedPIDs())
                    surfaceController.updateScreenRender()
                    invalidate()
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
                    renderingThread.stop()
                    surfaceController.renderFrame()
                    fps.stop()
                    invalidate()
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

            addAction(AA_SCREEN_RENDERER_CHANGED_EVENT)
            addAction(AA_VIRTUAL_SCREEN_VISIBILITY_CHANGED_EVENT)
        })
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        carContext.unregisterReceiver(broadcastReceiver)
    }

    override fun onGetTemplate(): Template {
        return try {
            return if (dataLogger.status() == WorkflowStatus.Connecting) {
                NavigationTemplate.Builder()
                    .setNavigationInfo(RoutingInfo.Builder().setLoading(true).build())
                    .setActionStrip(actions())
                    .build()
            } else {
                var template = NavigationTemplate.Builder()

                profilesActionStrip()?.let {
                    template = template.setMapActionStrip(it)
                }

                template.setActionStrip(actions()).build()
            }
        } catch (e: Exception) {
            Log.e(LOG_KEY, "Failed to build template", e)
            PaneTemplate.Builder(Pane.Builder().setLoading(true).build())
                .setHeaderAction(Action.BACK)
                .setTitle(carContext.getString(R.string.pref_aa_car_error))
                .build()
        }
    }

    private fun createAction(iconResId: Int, iconColorTint: CarColor, func: () -> Unit): Action =
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

    private fun profilesActionStrip(): ActionStrip? {

        var added  = false
        var builder = ActionStrip.Builder()

        if (settings.isVirtualScreenEnabled(1)) {
            added = true
            builder = builder.addAction(createAction(R.drawable.action_virtual_screen_1, mapColor(settings.colorTheme().actionsBtnVirtualScreensColor)) {
                settings.applyVirtualScreen1()
                metricsCollector.applyFilter(settings.getSelectedPIDs())
                surfaceController.renderFrame()
            })
        }

        if (settings.isVirtualScreenEnabled(2)) {
            added = true
            builder = builder.addAction(createAction(R.drawable.action_virtual_screen_2, mapColor(settings.colorTheme().actionsBtnVirtualScreensColor)) {
                settings.applyVirtualScreen2()
                metricsCollector.applyFilter(settings.getSelectedPIDs())
                surfaceController.renderFrame()
            })
        }

        if (settings.isVirtualScreenEnabled(3)) {

            added = true
            builder = builder.addAction(createAction(R.drawable.action_virtual_screen_3, mapColor(settings.colorTheme().actionsBtnVirtualScreensColor)) {
                settings.applyVirtualScreen3()
                metricsCollector.applyFilter(settings.getSelectedPIDs())
                surfaceController.renderFrame()
            })
        }

        if (settings.isVirtualScreenEnabled(4)) {
            added = true
            builder = builder.addAction(createAction(R.drawable.action_virtual_screen_4, mapColor(settings.colorTheme().actionsBtnVirtualScreensColor)) {
                settings.applyVirtualScreen4()
                metricsCollector.applyFilter(settings.getSelectedPIDs())
                surfaceController.renderFrame()
            })
        }
        return if (added) {
            builder.build()
        } else {
            null
        }
    }

    private fun actions(): ActionStrip {
        var builder = ActionStrip.Builder()

        builder = if (dataLogger.isRunning()) {
            builder.addAction(createAction(R.drawable.action_disconnect, mapColor(settings.colorTheme().actionsBtnDisconnectColor)) {
                stopDataLogging()
                toast.show(carContext, R.string.toast_connection_disconnect)
            })
        } else {
            builder.addAction(createAction(R.drawable.actions_connect, mapColor(settings.colorTheme().actionsBtnConnectColor)) {
                dataLogger.start()
            })
        }

        builder = builder.addAction(createAction(R.drawable.config, CarColor.BLUE) {
            sendBroadcastEvent(AA_EDIT_PREF_SCREEN)
            toast.show(carContext, R.string.pref_aa_get_to_app_conf)
        })

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

    private fun stopDataLogging() {
        Log.i(LOG_KEY, "Stopping data logging process")
        dataLogger.stop()
        fps.stop()
        renderingThread.stop()
    }

    private fun submitRenderingTask() {
        if (!renderingThread.isRunning() && dataLogger.status() == WorkflowStatus.Connected) {
            renderingThread.start()
            fps.start()
        }
    }

    init {

        lifecycle.addObserver(this)
        dataLogger.observe(this) {
            metricsCollector.append(it)
        }

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
    }

    private fun navigationManager() = carContext.getCarService(NavigationManager::class.java)
}