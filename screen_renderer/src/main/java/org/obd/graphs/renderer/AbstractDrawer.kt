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
package org.obd.graphs.renderer

import android.content.Context
import android.graphics.*
import org.obd.graphs.ValueConverter
import org.obd.graphs.bl.collector.Metric
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.datalogger.WorkflowStatus
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.bl.query.namesRegistry
import org.obd.graphs.format
import org.obd.graphs.commons.R
import org.obd.graphs.profile.profile
import org.obd.graphs.renderer.drag.MARGIN_END

private const val STATUS_KEY_FONT_SIZE = 12f
private const val STATUS_VALUE_FONT_SIZE = 18f

private const val CURRENT_MIN = 22f
private const val CURRENT_MAX = 72f
private const val NEW_MAX = 1.6f
private const val NEW_MIN = 0.6f

@Suppress("NOTHING_TO_INLINE")
internal abstract class AbstractDrawer(context: Context, protected val settings: ScreenSettings) {

    protected val valueConverter: ValueConverter = ValueConverter()
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


    private val defaultBackground: Bitmap =
        BitmapFactory.decodeResource(context.resources, R.drawable.background)

    open fun getBackground(): Bitmap = defaultBackground

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

    fun valueColorScheme(metric: Metric) = if (settings.isAlertingEnabled() &&
            (metric.source.isUpperAlert || metric.source.isLowerAlert)) {
        settings.getColorTheme().currentValueInAlertColor
    } else {
        settings.getColorTheme().currentValueColor
    }

    fun histogramColorScheme(metric: Metric) = if (settings.isAlertingEnabled() && (metric.inLowerAlertRisedHist || metric.inUpperAlertRisedHist)) {
        settings.getColorTheme().currentValueInAlertColor
    } else {
        settings.getColorTheme().currentValueColor
    }

    fun minValueColorScheme(metric: Metric) = if (settings.isAlertingEnabled() && metric.inLowerAlertRisedHist) {
        settings.getColorTheme().currentValueInAlertColor
    } else {
        settings.getColorTheme().currentValueColor
    }

    fun maxValueColorScheme(metric: Metric) = if (settings.isAlertingEnabled() && metric.inUpperAlertRisedHist) {
        settings.getColorTheme().currentValueInAlertColor
    } else {
        settings.getColorTheme().currentValueColor
    }


    open fun recycle() {
        getBackground().recycle()
    }


    inline fun calculateFontSize(multiplier: Float, fontSize: Int): Float =
        multiplier * valueConverter.scaleToNewRange(
            fontSize.toFloat(),
            CURRENT_MIN,
            CURRENT_MAX,
            NEW_MIN,
            NEW_MAX
        )

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
            canvas.drawBitmap(getBackground(), rect.left.toFloat(), rect.top.toFloat(), backgroundPaint)
        }
    }

    fun drawStatusPanel(
        canvas: Canvas, top: Float, left: Float, fps: Fps, metricsCollector: MetricsCollector? = null,
        drawContextInfo: Boolean = false
    ) {

        var text = statusLabel
        var marginLeft = left

        drawText(
            canvas,
            text,
            marginLeft,
            top,
            Color.LTGRAY,
            STATUS_KEY_FONT_SIZE,
            statusPaint
        )

        marginLeft += getTextWidth(text, statusPaint) + 2f

        val color: Int
        val colorTheme = settings.getColorTheme()
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
            STATUS_VALUE_FONT_SIZE,
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
            STATUS_KEY_FONT_SIZE,
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
            STATUS_VALUE_FONT_SIZE,
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
                STATUS_KEY_FONT_SIZE,
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


        if (drawContextInfo) {
            metricsCollector?.let {

                metricsCollector.getMetric(namesRegistry.getAmbientTempPID())?.let {
                    marginLeft += getTextWidth(text, statusPaint) + 12F
                    text = ambientTempLabel
                    drawText(
                        canvas,
                        text,
                        marginLeft,
                        top,
                        Color.LTGRAY,
                        STATUS_KEY_FONT_SIZE,
                        statusPaint
                    )

                    marginLeft += getTextWidth(text, statusPaint) + 4F
                    drawText(
                        canvas,
                        "${it.source.format(castToInt = false)}${it.pid().units ?: ""}",
                        marginLeft,
                        top,
                        Color.WHITE,
                        STATUS_VALUE_FONT_SIZE,
                        statusPaint
                    )
                }

                metricsCollector.getMetric(namesRegistry.getAtmPressurePID())?.let {
                    marginLeft += getTextWidth(text, statusPaint) + 12F
                    text = atmPressureLabel
                    drawText(
                        canvas,
                        text,
                        marginLeft,
                        top,
                        Color.LTGRAY,
                        STATUS_KEY_FONT_SIZE,
                        statusPaint
                    )

                    marginLeft += getTextWidth(text, statusPaint) + 4F
                    drawText(
                        canvas,
                        "${it.source.format(castToInt = false)}${it.pid().units ?: ""}",
                        marginLeft,
                        top,
                        Color.WHITE,
                        STATUS_VALUE_FONT_SIZE,
                        statusPaint
                    )
                }
            }
        }
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

    protected fun drawTitle(
        canvas: Canvas,
        metric: Metric,
        left: Float,
        top: Float,
        textSize: Float,
        color: Int = Color.WHITE
    ): Int {

        var top1 = top
        titlePaint.textSize = textSize
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        titlePaint.color = color

        val description = if (metric.source.command.pid.longDescription == null || metric.source.command.pid.longDescription.isEmpty()) {
            metric.source.command.pid.description
        } else {
            metric.source.command.pid.longDescription
        }

        if (settings.isBreakLabelTextEnabled()) {

            val text = description.split("\n")
            if (text.size == 1) {
                canvas.drawText(
                    text[0],
                    left,
                    top,
                    titlePaint
                )
                return titlePaint.textSize.toInt()
            } else {
                titlePaint.textSize = textSize
                var vPos = top

                text.forEach {
                    canvas.drawText(
                        it,
                        left,
                        vPos,
                        titlePaint
                    )
                    vPos += titlePaint.textSize

                }
                top1 += titlePaint.textSize
                return titlePaint.textSize.toInt() * text.size
            }
        } else {
            val text = description.replace("\n", " ")
            canvas.drawText(
                text,
                left,
                top,
                titlePaint
            )
            return titlePaint.textSize.toInt()
        }
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

    fun getMarginLeft(left: Float): Float = 10 + left
}
