package org.obd.graphs.renderer

import android.content.Context
import android.graphics.*
import org.obd.graphs.bl.collector.CarMetric
import org.obd.graphs.ValueScaler
import org.obd.graphs.bl.datalogger.WorkflowStatus
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.commons.R
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.profile.PROFILE_NAME_PREFIX
import org.obd.graphs.profile.getSelectedProfile


const val MARGIN_END = 30
const val ROW_SPACING = 12
const val MARGIN_START = 15

internal class DrawingManager(context: Context,  private val settings: ScreenSettings) {

    private val valueScaler: ValueScaler = ValueScaler()

    private val paint = Paint()
    private val alertingLegendPaint = Paint()
    private val statusPaint = Paint()
    private val valuePaint = Paint()
    private val backgroundPaint = Paint()
    private var canvas: Canvas? = null

    private val background: Bitmap =
        BitmapFactory.decodeResource(context.resources, R.drawable.background)

    private val statusLabel: String
    private val profileLabel: String
    private val fpsLabel: String

    init {

        valuePaint.color = Color.WHITE
        valuePaint.isAntiAlias = true
        valuePaint.style = Paint.Style.FILL

        statusPaint.color = Color.WHITE
        statusPaint.isAntiAlias = true
        statusPaint.style = Paint.Style.FILL

        paint.color = Color.BLACK
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL

        alertingLegendPaint.isAntiAlias = true
        alertingLegendPaint.style = Paint.Style.FILL_AND_STROKE

        profileLabel = context.resources.getString(R.string.status_bar_profile)
        fpsLabel = context.resources.getString(R.string.status_bar_fps)
        statusLabel = context.resources.getString(R.string.status_bar_status)
    }

    fun updateCanvas(canvas: Canvas) {
        this.canvas = canvas
    }

    fun drawBackground(area: Rect) {
        canvas?.let {
            it.drawRect(area, paint)
            it.drawColor(Color.BLACK)
            it.drawBitmap(background, 0f, 0f, backgroundPaint)
        }
    }

    fun drawText(
        text: String,
        horizontalPos: Float,
        verticalPos: Float,
        color: Int,
        textSize: Float

    ): Float = drawText(text, horizontalPos, verticalPos, color, textSize, paint)

    fun drawProgressBar(
        start: Float,
        width: Float,
        verticalPos: Float,
        it: CarMetric,
        color: Int
    ) {
        paint.color = color

        val progress = valueScaler.scaleToNewRange(
            it.source.value?.toFloat()?:it.source.command.pid.min.toFloat(),
            it.source.command.pid.min.toFloat(),  it.source.command.pid.max.toFloat(), start, start + width - MARGIN_END
        )

        canvas?.drawRect(
            start - 6,
            verticalPos + 4,
            progress,
            verticalPos + calculateProgressBarHeight(),
            paint
        )
    }

