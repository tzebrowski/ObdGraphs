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
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.lifecycle.LifecycleOwner
import org.obd.graphs.*
import org.obd.graphs.aa.*
import org.obd.graphs.aa.screen.*
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.bl.datalogger.dataLoggerPreferences
import org.obd.graphs.bl.query.Query
import org.obd.graphs.bl.query.QueryStrategyType
import org.obd.graphs.profile.PROFILE_CHANGED_EVENT
import org.obd.graphs.profile.PROFILE_RESET_EVENT
import org.obd.graphs.renderer.Fps
import org.obd.graphs.renderer.SurfaceRendererType
import org.obd.graphs.renderer.Identity

const val GAUGE_VIRTUAL_SCREEN_1_SETTINGS_CHANGED = "pref.aa.gauge.pids.profile_1.event.changed"
const val GAUGE_VIRTUAL_SCREEN_2_SETTINGS_CHANGED = "pref.aa.gauge.pids.profile_2.event.changed"
const val GAUGE_VIRTUAL_SCREEN_3_SETTINGS_CHANGED = "pref.aa.gauge.pids.profile_3.event.changed"
const val GAUGE_VIRTUAL_SCREEN_4_SETTINGS_CHANGED = "pref.aa.gauge.pids.profile_4.event.changed"


private enum class DefaultScreen(private val code: Int): Identity {
    NOT_SET(-1);

    override fun id(): Int {
        return this.code
    }
}

private const val LOW_FREQ_PID_SELECTION_CHANGED_EVENT = "pref.pids.generic.low.event.changed"
private const val LOG_TAG = "SurfaceRendererScreen"

