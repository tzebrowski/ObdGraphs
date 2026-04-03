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
import org.obd.graphs.renderer.cache.TextCache
import org.obd.graphs.renderer.giulia.GiuliaDrawer
import org.obd.graphs.toNumber

private const val CURRENT_MIN = 22f
private const val CURRENT_MAX = 72f
private const val NEW_MAX = 1.6f
private const val NEW_MIN = 0.6f

const val MAX_ITEM_IN_THE_ROW = 6

internal class TripMetricDescriptor(
    val fetcher: (TripInfoDetails) -> Metric?,
    val castToInt: Boolean = false,
    val statsEnabled: Boolean = true,
    val unitEnabled: Boolean = true,
    val valueDoublePrecision: Int = 2,
    val statsDoublePrecision: Int = 2
)

internal class BottomMetricDescriptor(
    val fetcher: (TripInfoDetails) -> Metric?,
    val castToInt: Boolean
)

internal class TripInfoLayoutCache {
    val area = Rect()
    var valueTextSize: Float = 0f
    var textSizeBase: Float = 0f
    var bottomRowTextSizeBase: Float = 0f
    var bottomColWidth: Float = 0f
    var activeBottomMetricsCount: Int = -1

    fun requiresLayoutUpdate(newArea: Rect, newBottomMetricsCount: Int): Boolean {
        return area != newArea || activeBottomMetricsCount != newBottomMetricsCount
    }
}

