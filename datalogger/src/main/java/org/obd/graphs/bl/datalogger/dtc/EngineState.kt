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

import org.obd.graphs.bl.datalogger.MetricsProcessor
import org.obd.graphs.bl.datalogger.Pid
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.api.model.VehicleCapabilities

// Above this the engine runs: idle is ~700 rpm, a stopped engine reads 0.
private const val ENGINE_RUNNING_RPM = 300.0

// A reading older than this says nothing: the running query may not poll the engine at all.
const val ENGINE_STATE_MAX_AGE_MS = 10_000L

data class EngineReading(
    val running: Boolean,
    val timestamp: Long
)

// True / false only from a recent reading, null when unknown: the clear pre-check warns on true and stays quiet
// otherwise, never blocking a clear on a guess.
fun EngineReading?.runningAt(now: Long): Boolean? = this?.takeIf { now - it.timestamp <= ENGINE_STATE_MAX_AGE_MS }?.running

// The engine state as last delivered by the running query: engine speed (OBD-II 01 0C or the GME one) or the vehicle
// status PID. Most modules refuse a clear with the engine running (7F 14 22), so the clear dialogs ask this first.
object EngineStateTracker : MetricsProcessor {
    @Volatile
    private var last: EngineReading? = null

    private val rpmPids = setOf(Pid.ENGINE_SPEED_PID_ID.id, Pid.EXT_ENGINE_SPEED_PID_ID.id)

    fun engineRunning(now: Long = System.currentTimeMillis()): Boolean? = last.runningAt(now)

    override fun postValue(obdMetric: ObdMetric) {
        val running =
            when (obdMetric.command.pid.id) {
                in rpmPids -> (obdMetric.value as? Number)?.toDouble()?.let { it > ENGINE_RUNNING_RPM }
                Pid.VEHICLE_STATUS_PID_ID.id -> (obdMetric.value as? Map<*, *>)?.get("engine.running") as? Boolean
                else -> null
            } ?: return
        last = EngineReading(running, System.currentTimeMillis())
    }

    override fun onRunning(vehicleCapabilities: VehicleCapabilities?) {
        last = null
    }

    override fun onStopped() {
        last = null
    }
}
