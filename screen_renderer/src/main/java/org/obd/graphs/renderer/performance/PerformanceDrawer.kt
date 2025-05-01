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
package org.obd.graphs.renderer.performance

import android.content.Context
import android.graphics.*
import org.obd.graphs.bl.collector.Metric
import org.obd.graphs.renderer.AbstractDrawer
import org.obd.graphs.renderer.GaugeProgressBarType
import org.obd.graphs.renderer.ScreenSettings
import org.obd.graphs.renderer.gauge.DrawerSettings
import org.obd.graphs.renderer.gauge.GaugeDrawer
import org.obd.graphs.renderer.trip.TripInfoDrawer


private const val MAX_ITEMS_IN_ROW = 6
 @Suppress("NOTHING_TO_INLINE")
internal class PerformanceDrawer(context: Context, settings: ScreenSettings) : AbstractDrawer(context, settings) {

    private val gaugeDrawer = GaugeDrawer(
        settings = settings, context = context,
        drawerSettings = DrawerSettings(
            gaugeProgressBarType = GaugeProgressBarType.LONG)
    )

    private val tripInfoDrawer = TripInfoDrawer(context, settings)

    private val background: Bitmap =
        BitmapFactory.decodeResource(context.resources, org.obd.graphs.renderer.R.drawable.drag_race_bg)

    override fun getBackground(): Bitmap = background

    override fun recycle() {
        super.getBackground().recycle()
        this.background.recycle()
    }

