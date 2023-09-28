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
package org.obd.graphs.renderer

import android.content.Context
import android.graphics.*
import org.obd.graphs.ValueScaler
import org.obd.graphs.bl.collector.CarMetric
import org.obd.graphs.bl.datalogger.WorkflowStatus
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.commons.R
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.profile.PROFILE_NAME_PREFIX
import org.obd.graphs.profile.getSelectedProfile


const val MARGIN_END = 30

internal class GiuliaDrawer(context: Context, private val settings: ScreenSettings) {

    private val valueScaler: ValueScaler = ValueScaler()

    private val paint = Paint()
    private val alertingLegendPaint = Paint()
    private val statusPaint = Paint()
    private val valuePaint = Paint()
    private val backgroundPaint = Paint()
    private val titlePaint = Paint()

    var canvas: Canvas? = null

    private val background: Bitmap =
        BitmapFactory.decodeResource(context.resources, R.drawable.background)

    private val regularFont: Typeface = Typeface.createFromAsset(context.assets, "fonts/Roboto-Regular.ttf")
    private val italicFont = Typeface.createFromAsset(context.assets, "fonts/Roboto-LightItalic.ttf")

    private val statusLabel: String
    private val profileLabel: String
    private val fpsLabel: String

    init {

        valuePaint.color = Color.WHITE
        valuePaint.isAntiAlias = true
        valuePaint.style = Paint.Style.FILL
        valuePaint.typeface = regularFont


        titlePaint.isAntiAlias = true
        titlePaint.style = Paint.Style.FILL
        titlePaint.typeface = italicFont
        titlePaint.color = Color.LTGRAY

        statusPaint.color = Color.WHITE
        statusPaint.isAntiAlias = true
        statusPaint.style = Paint.Style.FILL
        statusPaint.typeface = regularFont

        paint.color = Color.BLACK
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL

        alertingLegendPaint.isAntiAlias = true
        alertingLegendPaint.style = Paint.Style.FILL_AND_STROKE

        profileLabel = context.resources.getString(R.string.status_bar_profile)
        fpsLabel = context.resources.getString(R.string.status_bar_fps)
        statusLabel = context.resources.getString(R.string.status_bar_status)
    }

    fun recycle() {
        background.recycle()
    }

    fun drawBackground(area: Rect) {
        canvas?.let {
            it.drawRect(area, paint)
            it.drawColor(settings.getBackgroundColor())
            if (settings.isBackgroundDrawingEnabled()) {
                it.drawBitmap(background, area.left.toFloat(), area.top.toFloat(), backgroundPaint)
            }
        }
    }

    fun drawText(
        text: String,
        left: Float,
        top: Float,
        color: Int,
        textSize: Float

    ): Float = drawText(text, left, top, color, textSize, paint)

    fun drawProgressBar(
        left: Float,
        width: Float,
        top: Float,
        it: CarMetric,
        color: Int
    ) {
        paint.color = color

        val progress = valueScaler.scaleToNewRange(
            it.source.value?.toFloat() ?: it.source.command.pid.min.toFloat(),
            it.source.command.pid.min.toFloat(), it.source.command.pid.max.toFloat(), left, left + width - MARGIN_END
        )

        canvas?.drawRect(
            left - 6,
            top + 4,
            progress,
            top + calculateProgressBarHeight(),
            paint
        )
    }

    fun drawAlertingLegend(metric: CarMetric, left: Float, top: Float) {
        if (settings.isAlertLegendEnabled() && (metric.source.command.pid.alertLowerThreshold != null ||
                    metric.source.command.pid.alertUpperThreshold != null)
        ) {

            val text = "  alerting "
            drawText(
                text,
                left,
                top,
                Color.LTGRAY,
                12f,
                alertingLegendPaint
            )

            val hPos = left + getTextWidth(text, alertingLegendPaint) + 2f

            var label = ""
            if (metric.source.command.pid.alertLowerThreshold != null) {
                label += "X<${metric.source.command.pid.alertLowerThreshold}"
            }

            if (metric.source.command.pid.alertUpperThreshold != null) {
                label += " X>${metric.source.command.pid.alertUpperThreshold}"
            }

            drawText(
                label,
                hPos + 4,
                top,
                Color.YELLOW,
                14f,
                alertingLegendPaint
            )
        }
    }

