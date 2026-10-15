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

// The DTC list as stored: codes, the modules the read targeted, how each answered.
data class DtcStoredList(
    val codes: List<DiagnosticTroubleCode>,
    val scannedModules: List<String>,
    val moduleStatus: Map<String, DtcModuleStatus> = emptyMap()
)

// A read or clear of some modules updates those modules only: the other modules' codes stay from the previous list.
// Without it, clearing one module left a list of that module alone, as if the others had no codes.
// Only a user-started READ / CLEAR of named modules, over a module list of the same vehicle (known VIN). A connect
// scan covers every configured module and replaces the list, so a module removed from the table does not linger.
fun DtcStoredList.mergedOver(
    previous: DtcStoredList?,
    kind: DtcScanKind,
    sameVehicle: Boolean
): DtcStoredList {
    if (previous == null || !sameVehicle || kind == DtcScanKind.CONNECT) return this
    if (scannedModules.isEmpty() || previous.scannedModules.isEmpty()) return this
    val kept = previous.scannedModules.filter { it !in scannedModules }.toSet()
    if (kept.isEmpty()) return this
    return DtcStoredList(
        codes = previous.codes.filter { it.module in kept } + codes,
        scannedModules = previous.scannedModules.filter { it in kept } + scannedModules,
        moduleStatus = previous.moduleStatus.filterKeys { it in kept } + moduleStatus
    )
}

// Both reads stamped with the same known vehicle; UNKNOWN never matches, its codes may be another car's.
fun isSameVehicle(
    previous: DtcReadInfo?,
    vin: String?
): Boolean = previous != null && historyKey(vin) != UNKNOWN_VEHICLE && historyKey(previous.vin) == historyKey(vin)
