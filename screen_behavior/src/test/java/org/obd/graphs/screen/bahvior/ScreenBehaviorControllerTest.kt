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
package org.obd.graphs.screen.bahvior

import io.mockk.*
import org.junit.Before
import org.junit.Test
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.renderer.api.*
import org.obd.graphs.screen.behaviour.ScreenBehaviorController
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class ScreenBehaviorControllerTest: TestSetup() {

    private val metricsCollector: MetricsCollector = mockk(relaxed = true)
    private val fps: Fps = mockk()
    private val mockRenderer: SurfaceRenderer = mockk(relaxed = true)

    // Mock the specific settings required by the behaviors
    private val settingsMap: Map<SurfaceRendererType, ScreenSettings> = mapOf(
        SurfaceRendererType.GIULIA to mockk(relaxed = true),
        SurfaceRendererType.DRAG_RACING to mockk(relaxed = true)
    )

    private lateinit var controller: ScreenBehaviorController

    @Before
    override fun setup() {
        super.setup()
        mockkObject(SurfaceRenderer.Companion)
        every {
            SurfaceRenderer.allocate(any(), any(), any(), any(), any())
        } returns mockRenderer

        controller = ScreenBehaviorController(context, metricsCollector, settingsMap, fps)
    }


    @Test
    fun `getScreenBehavior should return null for invalid Identity`() {
        val invalidIdentity = object : Identity { override fun id() = 999 }
        assertNull(controller.getScreenBehavior(invalidIdentity))
    }

    @Test
    fun `getScreenBehavior should cache and return the exact same instance on multiple calls`() {
        // First call allocates the behavior
        val behavior1 = controller.getScreenBehavior(SurfaceRendererType.GIULIA)
        assertNotNull(behavior1)

        // Second call should pull from the behaviorsCache
        val behavior2 = controller.getScreenBehavior(SurfaceRendererType.GIULIA)

        // Assert they are the exact same object in memory
        assertEquals(behavior1, behavior2)

        // Verify the surface renderer was only allocated once
        verify(exactly = 1) {
            SurfaceRenderer.allocate(any(), any(), any(), any(), eq(SurfaceRendererType.GIULIA))
        }
    }

    @Test
    fun `recycle should only recycle initialized behaviors`() {
        // Initialize ONLY Giulia
        controller.getScreenBehavior(SurfaceRendererType.GIULIA)

        // Call recycle
        controller.recycle()

        // Verify the mocked surface renderer had recycle() called exactly once
        verify(exactly = 1) { mockRenderer.recycle() }

        // Ensure DragRacing was NEVER allocated because we didn't request it
        verify(exactly = 0) {
            SurfaceRenderer.allocate(any(), any(), any(), any(), eq(SurfaceRendererType.DRAG_RACING))
        }
    }
}
