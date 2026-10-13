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
package org.obd.graphs.bl.readiness

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.obd.graphs.bl.datalogger.Pid
import org.obd.graphs.bl.datalogger.dtc.DtcClearOutcome
import org.obd.metrics.api.model.ReadinessMonitor
import org.obd.metrics.api.model.ReadinessMonitorId
import org.obd.metrics.api.model.ReadinessStatus
import org.obd.metrics.pid.PidDefinition
import org.obd.metrics.pid.PidDefinitionRegistry

class ReadinessTest {
    private fun status(vararg monitors: ReadinessMonitor) = ReadinessStatus(false, 0, false, monitors.toList())

    private val catalystIncomplete = ReadinessMonitor(ReadinessMonitorId.CATALYST, true, false)
    private val misfireComplete = ReadinessMonitor(ReadinessMonitorId.MISFIRE, true, true)
    private val evapComplete = ReadinessMonitor(ReadinessMonitorId.EVAPORATIVE_SYSTEM, true, true)
    private val secondaryAirUnsupported = ReadinessMonitor(ReadinessMonitorId.SECONDARY_AIR, false, false)

    @Test
    fun `summary counts complete out of supported monitors`() {
        val summary = status(misfireComplete, catalystIncomplete, evapComplete, secondaryAirUnsupported).summary()
        assertEquals(ReadinessSummary(2, 3), summary)
        assertFalse(summary.allComplete)
        assertTrue(status(misfireComplete, secondaryAirUnsupported).summary().allComplete)
        assertFalse(status(secondaryAirUnsupported).summary().allComplete)
    }

    @Test
    fun `shown monitors drop unsupported ones and put incomplete first`() {
        val shown = status(misfireComplete, catalystIncomplete, evapComplete, secondaryAirUnsupported).shownMonitors()
        assertEquals(
            listOf(ReadinessMonitorId.CATALYST, ReadinessMonitorId.MISFIRE, ReadinessMonitorId.EVAPORATIVE_SYSTEM),
            shown.map { it.id }
        )
    }

    @Test
    fun `state keeps each status and counter apart`() {
        val sinceClear = status(catalystIncomplete)
        val driveCycle = status(misfireComplete)
        val state =
            ReadinessState()
                .update(Pid.READINESS_SINCE_CLEAR_PID_ID.id, sinceClear, null)
                .update(Pid.READINESS_DRIVE_CYCLE_PID_ID.id, driveCycle, null)
                .update(Pid.DISTANCE_SINCE_CODES_CLEARED_PID_ID.id, 120, "km")

        assertSame(sinceClear, state.status(ReadinessView.SINCE_CLEAR))
        assertSame(driveCycle, state.status(ReadinessView.DRIVE_CYCLE))
        assertEquals(ReadinessCounter(120, "km"), state.counters[Pid.DISTANCE_SINCE_CODES_CLEARED_PID_ID.id])
    }

    @Test
    fun `snapshot keeps the since-clear summary and survives encoding`() {
        val snapshot = ReadinessStatus(true, 8, false, listOf(catalystIncomplete, misfireComplete, secondaryAirUnsupported)).snapshot(1234L)

        assertEquals(ReadinessSnapshot(1234L, true, 8, 1, 2), snapshot)
        assertEquals(snapshot, ReadinessSnapshot.decode(snapshot.encode()))
        assertNull(ReadinessSnapshot.decode(null))
        assertNull(ReadinessSnapshot.decode("garbage"))
        assertNull(ReadinessSnapshot.decode("1;maybe;8;1;2"))
    }

    @Test
    fun `readiness needs the module defining 01 01`() {
        val withModule = mockk<PidDefinitionRegistry>()
        every { withModule.findBy(Pid.READINESS_SINCE_CLEAR_PID_ID.id) } returns mockk<PidDefinition>()
        val withoutModule = mockk<PidDefinitionRegistry>()
        every { withoutModule.findBy(Pid.READINESS_SINCE_CLEAR_PID_ID.id) } returns null

        assertTrue(withModule.hasReadinessPids())
        assertFalse(withoutModule.hasReadinessPids())
    }

    @Test
    fun `only a refused clear keeps the stored readiness result`() {
        assertTrue(DtcClearOutcome.CLEARED.resetsReadiness())
        assertTrue(DtcClearOutcome.CODES_REMAIN.resetsReadiness())
        assertFalse(DtcClearOutcome.REFUSED.resetsReadiness())
    }

    @Test
    fun `drive cycle is not reported once since-clear arrived without it`() {
        assertFalse(ReadinessState().isNotReported(ReadinessView.DRIVE_CYCLE))

        val sinceClearOnly = ReadinessState().update(Pid.READINESS_SINCE_CLEAR_PID_ID.id, status(misfireComplete), null)
        assertTrue(sinceClearOnly.isNotReported(ReadinessView.DRIVE_CYCLE))
        assertFalse(sinceClearOnly.isNotReported(ReadinessView.SINCE_CLEAR))

        val both = sinceClearOnly.update(Pid.READINESS_DRIVE_CYCLE_PID_ID.id, status(misfireComplete), null)
        assertFalse(both.isNotReported(ReadinessView.DRIVE_CYCLE))
    }

    @Test
    fun `undecoded replies and other PIDs leave the state unchanged`() {
        val state = ReadinessState().update(Pid.READINESS_SINCE_CLEAR_PID_ID.id, status(misfireComplete), null)
        assertEquals(state, state.update(Pid.READINESS_SINCE_CLEAR_PID_ID.id, null, null))
        assertEquals(state, state.update(Pid.ENGINE_SPEED_PID_ID.id, 800, "rpm"))
        assertEquals(state, state.update(Pid.TIME_MIL_ON_PID_ID.id, "n/a", "min"))
        assertNull(state.status(ReadinessView.DRIVE_CYCLE))
    }

    @Test
    fun `readiness PIDs are recognised by category`() {
        assertTrue(PidDefinition().apply { category = "readiness" }.isReadinessPid())
        assertFalse(PidDefinition().apply { category = "ibs" }.isReadinessPid())
        assertFalse(PidDefinition().isReadinessPid())
    }
}
