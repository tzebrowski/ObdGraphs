package org.obd.graphs.aa.iot

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import org.obd.graphs.ui.common.COLOR_DYNAMIC_SELECTOR_NORMAL

private const val DEFAULT_TEXT_SIZE = 16

fun valueToIcon(carContext: Context, value: String): CarIcon {
    val drawable = TextDrawable(carContext, value)
    val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    } else {
        Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
    }
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, canvas.height/2, canvas.width, canvas.height)
    drawable.draw(canvas)
    return CarIcon.Builder(IconCompat.createWithBitmap(bitmap)).build()
}

private class TextDrawable(context: Context, private val text: CharSequence) : Drawable() {
    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val intrinsicWidth: Int
    private val intrinsicHeight: Int

    init {
        paint.color = COLOR_DYNAMIC_SELECTOR_NORMAL
        paint.textAlign = Paint.Align.CENTER
        val textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            DEFAULT_TEXT_SIZE.toFloat(), context.resources.displayMetrics
        )
        paint.textSize = textSize * 1.5f
        intrinsicWidth = (paint.measureText(text, 0, text.length) + .5).toInt()
        intrinsicHeight = paint.getFontMetricsInt(null)
    }

    override fun draw(canvas: Canvas) {

        canvas.drawText(
            text, 0, text.length,
            bounds.centerX().toFloat(), bounds.centerY().toFloat(), paint
        )
    }

    override fun getOpacity(): Int {
        return paint.alpha
    }

    override fun getIntrinsicWidth(): Int {
        return intrinsicWidth
    }

    override fun getIntrinsicHeight(): Int {
        return intrinsicHeight
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(filter: ColorFilter?) {
        paint.colorFilter = filter
    }
}