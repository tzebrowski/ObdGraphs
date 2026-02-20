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
package org.obd.graphs.aa

import android.content.Intent
import android.content.res.Configuration
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.connection.CarConnection
import org.obd.graphs.aa.screen.CarScreen
import org.obd.graphs.aa.screen.CarScreenFactory
import org.obd.graphs.aa.screen.nav.GOTO_LAST_VSITED_SCREEN_EVENT
import org.obd.graphs.aa.screen.nav.START_DATA_LOGGING_EVENT
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.datalogger.DataLoggerRepository
import org.obd.graphs.preferences.initPrefs
import org.obd.graphs.renderer.api.Fps
import org.obd.graphs.sendBroadcastEvent
import org.obd.graphs.setCarContext

internal class CarSession : Session() {
    private val settings by lazy { CarSettings(carContext) }
    private val metricsCollector = MetricsCollector.instance()
    private val fps: Fps = Fps()
    private lateinit var screen: CarScreen

    override fun onCreateScreen(intent: Intent): Screen {
        setCarContext(carContext)
        initPrefs(carContext)
        screen = CarScreenFactory.instance(carContext, settings, metricsCollector, fps)
        CarConnection(carContext).type.observe(this, ::onConnectionStateUpdated)
        return screen
    }

    override fun onCarConfigurationChanged(newConfiguration: Configuration) {
        super.onCarConfigurationChanged(newConfiguration)
        screen.onCarConfigurationChanged()
    }

    private fun onConnectionStateUpdated(connectionState: Int) {
        when (connectionState) {
            CarConnection.CONNECTION_TYPE_PROJECTION -> {
                if (settings.isLoadLastVisitedScreenEnabled()) {
                    sendBroadcastEvent(GOTO_LAST_VSITED_SCREEN_EVENT)
                }

                if (settings.isAutomaticConnectEnabled() && !DataLoggerRepository.isRunning()) {
                    sendBroadcastEvent(START_DATA_LOGGING_EVENT)
                }
            }
        }
    }
}
