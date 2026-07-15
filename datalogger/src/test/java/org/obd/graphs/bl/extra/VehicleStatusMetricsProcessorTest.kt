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
package org.obd.graphs.bl.extra

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.obd.graphs.bl.TestSetup
import org.obd.graphs.bl.datalogger.DataLoggerSettings
import org.obd.graphs.bl.datalogger.Pid
import org.obd.graphs.bl.datalogger.dataLoggerSettings
import org.obd.graphs.sendBroadcastEvent
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.command.obd.ObdCommand
import org.obd.metrics.pid.PidDefinition
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class VehicleStatusMetricsProcessorTest : TestSetup() {

    private val vehicleStatusPid = PidDefinition(Pid.VEHICLE_STATUS_PID_ID.id, "22F0", "atsh6f1", "Vehicle Status", "22", "Codec")
    private val settings = DataLoggerSettings()

    private lateinit var processor: VehicleStatusMetricsProcessor

    @Before
    override fun setup() {
        super.setup()
        mockkObject(dataLoggerSettings)
        every { dataLoggerSettings.instance() } returns settings

        mockkStatic("org.obd.graphs.BroadcastKt")
        every { sendBroadcastEvent(any<String>()) } just Runs

        settings.vehicleStatusPanelEnabled = true
        settings.vehicleStatusDisconnectWhenOff = false

        processor = VehicleStatusMetricsProcessor()
    }

    @After
    fun tearDown() = unmockkAll()

    private fun statusMetric(
        engineRunning: Boolean,
        vehicleRunning: Boolean,
        keyStatusOn: Boolean,
        accelerating: Boolean = false,
        decelerating: Boolean = false
    ): ObdMetric =
        ObdMetric
            .builder()
            .command(ObdCommand(vehicleStatusPid))
            .value(
                mapOf(
                    "engine.running" to engineRunning,
                    "vehicle.running" to vehicleRunning,
                    "key.status" to keyStatusOn,
                    "vehicle.accelerating" to accelerating,
                    "vehicle.decelerating" to decelerating
                )
            ).build()

    @Test
    fun `engine and vehicle running fires the RUNNING event`() {
        processor.postValue(statusMetric(engineRunning = true, vehicleRunning = true, keyStatusOn = true))

        verify(exactly = 1) { sendBroadcastEvent(EVENT_VEHICLE_STATUS_VEHICLE_RUNNING) }
    }

    @Test
    fun `engine running but vehicle stationary fires the IDLING event`() {
        processor.postValue(statusMetric(engineRunning = true, vehicleRunning = false, keyStatusOn = true))

        verify(exactly = 1) { sendBroadcastEvent(EVENT_VEHICLE_STATUS_VEHICLE_IDLING) }
    }

    @Test
    fun `nothing running and key off fires IGNITION_OFF`() {
        // The processor's initial state is already engine=off/vehicle=off/key=off, so the very
        // first reading needs to move away from that state first, otherwise the change-detection
        // guard sees no diff and fires nothing - establish a running state, then transition to off.
        processor.postValue(statusMetric(engineRunning = true, vehicleRunning = true, keyStatusOn = true))

        processor.postValue(statusMetric(engineRunning = false, vehicleRunning = false, keyStatusOn = false))

        verify(exactly = 1) { sendBroadcastEvent(EVENT_VEHICLE_STATUS_IGNITION_OFF) }
    }

    @Test
    fun `nothing running and key on fires IGNITION_ON`() {
        processor.postValue(statusMetric(engineRunning = false, vehicleRunning = false, keyStatusOn = true))

        verify(exactly = 1) { sendBroadcastEvent(EVENT_VEHICLE_STATUS_IGNITION_ON) }
    }

    @Test
    fun `accelerating flag fires the ACCELERATING event alongside the state event`() {
        processor.postValue(
            statusMetric(engineRunning = true, vehicleRunning = true, keyStatusOn = true, accelerating = true)
        )

        verify(exactly = 1) { sendBroadcastEvent(EVENT_VEHICLE_STATUS_VEHICLE_RUNNING) }
        verify(exactly = 1) { sendBroadcastEvent(EVENT_VEHICLE_STATUS_VEHICLE_ACCELERATING) }
    }

    @Test
    fun `decelerating flag fires the DECELERATING event alongside the state event`() {
        processor.postValue(
            statusMetric(engineRunning = true, vehicleRunning = false, keyStatusOn = true, decelerating = true)
        )

        verify(exactly = 1) { sendBroadcastEvent(EVENT_VEHICLE_STATUS_VEHICLE_IDLING) }
        verify(exactly = 1) { sendBroadcastEvent(EVENT_VEHICLE_STATUS_VEHICLE_DECELERATING) }
    }

    @Test
    fun `repeated identical state does not re-fire events, but a state change does`() {
        processor.postValue(statusMetric(engineRunning = true, vehicleRunning = true, keyStatusOn = true))
        processor.postValue(statusMetric(engineRunning = true, vehicleRunning = true, keyStatusOn = true))

        verify(exactly = 1) { sendBroadcastEvent(EVENT_VEHICLE_STATUS_VEHICLE_RUNNING) }

        processor.postValue(statusMetric(engineRunning = true, vehicleRunning = false, keyStatusOn = true))

        verify(exactly = 1) { sendBroadcastEvent(EVENT_VEHICLE_STATUS_VEHICLE_IDLING) }
    }

    @Test
    fun `does nothing when both status panel and disconnect flags are disabled`() {
        settings.vehicleStatusPanelEnabled = false
        settings.vehicleStatusDisconnectWhenOff = false

        processor.postValue(statusMetric(engineRunning = true, vehicleRunning = true, keyStatusOn = true))

        verify(exactly = 0) { sendBroadcastEvent(any<String>()) }
    }

    @Test
    fun `ignores metrics that are not the vehicle status pid`() {
        val otherPid = PidDefinition(9999L, "0100", "q", "Other", "01", "Codec")
        val metric = ObdMetric.builder().command(ObdCommand(otherPid)).value(42).build()

        processor.postValue(metric)

        verify(exactly = 0) { sendBroadcastEvent(any<String>()) }
    }
}
