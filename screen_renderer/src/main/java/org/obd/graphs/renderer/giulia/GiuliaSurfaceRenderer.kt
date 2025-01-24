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
package org.obd.graphs.renderer.giulia

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.util.Log
import org.obd.graphs.ValueConverter
import org.obd.graphs.bl.collector.Metric
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.datalogger.dataLoggerPreferences
import org.obd.graphs.bl.query.Query
import org.obd.graphs.renderer.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round


private const val LOG_TAG = "GiuliaScreenRenderer"
private const val CURRENT_MIN = 22f
private const val CURRENT_MAX = 72f
private const val NEW_MAX = 1.6f
private const val NEW_MIN = 0.6f
private const val AREA_MAX_WIDTH = 500


@Suppress("NOTHING_TO_INLINE")
internal class GiuliaSurfaceRenderer(
    context: Context,
    private val  settings: ScreenSettings,
    private val  metricsCollector: MetricsCollector,
    private val fps: Fps,
    viewSettings: ViewSettings
) : CoreSurfaceRenderer(viewSettings) {

    private val valueConverter = ValueConverter()
    private val giuliaDrawer = GiuliaDrawer(context, settings)

    override fun applyMetricsFilter(query: Query) {
        val giuliaSettings =  settings.getGiuliaRendererSetting()
        if (dataLoggerPreferences.instance.individualQueryStrategyEnabled) {
            metricsCollector.applyFilter(enabled = giuliaSettings.selectedPIDs, order = giuliaSettings.getPIDsSortOrder())
        } else {
            val ids = query.getIDs()
            val intersection = giuliaSettings.selectedPIDs.filter { ids.contains(it) }.toSet()
            metricsCollector.applyFilter(enabled = intersection, order = giuliaSettings.getPIDsSortOrder())
        }
    }

    override fun onDraw(canvas: Canvas, drawArea: Rect?) {

        drawArea?.let { area ->

            if (area.isEmpty) {
                area[0, 0, canvas.width - 1] = canvas.height - 1
            }

            val (valueTextSize, textSizeBase) = calculateFontSize(area)

            giuliaDrawer.drawBackground(canvas, area)

            var top = getTop(area)
            var left = giuliaDrawer.getMarginLeft(area.left.toFloat())

            if (settings.isStatusPanelEnabled()) {
                giuliaDrawer.drawStatusPanel(canvas, top, left, fps)
                top += MARGIN_TOP
                giuliaDrawer.drawDivider(canvas, left, area.width().toFloat(), top, Color.DKGRAY)
                top += valueTextSize
            } else {
                top += MARGIN_TOP
            }

            val topCpy = top
            var initialLeft = initialLeft(area)
            val metricsCount = min(settings.getMaxItems(),metricsCollector.getMetrics().size)
            val pageSize = max(min( metricsCount,  round(metricsCount / settings.getMaxColumns().toFloat()).toInt()),1)

            if (Log.isLoggable(LOG_TAG,Log.VERBOSE)) {
                Log.v(LOG_TAG, "metricsCount=${metricsCollector.getMetrics().size}," +
                        "metricsLimit=$${ settings.getMaxItems()}  pageSize=$pageSize")
            }

            val metrics = metricsCollector.getMetrics()
            if (pageSize > 0 && metrics.isNotEmpty()){
                for (i in 0 until pageSize){
                    top = draw(canvas, area, metrics[i], textSizeBase, valueTextSize, left, top, initialLeft)
                }

                if (settings.getMaxColumns() > 1 && metricsCount > pageSize){
                    initialLeft += area.width() / 2 - 18
                    left += calculateLeftMargin(area)
                    top = calculateTop(textSizeBase, top, topCpy)

                    for (i in pageSize until metricsCount){
                        top = draw(canvas, area, metrics[i],  textSizeBase, valueTextSize, left, top, initialLeft)
                    }
                }
            }
        }
    }

    override fun recycle() {
        giuliaDrawer.recycle()
    }

    private inline fun calculateFontSize(
        area: Rect
    ): Pair<Float, Float> {

        val scaleRatio = valueConverter.scaleToNewRange(settings.getGiuliaRendererSetting().getFontSize().toFloat(), CURRENT_MIN, CURRENT_MAX, NEW_MIN, NEW_MAX)

        val areaWidth = min(
            when (settings.getMaxColumns()) {
                1 -> area.width()
                else -> area.width() / 2
            }, AREA_MAX_WIDTH
        )

        val valueTextSize = (areaWidth / 10f) * scaleRatio
        val textSizeBase = (areaWidth / 16f) * scaleRatio

        if (Log.isLoggable(LOG_TAG, Log.VERBOSE)) {
            Log.v(
                LOG_TAG,
                "areaWidth=$areaWidth valueTextSize=$valueTextSize textSizeBase=$textSizeBase scaleRatio=$scaleRatio"
            )
        }
        return Pair(valueTextSize, textSizeBase)
    }

    private inline fun calculateTop(
        textHeight: Float,
        top: Float,
        topCpy: Float
    ): Float = when (settings.getMaxColumns()) {
        1 -> top + (textHeight / 3) - 10
        else -> topCpy
    }

    private inline fun calculateLeftMargin(area: Rect): Int =
        when (settings.getMaxColumns()) {
            1 -> 0
            else -> (area.width() / 2)
        }

    private inline fun initialLeft(area: Rect): Float =
        when (settings.getMaxColumns()) {
            1 -> area.left + ((area.width()) - 42).toFloat()
            else -> area.left + ((area.width() / 2) - 32).toFloat()
        }


    private fun draw(
        canvas: Canvas,
        area: Rect,
        metric: Metric,
        textSizeBase: Float,
        valueTextSize: Float,
        left: Float,
        top: Float,
        initialLeft: Float
    ) = giuliaDrawer.drawMetric(
        canvas = canvas,
        area = area,
        metric = metric,
        textSizeBase = textSizeBase,
        valueTextSize = valueTextSize,
        left = left,
        top = top,
        valueLeft = initialLeft
    )
}
