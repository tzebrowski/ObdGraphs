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
package org.obd.graphs.aa.screen.iot

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.TypedValue
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat

private const val DEFAULT_TEXT_SIZE = 16

class ValueDrawable(private val carContext: Context) {
    fun draw(value: String, color: Int): CarIcon {

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = color
        paint.textAlign = Paint.Align.CENTER

        val textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            DEFAULT_TEXT_SIZE.toFloat(), carContext.resources.displayMetrics
        )

        paint.textSize = textSize * 1.5f

        val intrinsicWidth = (paint.measureText(value, 0, value.length) + .5).toInt()
        val intrinsicHeight = paint.getFontMetricsInt(null)

        val bitmap = bitmap(intrinsicWidth, intrinsicHeight)

        val canvas = Canvas(bitmap)
        val bounds = Rect()
        bounds[0, canvas.height, canvas.width] = canvas.height

        canvas.drawText(
            value, 0, value.length,
            bounds.centerX().toFloat(), bounds.centerY().toFloat(), paint
        )

        return CarIcon.Builder(IconCompat.createWithBitmap(bitmap)).build()
    }

    private fun bitmap(intrinsicWidth: Int, intrinsicHeight: Int): Bitmap =
        if (intrinsicWidth <= 0 || intrinsicHeight <= 0) {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        } else {
            Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
        }
}