@Suppress("NOTHING_TO_INLINE")
internal class TripInfoDrawer(
    context: Context,
    settings: ScreenSettings
) : AbstractDrawer(context, settings) {
    private val metricBuilder = MetricsBuilder()
    private val giuliaDrawer = GiuliaDrawer(context, settings)

    private val layoutCache = TripInfoLayoutCache()
    private val textCache = TextCache()
    private val defaultTypeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

    private val topMetricDescriptors = listOf(
        TripMetricDescriptor({ info: TripInfoDetails -> info.airTemp }, castToInt = true),
        TripMetricDescriptor({ info: TripInfoDetails -> info.coolantTemp }, castToInt = true),
        TripMetricDescriptor({ info: TripInfoDetails -> info.oilTemp }, castToInt = true),
        TripMetricDescriptor({ info: TripInfoDetails -> info.exhaustTemp }, castToInt = true),
        TripMetricDescriptor({ info: TripInfoDetails -> info.gearboxOilTemp }, castToInt = true),
        TripMetricDescriptor({ info: TripInfoDetails -> info.distance?.let { d -> metricBuilder.buildDiff(d) } }, statsEnabled = false),
        TripMetricDescriptor({ info: TripInfoDetails -> info.fuellevel }, valueDoublePrecision = 1, statsDoublePrecision = 1),
        TripMetricDescriptor({ info: TripInfoDetails -> info.fuelConsumption }, unitEnabled = false, statsDoublePrecision = 1),
        TripMetricDescriptor({ info: TripInfoDetails -> info.batteryVoltage }),
        TripMetricDescriptor({ info: TripInfoDetails -> info.ibs }, castToInt = true),
        TripMetricDescriptor({ info: TripInfoDetails -> info.oilLevel }),
        TripMetricDescriptor({ info: TripInfoDetails -> info.totalMisfires }, castToInt = true, unitEnabled = false, statsEnabled = false),
        TripMetricDescriptor({ info: TripInfoDetails -> info.oilDegradation }, unitEnabled = false),
        TripMetricDescriptor({ info: TripInfoDetails -> info.engineSpeed }, unitEnabled = false),
        TripMetricDescriptor({ info: TripInfoDetails -> info.vehicleSpeed }, unitEnabled = false),
        TripMetricDescriptor({ info: TripInfoDetails -> info.gearEngaged }, unitEnabled = false)
    )

    private val bottomMetricDescriptors = listOf(
        BottomMetricDescriptor({ info: TripInfoDetails -> info.intakePressure }, true),
        BottomMetricDescriptor({ info: TripInfoDetails -> info.oilPressure }, false),
        BottomMetricDescriptor({ info: TripInfoDetails -> info.torque }, true)
    )

    override fun invalidate() {
        super.invalidate()
        giuliaDrawer.invalidate()
        textCache.clear()
        layoutCache.area.setEmpty()
        layoutCache.activeBottomMetricsCount = -1
    }

    override fun recycle() {
        super.recycle()
        giuliaDrawer.recycle()
        textCache.clear()
    }

    inline fun drawScreen(
        canvas: Canvas,
        area: Rect,
        left: Float,
        top: Float,
        tripInfo: TripInfoDetails
    ) {
        var currentBottomCount = 0
        for (i in bottomMetricDescriptors.indices) {
            if (bottomMetricDescriptors[i].fetcher.invoke(tripInfo) != null) {
                currentBottomCount++
            }
        }

        if (layoutCache.requiresLayoutUpdate(area, currentBottomCount)) {
            calculateLayout(area, tripInfo, currentBottomCount)
        }

        val textSizeBase = layoutCache.textSizeBase
        val valueTextSize = layoutCache.valueTextSize
        val dynamicPadding = textSizeBase * 0.1f
        val x = maxItemWidth(area)

        var rowTop = top + (textSizeBase * 0.3f)
        var colIndex = 0

        for (i in topMetricDescriptors.indices) {
            val descriptor = topMetricDescriptors[i]
            val metric = descriptor.fetcher.invoke(tripInfo) ?: continue

            if (colIndex >= MAX_ITEM_IN_THE_ROW) {
                colIndex = 0
                rowTop += (textSizeBase * 1.8f)
            }

            drawMetric(
                metric = metric,
                top = rowTop,
                left = left + (colIndex * x) + dynamicPadding,
                canvas = canvas,
                textSizeBase = textSizeBase,
                statsEnabled = descriptor.statsEnabled,
                unitEnabled = descriptor.unitEnabled,
                area = area,
                valueDoublePrecision = descriptor.valueDoublePrecision,
                statsDoublePrecision = descriptor.statsDoublePrecision,
                castToInt = descriptor.castToInt
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

        var drawnBottomCount = 0
        for (i in bottomMetricDescriptors.indices) {
            val descriptor = bottomMetricDescriptors[i]
            val metric = descriptor.fetcher.invoke(tripInfo) ?: continue

            drawBottomMetric(
                metric = metric,
                castToInt = descriptor.castToInt,
                left = left,
                index = drawnBottomCount,
                area = area,
                dynamicPadding = dynamicPadding,
                colWidth = layoutCache.bottomColWidth,
                canvas = canvas,
                rowTextSizeBase = layoutCache.bottomRowTextSizeBase,
                valueTextSize = valueTextSize,
                rowTop = rowTop
            )
            drawnBottomCount++
        }
    }

    private fun calculateLayout(area: Rect, tripInfo: TripInfoDetails, validBottomMetricsCount: Int) {
        layoutCache.area.set(area)
        layoutCache.activeBottomMetricsCount = validBottomMetricsCount

        val scaleRatio = getScaleRatio()
        val areaWidth = area.width()

        layoutCache.valueTextSize = (areaWidth / 17f) * scaleRatio
        layoutCache.textSizeBase = (areaWidth / 22f) * scaleRatio

        if (validBottomMetricsCount > 0) {
            val colWidth = areaWidth / validBottomMetricsCount.toFloat()
            layoutCache.bottomColWidth = colWidth

            var rowTextSizeBase = layoutCache.textSizeBase

            bottomMetricDescriptors.forEach { descriptor ->
                val metric = descriptor.fetcher.invoke(tripInfo) ?: return@forEach
                val pid = metric.source.command.pid

                val description = pid.longDescription?.takeIf { it.isNotEmpty() } ?: pid.description
                val longestLine = description.split("\n").maxByOrNull { it.length } ?: description

                titlePaint.textSize = layoutCache.textSizeBase
                val titleWidth = getTextWidth(longestLine, titlePaint)
                val maxTitleWidth = colWidth * 0.75f

                if (titleWidth > maxTitleWidth && titleWidth > 0f) {
                    val scaleFactor = maxTitleWidth / titleWidth
                    rowTextSizeBase = minOf(rowTextSizeBase, layoutCache.textSizeBase * scaleFactor)
                }
            }
            layoutCache.bottomRowTextSizeBase = rowTextSizeBase
        }
    }

    fun drawBottomMetric(
        metric: Metric,
        castToInt: Boolean,
        left: Float,
        index: Int,
        area: Rect,
        dynamicPadding: Float,
        colWidth: Float,
        canvas: Canvas,
        rowTextSizeBase: Float,
        valueTextSize: Float,
        rowTop: Float
    ) {
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
            valueCastToInt = castToInt
        )
    }

    private inline fun getScaleRatio() =
        settings.getTripInfoScreenSettings().fontSize.toFloat().mapRange(
            CURRENT_MIN,
            CURRENT_MAX,
            NEW_MIN,
            NEW_MAX
        )

    private fun drawValue(
        canvas: Canvas,
        metric: Metric,
        top: Float,
        textSize: Float,
        left: Float,
        statsEnabled: Boolean,
        unitEnabled: Boolean,
        area: Rect,
        valueDoublePrecision: Int = 2,
        statsDoublePrecision: Int = 2,
        castToInt: Boolean = false
    ) {
        valuePaint.typeface = defaultTypeface
        valuePaint.color = valueColorScheme(metric)
        valuePaint.setShadowLayer(80f, 0f, 0f, Color.WHITE)
        valuePaint.textSize = textSize

        val text = textCache.value.get(metric.pid.id, metric.source.toNumber()) {
            metric.source.format(castToInt = castToInt, precision = valueDoublePrecision)
        }

        val textPadding = textSize * 0.05f

        canvas.drawText(text, left, top, valuePaint)
        var textWidth = getTextWidth(text, valuePaint) + textPadding

        if (unitEnabled) {
            metric.source.command.pid.units?.let {
                valuePaint.color = Color.LTGRAY
                valuePaint.textSize = (textSize * 0.4).toFloat()
                canvas.drawText(it, (left + textWidth), top, valuePaint)
                textWidth += getTextWidth(it, valuePaint) + textPadding
            }
        }

        if (settings.isStatisticsEnabled() && statsEnabled) {
            valuePaint.textSize = (textSize * 0.60).toFloat()
            val pid = metric.pid

            val minText = textCache.min.get(pid.id, metric.min) {
                metric.min.format(pid = pid, precision = statsDoublePrecision, castToInt = castToInt)
            }
            val maxText = textCache.max.get(pid.id, metric.max) {
                metric.max.format(pid = pid, precision = statsDoublePrecision, castToInt = castToInt)
            }

            val minWidth = getTextWidth(minText, valuePaint)
            val maxWidth = getTextWidth(maxText, valuePaint)
            val maxStatWidth = maxOf(minWidth, maxWidth)

            val itemWidth = textWidth + maxStatWidth

            if (itemWidth <= (maxItemWidth(area))) {
                valuePaint.color = minValueColorScheme(metric)
                canvas.drawText(minText, (left + textWidth), top, valuePaint)

                valuePaint.color = maxValueColorScheme(metric)
                canvas.drawText(
                    maxText,
                    (left + textWidth),
                    top - (getTextHeight(minText, valuePaint) * 1.1f),
                    valuePaint
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
        castToInt: Boolean = false
    ) {
        drawValue(
            canvas = canvas,
            metric = metric,
            top = top,
            textSize = textSizeBase * 0.8f,
            left = left,
            statsEnabled = statsEnabled,
            unitEnabled = unitEnabled,
            area = area,
            valueDoublePrecision = valueDoublePrecision,
            statsDoublePrecision = statsDoublePrecision,
            castToInt = castToInt
        )

        drawTitle(canvas, metric, left, top + (textSizeBase * 0.40f), textSizeBase * 0.35F)
    }
}
