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
package org.obd.graphs.renderer.drag

import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import org.obd.graphs.bl.collector.Metric
import org.obd.graphs.bl.drag.DragRacingEntry
import org.obd.graphs.bl.drag.DragRacingResults
import org.obd.graphs.bl.drag.VALUE_NOT_SET
import org.obd.graphs.bl.drag.dragRacingResultRegistry
import org.obd.graphs.getContext
import org.obd.graphs.renderer.AbstractDrawer
import org.obd.graphs.renderer.GaugeProgressBarType
import org.obd.graphs.renderer.ScreenSettings
import org.obd.graphs.renderer.gauge.DrawerSettings
import org.obd.graphs.renderer.gauge.GaugeDrawer
import org.obd.graphs.round
import org.obd.graphs.ui.common.COLOR_CARDINAL
import org.obd.graphs.ui.common.COLOR_DYNAMIC_SELECTOR_ECO


private const val CURRENT_MIN = 22f
private const val CURRENT_MAX = 72f
private const val NEW_MAX = 1.6f
private const val NEW_MIN = 0.6f
const val MARGIN_END = 30

private const val SHIFT_LIGHTS_MAX_SEGMENTS = 14
const val SHIFT_LIGHTS_WIDTH = 22


@Suppress("NOTHING_TO_INLINE")
internal class DragRacingDrawer(context: Context, settings: ScreenSettings) : AbstractDrawer(context, settings) {

    private val gaugeDrawer = GaugeDrawer(
        settings = settings, context = context,
        drawerSettings = DrawerSettings(
            gaugeProgressBarType = GaugeProgressBarType.LONG)
    )

    private val shiftLightPaint = Paint()
    private var segmentCounter = SHIFT_LIGHTS_MAX_SEGMENTS

    private val background: Bitmap =
        BitmapFactory.decodeResource(context.resources, org.obd.graphs.renderer.R.drawable.drag_race_bg)

    override fun getBackground(): Bitmap = background

