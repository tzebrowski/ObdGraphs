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
package org.obd.graphs.bl

import android.content.Intent
import io.mockk.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.obd.graphs.bl.datalogger.DataLoggerService
import org.obd.graphs.bl.datalogger.WorkflowOrchestrator
import org.obd.graphs.bl.query.Query
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowService

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33]) // Test on Android 13 (Tiramisu)
class DataLoggerServiceActionStartTest : TestSetup() {

    @Before
    override fun setup() {
        super.setup()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `onStartCommand with ACTION_START should promote to Foreground and start Orchestrator`() {
        // Arrange
        val intent = Intent(context, DataLoggerService::class.java).apply {
            action = "org.obd.graphs.logger.START" // Must match ACTION_START const
            putExtra("org.obd.graphs.logger.QUERY", mockk<Query>(relaxed = true))
        }

        // Pass the intent to the builder so it's available to the service
        val controller = Robolectric.buildService(DataLoggerService::class.java, intent)

        // Act
        val service = controller.create().get()
        // startCommand(0, 1) will use the intent provided in buildService
        controller.startCommand(0, 1)

        // Assert 1: Service is in foreground
        val shadowService: ShadowService = Shadows.shadowOf(service)
        assertEquals("Service should call startForeground", true, shadowService.isForegroundStopped.not())
        assertNotNull("Notification should be posted", shadowService.lastForegroundNotification)

        // Assert 2: Orchestrator start was called
        verify { anyConstructed<WorkflowOrchestrator>().start(any()) }
    }
}
