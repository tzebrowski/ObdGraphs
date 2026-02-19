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
import org.obd.graphs.AA_HIGH_FREQ_PID_SELECTION_CHANGED_EVENT
import org.obd.graphs.AA_VIRTUAL_SCREEN_RENDERER_CHANGED_EVENT
import org.obd.graphs.SCREEN_REFRESH_EVENT
import org.obd.graphs.aa.CarSettings
import org.obd.graphs.aa.R
import org.obd.graphs.aa.mapColor
import org.obd.graphs.aa.screen.CarScreen
import org.obd.graphs.aa.screen.GIULIA_VIRTUAL_SCREEN_1_SETTINGS_CHANGED
import org.obd.graphs.aa.screen.GIULIA_VIRTUAL_SCREEN_2_SETTINGS_CHANGED
import org.obd.graphs.aa.screen.GIULIA_VIRTUAL_SCREEN_3_SETTINGS_CHANGED
import org.obd.graphs.aa.screen.GIULIA_VIRTUAL_SCREEN_4_SETTINGS_CHANGED
import org.obd.graphs.aa.screen.createAction
import org.obd.graphs.aa.screen.withDataLogger
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.datalogger.dataLoggerSettings
import org.obd.graphs.bl.query.Query
import org.obd.graphs.bl.query.QueryStrategyType
import org.obd.graphs.profile.PROFILE_CHANGED_EVENT
import org.obd.graphs.profile.PROFILE_RESET_EVENT
import org.obd.graphs.registerReceiver
import org.obd.graphs.renderer.api.Fps
import org.obd.graphs.renderer.api.Identity
import org.obd.graphs.renderer.api.SurfaceRendererType

const val GAUGE_VIRTUAL_SCREEN_1_SETTINGS_CHANGED = "pref.aa.gauge.pids.profile_1.event.changed"
const val GAUGE_VIRTUAL_SCREEN_2_SETTINGS_CHANGED = "pref.aa.gauge.pids.profile_2.event.changed"
const val GAUGE_VIRTUAL_SCREEN_3_SETTINGS_CHANGED = "pref.aa.gauge.pids.profile_3.event.changed"
const val GAUGE_VIRTUAL_SCREEN_4_SETTINGS_CHANGED = "pref.aa.gauge.pids.profile_4.event.changed"

private const val AA_TRIP_INFO_PID_SELECTION_CHANGED_EVENT = "pref.aa.trip_info.pids.selected.event.changed"
private const val AA_PERFORMANCE_PID_SELECTION_CHANGED_EVENT = "pref.aa.performance.pids.selected.event.changed"

private enum class DefaultScreen(
    private val code: Int,
) : Identity {
    NOT_SET(-1),
    ;

    override fun id(): Int = this.code
}

private const val LOW_FREQ_PID_SELECTION_CHANGED_EVENT = "pref.pids.generic.low.event.changed"
private const val LOG_TAG = "SurfaceRendererScreen"

