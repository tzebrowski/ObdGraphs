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
import org.obd.metrics.command.dtc.DtcModuleStatus

// Per-vehicle DTC history: every stored DTC list is also kept as a scan record, keyed by VIN.
// Pure logic here; the file lives in DtcHistoryStore. Spec: doc/specs/dtc-history.md.

// History key for reads without a VIN reply (upper case, as historyKey() makes every key). Its scans get no
// new / gone markers: they may come from any car.
const val UNKNOWN_VEHICLE = "UNKNOWN"

const val DTC_HISTORY_MAX_SCANS = 30
const val DTC_HISTORY_MAX_VEHICLES = 5

enum class DtcScanKind {
    READ,

    // The read that follows a clear.
    CLEAR,

    // The automatic read on connect.
    CONNECT
}

// scannedModules: module labels the scan targeted (DiagnosticTroubleCode.module); empty = default single-ECU read.
// moduleStatus: how each targeted module answered; empty for scans recorded before it existed (= all answered).
data class DtcScan(
    val timestamp: Long = 0,
    val kind: DtcScanKind = DtcScanKind.READ,
    val scannedModules: List<String> = emptyList(),
    val codes: List<DiagnosticTroubleCode> = emptyList(),
    val moduleStatus: Map<String, DtcModuleStatus> = emptyMap()
) {
    // A module that did not answer (or refused) was targeted but not looked at.
    fun answered(module: String): Boolean =
        moduleStatus.entries.firstOrNull { it.key.equals(module, ignoreCase = true) }?.value.let {
            it == null || it == DtcModuleStatus.RESPONDED
        }
}

fun historyKey(vin: String?): String = vin?.uppercase() ?: UNKNOWN_VEHICLE

// The code's identity in history: the same key the DTC lists use (module + display code).
fun DiagnosticTroubleCode.historyKey(): String = DtcListItem.DtcRow(this).itemKey()

// Same codes in the same states from the same modules.
private fun DtcScan.signature() = scannedModules.toSet() to codes.map { it.historyKey() to it.statusMask }.toSet()

// Appends scan under vin. A connect scan equal to the previous connect scan replaces it: otherwise every
// ignition-on fills the cap with identical records. The oldest scans go first past DTC_HISTORY_MAX_SCANS,
// the least recently scanned vehicle past DTC_HISTORY_MAX_VEHICLES - never the newest records, so the
// scan before a clear (with its freeze frames) survives the clear.
fun Map<String, List<DtcScan>>.append(
    vin: String?,
    scan: DtcScan
): Map<String, List<DtcScan>> {
    val key = historyKey(vin)
    val scans = this[key].orEmpty()
    val previous = scans.lastOrNull()
    val replace =
        scan.kind == DtcScanKind.CONNECT && previous?.kind == DtcScanKind.CONNECT && previous.signature() == scan.signature()
    val updated = ((if (replace) scans.dropLast(1) else scans) + scan).takeLast(DTC_HISTORY_MAX_SCANS)
    return (this - key + (key to updated))
        .entries
        .sortedByDescending { (_, vehicleScans) -> vehicleScans.lastOrNull()?.timestamp ?: 0 }
        .take(DTC_HISTORY_MAX_VEHICLES)
        .associate { it.key to it.value }
}

// Whether this scan looked at the module the code comes from. A module scan covers the modules it targeted and
// that answered (a silent module's codes are unknown, not gone);
// a code with no module of its own (the default ECU, or untagged) is only comparable with scans of the same
// targets as the scan that reported it - a default single-ECU read is compared only with other default reads.
internal fun DtcScan.covers(
    code: DiagnosticTroubleCode,
    reportedBy: DtcScan
): Boolean {
    val module = code.module?.takeUnless { it.isBlank() || it.equals(DTC_DEFAULT_MODULE, ignoreCase = true) }
    return if (module == null) {
        scannedModules.toSet() == reportedBy.scannedModules.toSet() && scannedModules.all { answered(it) }
    } else {
        scannedModules.any { it.equals(module, ignoreCase = true) } && answered(module)
    }
}
