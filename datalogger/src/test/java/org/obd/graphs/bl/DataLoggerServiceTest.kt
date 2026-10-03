/*
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

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.obd.graphs.Permissions
import org.obd.graphs.bl.datalogger.DataLoggerRepository
import org.obd.graphs.bl.datalogger.DataLoggerService
import org.obd.graphs.bl.datalogger.WorkflowOrchestrator
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
        DataLoggerRepository.setWorkflowOrchestrator(mockOrchestrator)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `onStartCommand with missing permissions should stop service`() {
        // Arrange
        every { Permissions.hasNotificationPermissions(any()) } returns false

        every { context.sendBroadcast(any()) } just Runs

        val intent =
            Intent(context, DataLoggerService::class.java).apply {
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

        verify { context.sendBroadcast(any()) }
    }

    @Test
    fun `onStartCommand with ACTION_STOP should stop Orchestrator and Service`() {
        // Arrange
        val app = org.robolectric.RuntimeEnvironment.getApplication()
        val shadowPackageManager = Shadows.shadowOf(app.packageManager)

        val packageName = app.packageName
        val componentName = ComponentName(packageName, "MainActivity")

        shadowPackageManager.addActivityIfNotPresent(componentName)

        val launchIntentFilter = Intent(Intent.ACTION_MAIN)
        launchIntentFilter.addCategory(Intent.CATEGORY_LAUNCHER)
        launchIntentFilter.setPackage(packageName)

        val resolveInfo = android.content.pm.ResolveInfo()
        resolveInfo.activityInfo = android.content.pm.ActivityInfo()
        resolveInfo.activityInfo.packageName = packageName
        resolveInfo.activityInfo.name = componentName.className

        // 5. Register the resolve info
        shadowPackageManager.addResolveInfoForIntent(launchIntentFilter, resolveInfo)

        // --- Rest of your test ---

        val controller = Robolectric.buildService(DataLoggerService::class.java)
        val startIntent =
            Intent(app, DataLoggerService::class.java).apply {
                action = "org.obd.graphs.logger.START"
                putExtra("org.obd.graphs.logger.QUERY", mockk<Query>(relaxed = true))
            }
        val stopIntent =
            Intent(app, DataLoggerService::class.java).apply {
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
        val intent =
            Intent(context, DataLoggerService::class.java).apply {
                action = "org.obd.graphs.logger.START"
                putExtra("org.obd.graphs.logger.QUERY", mockk<Query>(relaxed = true))
            }

        // Inject the mock (Ensures the service uses OUR object, not a real one)
        DataLoggerRepository.setWorkflowOrchestrator(mockOrchestrator)

        val controller = Robolectric.buildService(DataLoggerService::class.java, intent)

        // Act
        controller.create().get()
        controller.startCommand(0, 1)

        // Assert
        // Verify the specific mock instance we injected received the call
        verify { mockOrchestrator.start(any()) }
    }

    // A null intent is the system re-creating the service after the process was killed. From the
    // background Android 12+ refuses startForeground(), which crashed production.
    @Test
    fun `onStartCommand without an intent should stop instead of going foreground`() {
        val service = Robolectric.buildService(DataLoggerService::class.java).create().get()

        val result = service.onStartCommand(null, 0, 1)

        assertEquals(Service.START_NOT_STICKY, result)
        assertTrue(Shadows.shadowOf(service).isStoppedBySelf)
        assertNull(Shadows.shadowOf(service).lastForegroundNotification)
    }

    @Test
    fun `onStartCommand should stop the service when going foreground is refused`() {
        val service = spyk(Robolectric.buildService(DataLoggerService::class.java).create().get())
        every { service.startForeground(any(), any(), any<Int>()) } throws
            ForegroundServiceStartNotAllowedException("mAllowStartForeground false")

        val result = service.onStartCommand(Intent(), 0, 1)

        assertEquals(Service.START_NOT_STICKY, result)
        verify { service.stopSelf() }
        verify(exactly = 0) { mockOrchestrator.start(any()) }
    }

    @Test
    fun `onStartCommand should go foreground and not ask to be restarted after a kill`() {
        val service = Robolectric.buildService(DataLoggerService::class.java).create().get()

        val result = service.onStartCommand(Intent(), 0, 1)

        assertEquals(Service.START_NOT_STICKY, result)
        assertNotNull(Shadows.shadowOf(service).lastForegroundNotification)
        assertFalse(Shadows.shadowOf(service).isStoppedBySelf)
    }
}
