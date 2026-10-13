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

import org.obd.metrics.api.model.DiagnosticTroubleCode

// ISO 14229-1 DTC status byte bits (statusMask). Every code ObdMetrics decodes comes from its UDS
// parser, so the mask is always the ECU's real status byte.
private const val TEST_FAILED = 0x01
private const val TEST_FAILED_THIS_OPERATION_CYCLE = 0x02
private const val PENDING_DTC = 0x04
private const val CONFIRMED_DTC = 0x08
private const val TEST_FAILED_SINCE_LAST_CLEAR = 0x20
private const val WARNING_INDICATOR_REQUESTED = 0x80

// One state per code, the most urgent bit wins. Declaration order is the display order.
enum class DtcState {
    // The test is failing right now.
    ACTIVE,

    // Confirmed and stored, not failing at the moment.
    STORED,

    // Failed in the current or last cycle, not (yet) confirmed.
    PENDING,

    // Failed at some point since the last clear, no longer pending or confirmed.
    HISTORY,

    // No fault bit set (e.g. only "test not completed"), or no status reported: no badge.
    UNKNOWN
}

data class DtcStatus(
    val state: DtcState,
    // The ECU asks for the warning lamp (MIL) - independent of the state.
    val warningLamp: Boolean
)

fun DiagnosticTroubleCode.dtcStatus(): DtcStatus = dtcStatus(statusMask)

fun dtcStatus(statusMask: Int): DtcStatus {
    val state =
        when {
            statusMask and TEST_FAILED != 0 -> DtcState.ACTIVE
            statusMask and CONFIRMED_DTC != 0 -> DtcState.STORED
            statusMask and PENDING_DTC != 0 -> DtcState.PENDING
            statusMask and (TEST_FAILED_THIS_OPERATION_CYCLE or TEST_FAILED_SINCE_LAST_CLEAR) != 0 -> DtcState.HISTORY
            else -> DtcState.UNKNOWN
        }
    return DtcStatus(state, warningLamp = statusMask and WARNING_INDICATOR_REQUESTED != 0)
}

// Localized labels for statusLine(): state names plus the lamp marker ("MIL on").
data class DtcStatusLabels(
    val active: String,
    val stored: String,
    val pending: String,
    val history: String,
    val warningLamp: String
)

// The state's label, null for UNKNOWN (no badge).
fun DtcStatus.stateLabel(labels: DtcStatusLabels): String? =
    when (state) {
        DtcState.ACTIVE -> labels.active
        DtcState.STORED -> labels.stored
        DtcState.PENDING -> labels.pending
        DtcState.HISTORY -> labels.history
        DtcState.UNKNOWN -> null
    }

// "Active · MIL on", "Stored", "MIL on" (lamp without a fault state), null when there is nothing to show.
fun DtcStatus.statusLine(labels: DtcStatusLabels): String? =
    listOfNotNull(stateLabel(labels), labels.warningLamp.takeIf { warningLamp })
        .takeIf { it.isNotEmpty() }
        ?.joinToString(" · ")
