/*
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
package org.obd.graphs.renderer.performance

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.query.PREF_QUERY_PERFORMANCE_BOTTOM
import org.obd.graphs.bl.query.PREF_QUERY_PERFORMANCE_BOTTOM_SELECTED
import org.obd.graphs.bl.query.PREF_QUERY_PERFORMANCE_HIDDEN
import org.obd.graphs.bl.query.PREF_QUERY_PERFORMANCE_TOP
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.renderer.AbstractSurfaceRenderer
import org.obd.graphs.renderer.MARGIN_TOP
import org.obd.graphs.renderer.api.Fps
import org.obd.graphs.renderer.api.ScreenSettings
import org.obd.graphs.renderer.brake_boosting.BrakeBoostingDrawer

internal class PerformanceScreenSettings(
    private val original: ScreenSettings
) : ScreenSettings by original {
    // Performance has its own label-break setting, independent of the Giulia virtual screens.
    override fun isBreakLabelTextEnabled(): Boolean = original.getPerformanceScreenSettings().breakLabelTextEnabled
}

internal class PerformanceSurfaceRenderer(
    context: Context,
    settings: ScreenSettings,
    private val metricsCollector: MetricsCollector,
    private val fps: Fps
) : AbstractSurfaceRenderer(context),
    SharedPreferences.OnSharedPreferenceChangeListener {
    private val screenSettings = PerformanceScreenSettings(settings)
    private val performanceInfoDetails = PerformanceInfoDetails()
    private val performanceDrawer: PerformanceDrawer =
        PerformanceDrawer(context, screenSettings)
    private val breakBoostingDrawer = BrakeBoostingDrawer(context, screenSettings)

    private val metricsCache = MetricsCache()

    init {
        Prefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences?,
        key: String?
    ) {
        when (key) {
            PREF_QUERY_PERFORMANCE_TOP, PREF_QUERY_PERFORMANCE_BOTTOM, PREF_QUERY_PERFORMANCE_HIDDEN,
            PREF_QUERY_PERFORMANCE_BOTTOM_SELECTED, PERFORMANCE_SORT_ORDER_PREF_KEY, PERFORMANCE_BOTTOM_SORT_ORDER_PREF_KEY
            -> metricsCache.cacheReset()
        }
    }

    override fun invalidate() {
        metricsCache.cacheReset()
        performanceDrawer.invalidate()
    }

    override fun onDraw(
        canvas: Canvas,
        drawArea: Rect?
    ) {
        val performanceScreenSettings = screenSettings.getPerformanceScreenSettings()
        drawArea?.let {
            performanceDrawer.drawBackground(canvas, it)

            val area = getArea(it, canvas)
            var top = getTop(area)
            val left = performanceDrawer.getMarginLeft(area.left.toFloat())

            if (screenSettings.isStatusPanelEnabled()) {
                performanceDrawer.drawStatusPanel(
                    canvas,
                    top,
                    left,
                    fps,
                    metricsCollector,
                    drawContextInfo = true
                )
                top += MARGIN_TOP
                performanceDrawer.drawDivider(
                    canvas,
                    left,
                    area.width().toFloat(),
                    top,
                    Color.DKGRAY
                )
                top += 40
            } else {
                top += MARGIN_TOP
            }

            metricsCache.update(performanceScreenSettings, metricsCollector)

            if (breakBoostingDrawer.isBrakeBoosting(
                    brakeBoostingSettings = performanceScreenSettings.brakeBoostingSettings,
                    gasMetric = metricsCache.brakeBoosting.gasMetric,
                    arbitraryMetric = metricsCache.brakeBoosting.arbitraryMetric,
                    vehicleSpeedMetric = metricsCache.brakeBoosting.vehicleSpeedMetric
                )
            ) {
                top -= 30f

                breakBoostingDrawer.drawScreen(
                    canvas,
                    area,
                    top,
                    gas = metricsCache.brakeBoosting.gasMetric,
                    torque = metricsCache.brakeBoosting.arbitraryMetric
                )
            } else {
                performanceDrawer.drawScreen(
                    canvas = canvas,
                    area = area,
                    left = left,
                    top = top,
                    performanceInfoDetails =
                    performanceInfoDetails.apply {
                        this.bottomMetrics = metricsCache.bottomMetrics
                        this.topMetrics = metricsCache.topMetrics
                    }
                )
            }
        }
    }

    override fun recycle() {
        Prefs.unregisterOnSharedPreferenceChangeListener(this)
        performanceDrawer.recycle()
    }
}
