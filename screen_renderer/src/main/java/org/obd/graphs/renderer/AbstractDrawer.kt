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
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.datalogger.WorkflowStatus
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.bl.query.isAmbientTemp
import org.obd.graphs.bl.query.isAtmPressure
import org.obd.graphs.commons.R
import org.obd.graphs.profile.profile
import org.obd.graphs.renderer.drag.MARGIN_END

internal abstract class AbstractDrawer (context: Context, protected val settings: ScreenSettings) {

    protected val valueScaler: ValueScaler = ValueScaler()

    private val statusPaint = Paint()

    protected val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.FILL
        strokeCap = Paint.Cap.BUTT
    }

    protected val alertingLegendPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    protected val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    protected val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val regularFont: Typeface = Typeface.createFromAsset(context.assets, "fonts/Roboto-Regular.ttf")
    private val italicFont = Typeface.createFromAsset(context.assets, "fonts/Roboto-LightItalic.ttf")

    private val statusLabel: String
    private val profileLabel: String
    private val fpsLabel: String

    private val ambientTempLabel: String
    private val atmPressureLabel: String


    protected val background: Bitmap =
        BitmapFactory.decodeResource(context.resources, R.drawable.background)

    init {

        valuePaint.color = Color.WHITE
        valuePaint.style = Paint.Style.FILL
        valuePaint.typeface = regularFont


        titlePaint.style = Paint.Style.FILL
        titlePaint.typeface = italicFont
        titlePaint.color = Color.LTGRAY

        statusPaint.color = Color.WHITE
        statusPaint.style = Paint.Style.FILL
        statusPaint.typeface = regularFont

        alertingLegendPaint.style = Paint.Style.FILL_AND_STROKE

        profileLabel = context.resources.getString(R.string.status_bar_profile)
        fpsLabel = context.resources.getString(R.string.status_bar_fps)
        statusLabel = context.resources.getString(R.string.status_bar_status)

        ambientTempLabel = context.resources.getString(R.string.status_bar_ambient_temp)
        atmPressureLabel = context.resources.getString(R.string.status_bar_atm_pressure)
    }

    fun recycle() {
        background.recycle()
    }

    fun drawDivider(
        canvas: Canvas,
        left: Float,
        width: Float,
        top: Float,
        color: Int
    ) {

        paint.color = color
        paint.strokeWidth = 2f
        canvas.drawLine(
            left - 6,
            top + 4,
            left + width - MARGIN_END,
            top + 4,
            paint
        )
    }


    fun drawBackground(canvas: Canvas, rect: Rect, color: Int = settings.getBackgroundColor()) {
        canvas.drawRect(rect, paint)
        canvas.drawColor(color)
        if (settings.isBackgroundDrawingEnabled()) {
            canvas.drawBitmap(background, rect.left.toFloat(), rect.top.toFloat(), backgroundPaint)
        }
    }

    fun drawStatusPanel(canvas: Canvas, top: Float, left: Float, fps: Fps, metricsCollector: MetricsCollector? = null): Float {

        var text = statusLabel
        var marginLeft = left

        drawText(
            canvas,
            text,
            marginLeft,
            top,
            Color.LTGRAY,
            12f,
            statusPaint
        )

        marginLeft += getTextWidth(text, statusPaint) + 2f

        val color: Int
        val colorTheme = settings.colorTheme()
        text = dataLogger.status().name.lowercase()

        color = when (dataLogger.status()) {
            WorkflowStatus.Disconnected -> {
                colorTheme.statusDisconnectedColor
            }

            WorkflowStatus.Stopping -> {
                colorTheme.statusDisconnectedColor
            }

            WorkflowStatus.Connecting -> {
                colorTheme.statusConnectingColor
            }

            WorkflowStatus.Connected -> {
                colorTheme.statusConnectedColor
            }
        }

        drawText(
            canvas,
            text,
            marginLeft,
            top,
            color,
            18f,
            statusPaint
        )

        marginLeft += getTextWidth(text, statusPaint) + 12F

        text = profileLabel
        drawText(
            canvas,
            text,
            marginLeft,
            top,
            Color.LTGRAY,
            12f,
            statusPaint
        )

        marginLeft += getTextWidth(text, statusPaint) + 4F
        text = profile.getCurrentProfileName()

        drawText(
            canvas,
            text,
            marginLeft,
            top,
            colorTheme.currentProfileColor,
            18f,
            statusPaint
        )

        if (settings.isFpsCounterEnabled()) {
            marginLeft += getTextWidth(text, statusPaint) + 12F
            text = fpsLabel
            drawText(
                canvas,
                text,
                marginLeft,
                top,
                Color.WHITE,
                12f,
                statusPaint
            )

            marginLeft += getTextWidth(text, statusPaint) + 4F
            drawText(
                canvas,
                fps.get().toString(),
                marginLeft,
                top,
                Color.YELLOW,
                16f,
                statusPaint
            )
        }

        metricsCollector?.let {
            if (settings.getDragRacingSettings().contextInfoEnabled) {
                metricsCollector.getMetrics().firstOrNull { it.source.isAmbientTemp() }?.let {
                    marginLeft += getTextWidth(text, statusPaint) + 12F
                    text = ambientTempLabel
                    drawText(
                        canvas,
                        text,
                        marginLeft,
                        top,
                        Color.WHITE,
                        12f,
                        statusPaint
                    )

                    marginLeft += getTextWidth(text, statusPaint) + 4F
                    drawText(
                        canvas,
                        it.valueToString(),
                        marginLeft,
                        top,
                        Color.YELLOW,
                        16f,
                        statusPaint
                    )
                }

                metricsCollector.getMetrics().firstOrNull { it.source.isAtmPressure() }?.let {
                    marginLeft += getTextWidth(text, statusPaint) + 12F
                    text = atmPressureLabel
                    drawText(
                        canvas,
                        text,
                        marginLeft,
                        top,
                        Color.WHITE,
                        12f,
                        statusPaint
                    )

                    marginLeft += getTextWidth(text, statusPaint) + 4F
                    drawText(
                        canvas,
                        it.valueToString(),
                        marginLeft,
                        top,
                        Color.YELLOW,
                        16f,
                        statusPaint
                    )
                }
            }
        }

        return getStatusBarTopMargin(top)
    }

    protected fun drawText(
        canvas: Canvas,
        text: String,
        left: Float,
        top: Float,
        color: Int,
        textSize: Float,
        paint1: Paint
    ): Float {
        paint1.color = color
        paint1.textSize = textSize
        canvas.drawText(text, left, top, paint1)
        return (left + getTextWidth(text, paint1) * 1.25f)
    }

    protected fun getTextWidth(text: String, paint: Paint): Int {
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        return bounds.left + bounds.width()
    }

    protected fun getTextHeight(text: String, paint: Paint): Int {
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        return bounds.height()
    }

    fun getMarginLeft(left: Float): Float = 12 + left

    private fun getStatusBarTopMargin(top: Float): Float = top - paint.fontMetrics.ascent
}