    fun drawStatusBar(area: Rect, fps: Double): Float {
        val top = area.top + 6f
        var text = statusLabel
        var left = getMarginLeft(area)

        drawText(
            text,
            left,
            top,
            Color.LTGRAY,
            12f,
            statusPaint
        )

        left += getTextWidth(text, statusPaint) + 2f

        val color: Int
        val colorTheme = settings.colorTheme()
        text = dataLogger.status().name.lowercase()

        color = when (dataLogger.status()) {
            WorkflowStatus.Disconnected -> colorTheme.statusDisconnectedColor

            WorkflowStatus.Stopping -> colorTheme.statusDisconnectedColor

            WorkflowStatus.Connecting -> colorTheme.statusConnectingColor

            WorkflowStatus.Connected -> colorTheme.statusConnectedColor
        }

        drawText(
            text,
            left,
            top,
            color,
            18f,
            statusPaint
        )

        left += getTextWidth(text, statusPaint) + 12F

        text = profileLabel
        drawText(
            text,
            left,
            top,
            Color.LTGRAY,
            12f,
            statusPaint
        )

        left += getTextWidth(text, statusPaint) + 4F
        text = Prefs.getString("$PROFILE_NAME_PREFIX.${getSelectedProfile()}", "")!!

        drawText(
            text,
            left,
            top,
            colorTheme.currentProfileColor,
            18f,
            statusPaint
        )

        if (settings.isFpsCounterEnabled()) {
            left += getTextWidth(text, statusPaint) + 12F
            text = fpsLabel
            drawText(
                text,
                left,
                top,
                Color.WHITE,
                12f,
                statusPaint
            )

            left += getTextWidth(text, statusPaint) + 4F
            drawText(
                fps.toString(),
                left,
                top,
                Color.YELLOW,
                16f,
                statusPaint
            )
        }

        return getStatusBarSpacing(area)
    }

    fun drawValue(
        metric: CarMetric,
        left: Float,
        top: Float,
        textSize: Float
    ) {
        val colorTheme = settings.colorTheme()
        valuePaint.color = if (inAlert(metric)) {
            colorTheme.currentValueInAlertColor
        } else {
            colorTheme.currentValueColor
        }

        valuePaint.textSize = textSize
        valuePaint.textAlign = Paint.Align.RIGHT
        val text = metric.source.valueToString()
        canvas?.drawText(text, left, top, valuePaint)

        valuePaint.color = Color.LTGRAY
        valuePaint.textAlign = Paint.Align.LEFT
        valuePaint.textSize = (textSize * 0.4).toFloat()
        canvas?.drawText(metric.source.command.pid.units, (left + 2), top, valuePaint)
    }

    fun drawTitle(
        metric: CarMetric,
        left: Float,
        top: Float,
        textSize: Float
    ) {

        titlePaint.textSize = textSize
        if (settings.isBreakLabelTextEnabled()) {
            val text = metric.source.command.pid.description.split("\n")
            if (text.size == 1) {
                canvas?.drawText(
                    text[0],
                    left,
                    top,
                    titlePaint
                )
            } else {
                paint.textSize = textSize * 0.8f
                var vPos = top - 12
                text.forEach {
                    canvas?.drawText(
                        it,
                        left,
                        vPos,
                        titlePaint
                    )
                    vPos += titlePaint.textSize
                }
            }
        } else {
            val text = metric.source.command.pid.description.replace("\n", " ")
            canvas?.drawText(
                text,
                left,
                top,
                titlePaint
            )

        }
    }

    fun drawDivider(
        left: Float,
        width: Float,
        top: Float,
        color: Int
    ) {

        paint.color = color
        paint.strokeWidth = 2f
        canvas?.drawLine(
            left - 6,
            top + 4,
            left + width - MARGIN_END,
            top + 4,
            paint
        )
    }

    private fun drawText(
        text: String,
        left: Float,
        top: Float,
        color: Int,
        textSize: Float,
        paint1: Paint
    ): Float {
        paint1.color = color
        paint1.textSize = textSize
        canvas?.drawText(text, left, top, paint1)
        return (left + getTextWidth(text, paint1) * 1.25f)
    }

    private fun getStatusBarSpacing(area: Rect): Float = area.top - paint.fontMetrics.ascent + 12

    private fun getTextWidth(text: String, paint: Paint): Int {
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        return bounds.left + bounds.width()
    }

    private fun calculateProgressBarHeight() = when (settings.getMaxColumns()) {
        1 -> 16
        else -> 10
    }

    private fun inAlert(metric: CarMetric) = settings.isAlertingEnabled() && metric.isInAlert()


    fun getMarginLeft(drawArea: Rect): Float = 12 + drawArea.left.toFloat()
}