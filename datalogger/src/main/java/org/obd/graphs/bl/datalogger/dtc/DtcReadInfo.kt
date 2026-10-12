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
import org.obd.metrics.command.dtc.DiagnosticTroubleCodeClearStatus

// When the stored codes were read, and from which vehicle. Stored next to the codes, so both the
// phone dialog and the AA screen can tell the driver how old the list is.
data class DtcReadInfo(
    val timestamp: Long = 0,
    val vin: String? = null
)

// Only a known VIN on both sides counts - without one, "another vehicle" cannot be told apart
// from "VIN not reported".
fun DtcReadInfo.isFromOtherVehicle(currentVin: String?): Boolean =
    vin != null && currentVin != null && !vin.equals(currentVin, ignoreCase = true)

// Localized format strings for summary(): "Last read: %1$s", "VIN %1$s", "another vehicle (VIN %1$s)".
// The parts are joined in code - string resources lose a leading space.
data class DtcReadInfoLabels(
    val lastRead: String,
    val vin: String,
    val otherVehicle: String
)

// One line for the top of the DTC list. formattedTime comes from the UI (locale-aware formatting).
fun DtcReadInfo.summary(
    formattedTime: String,
    currentVin: String?,
    labels: DtcReadInfoLabels
): String =
    listOfNotNull(labels.lastRead.format(formattedTime), vehicleLine(currentVin, labels)).joinToString(" · ")

// The vehicle part alone (AA puts it on the row's second line), null when no VIN was recorded.
fun DtcReadInfo.vehicleLine(
    currentVin: String?,
    labels: DtcReadInfoLabels
): String? =
    when {
        vin == null -> null
        isFromOtherVehicle(currentVin) -> labels.otherVehicle.format(vin)
        else -> labels.vin.format(vin)
    }

// ISO 3779: 17 characters, no I, O or Q.
private val VIN_PATTERN = Regex("[A-HJ-NPR-Z0-9]{17}")

// Vehicle metadata is keyed by the metadata PID's label, which differs per profile and is translated
// ("VIN", "Vehicle Identification Number", "Numer identyfikacyjny pojazdu", ...), so the label alone
// cannot find it. A key mentioning VIN wins (its value may carry padding); otherwise any value that
// is exactly a VIN - letters and digits mixed, so a numeric part number never qualifies.
fun vinFrom(metadata: Map<String, String>): String? =
    metadata.entries
        .filter { it.key.contains("vin", ignoreCase = true) }
        .firstNotNullOfOrNull { VIN_PATTERN.find(it.value.uppercase())?.value }
        ?: metadata.values
            .map { it.trim().uppercase() }
            .firstOrNull { VIN_PATTERN.matches(it) && it.any(Char::isLetter) && it.any(Char::isDigit) }

// What a clear achieved, judged from the clear reply and the read that follows it. The read is the
// real verdict: an ECU can acknowledge the clear while an active fault sets the code again at once.
enum class DtcClearOutcome {
    CLEARED,
    REFUSED,
    CODES_REMAIN
}

fun dtcClearOutcome(
    status: DiagnosticTroubleCodeClearStatus?,
    codesAfterClear: Collection<DiagnosticTroubleCode>
): DtcClearOutcome =
    when {
        status == DiagnosticTroubleCodeClearStatus.ERR -> DtcClearOutcome.REFUSED
        codesAfterClear.any { it.standardCode.isNotEmpty() } -> DtcClearOutcome.CODES_REMAIN
        else -> DtcClearOutcome.CLEARED
    }
