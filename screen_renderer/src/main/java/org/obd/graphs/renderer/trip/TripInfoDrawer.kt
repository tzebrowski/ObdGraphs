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
package org.obd.graphs.renderer.trip

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import org.obd.graphs.bl.collector.Metric
import org.obd.graphs.bl.collector.MetricsBuilder
import org.obd.graphs.format
import org.obd.graphs.mapRange
import org.obd.graphs.renderer.AbstractDrawer
import org.obd.graphs.renderer.MARGIN_END
import org.obd.graphs.renderer.api.ScreenSettings
import org.obd.graphs.renderer.giulia.GiuliaDrawer

private const val CURRENT_MIN = 22f
private const val CURRENT_MAX = 72f
private const val NEW_MAX = 1.6f
private const val NEW_MIN = 0.6f

const val MAX_ITEM_IN_THE_ROW = 6


internal data class TripMetricConfig(
    val metric: Metric,
    val castToInt: Boolean = false,
    val statsEnabled: Boolean = true,
    val unitEnabled: Boolean = true,
    val valueDoublePrecision: Int = 2,
    val statsDoublePrecision: Int = 2
)

@Suppress("NOTHING_TO_INLINE")
internal class TripInfoDrawer(
    context: Context,
    settings: ScreenSettings,
) : AbstractDrawer(context, settings) {
    private val metricBuilder = MetricsBuilder()

    private val giuliaDrawer = GiuliaDrawer(context, settings)

    inline fun drawScreen(
        canvas: Canvas,
        area: Rect,
        left: Float,
        top: Float,
        tripInfo: TripInfoDetails,
    ) {
        val (valueTextSize, textSizeBase) = calculateFontSize(area)

        val dynamicPadding = textSizeBase * 0.1f
        val x = maxItemWidth(area) + dynamicPadding

        val topMetrics = listOfNotNull(
            tripInfo.airTemp?.let { TripMetricConfig(it, castToInt = true) },
            tripInfo.coolantTemp?.let { TripMetricConfig(it, castToInt = true) },
            tripInfo.oilTemp?.let { TripMetricConfig(it, castToInt = true) },
            tripInfo.exhaustTemp?.let { TripMetricConfig(it, castToInt = true) },
            tripInfo.gearboxOilTemp?.let { TripMetricConfig(it, castToInt = true) },
            tripInfo.distance?.let { TripMetricConfig(metricBuilder.buildDiff(it), statsEnabled = false) },
            tripInfo.fuellevel?.let { TripMetricConfig(it, valueDoublePrecision = 1, statsDoublePrecision = 1) },
            tripInfo.fuelConsumption?.let { TripMetricConfig(it, unitEnabled = false, statsDoublePrecision = 1) },
            tripInfo.batteryVoltage?.let { TripMetricConfig(it) },
            tripInfo.ibs?.let { TripMetricConfig(it, castToInt = true) },
            tripInfo.oilLevel?.let { TripMetricConfig(it) },
            tripInfo.totalMisfires?.let { TripMetricConfig(it, castToInt = true, unitEnabled = false) },
            tripInfo.oilDegradation?.let { TripMetricConfig(it, unitEnabled = false) },
            tripInfo.engineSpeed?.let { TripMetricConfig(it, unitEnabled = false) },
            tripInfo.vehicleSpeed?.let { TripMetricConfig(it, unitEnabled = false) },
            tripInfo.gearEngaged?.let { TripMetricConfig(it, unitEnabled = false) },
        )

        var rowTop = top + (textSizeBase * 0.3f)
        var colIndex = 0

        topMetrics.forEach { config ->
            if (colIndex >= MAX_ITEM_IN_THE_ROW) {
                colIndex = 0
                rowTop += (textSizeBase * 1.8f)
            }

            drawMetric(
                metric = config.metric,
                top = rowTop,
                left = left + (colIndex * x),
                canvas = canvas,
                textSizeBase = textSizeBase,
                statsEnabled = config.statsEnabled,
                unitEnabled = config.unitEnabled,
                area = area,
                valueDoublePrecision = config.valueDoublePrecision,
                statsDoublePrecision = config.statsDoublePrecision,
                castToInt = config.castToInt
            )
            colIndex++
        }

        rowTop += 2.2f * textSizeBase

        giuliaDrawer.drawDivider(
            canvas = canvas,
            left = left,
            width = area.width().toFloat(),
            top = rowTop - (textSizeBase * 0.8f),
            color = Color.DKGRAY
        )

        rowTop += 6

        val bottomMetrics =
            listOfNotNull(
                tripInfo.intakePressure?.let { Pair(it, true) },
                tripInfo.oilPressure?.let { Pair(it, false) },
                tripInfo.torque?.let { Pair(it, true) },
            )

        if (bottomMetrics.isNotEmpty()) {
            val itemsCount = bottomMetrics.size
            val colWidth = area.width() / itemsCount.toFloat()

            var rowTextSizeBase = textSizeBase
            bottomMetrics.forEach { (metric, _) ->
                val description =
                    metric.source.command.pid.longDescription
                        ?.takeIf { it.isNotEmpty() } ?: metric.source.command.pid.description

                val longestLine = description.split("\n").maxByOrNull { it.length } ?: description
                titlePaint.textSize = textSizeBase
                val titleWidth = getTextWidth(longestLine, titlePaint)
                val maxTitleWidth = colWidth * 0.75f

                if (titleWidth > maxTitleWidth && titleWidth > 0f) {
                    val scaleFactor = maxTitleWidth / titleWidth
                    rowTextSizeBase = minOf(rowTextSizeBase, textSizeBase * scaleFactor)
                }
            }

            bottomMetrics.forEachIndexed { index, paired ->
                drawMetric(paired, left, index, area, dynamicPadding, colWidth, canvas, rowTextSizeBase, valueTextSize, rowTop)
            }
        }
    }

    fun drawMetric(
        paired: Pair<Metric, Boolean>,
        left: Float,
        index: Int,
        area: Rect,
        dynamicPadding: Float,
        colWidth: Float,
        canvas: Canvas,
        rowTextSizeBase: Float,
        valueTextSize: Float,
        rowTop: Float,
    ) {
        val metric = paired.first
        val castToInt = paired.second

        val metricLeft = left + (index * colWidth) + dynamicPadding
        val metricRight = metricLeft + colWidth - dynamicPadding

        val valueLeft = metricRight - MARGIN_END

        val boundedArea = Rect(metricLeft.toInt(), area.top, metricRight.toInt(), area.bottom)

        giuliaDrawer.drawMetric(
            canvas = canvas,
            area = boundedArea,
            metric = metric,
            textSizeBase = rowTextSizeBase * 0.8f,
            valueTextSize = valueTextSize * 0.8f,
            left = metricLeft,
            top = rowTop,
            valueLeft = valueLeft,
            valueCastToInt = castToInt,
        )
    }

    private inline fun calculateFontSize(area: Rect): Pair<Float, Float> {
        val scaleRatio = getScaleRatio()

        val areaWidth = area.width()
        val valueTextSize = (areaWidth / 17f) * scaleRatio
        val textSizeBase = (areaWidth / 22f) * scaleRatio
        return Pair(valueTextSize, textSizeBase)
    }

    private inline fun getScaleRatio() =
        settings.getTripInfoScreenSettings().fontSize.toFloat().mapRange(
            CURRENT_MIN,
            CURRENT_MAX,
            NEW_MIN,
            NEW_MAX,
        )

    private fun drawValue(
        canvas: Canvas,
        metric: Metric,
        top: Float,
        textSize: Float,
        left: Float,
        typeface: Typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL),
        statsEnabled: Boolean,
        unitEnabled: Boolean,
        area: Rect,
        valueDoublePrecision: Int = 2,
        statsDoublePrecision: Int = 2,
        castToInt: Boolean = false,
    ) {
        valuePaint.typeface = typeface
        valuePaint.color = valueColorScheme(metric)

        valuePaint.setShadowLayer(80f, 0f, 0f, Color.WHITE)
        valuePaint.textSize = textSize
        val text = metric.source.format(castToInt = castToInt, precision = valueDoublePrecision)

        val textPadding = textSize * 0.05f

        canvas.drawText(text, left, top, valuePaint)
        var textWidth = getTextWidth(text, valuePaint) + textPadding

        if (unitEnabled) {
            metric.source.command.pid.units.let {
                valuePaint.color = Color.LTGRAY
                valuePaint.textSize = (textSize * 0.4).toFloat()
                canvas.drawText(it, (left + textWidth), top, valuePaint)
                textWidth += getTextWidth(it, valuePaint) + textPadding
            }
        }

        if (settings.isStatisticsEnabled() && statsEnabled) {
            valuePaint.textSize = (textSize * 0.60).toFloat()
            val pid = metric.pid
            val itemWidth = textWidth + getTextWidth(metric.max.format(pid = pid), valuePaint)
            if (itemWidth <= maxItemWidth(area)) {
                val min = metric.min.format(pid = pid, precision = statsDoublePrecision, castToInt = castToInt)
                valuePaint.color = minValueColorScheme(metric)
                canvas.drawText(min, (left + textWidth), top, valuePaint)

                valuePaint.color = maxValueColorScheme(metric)
                canvas.drawText(
                    metric.max.format(pid = pid, precision = statsDoublePrecision, castToInt = castToInt),
                    (left + textWidth),
                    top - (getTextHeight(min, valuePaint) * 1.1f),
                    valuePaint,
                )
            }
        }
    }

    private inline fun maxItemWidth(area: Rect) = (area.width() / MAX_ITEM_IN_THE_ROW)

    inline fun drawMetric(
        metric: Metric,
        top: Float,
        left: Float,
        canvas: Canvas,
        textSizeBase: Float,
        statsEnabled: Boolean = false,
        unitEnabled: Boolean = true,
        area: Rect,
        valueDoublePrecision: Int = 2,
        statsDoublePrecision: Int = 2,
        castToInt: Boolean = false,
    ) {
        drawValue(
            canvas,
            metric,
            top = top,
            textSize = textSizeBase * 0.8f,
            left = left,
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL),
            statsEnabled = statsEnabled,
            unitEnabled = unitEnabled,
            area = area,
            valueDoublePrecision = valueDoublePrecision,
            statsDoublePrecision = statsDoublePrecision,
            castToInt = castToInt,
        )

        drawTitle(canvas, metric, left, top + (textSizeBase * 0.40f), textSizeBase * 0.35F)
    }
}
