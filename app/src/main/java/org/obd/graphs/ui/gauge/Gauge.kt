package org.obd.graphs.ui.gauge

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import org.obd.graphs.R
import org.obd.graphs.ui.common.isTablet
import org.obd.graphs.ui.graph.ValueScaler
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin


private const val DEFAULT_LONG_POINTER_SIZE = 1
private const val SCALE_STEP = 2

// This class is an extension of https://github.com/pkleczko/CustomGauge
class Gauge(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private lateinit var paint: Paint
    private var strokeColor = 0
    private lateinit var progressRect: RectF
    private lateinit var decorRect: RectF
    private var radius: Float = 0f
    private var calculatedHeight: Float = 0f
    private var calculatedWidth: Float = 0f
    private var startAngle = 0
    private var sweepAngle = 0
    var startValue: Float = 0f
    private var strokeWidth = 0f
    private var pointAngle = 0.0
    private var point = 0
    private var pointSize = 0
    private var pointStartColor = 0
    private var pointEndColor = 0
    private var dividerColor: Int = 0
    private var dividerSize: Float = 0f
    private var dividerStepAngle = 0
    private var dividersCount = 0
    private var isDividerDrawFirst = false
    private var isDividerDrawLast = false
    private var linearGradient: LinearGradient
    var gaugeDrawScale = false
    private val numbersPaint = Paint()
    private val initialized = AtomicBoolean(false)
    private val valueScaler = ValueScaler()

    private var strokeCap: String = Paint.Cap.BUTT.name
        set(newValue) {
            field = newValue
            if (::paint.isInitialized) {
                paint.strokeCap = Paint.Cap.valueOf(strokeCap)
            }
        }

    var endValue: Float = 0.0f
        set(newValue) {
            field = newValue
            pointAngle = abs(sweepAngle).toDouble() / (endValue - startValue)
            invalidate()
        }

    var value: Float = 0.0f
        set(newValue) {
            field = newValue
            point = (startAngle + (value - startValue) * pointAngle).toInt()
            invalidate()
        }

    init {
        val styledAttributes = context.obtainStyledAttributes(attrs, R.styleable.Gauge, 0, 0)
        strokeWidth = styledAttributes.getDimension(R.styleable.Gauge_gaugeStrokeWidth, 10f)
        strokeColor = styledAttributes.getColor(
            R.styleable.Gauge_gaugeStrokeColor,
            Color.parseColor("#0D000000")
        )
        strokeCap = styledAttributes.getString(R.styleable.Gauge_gaugeStrokeCap)!!
        startAngle = styledAttributes.getInt(R.styleable.Gauge_gaugeStartAngle, 0)
        sweepAngle = styledAttributes.getInt(R.styleable.Gauge_gaugeSweepAngle, 360)
        startValue = styledAttributes.getInt(R.styleable.Gauge_gaugeStartValue, 0).toFloat()
        endValue = styledAttributes.getInt(R.styleable.Gauge_gaugeEndValue, 1000).toFloat()
        pointSize = styledAttributes.getInt(R.styleable.Gauge_gaugePointSize, 0)
        pointStartColor = styledAttributes.getColor(
            R.styleable.Gauge_gaugePointStartColor,
            ContextCompat.getColor(context, R.color.white)
        )
        pointEndColor = styledAttributes.getColor(
            R.styleable.Gauge_gaugePointEndColor,
            ContextCompat.getColor(context, R.color.white)
        )
        val dividerSize = styledAttributes.getInt(R.styleable.Gauge_gaugeDividerSize, 0)
        dividerColor = styledAttributes.getColor(
            R.styleable.Gauge_gaugeDividerColor,
            ContextCompat.getColor(context, R.color.white)
        )
        val dividerStep = styledAttributes.getInt(R.styleable.Gauge_gaugeDividerStep, 0)
        isDividerDrawFirst =
            styledAttributes.getBoolean(R.styleable.Gauge_gaugeDividerDrawFirst, true)
        isDividerDrawLast =
            styledAttributes.getBoolean(R.styleable.Gauge_gaugeDividerDrawLast, true)
        pointAngle = abs(sweepAngle).toDouble() / (endValue - startValue)
        if (dividerSize > 0) {
            this.dividerSize = sweepAngle / (abs(endValue - startValue) / dividerSize)
            dividersCount = 100 / dividerStep
            dividerStepAngle = sweepAngle / dividersCount
        }
        linearGradient = LinearGradient(
            width.toFloat(), height.toFloat(), 0f, 0f,
            pointEndColor,
            pointStartColor, Shader.TileMode.CLAMP
        )
        gaugeDrawScale = styledAttributes.getBoolean(R.styleable.Gauge_gaugeDrawScale, false)
        styledAttributes.recycle()
    }

    internal fun init() {

        val rescaleValue = calculateRescaleValue()
        val decorLineOffset = if (isTablet(context)) 12 * rescaleValue else 20 * rescaleValue

        paint = Paint()
        paint.color = strokeColor
        paint.strokeWidth = strokeWidth
        paint.isAntiAlias = true

        if (TextUtils.isEmpty(strokeCap)) {
            paint.strokeCap = Paint.Cap.BUTT
        } else {
            paint.strokeCap = Paint.Cap.valueOf(strokeCap)
        }

        paint.style = Paint.Style.STROKE

        value = startValue
        point = startAngle

        numbersPaint.color = resources.getColor(R.color.md_grey_500, null)
        strokeWidth *= rescaleValue
        numbersPaint.textSize = strokeWidth * 0.8f

        val padding = strokeWidth

        val size = measuredWidth.toFloat()

        calculatedWidth = size - 2 * padding
        val height = size - 2 * padding

        calculatedHeight =
            if (measuredWidth > measuredHeight) measuredWidth.toFloat() else measuredHeight.toFloat()

        radius = if (calculatedWidth < height) calculatedWidth / 2 else height / 2

        calculatedHeight =
            if (measuredWidth > measuredHeight) measuredWidth.toFloat() else measuredHeight.toFloat()

        val rectLeft = (width - 2 * padding) / 2 - radius + padding
        val rectTop = (calculatedHeight - 2 * padding) / 2 - radius + padding

        val rectRight = (width - 2 * padding) / 2 - radius + padding + calculatedWidth
        val rectBottom = (getHeight() - 2 * padding) / 2 - radius + padding + height
        progressRect = RectF()
        progressRect[rectLeft, rectTop, rectRight] = rectBottom

        decorRect = RectF()
        decorRect[progressRect.left - decorLineOffset,
                progressRect.top - decorLineOffset,
                progressRect.right + decorLineOffset] =
            progressRect.bottom + decorLineOffset
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!initialized.getAndSet(true)) {
            init()
        }

        paint.color = strokeColor
        paint.shader = null
        canvas.drawArc(progressRect, startAngle.toFloat(), sweepAngle.toFloat(), false, paint)

        paint.color = resources.getColor(R.color.md_grey_500, null)
        paint.shader = null
        paint.strokeWidth = 5f
        paint.isAntiAlias = true
        canvas.drawArc(decorRect, startAngle.toFloat(), sweepAngle.toFloat(), false, paint)

        paint.strokeWidth = strokeWidth
        paint.color = pointStartColor
        paint.shader = linearGradient
        if (pointSize > 0) { //if size of pointer is defined
            if (point > startAngle + pointSize / 2) {
                canvas.drawArc(
                    progressRect, (point - pointSize / 2).toFloat(), pointSize.toFloat(), false,
                    paint
                )
            } else { //to avoid exceeding start/zero point
                canvas.drawArc(progressRect, point.toFloat(), pointSize.toFloat(), false, paint)
            }
        } else { //draw from start point to value point (long pointer)
            if (value == startValue) //use non-zero default value for start point (to avoid lack of pointer for start/zero value)
                canvas.drawArc(
                    progressRect, startAngle.toFloat(), DEFAULT_LONG_POINTER_SIZE.toFloat(), false,
                    paint
                ) else canvas.drawArc(
                progressRect, startAngle.toFloat(), (point - startAngle).toFloat(), false,
                paint
            )
        }
        if (dividerSize > 0) {
            drawDivider(canvas)
        }
        if (gaugeDrawScale) {
            drawScale(canvas)
        }
    }

    private fun drawDivider(canvas: Canvas) {
        paint.color = dividerColor
        paint.shader = null
        val i = if (isDividerDrawFirst) 0 else 1
        val max = if (isDividerDrawLast) dividersCount + 1 else dividersCount
        for (j in i..max step SCALE_STEP) {
            canvas.drawArc(
                progressRect,
                (startAngle + j * dividerStepAngle).toFloat(),
                dividerSize,
                false,
                paint
            )
        }
    }

    private fun drawScale(canvas: Canvas) {
        paint.shader = null
        val numberOfItems = (dividersCount / SCALE_STEP)

        val stepValue = round((endValue - startValue) / numberOfItems)
        val baseRadius = this.radius - 32.0f - "$endValue".length

        for (i in 0..numberOfItems) {
            val value = (round(startValue + stepValue * i)).toInt().toString()
            val rect = Rect()
            numbersPaint.getTextBounds(value, 0, value.length, rect)
            val angle = Math.PI / numberOfItems * (i - numberOfItems).toFloat()
            val x = (width / 2.0f + cos(angle) * baseRadius - rect.width() / 2).toFloat()
            val y =
                (calculatedHeight / 2.0f + sin(angle) * baseRadius + rect.height() / 2).toFloat()
            canvas.drawText(value, x, y, numbersPaint)
        }
    }

    private fun calculateRescaleValue(): Float = (valueScaler.scaleToNewRange(
        measuredWidth.toFloat() * measuredHeight.toFloat(),
        0.0f,
        Resources.getSystem().displayMetrics.widthPixels * Resources.getSystem().displayMetrics.heightPixels.toFloat(),
        1f,
        3f
    ).apply {
        if (!isTablet(context)) {
            return this * 0.85f
        }
    }
            )
}