package org.openobd2.core.logger.ui.gauge

import android.content.Context
import android.graphics.*
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import org.openobd2.core.logger.R
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin


private const val DEFAULT_LONG_POINTER_SIZE = 1
private const val SCALE_STEP = 2

// This class is an extension of https://github.com/pkleczko/CustomGauge
class Gauge : View {
    private lateinit var paint: Paint
    private var multiplier = 1f
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
    private lateinit var linearGradient: LinearGradient

    var gaugeDrawScale = false
    private val numbersPaint = Paint()

    internal fun scale (multiplier: Float) {
        this.multiplier = multiplier
    }

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

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        val styledAttributes = context.obtainStyledAttributes(attrs, R.styleable.Gauge, 0, 0)
        // stroke style
        strokeWidth = styledAttributes.getDimension(R.styleable.Gauge_gaugeStrokeWidth, 10f)
        strokeColor = styledAttributes.getColor(
            R.styleable.Gauge_gaugeStrokeColor,
            Color.parseColor("#0D000000")
        )
        strokeCap = styledAttributes.getString(R.styleable.Gauge_gaugeStrokeCap)!!

        // angle start and sweep (opposite direction 0, 270, 180, 90)
        startAngle = styledAttributes.getInt(R.styleable.Gauge_gaugeStartAngle, 0)
        sweepAngle = styledAttributes.getInt(R.styleable.Gauge_gaugeSweepAngle, 360)

        // scale (from mStartValue to mEndValue)
        startValue = styledAttributes.getInt(R.styleable.Gauge_gaugeStartValue, 0).toFloat()
        endValue = styledAttributes.getInt(R.styleable.Gauge_gaugeEndValue, 1000).toFloat()

        // pointer size and color
        pointSize = styledAttributes.getInt(R.styleable.Gauge_gaugePointSize, 0)
        pointStartColor = styledAttributes.getColor(
            R.styleable.Gauge_gaugePointStartColor,
            ContextCompat.getColor(context, R.color.white)
        )
        pointEndColor = styledAttributes.getColor(
            R.styleable.Gauge_gaugePointEndColor,
            ContextCompat.getColor(context, R.color.white)
        )

        // divider options
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

        // calculating one point sweep
        pointAngle = abs(sweepAngle).toDouble() / (endValue - startValue)

        // calculating divider step
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
        init()


    }

    internal fun init() {
        strokeWidth *= multiplier
        var decorLineOffset = 12 * multiplier

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
        numbersPaint.textSize = strokeWidth * 0.8f

        val padding = strokeWidth
        val size = measuredWidth.toFloat()

        calculatedWidth = size - 2 * padding
        val height = size - 2 * padding

        radius = if (calculatedWidth < height) calculatedWidth / 2 else height / 2

        calculatedHeight =
            if (measuredWidth > measuredHeight) measuredWidth.toFloat() else measuredHeight.toFloat()

        val rectLeft = (getWidth() - 2 * padding) / 2 - radius + padding
        val rectTop = (calculatedHeight - 2 * padding) / 2 - radius + padding

        val rectRight = (getWidth() - 2 * padding) / 2 - radius + padding + calculatedWidth
        val rectBottom = (getHeight() - 2 * padding) / 2 - radius + padding + height
        progressRect = RectF()
        progressRect[rectLeft, rectTop, rectRight] = rectBottom

        decorRect = RectF()
        decorRect[progressRect.left - decorLineOffset, progressRect.top - decorLineOffset, progressRect.right + decorLineOffset] =
            progressRect.bottom + decorLineOffset
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

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
        drawDivider(canvas)

        if (gaugeDrawScale) {
            drawScale(canvas)
        }

    }

    private fun drawDivider(canvas: Canvas) {
        if (dividerSize > 0) {
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
    }

    private fun drawScale(canvas: Canvas) {
        paint.shader = null
        val scaleStartAngle = startAngle - dividerSize
        val numberOfItems = (dividersCount / SCALE_STEP) - 1
        val stepValue = round(endValue / numberOfItems)
        val radius = this.radius - 21.0f - "$endValue".length
        for (i in 0..numberOfItems) {

            val txt = "${(round(stepValue * i)).toInt()}"
            val rect = Rect()

            numbersPaint.getTextBounds(txt, 0, txt.length, rect)
            val angle = scaleStartAngle + i * dividerSize
            val x = (width / 2.0f + cos(angle) * radius - rect.width() / 2)
            val y = (calculatedHeight / 2.0f + sin(angle) * radius + rect.height() / 2)
            canvas.drawText(txt, x, y, numbersPaint)
        }
    }
}