    fun drawAlertingLegend(metric: CarMetric, horizontalPos: Float, verticalPos: Float) {
        if (settings.isAlertLegendEnabled() && (metric.source.command.pid.alertLowerThreshold != null ||
                    metric.source.command.pid.alertUpperThreshold != null)
        ) {

            val text = "  alerting rule "
            drawText(
                text,
                horizontalPos,
                verticalPos,
                Color.LTGRAY,
                12f,
                alertingLegendPaint
            )

            val hPos = horizontalPos  + getTextWidth(text, alertingLegendPaint) + 2f

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
                verticalPos,
                Color.YELLOW,
                14f,
                alertingLegendPaint
            )
        }
    }

    fun drawStatusBar(area: Rect, fps: Double): Float {
        val statusVerticalPos = area.top + 6f
        var text = statusLabel
        var horizontalAlignment = MARGIN_START.toFloat()

        drawText(
            text,
            horizontalAlignment,
            statusVerticalPos,
            Color.LTGRAY,
            12f,
            statusPaint
        )

        horizontalAlignment += getTextWidth(text, statusPaint) + 2f

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
            horizontalAlignment,
            statusVerticalPos,
            color,
            18f,
            statusPaint
        )

        horizontalAlignment += getTextWidth(text, statusPaint) + 12F

        text = profileLabel
        drawText(
            text,
            horizontalAlignment,
            statusVerticalPos,
            Color.LTGRAY,
            12f,
            statusPaint
        )

        horizontalAlignment += getTextWidth(text, statusPaint) + 4F
        text = Prefs.getString("$PROFILE_NAME_PREFIX.${getSelectedProfile()}", "")!!

        drawText(
            text,
            horizontalAlignment,
            statusVerticalPos,
            colorTheme.currentProfileColor,
            18f,
            statusPaint
        )

        if (settings.isFpsCounterEnabled()) {
            horizontalAlignment += getTextWidth(text, statusPaint) + 12F
            text = fpsLabel
            drawText(
                text,
                horizontalAlignment,
                statusVerticalPos,
                Color.WHITE,
                12f,
                statusPaint
            )

            horizontalAlignment += getTextWidth(text, statusPaint) + 4F
            drawText(
                fps.toString(),
                horizontalAlignment,
                statusVerticalPos,
                Color.YELLOW,
                16f,
                statusPaint
            )
        }

        return getStatusBarSpacing(area)
    }

    fun drawValue(
        metric: CarMetric,
        horizontalPos: Float,
        verticalPos: Float,
        textSize: Float
    ) {
        val colorTheme = settings.colorTheme()
        valuePaint.color = if (inAlert(metric)){
            colorTheme.currentValueInAlertColor
        } else {
            colorTheme.currentValueColor
        }

        valuePaint.textSize = textSize
        valuePaint.textAlign = Paint.Align.RIGHT
        val text = metric.source.valueToString()
        canvas?.drawText(text, horizontalPos, verticalPos, valuePaint)

        valuePaint.color = Color.LTGRAY
        valuePaint.textAlign = Paint.Align.LEFT
        valuePaint.textSize = (textSize * 0.4).toFloat()
        canvas?.drawText(metric.source.command.pid.units, (horizontalPos + 2), verticalPos, valuePaint)
    }

    fun drawTitle(
        metric: CarMetric,
        horizontalPos: Float,
        verticalPos: Float,
        textSize: Float,
        maxItemsInColumn: Int
    ) {

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        paint.color = Color.LTGRAY
        paint.textSize = textSize
        if (maxItemsInColumn == 1) {
            val text = metric.source.command.pid.description.replace("\n", " ")
            canvas?.drawText(
                text,
                horizontalPos,
                verticalPos,
                paint
            )
        } else {
            val text = metric.source.command.pid.description.split("\n")
            if (text.size == 1) {
                canvas?.drawText(
                    text[0],
                    horizontalPos,
                    verticalPos,
                    paint
                )
            } else {
                paint.textSize = textSize * 0.8f
                var vPos = verticalPos - 12
                text.forEach {
                    canvas?.drawText(
                        it,
                        horizontalPos,
                        vPos,
                        paint
                    )
                    vPos += paint.textSize
                }
            }
        }
    }



    fun drawDivider(
        start: Float,
        width: Float,
        verticalPos: Float,
        color: Int
    ) {
        paint.color = color
        paint.strokeWidth = 2f
        canvas?.drawLine(
            start - 6,
            verticalPos + 4,
            start + width - MARGIN_END,
            verticalPos + 4,
            paint
        )
    }

    private fun drawText(
        text: String,
        horizontalPos: Float,
        verticalPos: Float,
        color: Int,
        textSize: Float,
        paint1: Paint
    ): Float {
        paint1.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint1.color = color
        paint1.textSize = textSize
        canvas?.drawText(text, horizontalPos, verticalPos, paint1)
        return (horizontalPos + getTextWidth(text, paint1) * 1.25f)
    }

    private fun getStatusBarSpacing(area: Rect): Float = area.top - paint.fontMetrics.ascent + 12

    private fun getTextWidth(text: String, paint: Paint): Int {
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        return bounds.left + bounds.width()
    }

    private fun calculateProgressBarHeight() = when (settings.getMaxItemsInColumn()) {
        1 -> 16
        else -> 10
    }

    private fun inAlert(metric: CarMetric) = settings.isAlertingEnabled() && metric.isInAlert()

}