internal class SurfaceRendererScreen(
    carContext: CarContext,
    settings: CarSettings,
    metricsCollector: MetricsCollector,
    fps: Fps,
    private val parent: NavTemplateCarScreen,
) : CarScreen(carContext, settings, metricsCollector, fps) {
    private val query = Query.instance()

    private var screenId: Identity = SurfaceRendererType.GIULIA
    private val surfaceRendererController = SurfaceRendererController(carContext, settings, metricsCollector, fps, query)

    private var broadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                when (intent?.action) {
                    AA_VIRTUAL_SCREEN_RENDERER_CHANGED_EVENT -> {
                        surfaceRendererController.allocateSurfaceRenderer(getSurfaceRendererType())
                    }

                    SCREEN_REFRESH_EVENT,
                    AA_TRIP_INFO_PID_SELECTION_CHANGED_EVENT,
                    AA_PERFORMANCE_PID_SELECTION_CHANGED_EVENT,
                    AA_HIGH_FREQ_PID_SELECTION_CHANGED_EVENT,
                    LOW_FREQ_PID_SELECTION_CHANGED_EVENT,
                    -> {
                        updateQuery()
                        renderFrame()
                    }

                    GIULIA_VIRTUAL_SCREEN_1_SETTINGS_CHANGED, GAUGE_VIRTUAL_SCREEN_1_SETTINGS_CHANGED -> handlePIDsListChangedEvent(1)
                    GIULIA_VIRTUAL_SCREEN_2_SETTINGS_CHANGED, GAUGE_VIRTUAL_SCREEN_2_SETTINGS_CHANGED -> handlePIDsListChangedEvent(2)
                    GIULIA_VIRTUAL_SCREEN_3_SETTINGS_CHANGED, GAUGE_VIRTUAL_SCREEN_3_SETTINGS_CHANGED -> handlePIDsListChangedEvent(3)
                    GIULIA_VIRTUAL_SCREEN_4_SETTINGS_CHANGED, GAUGE_VIRTUAL_SCREEN_4_SETTINGS_CHANGED -> handlePIDsListChangedEvent(4)

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

    fun resetSurfaceRenderer() {
        screenId = DefaultScreen.NOT_SET
    }

    fun switchSurfaceRenderer(newScreenId: Identity) {
        this.screenId = newScreenId
        Log.d(LOG_TAG, "Switch to new surface renderer screen: ${this.screenId} and updating query...")

        if (newScreenId is SurfaceRendererType) {
            surfaceRendererController.allocateSurfaceRenderer(newScreenId)

            getQueryStrategyForScreen()?.let { strategy ->
                query.setStrategy(strategy)

                if (strategy == QueryStrategyType.INDIVIDUAL_QUERY) {
                    query.update(metricsCollector.getMetrics().map { p -> p.source.command.pid.id }.toSet())
                }
                withDataLogger {
                    updateQuery(query = query)
                }
            }
        }
        renderFrame()
    }

    override fun actionStartDataLogging() {
        val strategy = getQueryStrategyForScreen() ?: return

        query.setStrategy(strategy)

        if (strategy == QueryStrategyType.INDIVIDUAL_QUERY) {
            query.update(metricsCollector.getMetrics().map { p -> p.source.command.pid.id }.toSet())
        }

        withDataLogger {
            start(query)
        }
    }

    fun isSurfaceRendererScreen(identity: Identity) = identity is SurfaceRendererType

    override fun getFeatureDescription(): List<FeatureDescription> =
        mutableListOf(
            FeatureDescription(
                SurfaceRendererType.DRAG_RACING,
                org.obd.graphs.commons.R.drawable.action_drag_race,
                carContext.getString(R.string.available_features_drag_race_screen_title),
            ),
            FeatureDescription(
                SurfaceRendererType.GAUGE,
                R.drawable.action_gauge,
                carContext.getString(R.string.available_features_gauge_screen_title),
            ),
            FeatureDescription(
                SurfaceRendererType.GIULIA,
                R.drawable.action_giulia_metics,
                carContext.getString(R.string.available_features_giulia_screen_title),
            ),
        ).apply {
            if (settings.getTripInfoScreenSettings().viewEnabled) {
                add(
                    FeatureDescription(
                        SurfaceRendererType.TRIP_INFO,
                        R.drawable.action_giulia,
                        carContext.getString(R.string.available_features_trip_info_screen_title),
                    ),
                )
            }
            if (settings.getPerformanceScreenSettings().viewEnabled) {
                add(
                    FeatureDescription(
                        SurfaceRendererType.PERFORMANCE,
                        org.obd.graphs.commons.R.drawable.action_drag_race,
                        carContext.getString(R.string.available_features_performance_screen_title),
                    ),
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
        registerReceiver(carContext, broadcastReceiver) {
            it.addAction(AA_HIGH_FREQ_PID_SELECTION_CHANGED_EVENT)
            it.addAction(LOW_FREQ_PID_SELECTION_CHANGED_EVENT)
            it.addAction(GIULIA_VIRTUAL_SCREEN_1_SETTINGS_CHANGED)
            it.addAction(GIULIA_VIRTUAL_SCREEN_2_SETTINGS_CHANGED)
            it.addAction(GIULIA_VIRTUAL_SCREEN_3_SETTINGS_CHANGED)
            it.addAction(GIULIA_VIRTUAL_SCREEN_4_SETTINGS_CHANGED)
            it.addAction(PROFILE_CHANGED_EVENT)
            it.addAction(PROFILE_RESET_EVENT)
            it.addAction(SCREEN_REFRESH_EVENT)
            it.addAction(AA_VIRTUAL_SCREEN_RENDERER_CHANGED_EVENT)
            it.addAction(AA_TRIP_INFO_PID_SELECTION_CHANGED_EVENT)
            it.addAction(AA_PERFORMANCE_PID_SELECTION_CHANGED_EVENT)

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
        if (!isSurfaceRendererScreen(screenId)) {
            Log.i(LOG_TAG, "Do not update the query. It's not surface renderer screen.")
            return
        }

        val strategy = getQueryStrategyForScreen() ?: return
        query.setStrategy(strategy)

        when (strategy) {
            QueryStrategyType.DRAG_RACING_QUERY,
            QueryStrategyType.TRIP_INFO_QUERY,
            QueryStrategyType.PERFORMANCE_QUERY,
            -> {
                Log.i(LOG_TAG, "Updating query for $strategy screen")
                metricsCollector.applyFilter(enabled = query.getIDs())
                Log.i(LOG_TAG, "User selection PIDs=${query.getIDs()}")
            }
            QueryStrategyType.INDIVIDUAL_QUERY -> {
                Log.i(LOG_TAG, "Updating query for individualQueryStrategyEnabled")
                val selectedPIDs = getSelectedPIDs()
                metricsCollector.applyFilter(enabled = selectedPIDs, order = sortOrder())
                query.update(metricsCollector.getMetrics().map { p -> p.source.command.pid.id }.toSet())
                Log.i(LOG_TAG, "User selection PIDs=$selectedPIDs")
            }
            QueryStrategyType.SHARED_QUERY -> {
                Log.i(LOG_TAG, "Updating query to SHARED_QUERY strategy")
                val selectedPIDs = getSelectedPIDs()
                val queryIds = query.getIDs()
                val intersection = selectedPIDs.filter { queryIds.contains(it) }.toSet()

                Log.i(LOG_TAG, "Query=$queryIds, user selection=$selectedPIDs, intersection=$intersection")
                metricsCollector.applyFilter(enabled = intersection, order = sortOrder())
            }
            else -> {}
        }

        withDataLogger {
            updateQuery(query)
        }
    }

    private fun getQueryStrategyForScreen(): QueryStrategyType? =
        when (screenId) {
            SurfaceRendererType.DRAG_RACING -> QueryStrategyType.DRAG_RACING_QUERY
            SurfaceRendererType.TRIP_INFO -> QueryStrategyType.TRIP_INFO_QUERY
            SurfaceRendererType.PERFORMANCE -> QueryStrategyType.PERFORMANCE_QUERY
            SurfaceRendererType.GIULIA, SurfaceRendererType.GAUGE -> {
                if (dataLoggerSettings.instance().adapter.individualQueryStrategyEnabled) {
                    QueryStrategyType.INDIVIDUAL_QUERY
                } else {
                    QueryStrategyType.SHARED_QUERY
                }
            }
            else -> null
        }

    private fun setCurrentVirtualScreen(id: Int) =
        when (screenId) {
            SurfaceRendererType.GIULIA -> settings.getGiuliaRendererSetting().setVirtualScreen(id)
            SurfaceRendererType.GAUGE -> settings.getGaugeRendererSetting().setVirtualScreen(id)
            else -> {}
        }

    private fun getCurrentVirtualScreen() =
        when (screenId) {
            SurfaceRendererType.GIULIA -> settings.getGiuliaRendererSetting().getVirtualScreen()
            SurfaceRendererType.GAUGE -> settings.getGaugeRendererSetting().getVirtualScreen()
            else -> -1
        }

    private fun getSelectedPIDs(): Set<Long> =
        when (screenId) {
            SurfaceRendererType.GIULIA -> settings.getGiuliaRendererSetting().selectedPIDs
            SurfaceRendererType.GAUGE -> settings.getGaugeRendererSetting().selectedPIDs
            else -> emptySet()
        }

    private fun getVerticalActionStrip(): ActionStrip? {
        var added = false
        var builder = ActionStrip.Builder()

        mapOf(
            1 to R.drawable.action_virtual_screen_1,
            2 to R.drawable.action_virtual_screen_2,
            3 to R.drawable.action_virtual_screen_3,
            4 to R.drawable.action_virtual_screen_4,
        ).forEach { (k, v) ->
            if (settings.isVirtualScreenEnabled(k)) {
                added = true

                val color =
                    if (getCurrentVirtualScreen() == k) {
                        CarColor.GREEN
                    } else {
                        mapColor(settings.getColorTheme().actionsBtnVirtualScreensColor)
                    }

                builder =
                    builder.addAction(
                        createAction(carContext, v, color) {
                            parent.invalidate()
                            setCurrentVirtualScreen(k)
                            updateQuery()
                            renderFrame()
                        },
                    )
            }
        }

        return if (added) {
            builder.build()
        } else {
            null
        }
    }

    private fun getSurfaceRendererType(): SurfaceRendererType =
        if (screenId is SurfaceRendererType) screenId as SurfaceRendererType else SurfaceRendererType.GIULIA

    private fun sortOrder(): Map<Long, Int>? =
        when (screenId) {
            SurfaceRendererType.GIULIA -> settings.getGiuliaRendererSetting().getPIDsSortOrder()
            SurfaceRendererType.GAUGE -> settings.getGaugeRendererSetting().getPIDsSortOrder()
            else -> null
        }
}
