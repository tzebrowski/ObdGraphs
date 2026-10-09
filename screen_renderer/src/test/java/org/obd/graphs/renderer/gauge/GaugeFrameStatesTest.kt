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
package org.obd.graphs.renderer.gauge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val SECOND = 1_000_000_000L
private const val FRAME = SECOND / 60

class GaugeFrameStatesTest {
    @Test
    fun `needle starts on the value, then eases to the next one`() {
        val states = GaugeFrameStates()

        assertEquals(0.2f, states.needle(1, 0.2f, nowNanos = SECOND))
        val next = states.needle(1, 0.8f, nowNanos = SECOND + FRAME)

        assertTrue("$next", next > 0.2f && next < 0.8f)
    }

    @Test
    fun `needle of each PID eases on its own`() {
        val states = GaugeFrameStates()
        states.needle(1, 0f, SECOND)
        states.needle(2, 1f, SECOND)

        assertEquals(1f, states.needle(2, 1f, SECOND + FRAME))
        assertTrue(states.needle(1, 1f, SECOND + FRAME) < 1f)
    }

    @Test
    fun `dial goes stale when no new reading arrives`() {
        val states = GaugeFrameStates(staleAfterNanos = 5 * SECOND)
        val reading = Any()

        assertFalse(states.isStale(1, reading, hasValue = true, nowNanos = SECOND))
        assertFalse(states.isStale(1, reading, hasValue = true, nowNanos = 5 * SECOND))
        assertTrue(states.isStale(1, reading, hasValue = true, nowNanos = 7 * SECOND))
        // A new reading (the collector replaces the source object) makes it live again.
        assertFalse(states.isStale(1, Any(), hasValue = true, nowNanos = 8 * SECOND))
    }

    @Test
    fun `dial without a value is never stale`() {
        val states = GaugeFrameStates(staleAfterNanos = SECOND)

        states.isStale(1, null, hasValue = false, nowNanos = SECOND)
        assertFalse(states.isStale(1, null, hasValue = false, nowNanos = 100 * SECOND))
    }

    @Test
    fun `PIDs not drawn any more are evicted so their caches can be freed`() {
        val states = GaugeFrameStates(evictAfterNanos = 3 * SECOND)
        states.markDrawn(1, SECOND)
        states.markDrawn(2, SECOND)
        assertEquals(emptySet<Long>(), states.evict(SECOND))

        states.markDrawn(1, 5 * SECOND)

        assertEquals(setOf(2L), states.evict(5 * SECOND))
    }

    @Test
    fun `eviction runs at most once per period`() {
        val states = GaugeFrameStates(evictAfterNanos = 3 * SECOND)
        states.markDrawn(1, SECOND)
        states.evict(SECOND)

        // Due by age, but the last sweep was only 2.5 s ago.
        assertEquals(emptySet<Long>(), states.evict(3 * SECOND + SECOND / 2))
        assertEquals(setOf(1L), states.evict(5 * SECOND))
    }

    @Test
    fun `evicted PID starts afresh`() {
        val states = GaugeFrameStates(evictAfterNanos = 3 * SECOND)
        states.markDrawn(1, SECOND)
        states.needle(1, 0.1f, SECOND)
        states.evict(10 * SECOND)

        assertEquals(0.9f, states.needle(1, 0.9f, 10 * SECOND + FRAME))
    }
}
