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
package org.obd.graphs.renderer.break_boosting

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import org.obd.graphs.bl.collector.Metric
import org.obd.graphs.bl.drag.dragRacingResultRegistry
import org.obd.graphs.getContext
import org.obd.graphs.renderer.AbstractDrawer
import org.obd.graphs.renderer.GaugeProgressBarType
import org.obd.graphs.renderer.ScreenSettings
import org.obd.graphs.renderer.gauge.DrawerSettings
import org.obd.graphs.renderer.gauge.GaugeDrawer

internal class BreakBoostingDrawer(context: Context, settings: ScreenSettings) : AbstractDrawer(context, settings) {

    private val gaugeDrawer = GaugeDrawer(
        settings = settings, context = context,
        drawerSettings = DrawerSettings(
            gaugeProgressBarType = GaugeProgressBarType.LONG, startAngle = 180f, sweepAngle = 120f
        )
    )

    private val background: Bitmap =
        BitmapFactory.decodeResource(context.resources, org.obd.graphs.renderer.R.drawable.drag_race_bg)

    override fun getBackground(): Bitmap = background

    fun drawScreen(
        canvas: Canvas,
        area: Rect,
        pTop: Float,
        gas: Metric?,
        torque: Metric?
    ) {
        drawGaugesBreakBoosting(area, canvas, pTop - 30, gas = gas, torque = torque)
    }

    fun isBreakBoosting(gas: Metric?,torque: Metric?) =
        dragRacingResultRegistry.getResult().readyToRace &&
                settings.getDragRacingScreenSettings().displayMetricsEnabled &&
                settings.getDragRacingScreenSettings().displayMetricsExtendedEnabled &&
                gas != null && torque != null && (gas.value as Number).toInt() > 0


    private fun drawGaugesBreakBoosting(
        area: Rect,
        canvas: Canvas,
        top: Float,
        gas: Metric?,
        torque: Metric?
    ) {
        val marginLeft = 20f
        if (settings.isAA() || isLandscape()) {
            val gaugeWidth = area.width() / 1.8f

            val marginTop = gaugeWidth / 8

            drawGauge(
                gas, canvas, top + marginTop, area.left.toFloat() + 2 * marginLeft,
                gaugeWidth, labelCenterYPadding = 18f
            )

            drawGauge(
                torque, canvas, top + marginTop, (area.left + marginLeft + gaugeWidth * 0.8f),
                gaugeWidth, labelCenterYPadding = 18f
            )

        } else {
            val gaugeWidth = area.width().toFloat()

            drawGauge(
                gas, canvas, top, area.left.toFloat() + 2 * marginLeft,
                gaugeWidth, labelCenterYPadding = 18f
            )

            drawGauge(
                torque, canvas, top + gaugeWidth, area.left.toFloat() + 2 * marginLeft,
                gaugeWidth, labelCenterYPadding = 18f
            )
        }
    }


    private fun isLandscape() = getContext()!!.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    private fun drawGauge(
        metric: Metric?,
        canvas: Canvas,
        top: Float,
        left: Float,
        width: Float,
        labelCenterYPadding: Float = 10f,
    ): Boolean =
        if (metric == null) {
            false
        } else {
            gaugeDrawer.drawGauge(
                canvas = canvas,
                left = left,
                top = top,
                width = width,
                metric = metric,
                labelCenterYPadding = labelCenterYPadding,
                fontSize = settings.getDragRacingScreenSettings().fontSize,
                scaleEnabled = false,
                statsEnabled = false
            )
            true
        }
}
