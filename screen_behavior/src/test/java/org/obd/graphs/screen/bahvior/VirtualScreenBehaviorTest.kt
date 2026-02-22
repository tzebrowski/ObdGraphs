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
import org.obd.graphs.bl.collector.Metric
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.datalogger.DataLoggerSettings
import org.obd.graphs.bl.datalogger.dataLoggerSettings
import org.obd.graphs.bl.query.Query
import org.obd.graphs.bl.query.QueryStrategyType
import org.obd.graphs.renderer.api.*
import org.obd.graphs.screen.behaviour.GiuliaScreenBehavior
import kotlin.test.assertEquals

internal class VirtualScreenBehaviorTest : TestSetup() {

    private val metricsCollector: MetricsCollector = mockk(relaxed = true)
    private val fps: Fps = mockk()

    private val giuliaSettings = mockk<ScreenSettings>(relaxed = true)

    private val giuliaVirtualConfig = mockk<GiuliaRendererSettings>(relaxed = true)

    private val mockQuery = mockk<Query>(relaxed = true)
    private val mockDataLoggerSettings = mockk<DataLoggerSettings>(relaxed = true)

    private lateinit var behavior: GiuliaScreenBehavior

    @Before
    override fun setup() {
        super.setup()

        mockkObject(SurfaceRenderer.Companion)
        every { SurfaceRenderer.allocate(any(), any(), any(), any(), any()) } returns mockk(relaxed = true)

        mockkObject(Query.Companion)
        every { Query.instance() } returns mockQuery

        mockkObject(dataLoggerSettings)
        every { dataLoggerSettings.instance() } returns mockDataLoggerSettings

        // 3. Bridge the two mocks together. When the behavior asks for the config, give it giuliaVirtualConfig
        every { giuliaSettings.getGiuliaRendererSetting() } returns giuliaVirtualConfig

        val settingsMap = mapOf(SurfaceRendererType.GIULIA to giuliaSettings)
        behavior = GiuliaScreenBehavior(context, metricsCollector, settingsMap, fps)
    }

    @Test
    fun `virtual screen delegates get and set to config`() {
        every { giuliaVirtualConfig.getVirtualScreen() } returns 2

        assertEquals(2, behavior.getCurrentVirtualScreen())

        behavior.setCurrentVirtualScreen(3)
        verify(exactly = 1) { giuliaVirtualConfig.setVirtualScreen(3) }
    }

    @Test
    fun `applyFilters with INDIVIDUAL_QUERY applies selected PIDs exactly`() {
        every { mockDataLoggerSettings.adapter.individualQueryStrategyEnabled } returns true

        val selectedPids = setOf(10L, 20L)
        val sortOrder = mapOf(10L to 1, 20L to 2)

        every { giuliaVirtualConfig.selectedPIDs } returns selectedPids
        every { giuliaVirtualConfig.getPIDsSortOrder() } returns sortOrder

        val mockMetric = mockk<Metric>(relaxed = true)
        every { mockMetric.source.command.pid.id } returns 10L
        every { metricsCollector.getMetrics() } returns listOf(mockMetric)

        behavior.query()

        verify { mockQuery.setStrategy(QueryStrategyType.INDIVIDUAL_QUERY) }
        verify { metricsCollector.applyFilter(enabled = selectedPids, order = sortOrder) }
        verify { mockQuery.update(setOf(10L)) }
    }

    @Test
    fun `applyFilters with SHARED_QUERY calculates intersection`() {
        every { mockDataLoggerSettings.adapter.individualQueryStrategyEnabled } returns false

        val selectedPids = setOf(10L, 20L, 30L)
        val activeQueryIds = mutableSetOf(20L, 30L, 40L) // The intersection is 20L and 30L

        every { giuliaVirtualConfig.selectedPIDs } returns selectedPids
        every { giuliaVirtualConfig.getPIDsSortOrder() } returns null
        every { mockQuery.getIDs() } returns activeQueryIds

        behavior.query()

        verify { mockQuery.setStrategy(QueryStrategyType.SHARED_QUERY) }

        verify { metricsCollector.applyFilter(enabled = setOf(20L, 30L), order = null) }
    }
}