internal class SurfaceRendererScreen(
    carContext: CarContext,
    settings: CarSettings,
    metricsCollector: MetricsCollector,
    fps: Fps,
    private val parent: NavTemplateCarScreen
) : CarScreen(carContext, settings, metricsCollector, fps) {

    private val query = Query.instance()

    private var screenId : Identity = SurfaceRendererType.GIULIA
    private val surfaceRendererController = SurfaceRendererController(carContext, settings, metricsCollector, fps, query)

    private var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            when (intent?.action) {
                AA_VIRTUAL_SCREEN_RENDERER_CHANGED_EVENT -> surfaceRendererController.allocateSurfaceRenderer(getSurfaceRendererType())
                AA_REFRESH_EVENT -> {
                    Log.i(LOG_TAG,"Received forced refresh screen event for screen ${screenId}. " +
                            "Is renderer: ${isSurfaceRendererScreen(screenId)}")

                    if (isSurfaceRendererScreen(screenId)) {

                        if (screenId == SurfaceRendererType.GAUGE || screenId == SurfaceRendererType.GIULIA) {
                            setCurrentVirtualScreen(getCurrentVirtualScreen())
                        }

                        updateQuery()
                        renderFrame()
                    }
                }

                AA_VIRTUAL_SCREEN_REFRESH_EVENT -> {
                    updateQuery()
                    renderFrame()
                }

                AA_TRIP_INFO_PID_SELECTION_CHANGED_EVENT -> {
                    updateQuery()
                    renderFrame()
                }

                AA_HIGH_FREQ_PID_SELECTION_CHANGED_EVENT -> {
                    updateQuery()
                    renderFrame()
                }

                LOW_FREQ_PID_SELECTION_CHANGED_EVENT -> {
                    updateQuery()
                    renderFrame()
                }

                GIULIA_VIRTUAL_SCREEN_1_SETTINGS_CHANGED, GAUGE_VIRTUAL_SCREEN_1_SETTINGS_CHANGED -> handlePIDsListChangedEvent(1)
                GIULIA_VIRTUAL_SCREEN_2_SETTINGS_CHANGED, GAUGE_VIRTUAL_SCREEN_2_SETTINGS_CHANGED -> handlePIDsListChangedEvent(2)
                GIULIA_VIRTUAL_SCREEN_3_SETTINGS_CHANGED, GAUGE_VIRTUAL_SCREEN_3_SETTINGS_CHANGED -> handlePIDsListChangedEvent(3)
                GIULIA_VIRTUAL_SCREEN_4_SETTINGS_CHANGED, GAUGE_VIRTUAL_SCREEN_4_SETTINGS_CHANGED ->  handlePIDsListChangedEvent(4)

                PROFILE_CHANGED_EVENT -> {
                    settings.handleProfileChanged()
                    updateQuery()
                    surfaceRendererController.allocateSurfaceRenderer(getSurfaceRendererType())
                    renderFrame()
                }

                PROFILE_RESET_EVENT -> {
                    updateQuery()
                    surfaceRendererController.allocateSurfaceRenderer(getSurfaceRendererType())
                    renderFrame()
                }
            }
        }
        private fun handlePIDsListChangedEvent(id: Int) {
            if (getCurrentVirtualScreen() == id) {
                setCurrentVirtualScreen(id)
            }
            updateQuery()
            renderFrame()
        }
    }


    fun getLifecycleObserver() = surfaceRendererController

    fun resetSurfaceRenderer(){
        screenId = DefaultScreen.NOT_SET
    }


    fun switchSurfaceRenderer(screenId: Identity) {
        this.screenId = screenId
        Log.i(LOG_TAG, "Switch to new surface renderer screen: ${this.screenId} and updating query...")

        when (this.screenId as SurfaceRendererType){
            SurfaceRendererType.GIULIA, SurfaceRendererType.GAUGE -> {

                metricsCollector.applyFilter(enabled = getSelectedPIDs())

                if (dataLoggerPreferences.instance.individualQueryStrategyEnabled) {
                    query.setStrategy(QueryStrategyType.INDIVIDUAL_QUERY_FOR_EACH_VIEW)
                    query.update(metricsCollector.getMetrics().map { p -> p.source.command.pid.id }.toSet())
                } else {
                    query.setStrategy(QueryStrategyType.SHARED_QUERY)
                }

                dataLogger.updateQuery(query = query)
                surfaceRendererController.allocateSurfaceRenderer(screenId as SurfaceRendererType)
            }

            SurfaceRendererType.DRAG_RACING -> {
                dataLogger.updateQuery(query = query.apply {
                    setStrategy(QueryStrategyType.DRAG_RACING_QUERY)
                })
                surfaceRendererController.allocateSurfaceRenderer(surfaceRendererType = SurfaceRendererType.DRAG_RACING)
            }

            SurfaceRendererType.TRIP_INFO -> {

                dataLogger.updateQuery(query = query.apply {
                    setStrategy(QueryStrategyType.TRIP_INFO_QUERY)
                })
                surfaceRendererController.allocateSurfaceRenderer(surfaceRendererType = SurfaceRendererType.TRIP_INFO)
            }

            SurfaceRendererType.PERFORMANCE -> {

                dataLogger.updateQuery(query = query.apply {
                    setStrategy(QueryStrategyType.PERFORMANCE)
                })
                surfaceRendererController.allocateSurfaceRenderer(surfaceRendererType = SurfaceRendererType.PERFORMANCE)
            }
        }

        renderFrame()
    }

    override fun actionStartDataLogging(){
        Log.e(LOG_TAG, "1 Action start data logging for $screenId")
        when (screenId) {
            SurfaceRendererType.GIULIA , SurfaceRendererType.GAUGE -> {
                if (dataLoggerPreferences.instance.individualQueryStrategyEnabled) {
                    query.setStrategy(QueryStrategyType.INDIVIDUAL_QUERY_FOR_EACH_VIEW)
                    query.update(metricsCollector.getMetrics().map { p -> p.source.command.pid.id }.toSet())
                } else {
                    query.setStrategy(QueryStrategyType.SHARED_QUERY)
                }
                dataLogger.start(query)
            }

            SurfaceRendererType.DRAG_RACING ->
                dataLogger.start(query.apply{
                    setStrategy(QueryStrategyType.DRAG_RACING_QUERY)
                })

            SurfaceRendererType.TRIP_INFO ->
                dataLogger.start(query.apply{
                    setStrategy(QueryStrategyType.TRIP_INFO_QUERY)
                })

            SurfaceRendererType.PERFORMANCE ->
                dataLogger.start(query.apply{
                    setStrategy(QueryStrategyType.PERFORMANCE)
                })
        }
    }

    fun isSurfaceRendererScreen(identity: Identity) = identity is SurfaceRendererType
    override fun getFeatureDescription(): List<FeatureDescription>  = mutableListOf(
        FeatureDescription(SurfaceRendererType.DRAG_RACING, org.obd.graphs.commons.R.drawable.action_drag_race,
            carContext.getString(R.string.available_features_drag_race_screen_title)),
        FeatureDescription(SurfaceRendererType.GAUGE, R.drawable.action_gauge, carContext.getString(R.string.available_features_gauge_screen_title)),
        FeatureDescription(SurfaceRendererType.GIULIA, R.drawable.action_giulia_metics, carContext.getString(R.string.available_features_giulia_screen_title)))
            .apply {
                if (settings.getTripInfoScreenSettings().viewEnabled) {
                    add(
                        FeatureDescription(
                            SurfaceRendererType.TRIP_INFO,
                            R.drawable.action_giulia,
                            carContext.getString(R.string.available_features_trip_info_screen_title)
                        )
                    )
                }
                if (settings.getDynamicScreenSettings().viewEnabled) {
                    add(
                        FeatureDescription(SurfaceRendererType.PERFORMANCE, org.obd.graphs.commons.R.drawable.action_drag_race,
                            carContext.getString(R.string.available_features_performance_screen_title)),
                    )
                }
             }

    fun renderFrame() {
        if (isSurfaceRendererScreen(screenId)) {
            surfaceRendererController.renderFrame()
        }
    }

    override fun onCarConfigurationChanged() {
        if (isSurfaceRendererScreen(screenId)) {
            surfaceRendererController.renderFrame()
        }
    }

    override fun onGetTemplate(): Template {
        var template = NavigationTemplate.Builder()

        if (screenId == SurfaceRendererType.GIULIA || screenId == SurfaceRendererType.GAUGE) {
            getVerticalActionStrip()?.let {
                template = template.setMapActionStrip(it)
            }
        }

        return template.setActionStrip(getHorizontalActionStrip()).build()
    }

    override fun onCreate(owner: LifecycleOwner) {
        registerReceiver(carContext,broadcastReceiver) {
            it.addAction(AA_HIGH_FREQ_PID_SELECTION_CHANGED_EVENT)
            it.addAction(LOW_FREQ_PID_SELECTION_CHANGED_EVENT)
            it.addAction(GIULIA_VIRTUAL_SCREEN_1_SETTINGS_CHANGED)
            it.addAction(GIULIA_VIRTUAL_SCREEN_2_SETTINGS_CHANGED)
            it.addAction(GIULIA_VIRTUAL_SCREEN_3_SETTINGS_CHANGED)
            it.addAction(GIULIA_VIRTUAL_SCREEN_4_SETTINGS_CHANGED)
            it.addAction(PROFILE_CHANGED_EVENT)
            it.addAction(PROFILE_RESET_EVENT)
            it.addAction(AA_VIRTUAL_SCREEN_REFRESH_EVENT)
            it.addAction(AA_VIRTUAL_SCREEN_RENDERER_CHANGED_EVENT)
            it.addAction(AA_REFRESH_EVENT)
            it.addAction(AA_TRIP_INFO_PID_SELECTION_CHANGED_EVENT)

            it.addAction(GAUGE_VIRTUAL_SCREEN_1_SETTINGS_CHANGED)
            it.addAction(GAUGE_VIRTUAL_SCREEN_2_SETTINGS_CHANGED)
            it.addAction(GAUGE_VIRTUAL_SCREEN_3_SETTINGS_CHANGED)
            it.addAction(GAUGE_VIRTUAL_SCREEN_4_SETTINGS_CHANGED)
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        carContext.unregisterReceiver(broadcastReceiver)
    }

    private fun updateQuery() {

        if (isSurfaceRendererScreen(screenId)) {

            val selectedPIDs = getSelectedPIDs()

            metricsCollector.applyFilter(enabled = selectedPIDs)

            if (screenId == SurfaceRendererType.DRAG_RACING) {
                Log.i(LOG_TAG, "Updating query for  DRAG_RACING_SCREEN_ID screen")

                query.setStrategy(QueryStrategyType.DRAG_RACING_QUERY)
                metricsCollector.applyFilter(enabled = query.getIDs())
                Log.i(LOG_TAG, "User selection PIDs=${query.getIDs()}")
                dataLogger.updateQuery(query)

            } else if (screenId == SurfaceRendererType.TRIP_INFO) {
                Log.i(LOG_TAG, "Updating query for  TRIP_INFO_SCREEN_ID screen")

                query.setStrategy(QueryStrategyType.TRIP_INFO_QUERY)
                metricsCollector.applyFilter(enabled = query.getIDs())
                Log.i(LOG_TAG, "User selection PIDs=${query.getIDs()}")
                dataLogger.updateQuery(query)

            } else if (screenId == SurfaceRendererType.PERFORMANCE) {
                Log.i(LOG_TAG, "Updating query for  DYNAMIC_SCREEN_ID screen")

                query.setStrategy(QueryStrategyType.PERFORMANCE)
                metricsCollector.applyFilter(enabled = query.getIDs())
                Log.i(LOG_TAG, "User selection PIDs=${query.getIDs()}")
                dataLogger.updateQuery(query)

            } else if (dataLoggerPreferences.instance.individualQueryStrategyEnabled) {
                Log.i(LOG_TAG, "Updating query for  individualQueryStrategyEnabled")

                metricsCollector.applyFilter(enabled = selectedPIDs, order = sortOrder())

                query.setStrategy(QueryStrategyType.INDIVIDUAL_QUERY_FOR_EACH_VIEW)
                query.update(metricsCollector.getMetrics().map { p -> p.source.command.pid.id }.toSet())
                Log.i(LOG_TAG, "User selection PIDs=${selectedPIDs}")

                dataLogger.updateQuery(query)
            } else {
                Log.i(LOG_TAG, "Updating query to SHARED_QUERY strategy")

                query.setStrategy(QueryStrategyType.SHARED_QUERY)
                val query = query.getIDs()
                val intersection = selectedPIDs.filter { query.contains(it) }.toSet()

                Log.i(LOG_TAG, "Query=$query,user selection=$selectedPIDs, intersection=$intersection")

                metricsCollector.applyFilter(enabled = intersection, order = sortOrder())
            }
        } else {
            Log.i(LOG_TAG, "Do not update the query. It's not surface renderer screen.")
        }
    }


    private fun setCurrentVirtualScreen(id: Int) = when (screenId) {
        SurfaceRendererType.GIULIA -> settings.getGiuliaRendererSetting().setVirtualScreen(id)
        SurfaceRendererType.GAUGE -> settings.getGaugeRendererSetting().setVirtualScreen(id)
        else -> {}
    }

    private fun getCurrentVirtualScreen() = when (screenId) {
        SurfaceRendererType.GIULIA ->  settings.getGiuliaRendererSetting().getVirtualScreen()
        SurfaceRendererType.GAUGE -> settings.getGaugeRendererSetting().getVirtualScreen()
        else -> -1
    }


    private fun getSelectedPIDs(): Set<Long>  =  if (screenId == SurfaceRendererType.GIULIA) {
            settings.getGiuliaRendererSetting().selectedPIDs
        } else {
            settings.getGaugeRendererSetting().selectedPIDs
        }

    private fun getVerticalActionStrip(): ActionStrip? {

        var added = false
        var builder = ActionStrip.Builder()

        mapOf(1 to R.drawable.action_virtual_screen_1,
            2 to R.drawable.action_virtual_screen_2,
            3 to R.drawable.action_virtual_screen_3,
            4 to R.drawable.action_virtual_screen_4).forEach { (k, v) ->
            if (settings.isVirtualScreenEnabled(k)) {
                added = true

                val color = if (getCurrentVirtualScreen() == k) {
                    CarColor.GREEN
                } else {
                    mapColor(settings.getColorTheme().actionsBtnVirtualScreensColor)
                }

                builder = builder.addAction(createAction(carContext, v, color) {
                    parent.invalidate()
                    setCurrentVirtualScreen(k)
                    updateQuery()
                    renderFrame()
                })
            }
        }

        return if (added) {
            builder.build()
        } else {
            null
        }
    }

    private fun getSurfaceRendererType (): SurfaceRendererType =
        if (screenId is SurfaceRendererType) screenId as SurfaceRendererType else  SurfaceRendererType.GIULIA

    private fun sortOrder(): Map<Long, Int>?  = if (this.screenId == SurfaceRendererType.GIULIA) {
        settings.getGiuliaRendererSetting().getPIDsSortOrder()
    } else {
        settings.getGaugeRendererSetting().getPIDsSortOrder()
    }
}
