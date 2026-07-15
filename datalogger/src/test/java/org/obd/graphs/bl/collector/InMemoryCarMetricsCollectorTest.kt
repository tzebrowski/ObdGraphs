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
package org.obd.graphs.bl.collector

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.obd.graphs.bl.TestSetup
import org.obd.graphs.bl.datalogger.DataLoggerRepository
import org.obd.graphs.bl.datalogger.Pid
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.command.obd.ObdCommand
import org.obd.metrics.diagnostic.Diagnostics
import org.obd.metrics.diagnostic.Histogram
import org.obd.metrics.diagnostic.HistogramSupplier
import org.obd.metrics.diagnostic.Rate
import org.obd.metrics.diagnostic.RateType
import org.obd.metrics.pid.PidDefinition
import org.obd.metrics.pid.PidDefinitionRegistry
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Optional

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class InMemoryCarMetricsCollectorTest : TestSetup() {

    private val rpmPid = PidDefinition(Pid.ENGINE_SPEED_PID_ID.id, "010C", "atsh6f1", "Engine RPM", "01", "SomeCodec")
    private val speedPid = PidDefinition(Pid.VEHICLE_SPEED_PID_ID.id, "010D", "atsh6f1", "Vehicle Speed", "01", "SomeCodec")

    @MockK
    lateinit var mockRegistry: PidDefinitionRegistry

    @MockK
    lateinit var mockDiagnostics: Diagnostics

    @MockK
    lateinit var mockHistogramSupplier: HistogramSupplier

    private lateinit var collector: InMemoryCarMetricsCollector

    @Before
    override fun setup() {
        super.setup()
        mockkObject(DataLoggerRepository)

        every { mockRegistry.findBy(rpmPid.id) } returns rpmPid
        every { mockRegistry.findBy(speedPid.id) } returns speedPid
        every { DataLoggerRepository.getPidDefinitionRegistry() } returns mockRegistry

        every { mockHistogramSupplier.findBy(any()) } returns null
        every { mockDiagnostics.histogram() } returns mockHistogramSupplier
        every { DataLoggerRepository.getDiagnostics() } returns mockDiagnostics

        every { DataLoggerRepository.findRateFor(any()) } returns Optional.empty()
        every { DataLoggerRepository.findHistogramFor(any()) } returns emptyHistogram()

        collector = InMemoryCarMetricsCollector()
    }

    @After
    fun tearDown() = unmockkAll()

    private fun emptyHistogram(): Histogram =
        mockk(relaxed = true) {
            every { min } returns 0.0
            every { max } returns 0.0
            every { mean } returns 0.0
        }

    private fun metricFor(
        pid: PidDefinition,
        value: Any?,
        lowerAlert: Boolean = false,
        upperAlert: Boolean = false
    ): ObdMetric =
        ObdMetric
            .builder()
            .command(ObdCommand(pid))
            .value(value)
            .lowerAlert(lowerAlert)
            .upperAlert(upperAlert)
            .build()

    @Test
    fun `append with forceAppend adds a new metric`() {
        collector.append(metricFor(rpmPid, 3000), forceAppend = true)

        val result = collector.getMetric(rpmPid.id)

        assertNotNull(result)
        assertEquals(3000, result?.value)
    }

    @Test
    fun `append without forceAppend ignores an unknown pid`() {
        collector.append(metricFor(rpmPid, 3000), forceAppend = false)

        assertNull(collector.getMetric(rpmPid.id))
    }

    @Test
    fun `append with null input is a no-op`() {
        collector.append(null, forceAppend = true)

        assertTrue(collector.getMetrics(enabled = false).isEmpty())
    }

    @Test
    fun `append updates value, rate and histogram of an existing metric`() {
        collector.append(metricFor(rpmPid, 1000), forceAppend = true)

        every { DataLoggerRepository.findRateFor(any()) } returns Optional.of(Rate(RateType.MEAN, 12.5, "rpm"))
        val histogram =
            mockk<Histogram>(relaxed = true) {
                every { min } returns 800.0
                every { max } returns 5000.0
                every { mean } returns 2500.0
            }
        every { DataLoggerRepository.findHistogramFor(any()) } returns histogram

        collector.append(metricFor(rpmPid, 4200), forceAppend = true)

        val metric = collector.getMetric(rpmPid.id)!!
        assertEquals(4200, metric.value)
        assertEquals(12.5, metric.rate)
        assertEquals(800.0, metric.min, 0.0001)
        assertEquals(5000.0, metric.max, 0.0001)
        assertEquals(2500.0, metric.mean, 0.0001)
    }

    @Test
    fun `append latches lower and upper alert flags once raised`() {
        collector.append(metricFor(rpmPid, 1000, lowerAlert = true, upperAlert = true), forceAppend = true)
        // A subsequent, non-alerting reading should not clear the latched flags.
        collector.append(metricFor(rpmPid, 2000, lowerAlert = false, upperAlert = false), forceAppend = true)

        val metric = collector.getMetric(rpmPid.id)!!
        assertTrue(metric.inLowerAlertRisedHist)
        assertTrue(metric.inUpperAlertRisedHist)
    }

    @Test
    fun `alertReset clears latched alert flags`() {
        collector.append(metricFor(rpmPid, 1000, lowerAlert = true, upperAlert = true), forceAppend = true)

        collector.alertReset()

        val metric = collector.getMetric(rpmPid.id)!!
        assertFalse(metric.inLowerAlertRisedHist)
        assertFalse(metric.inUpperAlertRisedHist)
    }

    @Test
    fun `applyFilter builds missing pids and exposes them as enabled`() {
        collector.applyFilter(enabled = setOf(rpmPid.id, speedPid.id))

        assertEquals(2, collector.getMetrics(enabled = true).size)
        assertNull(collector.getMetric(rpmPid.id, enabled = false))
    }

    @Test
    fun `applyFilter disables metrics that are no longer in the enabled set`() {
        collector.applyFilter(enabled = setOf(rpmPid.id, speedPid.id))

        collector.applyFilter(enabled = setOf(rpmPid.id))

        assertEquals(1, collector.getMetrics(enabled = true).size)
        assertNotNull(collector.getMetric(speedPid.id, enabled = false))
        assertNull(collector.getMetric(speedPid.id, enabled = true))
    }

    @Test
    fun `applyFilter orders visible metrics using the supplied order map`() {
        collector.applyFilter(
            enabled = setOf(rpmPid.id, speedPid.id),
            order = mapOf(speedPid.id to 0, rpmPid.id to 1)
        )

        val ids = collector.getMetrics(enabled = true).map { it.pid.id }
        assertEquals(listOf(speedPid.id, rpmPid.id), ids)
    }

    @Test
    fun `applyFilter falls back to pid id ordering when order map is absent`() {
        collector.applyFilter(enabled = setOf(speedPid.id, rpmPid.id), order = null)

        val ids = collector.getMetrics(enabled = true).map { it.pid.id }
        assertEquals(listOf(rpmPid.id, speedPid.id), ids)
    }

    @Test
    fun `getMetric by Pid enum delegates to id lookup`() {
        collector.applyFilter(enabled = setOf(Pid.ENGINE_SPEED_PID_ID.id))

        assertNotNull(collector.getMetric(Pid.ENGINE_SPEED_PID_ID))
        assertNull(collector.getMetric(Pid.VEHICLE_SPEED_PID_ID))
    }

    @Test
    fun `getMetric returns null when enabled state does not match`() {
        collector.applyFilter(enabled = setOf(rpmPid.id))

        assertNull(collector.getMetric(rpmPid.id, enabled = false))
        assertNotNull(collector.getMetric(rpmPid.id, enabled = true))
    }
}