    fun drawScreen (canvas: Canvas,
                    area: Rect,
                    left: Float,
                    pTop: Float,
                    dragRacingResults: DragRacingResults,
                    dragRaceDetails: DragRaceDetails){


        val dragRacingSettings = settings.getDragRacingScreenSettings()
        if (dragRacingSettings.shiftLightsEnabled) {
            dragRacingResultRegistry.setShiftLightsRevThreshold(dragRacingSettings.shiftLightsRevThreshold)
            // permanent white boxes
            drawShiftLights(canvas, area, blinking = false)
        }

        if (isShiftLight(dragRacingResults)) {
            drawShiftLights(canvas, area, blinking = true)
        }

        if (dragRacingResults.readyToRace){
            drawShiftLights(canvas, area, color = COLOR_DYNAMIC_SELECTOR_ECO, blinking = true)
        }

        var top = pTop

        if (settings.getDragRacingScreenSettings().displayMetricsEnabled) {
            top -=30f

            if (settings.getDragRacingScreenSettings().displayMetricsExtendedEnabled) {

                if (settings.isAA() || getContext()!!.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {

                    val gaugeWidth = area.width() / 3.7f
                    drawGauge(
                        dragRaceDetails.gas, canvas, top, area.left.toFloat() ,
                        gaugeWidth * 0.9f, labelCenterYPadding = 18f
                    )

                    drawGauge(
                        dragRaceDetails.intakePressure, canvas, top, (area.left + gaugeWidth) - 26,
                        gaugeWidth
                    )

                    drawGauge(
                        dragRaceDetails.vehicleSpeed, canvas, top, (area.left + 2 * gaugeWidth) - 38,
                        gaugeWidth
                    )

                    drawGauge(
                        dragRaceDetails.torque, canvas, top, (area.left + 3 * gaugeWidth) - 50,
                        gaugeWidth * 0.9f, labelCenterYPadding = 18f
                    )


                } else {
                    val gaugeWidth = area.width() / 2.0f
                    drawGauge(
                        dragRaceDetails.intakePressure, canvas, top, (area.left ) - 20f,
                        gaugeWidth
                    )

                    drawGauge(
                        dragRaceDetails.vehicleSpeed, canvas, top, (area.left +  gaugeWidth) - 30f,
                        gaugeWidth
                    )

                    drawGauge(
                        dragRaceDetails.gas, canvas, top + gaugeWidth , (area.left ) - 20f,
                        gaugeWidth, labelCenterYPadding = 18f
                    )

                    drawGauge(
                        dragRaceDetails.torque, canvas, top + gaugeWidth, (area.left +  gaugeWidth) - 30f,
                        gaugeWidth, labelCenterYPadding = 18f
                    )
                }

            } else {
                val gaugeWidth = area.width() / 3.2f
                drawGauge(
                    dragRaceDetails.vehicleSpeed,  canvas, top, area.width().toFloat()/2 - gaugeWidth/2,
                    gaugeWidth, labelCenterYPadding = 18f
                )
            }

            top += area.height() / 2.2f

            if (settings.getDragRacingScreenSettings().metricsFrequencyReadEnabled) {
                dragRaceDetails.vehicleSpeed?.let {

                    val textSizeBase = calculateFontSize(area)
                    val frequencyTextSize = textSizeBase * 0.45f
                    val text = "Read frequency: ${it.rate?.round(2)} read/sec"

                    drawText(
                        canvas,
                        text,
                        left,
                        top,
                        frequencyTextSize
                    )
                }
            }
        }

        drawDragRaceResults(
            canvas = canvas,
            area = area,
            left = left,
            top = top,
            dragRacingResults = dragRacingResults)

    }

    private fun drawGauge(
        metric: Metric?,
        canvas: Canvas,
        top: Float,
        left: Float,
        width: Float,
        labelCenterYPadding: Float = 22f,
    ): Boolean  =
        if (metric == null){
            false
        }else {
            gaugeDrawer.drawGauge(
                canvas = canvas,
                left = left,
                top = top ,
                width = width,
                metric = metric,
                labelCenterYPadding =  labelCenterYPadding,
                fontSize = settings.getPerformanceScreenSettings().fontSize,
                scaleEnabled = false,
                statsEnabled = false
            )
            true
        }

    private inline fun drawDragRaceResults(
        canvas: Canvas,
        area: Rect,
        left: Float,
        top: Float,
        dragRacingResults: DragRacingResults

    ) {

        val fontSizeFactor : Float  = if (settings.getDragRacingScreenSettings().displayMetricsEnabled){
            0.7f
        } else {
            1f
        }

        val  textSizeBase = calculateFontSize(area) * fontSizeFactor

        val currentXPos = area.centerX() / 1.5f
        val lastXPos = area.centerX() + 60f
        val bestXPos = area.centerX() * 1.60f

        // legend
        drawText(canvas, "Current", currentXPos, top, textSizeBase, color = Color.LTGRAY, typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC))
        drawText(canvas, "Last", lastXPos, top, textSizeBase, color = Color.LTGRAY, typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC))
        drawText(canvas, "Best", bestXPos, top, textSizeBase, color = Color.LTGRAY, typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC))

        // 0-60
        var rowTop = top + textSizeBase + 12f
        drawDragRacingEntry(area, dragRacingResults._0_60, "0-60 km/h",  rowTop, left,canvas, textSizeBase)

        // 0 - 100
        rowTop = top + (2 * textSizeBase) + 24f
        drawDragRacingEntry(area, dragRacingResults._0_100, "0-100 km/h",  rowTop, left, canvas, textSizeBase)

        // 60 - 140
        rowTop = top + (3 * textSizeBase) + 36f
        drawDragRacingEntry(area,dragRacingResults._60_140, "60-140 km/h", rowTop, left,canvas, textSizeBase)

        // 0 - 160
        rowTop = top + (4 * textSizeBase) + 48f
        drawDragRacingEntry(area, dragRacingResults._0_160, "0-160 km/h", rowTop, left, canvas, textSizeBase)

        // 100 - 200
        rowTop = top + (5 * textSizeBase) + 60f
        drawDragRacingEntry(area, dragRacingResults._100_200, "100-200 km/h", rowTop, left, canvas, textSizeBase)
    }

    private fun drawText(
        canvas: Canvas,
        text: String,
        left: Float,
        top: Float,
        textSize: Float,
        typeface: Typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL),
        color: Int = Color.WHITE
    ) {
        titlePaint.textSize = textSize
        titlePaint.typeface = typeface
        titlePaint.color = color
        canvas.drawText(
            text.replace("\n", " "),
            left,
            top,
            titlePaint
        )
    }


    private inline fun calculateFontSize(
        area: Rect
    ): Float {

        val scaleRatio = valueConverter.scaleToNewRange(settings.getDragRacingScreenSettings().fontSize.toFloat(),
            CURRENT_MIN, CURRENT_MAX, NEW_MIN, NEW_MAX)

        val areaWidth = area.width()
        val textSizeBase = (areaWidth / 21f) * scaleRatio
        return textSizeBase
    }


    private inline fun drawDragRacingEntry(
        area: Rect,
        dragRacingEntry: DragRacingEntry,
        label: String,
        top: Float,
        left: Float,
        canvas: Canvas,
        textSizeBase: Float
    ) {


        val currentXPos = area.centerX() / 1.5f
        val lastXPos = area.centerX() + 60f
        val bestXPos = area.centerX() * 1.60f

        drawText(canvas, label, left, top, textSizeBase, color = Color.LTGRAY)
        drawText(canvas, timeToString(dragRacingEntry.current), currentXPos, top, textSizeBase,
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD))

        if (settings.getDragRacingScreenSettings().vehicleSpeedDisplayDebugEnabled) {
            val width = getTextWidth(timeToString(dragRacingEntry.current), titlePaint) * 1.25f
            drawText(canvas, speedToString(dragRacingEntry.currentSpeed), currentXPos + width, top, textSizeBase / 1.5f)
        }

        drawText(canvas, timeToString(dragRacingEntry.last), lastXPos, top, textSizeBase)

        drawText(canvas, timeToString(dragRacingEntry.best), bestXPos, top, textSizeBase, color = COLOR_CARDINAL)

        if (dragRacingEntry.best != VALUE_NOT_SET){
            val width = getTextWidth(timeToString(dragRacingEntry.best), titlePaint) * 1.15f
            val height = getTextHeight(timeToString(dragRacingEntry.best), titlePaint) / 2
            if (dragRacingEntry.bestAmbientTemp != VALUE_NOT_SET.toInt()){
                drawText(canvas, "${dragRacingEntry.bestAmbientTemp}C", bestXPos + width, top - height, textSizeBase * 0.5f, color = Color.LTGRAY)
            }
            if (dragRacingEntry.bestAtmPressure != VALUE_NOT_SET.toInt()){
                drawText(canvas, "${dragRacingEntry.bestAtmPressure}hpa", bestXPos + width, top + height/2, textSizeBase * 0.5f, color = Color.LTGRAY)
            }
        }
    }

    private inline fun drawShiftLights(
        canvas: Canvas,
        area: Rect,
        color: Int = settings.getColorTheme().progressColor,
        shiftLightsWidth: Int = SHIFT_LIGHTS_WIDTH,
        blinking: Boolean = false
    ) {
        val segmentHeight = area.height().toFloat() / SHIFT_LIGHTS_MAX_SEGMENTS
        val leftMargin = 4f
        val topMargin = 6f

        shiftLightPaint.color = Color.WHITE
        for (i in 1..SHIFT_LIGHTS_MAX_SEGMENTS) {

            val top = area.top + (i * segmentHeight)
            val bottom = top + segmentHeight - topMargin

            canvas.drawRect(
                area.left - shiftLightsWidth + leftMargin, top, area.left.toFloat() + leftMargin,
                bottom, shiftLightPaint
            )

            val left = calculateShiftLightMargin(area, leftMargin)
            canvas.drawRect(
                left, top, left + shiftLightsWidth,
                bottom, shiftLightPaint
            )
        }
        if (blinking) {
            shiftLightPaint.color = color

            for (i in SHIFT_LIGHTS_MAX_SEGMENTS downTo segmentCounter) {

                val top = area.top + (i * segmentHeight)
                val bottom = top + segmentHeight - topMargin

                canvas.drawRect(
                    area.left - shiftLightsWidth + leftMargin, top, area.left.toFloat() + leftMargin,
                    bottom, shiftLightPaint
                )

                val left = calculateShiftLightMargin(area, leftMargin)

                canvas.drawRect(
                    left, top, left + shiftLightsWidth,
                    bottom, shiftLightPaint
                )
            }

            segmentCounter--

            if (segmentCounter == 0) {
                segmentCounter = SHIFT_LIGHTS_MAX_SEGMENTS
            }
        }
    }

    private fun isShiftLight(dragRaceResults: DragRacingResults) =
        settings.getDragRacingScreenSettings().shiftLightsEnabled && dragRaceResults.enableShiftLights


    private inline fun calculateShiftLightMargin(area: Rect, leftMargin: Float): Float  = area.left + area.width().toFloat() - leftMargin
    private inline fun timeToString(value: Long): String = if (value == VALUE_NOT_SET) "--.--" else (value / 1000.0).round(2).toString()
    private inline fun speedToString(value: Int): String = if (value == VALUE_NOT_SET.toInt()) "" else "$value km/h"
}
