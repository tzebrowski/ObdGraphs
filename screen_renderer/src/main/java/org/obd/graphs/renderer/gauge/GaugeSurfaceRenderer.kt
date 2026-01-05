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
package org.obd.graphs.renderer.gauge

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import org.obd.graphs.bl.collector.Metric
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.datalogger.dataLoggerSettings
import org.obd.graphs.bl.query.Query
import org.obd.graphs.renderer.*
import org.obd.graphs.renderer.CoreSurfaceRenderer
import kotlin.math.min


@Suppress("NOTHING_TO_INLINE")
internal class GaugeSurfaceRenderer(
    context: Context,
    private val settings: ScreenSettings,
    private val metricsCollector: MetricsCollector,
    private val fps: Fps,
    viewSettings: ViewSettings
) : CoreSurfaceRenderer(viewSettings) {

    private val gaugeDrawer = GaugeDrawer(
        settings = settings, context = context,
        drawerSettings = DrawerSettings(gaugeProgressBarType = settings.getGaugeRendererSetting().gaugeProgressBarType)
    )

    override fun applyMetricsFilter(query: Query) {
        val gaugeSettings  = settings.getGaugeRendererSetting()
        if (dataLoggerSettings.instance().adapter.individualQueryStrategyEnabled) {
            metricsCollector.applyFilter(enabled = gaugeSettings.selectedPIDs, order = gaugeSettings.getPIDsSortOrder())
        } else {
            val ids = query.getIDs()
            val intersection = gaugeSettings.selectedPIDs.filter { ids.contains(it) }.toSet()
            metricsCollector.applyFilter(enabled = intersection, order = gaugeSettings.getPIDsSortOrder())
        }
    }

    override fun getTop(area: Rect): Float   = if (settings.isStatusPanelEnabled()) {
        area.top + viewSettings.marginTop.toFloat() + getDefaultTopMargin()
    } else {
        area.top + viewSettings.marginTop.toFloat()
    }

    override fun onDraw(canvas: Canvas, drawArea: Rect?) {

        drawArea?.let { area ->

            if (area.isEmpty) {
                area[0, 0, canvas.width - 1] = canvas.height - 1
            }

            gaugeDrawer.drawBackground(canvas, area)

            var top = getTop(area)

            if (settings.isStatusPanelEnabled()) {
                val left = gaugeDrawer.getMarginLeft(area.left.toFloat())
                gaugeDrawer.drawStatusPanel(canvas,top, left, fps)
                top += MARGIN_TOP
                gaugeDrawer.drawDivider(canvas, left, area.width().toFloat(), top, Color.DKGRAY)
                top += 10
            }

            val maxItems = min(
                metricsCollector.getMetrics().size,
                settings.getMaxItems()
            )

            val metrics = metricsCollector.getMetrics()

            when (maxItems) {
                0 -> {}
                1 -> {
                    gaugeDrawer.drawGauge(
                        canvas = canvas,
                        left = area.left + area.width() / 6f,
                        top = top + 6f,
                        width = area.width() * widthScaleRatio(maxItems),
                        metric = metrics[0],
                        labelCenterYPadding = 22f
                    )
                }

                2 -> {

                    gaugeDrawer.drawGauge(
                        canvas = canvas,
                        left = area.left.toFloat(),
                        top = top + area.height() / 7,
                        width = area.width() / 2 * widthScaleRatio(maxItems),
                        metric = metrics[0],
                        labelCenterYPadding = 22f
                    )

                    gaugeDrawer.drawGauge(
                        canvas = canvas,
                        left = (area.left + area.width() / 2f) - 10,
                        top = top + area.height() / 7,
                        width = area.width() / 2 * widthScaleRatio(maxItems),
                        metric = metrics[1],
                        labelCenterYPadding = 22f
                    )
                }
                4 -> {
                    draw(canvas, area, metrics, marginLeft = area.width() / 8f, top = top, maxItems = maxItems)
                }
                3 -> {
                    draw(canvas, area, metrics, marginLeft = area.width() / 8f, top = top, maxItems = maxItems)
                }
                else -> {
                    draw(canvas, area, metrics, top = top,labelCenterYPadding = 20f, maxItems = maxItems)
                }
            }
        }
    }

    override fun recycle() {
        gaugeDrawer.recycle()
    }

    private fun draw(
        canvas: Canvas,
        area: Rect,
        metrics: List<Metric>,
        marginLeft: Float = 5f,
        top: Float,
        labelCenterYPadding: Float = 0f,
        maxItems: Int,
    ) {

        val firstHalf = metrics.subList(0, maxItems / 2)
        val secondHalf = metrics.subList(maxItems / 2, maxItems)
        val height = (area.height() / 2)

        val widthDivider = when (maxItems) {
            2 -> 2
            1 -> 1
            else -> secondHalf.size
        }

        val width = ((area.width()) / widthDivider).toFloat() * widthScaleRatio(maxItems)
        var left = marginLeft
        val padding = 10f
        firstHalf.forEach {
            gaugeDrawer.drawGauge(
                canvas = canvas,
                left = area.left + left,
                top = top,
                width = width,
                metric = it,
                labelCenterYPadding = labelCenterYPadding

            )
            left += width - padding
        }
        if (maxItems > 1) {
            left = marginLeft
            secondHalf.forEach {
                gaugeDrawer.drawGauge(
                    canvas = canvas,
                    left = area.left + left,
                    top = top + height - 8f,
                    width = width,
                    metric = it,
                    labelCenterYPadding = labelCenterYPadding
                )
                left += width - padding
            }
        }
    }

    private inline fun widthScaleRatio(maxItems: Int): Float = when (maxItems) {
        1 -> 0.65f
        2 -> 1f
        3 -> 0.8f
        4 -> 0.8f
        5 -> 1.02f
        6 -> 1.02f
        7 -> 1.02f
        8 -> 1.02f
        else -> 1f
    }
}
