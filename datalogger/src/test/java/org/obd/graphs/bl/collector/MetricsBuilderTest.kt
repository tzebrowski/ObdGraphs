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
import org.junit.Assert.assertNull
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
import org.obd.metrics.pid.PidDefinition
import org.obd.metrics.pid.PidDefinitionRegistry
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MetricsBuilderTest : TestSetup() {

    private val rpmPid = PidDefinition(Pid.ENGINE_SPEED_PID_ID.id, "010C", "atsh6f1", "Engine RPM", "01", "SomeCodec")
    private val speedPid = PidDefinition(Pid.VEHICLE_SPEED_PID_ID.id, "010D", "atsh6f1", "Vehicle Speed", "01", "SomeCodec")
    private val gasPid = PidDefinition(Pid.GAS_PID_ID.id, "0111", "atsh6f1", "Gas", "01", "SomeCodec")

    @MockK
    lateinit var mockRegistry: PidDefinitionRegistry

    @MockK
    lateinit var mockDiagnostics: Diagnostics

    @MockK
    lateinit var mockHistogramSupplier: HistogramSupplier

    private lateinit var metricsBuilder: MetricsBuilder

    @Before
    override fun setup() {
        super.setup()
        mockkObject(DataLoggerRepository)

        every { mockRegistry.findBy(rpmPid.id) } returns rpmPid
        every { mockRegistry.findBy(speedPid.id) } returns speedPid
        every { mockRegistry.findBy(gasPid.id) } returns gasPid
        every { DataLoggerRepository.getPidDefinitionRegistry() } returns mockRegistry

        every { mockHistogramSupplier.findBy(any()) } returns null
        every { mockDiagnostics.histogram() } returns mockHistogramSupplier
        every { DataLoggerRepository.getDiagnostics() } returns mockDiagnostics

        metricsBuilder = MetricsBuilder()
    }

    @After
    fun tearDown() = unmockkAll()

    private fun histogram(
        min: Double,
        max: Double,
        mean: Double,
        latestValue: Double?
    ): Histogram {
        val h = mockk<Histogram>(relaxed = true)
        every { h.min } returns min
        every { h.max } returns max
        every { h.mean } returns mean
        every { h.latestValue } returns latestValue
        return h
    }

    @Test
    fun `buildFor ObdMetric populates stats from the histogram when present`() {
        every { mockHistogramSupplier.findBy(rpmPid) } returns histogram(1.0, 9.0, 5.0, 7.0)
        val obdMetric = ObdMetric.builder().command(ObdCommand(rpmPid)).build()

        val result = metricsBuilder.buildFor(obdMetric)

        assertEquals(1.0, result.min, 0.0001)
        assertEquals(9.0, result.max, 0.0001)
        assertEquals(5.0, result.mean, 0.0001)
        assertEquals(7.0, result.value)
    }

    @Test
    fun `buildFor ObdMetric defaults to zero stats when no histogram exists`() {
        val obdMetric = ObdMetric.builder().command(ObdCommand(rpmPid)).build()

        val result = metricsBuilder.buildFor(obdMetric)

        assertEquals(0.0, result.min, 0.0001)
        assertEquals(0.0, result.max, 0.0001)
        assertEquals(0.0, result.mean, 0.0001)
        assertEquals(0, result.value)
    }

    @Test
    fun `buildFor ids skips ids the registry cannot resolve`() {
        val unknownId = 424242L
        every { mockRegistry.findBy(unknownId) } returns null

        val result = metricsBuilder.buildFor(setOf(rpmPid.id, unknownId))

        assertEquals(1, result.size)
        assertEquals(rpmPid.id, result[0].pid.id)
    }

    @Test
    fun `buildFor ids without explicit order sorts by pid id ascending`() {
        val result = metricsBuilder.buildFor(linkedSetOf(speedPid.id, rpmPid.id))

        assertEquals(listOf(rpmPid.id, speedPid.id), result.map { it.pid.id })
    }

    @Test
    fun `buildFor ids with sortOrder honors the order map and falls back to pid id for unlisted entries`() {
        val result =
            metricsBuilder.buildFor(
                setOf(rpmPid.id, speedPid.id, gasPid.id),
                sortOrder = mapOf(speedPid.id to 0)
            )

        // speedPid has an explicit order, so it always sorts first; the two unlisted ids
        // (both implicitly Int.MAX_VALUE) fall back to comparing pid ids against each other:
        // rpmPid.id (13) < gasPid.id (7007).
        assertEquals(listOf(speedPid.id, rpmPid.id, gasPid.id), result.map { it.pid.id })
    }

    @Test
    fun `buildFor ids with a null sortOrder preserves the original set iteration order`() {
        val result = metricsBuilder.buildFor(linkedSetOf(speedPid.id, rpmPid.id, gasPid.id), sortOrder = null)

        assertEquals(listOf(speedPid.id, rpmPid.id, gasPid.id), result.map { it.pid.id })
    }

    @Test
    fun `buildDiff copies max minus min into the rebuilt source but the returned value still comes from the histogram`() {
        val sourceMetric =
            Metric.newInstance(
                source = ObdMetric.builder().command(ObdCommand(rpmPid)).value(42).build(),
                value = 42,
                min = 10.0,
                max = 90.0,
                mean = 50.0
            )

        val diff = metricsBuilder.buildDiff(sourceMetric)

        // buildDiff() feeds `max - min` into ObdMetric.value(), but buildFor(ObdMetric) always
        // re-derives the final Metric.value from the histogram lookup (0, since none is stubbed here) -
        // the diff value only ever survives inside the rebuilt source ObdMetric.
        assertEquals(80.0, diff.source.value)
        assertEquals(0, diff.value)
    }

    @Test
    fun `buildDiff sets a null source value when the original metric had no reading`() {
        val sourceMetric =
            Metric.newInstance(
                source = ObdMetric.builder().command(ObdCommand(rpmPid)).value(null).build(),
                value = 0,
                min = 10.0,
                max = 90.0,
                mean = 50.0
            )

        val diff = metricsBuilder.buildDiff(sourceMetric)

        assertNull(diff.source.value)
    }
}
