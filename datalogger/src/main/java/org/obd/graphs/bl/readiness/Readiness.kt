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

import org.obd.graphs.bl.datalogger.Pid
import org.obd.graphs.bl.datalogger.dtc.DtcClearOutcome
import org.obd.metrics.api.model.ReadinessMonitor
import org.obd.metrics.api.model.ReadinessStatus
import org.obd.metrics.pid.PidDefinition

// Shared by the phone readiness screen (and an AA one later) - keep the display logic here, Android-free.

// ObdMetrics tags the readiness PIDs (mode 01 PIDs 01 / 41) with this category.
const val READINESS_PID_CATEGORY = "readiness"

// Readiness PIDs decode to a ReadinessStatus, not a number: on a gauge or in a grid they would only ever show NaN,
// so the PID pickers leave them out. The readiness screen queries them itself.
fun PidDefinition.isReadinessPid(): Boolean = READINESS_PID_CATEGORY.equals(category, ignoreCase = true)

// Which status the screen shows: since the codes were last cleared (01 01) or this drive cycle (01 41).
enum class ReadinessView { SINCE_CLEAR, DRIVE_CYCLE }

// "5 / 7": complete out of supported (or, for the drive cycle, enabled) monitors.
data class ReadinessSummary(
    val complete: Int,
    val supported: Int
) {
    val allComplete: Boolean get() = supported > 0 && complete == supported
}

fun ReadinessStatus.summary(): ReadinessSummary =
    monitors.filter { it.isSupported }.let { supported -> ReadinessSummary(supported.count { it.isComplete }, supported.size) }

// The last since-clear result, so the DTC list can show it without polling the ECU. One line in the preferences.
const val PREF_READINESS_LAST = "pref.readiness.last"

data class ReadinessSnapshot(
    val timestamp: Long,
    val milOn: Boolean,
    val emissionDtcCount: Int,
    val complete: Int,
    val supported: Int
) {
    fun encode(): String = listOf(timestamp, milOn, emissionDtcCount, complete, supported).joinToString(";")

    companion object {
        // Null for a missing or malformed value - the DTC list then offers the check instead of a result.
        fun decode(value: String?): ReadinessSnapshot? =
            value?.split(";")?.takeIf { it.size == 5 }?.let { parts ->
                runCatching {
                    ReadinessSnapshot(parts[0].toLong(), parts[1].toBooleanStrict(), parts[2].toInt(), parts[3].toInt(), parts[4].toInt())
                }.getOrNull()
            }
    }
}

// A clear the ECU accepted resets every monitor, so the stored result no longer describes the car. Only a refused
// clear leaves it valid (codes that remain after an accepted clear came back, the monitors were still reset).
fun DtcClearOutcome.resetsReadiness(): Boolean = this != DtcClearOutcome.REFUSED

fun ReadinessStatus.snapshot(timestamp: Long): ReadinessSnapshot =
    summary().let { ReadinessSnapshot(timestamp, isMilOn, emissionDtcCount, it.complete, it.supported) }

// The monitors worth a row: the ones this engine has (enabled this cycle for the drive-cycle view), incomplete
// first - they are what the screen is opened for - then in J1979 order.
fun ReadinessStatus.shownMonitors(): List<ReadinessMonitor> =
    monitors.filter { it.isSupported }.sortedBy { if (it.isComplete) 1 else 0 }

// The latest values the readiness screen got from the readiness query. Counters are kept as numbers + units.
data class ReadinessState(
    val sinceClear: ReadinessStatus? = null,
    val driveCycle: ReadinessStatus? = null,
    val counters: Map<Long, ReadinessCounter> = emptyMap()
) {
    fun status(view: ReadinessView): ReadinessStatus? =
        when (view) {
            ReadinessView.SINCE_CLEAR -> sinceClear
            ReadinessView.DRIVE_CYCLE -> driveCycle
        }

    // 01 01 and 01 41 are requested in one batch, so once 01 01 has been answered a missing 01 41 means the ECU does
    // not report it (the 2.0 GME does not), not that the reply is still on its way.
    fun isNotReported(view: ReadinessView): Boolean = status(view) == null && sinceClear != null
}

data class ReadinessCounter(
    val value: Number,
    val units: String?
)

// Counters in display order: distance and time since the codes were cleared, then with the MIL on.
val READINESS_COUNTER_PIDS =
    listOf(
        Pid.DISTANCE_SINCE_CODES_CLEARED_PID_ID.id,
        Pid.TIME_SINCE_CODES_CLEARED_PID_ID.id,
        Pid.DISTANCE_MIL_ON_PID_ID.id,
        Pid.TIME_MIL_ON_PID_ID.id
    )

// Folds one decoded reply into the state; anything else (other PIDs, a value that failed to decode) leaves it as is.
fun ReadinessState.update(
    pidId: Long,
    value: Any?,
    units: String?
): ReadinessState =
    when {
        pidId == Pid.READINESS_SINCE_CLEAR_PID_ID.id && value is ReadinessStatus -> copy(sinceClear = value)
        pidId == Pid.READINESS_DRIVE_CYCLE_PID_ID.id && value is ReadinessStatus -> copy(driveCycle = value)
        pidId in READINESS_COUNTER_PIDS && value is Number -> copy(counters = counters + (pidId to ReadinessCounter(value, units)))
        else -> this
    }
