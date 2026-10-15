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
package org.obd.graphs.bl.datalogger.dtc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.obd.graphs.bl.datalogger.Pid
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.command.obd.ObdCommand
import org.obd.metrics.pid.PidDefinition

class EngineStateTest {
    private fun metric(
        pid: Pid,
        value: Any
    ): ObdMetric =
        ObdMetric
            .builder()
            .command(ObdCommand(PidDefinition(pid.id, "1", "01", "0C", "Engine speed", "rpm")))
            .value(value)
            .build()

    @Test
    fun `a reading counts only while it is recent`() {
        val reading = EngineReading(running = true, timestamp = 1_000)

        assertEquals(true, reading.runningAt(1_000 + ENGINE_STATE_MAX_AGE_MS))
        assertNull(reading.runningAt(1_001 + ENGINE_STATE_MAX_AGE_MS))
        assertNull((null as EngineReading?).runningAt(1_000))
    }

    @Test
    fun `engine speed and the vehicle status tell whether the engine runs`() {
        EngineStateTracker.onStopped()
        assertNull(EngineStateTracker.engineRunning())

        EngineStateTracker.postValue(metric(Pid.ENGINE_SPEED_PID_ID, 780))
        assertEquals(true, EngineStateTracker.engineRunning())

        EngineStateTracker.postValue(metric(Pid.EXT_ENGINE_SPEED_PID_ID, 0.0))
        assertEquals(false, EngineStateTracker.engineRunning())

        EngineStateTracker.postValue(metric(Pid.VEHICLE_STATUS_PID_ID, mapOf("engine.running" to true)))
        assertEquals(true, EngineStateTracker.engineRunning())

        // Other PIDs leave the state as it was.
        EngineStateTracker.postValue(metric(Pid.COOLANT_TEMP_PID_ID, 90))
        assertEquals(true, EngineStateTracker.engineRunning())

        EngineStateTracker.onStopped()
        assertNull(EngineStateTracker.engineRunning())
    }
}
