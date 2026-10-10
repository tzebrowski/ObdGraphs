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

private const val NANOS_PER_SECOND = 1_000_000_000L

// Low-priority PIDs are read every few seconds; a shorter limit would grey out healthy dials.
internal const val STALE_AFTER_NANOS = 10 * NANOS_PER_SECOND

// A dial not drawn for this long (removed, scrolled away, other screen) gives up its cached bitmaps.
internal const val EVICT_AFTER_NANOS = 3 * NANOS_PER_SECOND

private class GaugeFrameState {
    var needle = Float.NaN
    var lastNeedleNanos = 0L
    var lastDrawnNanos = 0L
    var lastSource: Any? = null
    var lastUpdateNanos = 0L
}

/**
 * Per-PID state that lives across frames: the eased needle, when the value last changed, when the
 * dial was last drawn. Pure (the clock is passed in) so it can be unit-tested.
 */
internal class GaugeFrameStates(
    private val staleAfterNanos: Long = STALE_AFTER_NANOS,
    private val evictAfterNanos: Long = EVICT_AFTER_NANOS
) {
    private val states = HashMap<Long, GaugeFrameState>()
    private var lastEvictionNanos = Long.MIN_VALUE

    private fun state(
        id: Long,
        nowNanos: Long
    ): GaugeFrameState = states.getOrPut(id) { GaugeFrameState().also { it.lastUpdateNanos = nowNanos } }

    /** Records that the dial of [id] is drawn this frame; one not drawn for a while gets evicted. */
    fun markDrawn(
        id: Long,
        nowNanos: Long
    ) {
        state(id, nowNanos).lastDrawnNanos = nowNanos
    }

    /** The needle to draw this frame, eased from the last frame's towards [target]. */
    fun needle(
        id: Long,
        target: Float,
        nowNanos: Long
    ): Float {
        val state = state(id, nowNanos)
        val elapsed = if (state.needle.isNaN()) 0f else (nowNanos - state.lastNeedleNanos).toFloat() / NANOS_PER_SECOND
        state.needle = GaugeGeometry.easeNeedle(state.needle, target, elapsed)
        state.lastNeedleNanos = nowNanos
        return state.needle
    }

    /**
     * True when the PID has a value but no new reading arrived for [staleAfterNanos]. The collector
     * replaces [source] on every reading, so a change of identity is a new reading.
     */
    fun isStale(
        id: Long,
        source: Any?,
        hasValue: Boolean,
        nowNanos: Long
    ): Boolean {
        val state = state(id, nowNanos)
        if (source !== state.lastSource) {
            state.lastSource = source
            state.lastUpdateNanos = nowNanos
        }
        return hasValue && nowNanos - state.lastUpdateNanos > staleAfterNanos
    }

    /**
     * Forgets the PIDs not drawn for [evictAfterNanos] and returns their ids, so the drawer can free
     * their caches. Runs at most once per eviction period; returns empty otherwise.
     */
    fun evict(nowNanos: Long): Set<Long> {
        if (lastEvictionNanos != Long.MIN_VALUE && nowNanos - lastEvictionNanos < evictAfterNanos) return emptySet()
        lastEvictionNanos = nowNanos

        val evicted = states.filterValues { nowNanos - it.lastDrawnNanos > evictAfterNanos }.keys.toSet()
        states.keys.removeAll(evicted)
        return evicted
    }

    fun clear() {
        states.clear()
        lastEvictionNanos = Long.MIN_VALUE
    }
}
