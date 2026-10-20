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

// ISO 14229-1 DTC status byte bits (statusMask). UDS modules send the real status byte. Codes of a module
// marked as OBD-II (services 03 / 07 / 0A) get the bit their service stands for from ObdMetrics: confirmed for 03,
// pending for 07, none for 0A (permanent is DiagnosticTroubleCode.isPermanent instead).
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
    val warningLamp: Boolean,
    // OBD-II service 0A: kept by the ECU until its own test passes again, no tool clears it. Independent of the state.
    val permanent: Boolean = false
)

fun DiagnosticTroubleCode.dtcStatus(): DtcStatus = dtcStatus(statusMask).copy(permanent = isPermanent)

// Reported only as permanent: what an accepted clear leaves behind. Not a fault the clear failed to remove.
fun DiagnosticTroubleCode.isPermanentOnly(): Boolean = dtcStatus().let { it.permanent && it.state == DtcState.UNKNOWN }

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

// Localized labels for statusLine(): state names plus the lamp marker ("MIL on") and "Permanent".
data class DtcStatusLabels(
    val active: String,
    val stored: String,
    val pending: String,
    val history: String,
    val warningLamp: String,
    val permanent: String
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

// "Active · MIL on", "Stored · Permanent", "MIL on" (lamp without a fault state), null when there is nothing to show.
fun DtcStatus.statusLine(labels: DtcStatusLabels): String? =
    listOfNotNull(stateLabel(labels), labels.warningLamp.takeIf { warningLamp }, labels.permanent.takeIf { permanent })
        .takeIf { it.isNotEmpty() }
        ?.joinToString(" · ")

// The AA row's icon (rows take no chips): the state's glyph, the permanent one for a code reported only as permanent,
// the plain fault triangle when there is no state. A state wins over permanent; the status line still says both.
enum class DtcRowIcon { ACTIVE, STORED, PENDING, HISTORY, PERMANENT, FAULT }

fun DtcStatus.rowIcon(): DtcRowIcon =
    when (state) {
        DtcState.ACTIVE -> DtcRowIcon.ACTIVE
        DtcState.STORED -> DtcRowIcon.STORED
        DtcState.PENDING -> DtcRowIcon.PENDING
        DtcState.HISTORY -> DtcRowIcon.HISTORY
        DtcState.UNKNOWN -> if (permanent) DtcRowIcon.PERMANENT else DtcRowIcon.FAULT
    }
