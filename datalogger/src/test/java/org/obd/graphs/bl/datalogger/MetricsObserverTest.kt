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
package org.obd.graphs.bl.datalogger

import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.obd.graphs.bl.TestSetup
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.api.model.Reply
import org.obd.metrics.api.model.VehicleCapabilities
import org.obd.metrics.command.obd.ObdCommand
import org.obd.metrics.pid.PidDefinition
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MetricsObserverTest : TestSetup() {

    private val pid = PidDefinition(100L, "010C", "atsh6f1", "RPM", "01", "Codec")

    private lateinit var observer: MetricsObserver

    @Before
    override fun setup() {
        super.setup()
        observer = MetricsObserver()
    }

    @After
    fun tearDown() = unmockkAll()

    private fun processor(): MetricsProcessor = mockk(relaxed = true)

    @Test
    fun `observe registers and initializes the processor`() {
        val p = processor()

        observer.observe(p)

        verify(exactly = 1) { p.init(observer) }
    }

    @Test
    fun `onNext dispatches an ObdMetric reply to all registered processors`() {
        val processorA = processor()
        val processorB = processor()
        observer.observe(processorA)
        observer.observe(processorB)

        val reply = ObdMetric.builder().command(ObdCommand(pid)).value(42).build()
        observer.onNext(reply)

        verify(exactly = 1) { processorA.postValue(reply) }
        verify(exactly = 1) { processorB.postValue(reply) }
    }

    @Test
    fun `onNext ignores non ObdMetric replies`() {
        val processorA = processor()
        observer.observe(processorA)

        val reply = mockk<Reply<*>>(relaxed = true)
        observer.onNext(reply)

        verify(exactly = 0) { processorA.postValue(any()) }
    }

    @Test
    fun `onNext swallows a processor exception and still notifies the remaining processors`() {
        val throwing = processor()
        every { throwing.postValue(any()) } throws RuntimeException("boom")
        val healthy = processor()
        observer.observe(throwing)
        observer.observe(healthy)

        val reply = ObdMetric.builder().command(ObdCommand(pid)).value(1).build()
        observer.onNext(reply)

        verify(exactly = 1) { healthy.postValue(reply) }
    }

    @Test
    fun `onStopped fans out to all registered processors`() {
        val processorA = processor()
        val processorB = processor()
        observer.observe(processorA)
        observer.observe(processorB)

        observer.onStopped()

        verify(exactly = 1) { processorA.onStopped() }
        verify(exactly = 1) { processorB.onStopped() }
    }

    @Test
    fun `onRunning fans out the same VehicleCapabilities to all registered processors`() {
        val processorA = processor()
        val processorB = processor()
        observer.observe(processorA)
        observer.observe(processorB)

        val capabilities = mockk<VehicleCapabilities>(relaxed = true)
        observer.onRunning(capabilities)

        verify(exactly = 1) { processorA.onRunning(capabilities) }
        verify(exactly = 1) { processorB.onRunning(capabilities) }
    }
}
