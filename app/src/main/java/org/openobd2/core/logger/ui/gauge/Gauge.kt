package org.openobd2.core.logger.ui.gauge

import android.content.Context
import android.graphics.*
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import org.openobd2.core.logger.R
import kotlin.math.abs

// This class is a copy of https://github.com/pkleczko/CustomGauge
// It includes minor extensions.
private const val DEFAULT_LONG_POINTER_SIZE = 1
class Gauge : View {
    private lateinit var paint: Paint
    var strokeWidth = 0f
    private var strokeColor = 0
    private lateinit var rectF: RectF
    private var startAngle = 0
    private var sweepAngle = 0
    var startValue:Float = 0f
    private var pointAngle = 0.0
    private var point = 0
    private var pointSize = 0
    private var pointStartColor = 0
    private var pointEndColor = 0
    private var dividerColor: Int = 0
    private var dividerSize:Float = 0f
    private var dividerStepAngle = 0
    private var dividersCount = 0
    private var isDividerDrawFirst = false
    private var isDividerDrawLast = false

    private var strokeCap: String = "BUTT"
        set(newValue) {
            field = newValue
            if (::paint.isInitialized){
                if (strokeCap == "BUTT") {
                    paint.strokeCap = Paint.Cap.BUTT
                } else if (strokeCap == "ROUND") {
                    paint.strokeCap = Paint.Cap.ROUND
                }
            }
        }

    var endValue:Float = 0.0f
        set(newValue) {
            field = newValue
            pointAngle = abs(sweepAngle).toDouble() / (endValue - startValue)
            invalidate()
        }

    var value:Float = 0.0f
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
        isDividerDrawLast = styledAttributes.getBoolean(R.styleable.Gauge_gaugeDividerDrawLast, true)

        // calculating one point sweep
        pointAngle = abs(sweepAngle).toDouble() / (endValue - startValue)

        // calculating divider step
        if (dividerSize > 0) {
            this.dividerSize = sweepAngle / (abs(endValue - startValue) / dividerSize)
            dividersCount = 100 / dividerStep
            dividerStepAngle = sweepAngle / dividersCount
        }
        styledAttributes.recycle()
        init()
    }

    fun init() {
        //main Paint
        paint = Paint()
        paint.color = strokeColor
        paint.strokeWidth = strokeWidth
        paint.isAntiAlias = true
        if (!TextUtils.isEmpty(strokeCap)) {
            if (strokeCap == "BUTT") paint.strokeCap =
                Paint.Cap.BUTT else if (strokeCap == "ROUND") paint.strokeCap =
                Paint.Cap.ROUND
        } else paint.strokeCap = Paint.Cap.BUTT
        paint.style = Paint.Style.STROKE
        rectF = RectF()
        value = startValue
        point = startAngle
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val padding = strokeWidth
        val size = measuredWidth.toFloat()

        val width = size - 2 * padding
        val height = size - 2 * padding
        val radius = if (width < height) width / 2 else height / 2

        val calculatedHeight:Float =  if (measuredWidth > measuredHeight) measuredWidth.toFloat() else measuredHeight.toFloat()

        val rectLeft = (getWidth() - 2 * padding) / 2 - radius + padding
        val rectTop = (calculatedHeight - 2 * padding) / 2 - radius + padding

        val rectRight = (getWidth() - 2 * padding) / 2 - radius + padding + width
        val rectBottom = (getHeight() - 2 * padding) / 2 - radius + padding + height
        rectF[rectLeft, rectTop, rectRight] = rectBottom
        paint.color = strokeColor
        paint.shader = null
        canvas.drawArc(rectF, startAngle.toFloat(), sweepAngle.toFloat(), false, paint)
        paint.color = pointStartColor

        val linearGradient = LinearGradient(
            getWidth().toFloat(), getHeight().toFloat(), 0f, 0f,
            pointEndColor,
            pointStartColor, Shader.TileMode.CLAMP
        )

        paint.shader = linearGradient
        if (pointSize > 0) { //if size of pointer is defined
            if (point > startAngle + pointSize / 2) {
                canvas.drawArc(
                    rectF, (point - pointSize / 2).toFloat(), pointSize.toFloat(), false,
                    paint
                )
            } else { //to avoid exceeding start/zero point
                canvas.drawArc(rectF, point.toFloat(), pointSize.toFloat(), false, paint)
            }
        } else { //draw from start point to value point (long pointer)
            if (value == startValue) //use non-zero default value for start point (to avoid lack of pointer for start/zero value)
                canvas.drawArc(
                    rectF, startAngle.toFloat(), DEFAULT_LONG_POINTER_SIZE.toFloat(), false,
                    paint
                ) else canvas.drawArc(
                rectF, startAngle.toFloat(), (point - startAngle).toFloat(), false,
                paint
            )
        }
        drawDivider(canvas)
    }

    private fun drawDivider(canvas: Canvas) {
        if (dividerSize > 0) {
            paint.color = dividerColor
            paint.shader = null
            var i = if (isDividerDrawFirst) 0 else 1
            val max = if (isDividerDrawLast) dividersCount + 1 else dividersCount
            while (i < max) {
                canvas.drawArc(
                    rectF,
                    (startAngle + i * dividerStepAngle).toFloat(),
                    dividerSize,
                    false,
                    paint
                )
                i++
            }
        }
    }
}