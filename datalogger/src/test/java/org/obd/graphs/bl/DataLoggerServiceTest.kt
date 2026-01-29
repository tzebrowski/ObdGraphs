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
import io.mockk.impl.annotations.MockK
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.obd.graphs.Permissions
import org.obd.graphs.bl.datalogger.DataLoggerService
import org.obd.graphs.bl.datalogger.WorkflowOrchestrator
import org.obd.graphs.bl.datalogger.setWorkflowOrchestrator
import org.obd.graphs.bl.query.Query
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33]) // Test on Android 13 (Tiramisu)
class DataLoggerServiceTest : TestSetup() {

    @MockK(relaxed = true)
    internal lateinit var mockOrchestrator: WorkflowOrchestrator

    @Before
    override fun setup() {
        super.setup()
        setWorkflowOrchestrator(mockOrchestrator)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `onStartCommand with missing permissions should stop service`() {
        // Arrange
        // Simulate missing permissions
        every { Permissions.hasNotificationPermissions(any()) } returns false

        val intent = Intent(context, DataLoggerService::class.java).apply {
            action = "org.obd.graphs.logger.START"
        }

        val controller = Robolectric.buildService(DataLoggerService::class.java, intent)

        // Act
        val service = controller.create().get()
        controller.startCommand(0, 1)

        // Assert
        val shadowService = Shadows.shadowOf(service)
        // Service should have stopped itself
        assertEquals("Service should be stopped if permissions are missing", true, shadowService.isStoppedBySelf)
    }

    @Test
    fun `onStartCommand with ACTION_STOP should stop Orchestrator and Service`() {
        // Arrange
        val controller = Robolectric.buildService(DataLoggerService::class.java)
        val startIntent = Intent(context, DataLoggerService::class.java).apply {
            action = "org.obd.graphs.logger.START"
            putExtra("org.obd.graphs.logger.QUERY", mockk<Query>(relaxed = true))
        }
        val stopIntent = Intent(context, DataLoggerService::class.java).apply {
            action = "org.obd.graphs.logger.STOP"
        }

        val service = controller.create().get()

        // Start first
        service.onStartCommand(startIntent, 0, 1)

        // Act: Stop
        service.onStartCommand(stopIntent, 0, 2)

        // Assert
        verify { mockOrchestrator.stop() }

        val shadowService = Shadows.shadowOf(service)
        assertEquals("Service should be stopped after ACTION_STOP", true, shadowService.isStoppedBySelf)
    }

    @Test
    fun `onBind should return LocalBinder`() {
        // Arrange
        val service = Robolectric.setupService(DataLoggerService::class.java)
        val intent = Intent(context, DataLoggerService::class.java)

        // Act
        val binder = service.onBind(intent)

        // Assert
        assertNotNull(binder)
        assertEquals(DataLoggerService.LocalBinder::class.java, binder::class.java)
    }

    @Test
    fun `onStartCommand with ACTION_START should promote to Foreground and start Orchestrator`() {
        // Arrange
        val intent = Intent(context, DataLoggerService::class.java).apply {
            action = "org.obd.graphs.logger.START"
            putExtra("org.obd.graphs.logger.QUERY", mockk<Query>(relaxed = true))
        }

        // Inject the mock (Ensures the service uses OUR object, not a real one)
        setWorkflowOrchestrator(mockOrchestrator)

        val controller = Robolectric.buildService(DataLoggerService::class.java, intent)

        // Act
        val service = controller.create().get()
        controller.startCommand(0, 1)

        // Assert
        // Verify the specific mock instance we injected received the call
        verify { mockOrchestrator.start(any()) }
    }
}
