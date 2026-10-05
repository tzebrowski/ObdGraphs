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
package org.obd.graphs.renderer.trip

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import org.obd.graphs.ViewPreferencesSerializer
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.query.PREF_QUERY_TRIP_INFO_BOTTOM
import org.obd.graphs.bl.query.PREF_QUERY_TRIP_INFO_SELECTED
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getLongSet
import org.obd.graphs.renderer.AbstractSurfaceRenderer
import org.obd.graphs.renderer.MARGIN_TOP
import org.obd.graphs.renderer.api.Fps
import org.obd.graphs.renderer.api.ScreenSettings

private const val SORT_ORDER_PREF_KEY = "$PREF_QUERY_TRIP_INFO_SELECTED.view.settings"
private const val BOTTOM_SORT_ORDER_PREF_KEY = "$PREF_QUERY_TRIP_INFO_BOTTOM.view.settings"

internal class TripInfoSurfaceRenderer(
    context: Context,
    private val settings: ScreenSettings,
    private val metricsCollector: MetricsCollector,
    private val fps: Fps
) : AbstractSurfaceRenderer(context),
    SharedPreferences.OnSharedPreferenceChangeListener {
    private val tripInfo = TripInfoDetails()

    // The plan is rebuilt only when the queried PIDs, the bottom row or an order changes.
    private var plannedIds = LongArray(0)

    @Volatile
    private var planOutdated = true

    // Trip Info has its own label-break setting, independent of the Giulia virtual screens.
    private val tripInfoDrawer =
        TripInfoDrawer(
            context,
            object : ScreenSettings by settings {
                override fun isBreakLabelTextEnabled(): Boolean = settings.getTripInfoScreenSettings().breakLabelTextEnabled
            }
        )

    init {
        Prefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences?,
        key: String?
    ) {
        when (key) {
            PREF_QUERY_TRIP_INFO_BOTTOM, SORT_ORDER_PREF_KEY, BOTTOM_SORT_ORDER_PREF_KEY -> planOutdated = true
        }
    }

    override fun invalidate() {
        planOutdated = true
        tripInfoDrawer.invalidate()
    }

    override fun onDraw(
        canvas: Canvas,
        drawArea: Rect?
    ) {
        drawArea?.let {
            tripInfoDrawer.drawBackground(canvas, it)

            val area = getArea(it, canvas)
            var top = getTop(area)
            val left = tripInfoDrawer.getMarginLeft(area.left.toFloat())

            if (settings.isStatusPanelEnabled()) {
                tripInfoDrawer.drawStatusPanel(canvas, top, left, fps, metricsCollector, drawContextInfo = true)
                top += MARGIN_TOP
                tripInfoDrawer.drawDivider(canvas, left, area.width().toFloat(), top, Color.DKGRAY)
                top += 40
            } else {
                top += MARGIN_TOP
            }

            updatePlan()
            updateMetrics(tripInfo.top)
            updateMetrics(tripInfo.bottom)

            tripInfoDrawer.drawScreen(
                canvas = canvas,
                area = area,
                left = left,
                top = top,
                tripInfo = tripInfo
            )
        }
    }

    override fun recycle() {
        Prefs.unregisterOnSharedPreferenceChangeListener(this)
        tripInfoDrawer.recycle()
    }

    private fun updateMetrics(items: List<TripInfoItem>) {
        for (i in items.indices) {
            val item = items[i]
            item.metric = metricsCollector.getMetric(item.descriptor.id)
        }
    }

    private fun updatePlan() {
        val metrics = metricsCollector.getMetrics()
        if (!planOutdated && plannedIds.size == metrics.size) {
            var same = true
            for (i in metrics.indices) {
                if (plannedIds[i] != metrics[i].pid.id) {
                    same = false
                    break
                }
            }
            if (same) return
        }

        planOutdated = false
        plannedIds = LongArray(metrics.size) { i -> metrics[i].pid.id }

        val plan =
            TripInfoMetrics.plan(
                available = plannedIds.toList(),
                bottomSelection = if (Prefs.contains(PREF_QUERY_TRIP_INFO_BOTTOM)) Prefs.getLongSet(PREF_QUERY_TRIP_INFO_BOTTOM) else null,
                sortOrder = ViewPreferencesSerializer(SORT_ORDER_PREF_KEY).getItemsSortOrder(),
                bottomSortOrder = ViewPreferencesSerializer(BOTTOM_SORT_ORDER_PREF_KEY).getItemsSortOrder()
            )
        tripInfo.top = plan.top.map { d -> TripInfoItem(d) }
        tripInfo.bottom = plan.bottom.map { d -> TripInfoItem(d) }
    }
}
