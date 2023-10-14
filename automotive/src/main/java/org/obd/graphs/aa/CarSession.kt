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
package org.obd.graphs.aa

import android.content.Intent
import android.content.res.Configuration
import android.util.Log
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.obd.graphs.renderer.Fps
import org.obd.graphs.bl.collector.CarMetricsCollector
import org.obd.graphs.setCarContext


private const val LOG_TAG = "CarSession"


internal class CarSession : Session(), DefaultLifecycleObserver {

    private lateinit var surfaceController: SurfaceController
    private val settings by lazy {  CarSettings(carContext) }
    private val metricsCollector = CarMetricsCollector()
    private val fps: Fps = Fps()

    override fun onCreateScreen(intent: Intent): Screen {
        lifecycle.addObserver(this)
        setCarContext(carContext)

        return if (settings.getScreenTemplate() == ScreenTemplateType.NAV) {
            surfaceController = SurfaceController(carContext, settings, metricsCollector, fps)
            lifecycle.addObserver(surfaceController)
            NavTemplateCarScreen(carContext, surfaceController, settings, metricsCollector, fps)
        } else {
            IotTemplateCarScreen(carContext, settings, metricsCollector)
        }
    }

    override fun onCarConfigurationChanged(newConfiguration: Configuration) {
        super.onCarConfigurationChanged(newConfiguration)
        if (settings.getScreenTemplate() == ScreenTemplateType.NAV)
            surfaceController.onCarConfigurationChanged()

    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        Log.d(LOG_TAG, "Received onResume event")
        if (settings.getScreenTemplate() == ScreenTemplateType.NAV)
            lifecycle.addObserver(surfaceController)
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        Log.d(LOG_TAG, "Received onPause event")
        if (settings.getScreenTemplate() == ScreenTemplateType.NAV)
            lifecycle.removeObserver(surfaceController)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        Log.d(LOG_TAG, "Received onDestroy event")
        if (settings.getScreenTemplate() == ScreenTemplateType.NAV)
            lifecycle.removeObserver(surfaceController)
    }
}