    inline fun drawScreen(
        canvas: Canvas,
        area: Rect,
        left: Float,
        top: Float,
        performanceInfoDetails: PerformanceInfoDetails
    ) {

        val textSize = calculateFontSize(multiplier = area.width() / 17f,
            fontSize = settings.getPerformanceScreenSettings().fontSize)

        val x = maxItemWidth(area) + 4

        var rowTop = top + 12f
        var leftAlignment = 0

        performanceInfoDetails.postICAirTemp?.let { tripInfoDrawer
            .drawMetric(it, top = rowTop, left = left + leftAlignment++, canvas, textSize, statsEnabled = true, area=area, castToInt = true) }
        performanceInfoDetails.coolantTemp?.let {  tripInfoDrawer.drawMetric(it, rowTop, left + (leftAlignment++) * x, canvas, textSize, statsEnabled = true,area=area, castToInt = true) }
        performanceInfoDetails.oilTemp?.let{ tripInfoDrawer.drawMetric(it, rowTop, left + (leftAlignment++) * x, canvas, textSize, statsEnabled = true,area=area, castToInt = true) }
        performanceInfoDetails.exhaustTemp?.let { tripInfoDrawer.drawMetric(it, rowTop, left + (leftAlignment++) * x, canvas, textSize, statsEnabled = true, area=area, castToInt = true) }
        performanceInfoDetails.gearboxOilTemp?.let { tripInfoDrawer.drawMetric(it, rowTop, left + (leftAlignment++) * x, canvas, textSize, statsEnabled = true, area=area, castToInt = true) }
        performanceInfoDetails.ambientTemp?.let{ tripInfoDrawer.drawMetric(it, rowTop, left + (leftAlignment++) * x, canvas, textSize, area=area) }


        if (leftAlignment < MAX_ITEMS_IN_ROW){
            performanceInfoDetails.preICAirTemp?.let{ tripInfoDrawer.drawMetric(it, rowTop, left + (leftAlignment++) * x, canvas, textSize,
                statsEnabled = true, area=area) }
        }
        if (leftAlignment < MAX_ITEMS_IN_ROW){
            performanceInfoDetails.wcaTemp?.let{ tripInfoDrawer.drawMetric(it, rowTop, left + (leftAlignment++) * x, canvas, textSize,
                statsEnabled = true, area=area) }
        }

        drawDivider(canvas, left, area.width().toFloat(), rowTop + textSize + 4, Color.DKGRAY)

        rowTop += textSize + 16

        var numGauges = 4
        if (performanceInfoDetails.vehicleSpeed == null) numGauges--
        if (performanceInfoDetails.gas == null) numGauges--
        if (performanceInfoDetails.torque == null) numGauges--
        if (performanceInfoDetails.intakePressure == null) numGauges--

        when (numGauges) {
            4 ->{
                drawGauge(performanceInfoDetails.torque, canvas, rowTop, area.left.toFloat(),  area.width() / 2.6f, labelCenterYPadding = 18f)
                drawGauge(performanceInfoDetails.intakePressure, canvas, rowTop, (area.left + area.width() / 1.65f),  area.width() / 2.6f, labelCenterYPadding = 18f)
                drawGauge(performanceInfoDetails.gas, canvas, rowTop - 4f, (area.left + area.width() / 2.6f), area.width() / 4.5f)
                drawGauge(performanceInfoDetails.vehicleSpeed, canvas, rowTop  + area.height() / 3f, (area.left + area.width() / 2.65f), area.width() / 4.1f)
            }
            3  -> {
                if (drawGauge(performanceInfoDetails.torque, canvas, rowTop, area.left.toFloat(),  area.width() / 2.6f, labelCenterYPadding = 18f)){
                    drawGauge(performanceInfoDetails.gas, canvas, rowTop - 4f, (area.left + area.width() /2.9f) , area.width() / 3.7f)
                } else {
                    drawGauge(performanceInfoDetails.gas, canvas, rowTop - 4f, area.left.toFloat(),  area.width() / 2.6f, labelCenterYPadding = 18f)
                }
                if (drawGauge(performanceInfoDetails.intakePressure, canvas, rowTop, (area.left + area.width() / 1.65f),  area.width() / 2.6f, labelCenterYPadding = 18f)){
                    drawGauge(performanceInfoDetails.vehicleSpeed, canvas, rowTop - 4f, (area.left + area.width() / 2.9f) , area.width() / 3.7f)
                } else {
                    drawGauge(performanceInfoDetails.vehicleSpeed, canvas, rowTop, (area.left + area.width() / 1.65f),  area.width() / 2.6f, labelCenterYPadding = 18f)
                }
            }


            2 -> {
                // left side
                if (!drawGauge(performanceInfoDetails.torque, canvas, rowTop, area.left.toFloat(),  area.height() + 10f, labelCenterYPadding = 10f)){
                    if (!drawGauge(performanceInfoDetails.gas, canvas, rowTop, area.left.toFloat(),  area.height() + 10f, labelCenterYPadding = 10f)){
                        drawGauge(performanceInfoDetails.vehicleSpeed, canvas, rowTop, area.left.toFloat(),  area.height() + 10f, labelCenterYPadding = 10f)
                    }
                }
                //right side
                if (!drawGauge(performanceInfoDetails.intakePressure, canvas, rowTop, (area.left + area.width() / 2f) - 6f,  area.height() + 10f, labelCenterYPadding = 10f)){
                   if (!drawGauge(performanceInfoDetails.vehicleSpeed, canvas, rowTop, (area.left + area.width() / 2f) - 6f,  area.height() + 10f, labelCenterYPadding = 10f)){
                        drawGauge(performanceInfoDetails.gas, canvas, rowTop, (area.left + area.width() / 2f) - 6f,  area.height() + 10f, labelCenterYPadding = 10f)
                    }
                }
            }

            1 -> {
                drawGaugeSingle(performanceInfoDetails.torque, canvas, rowTop, area)
                drawGaugeSingle(performanceInfoDetails.intakePressure, canvas, rowTop, area)
                drawGaugeSingle(performanceInfoDetails.gas, canvas, rowTop, area)
                drawGaugeSingle(performanceInfoDetails.vehicleSpeed, canvas, rowTop, area)
            }
        }
    }

    fun drawGaugeSingle(
        metric: Metric?,
        canvas: Canvas,
        rowTop: Float,
        area: Rect
    ) {
        drawGauge(
            metric,
            canvas,
            rowTop,
            area.left.toFloat() + (area.width() / 4f),
            area.height().toFloat() + 24,
            labelCenterYPadding = 8f
        )
    }

    fun drawGauge(
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
                scaleEnabled = false
            )
            true
        }

    private inline fun maxItemWidth(area: Rect) = (area.width() / 6)
}
