 /**
 * Copyright 2019-2026, Tomasz Żebrowski
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
package org.obd.graphs.renderer.brake_boosting

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import org.obd.graphs.bl.collector.Metric
import org.obd.graphs.bl.drag.DragRacingService
import org.obd.graphs.dpToPx
import org.obd.graphs.renderer.AbstractDrawer
import org.obd.graphs.renderer.api.BrakeBoostingSettings
import org.obd.graphs.renderer.api.GaugeProgressBarType
import org.obd.graphs.renderer.api.ScreenSettings
import org.obd.graphs.renderer.gauge.DrawerSettings
import org.obd.graphs.renderer.gauge.GaugeDrawer

internal class BrakeBoostingDrawer(context: Context, settings: ScreenSettings) : AbstractDrawer(context, settings) {

    private val gaugeDrawer = GaugeDrawer(
        settings = settings, context = context,
        drawerSettings = DrawerSettings(
            gaugeProgressBarType = GaugeProgressBarType.LONG, startAngle = 180f, sweepAngle = 180f
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
        drawGaugesBrakeBoosting(area, canvas, pTop - 30, gas = gas, torque = torque)
    }

    fun isBrakeBoosting(brakeBoostingSettings: BrakeBoostingSettings, gas: Metric?, torque: Metric?) =
        brakeBoostingSettings.viewEnabled &&
        DragRacingService.registry.getResult().readyToRace &&
                settings.getDragRacingScreenSettings().displayMetricsEnabled &&
                settings.getDragRacingScreenSettings().displayMetricsExtendedEnabled &&
                gas != null && torque != null && (gas.value as Number).toInt() > 0



    private fun drawGaugesBrakeBoosting(
        area: Rect,
        canvas: Canvas,
        top: Float,
        gas: Metric?,
        torque: Metric?
    ) {
        val marginPx = 10f.dpToPx

        val spacingPx = 10f.dpToPx

        val labelPaddingPx = 18f.dpToPx

        if (settings.isAA() || isLandscape()) {
            val gaugeWidth = (area.width() - (2 * marginPx) - spacingPx) / 2f
            val marginTop = gaugeWidth / 8

            val gasLeft = area.left + marginPx
            val torqueLeft = gasLeft + gaugeWidth + spacingPx // Use spacingPx here

            drawGauge(
                metric = gas,
                canvas = canvas,
                top = top + marginTop,
                left = gasLeft,
                width = gaugeWidth,
                labelCenterYPadding = labelPaddingPx
            )

            drawGauge(
                metric = torque,
                canvas = canvas,
                top = top + marginTop,
                left = torqueLeft,
                width = gaugeWidth,
                labelCenterYPadding = labelPaddingPx
            )

        } else {
            val gaugeWidth = area.width() - (2 * marginPx)
            val leftPosition = area.left + marginPx

            drawGauge(
                metric = gas,
                canvas = canvas,
                top = top,
                left = leftPosition,
                width = gaugeWidth,
                labelCenterYPadding = labelPaddingPx
            )

            drawGauge(
                metric = torque,
                canvas = canvas,
                top = top + gaugeWidth + spacingPx,
                left = leftPosition,
                width = gaugeWidth,
                labelCenterYPadding = labelPaddingPx
            )
        }
    }

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
