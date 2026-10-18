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

// Stands for "the vehicle answered the VIN request with an empty VIN" (an unprogrammed ECM sends 17
// spaces), as opposed to null = no VIN reply at all. Not a valid VIN: it never names a vehicle.
const val NO_VIN = "00000000000000000"

private fun String?.isKnownVin() = this != null && this != NO_VIN

// Only a known VIN on both sides counts - without one, "another vehicle" cannot be told apart
// from "VIN not reported" or "VIN not programmed".
fun DtcReadInfo.isFromOtherVehicle(currentVin: String?): Boolean =
    vin.isKnownVin() && currentVin.isKnownVin() && !vin.equals(currentVin, ignoreCase = true)

// Localized format strings for summary(): "Last read: %1$s", "VIN %1$s", "another vehicle (VIN %1$s)",
// and the plain text shown for NO_VIN ("VIN not programmed").
// The parts are joined in code - string resources lose a leading space.
data class DtcReadInfoLabels(
    val lastRead: String,
    val vin: String,
    val otherVehicle: String,
    val noVin: String
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
        vin == NO_VIN -> labels.noVin
        isFromOtherVehicle(currentVin) -> labels.otherVehicle.format(vin)
        else -> labels.vin.format(vin)
    }

// ISO 3779: 17 characters, no I, O or Q.
private val VIN_PATTERN = Regex("[A-HJ-NPR-Z0-9]{17}")

// Vehicle metadata is keyed by the metadata PID's label, which differs per profile and is translated
// ("VIN", "Vehicle Identification Number", "Numer identyfikacyjny pojazdu", ...). vinLabels are the labels of the
// profile's VIN PIDs (known from the registry): their entries win (a value may carry padding), then a key mentioning
// VIN, then any value that is exactly a VIN - letters and digits mixed, so a numeric part number never qualifies.
// When a VIN PID answered but carries no VIN (blank or zero filled), the result is NO_VIN instead of null.
fun vinFrom(
    metadata: Map<String, String>,
    vinLabels: Set<String> = emptySet()
): String? =
    metadata.entries
        .filter { it.key in vinLabels || it.key.contains("vin", ignoreCase = true) }
        .sortedByDescending { it.key in vinLabels }
        .firstNotNullOfOrNull { vinIn(it.value) }
        ?: metadata.values
            .map { it.trim().uppercase() }
            .firstOrNull { VIN_PATTERN.matches(it) && it.any(Char::isLetter) && it.any(Char::isDigit) }
        ?: NO_VIN.takeIf { vinLabels.any { label -> metadata[label]?.isEmptyVin() == true } }

// The VIN ObdMetrics read at the start of a DTC action: null when no VIN PID answered, NO_VIN for an ECU without a
// programmed VIN (blank or zero filled), null as well for a reply that holds no VIN.
fun vinFromReply(reply: String?): String? =
    when {
        reply == null -> null
        reply.isEmptyVin() -> NO_VIN
        else -> vinIn(reply)
    }

// A VIN inside a value that may carry padding; NO_VIN does not count.
private fun vinIn(value: String): String? = VIN_PATTERN.find(value.uppercase())?.value?.takeIf { it != NO_VIN }

// What an ECU without a programmed VIN sends: spaces (trimmed to nothing by the decoder), zeros or 0xFF.
private fun String.isEmptyVin(): Boolean = all { it.isWhitespace() || it == '0' || it == '\u0000' || it == '\u00FF' }

// What a clear achieved, judged from the clear reply and the read that follows it. The read is the
// real verdict: an ECU can acknowledge the clear while an active fault sets the code again at once.
enum class DtcClearOutcome {
    CLEARED,

    // Cleared; what the read still reports are permanent codes only (OBD-II service 0A), which no clear removes.
    PERMANENT_REMAIN,
    REFUSED,
    CODES_REMAIN
}

fun dtcClearOutcome(
    status: DiagnosticTroubleCodeClearStatus?,
    codesAfterClear: Collection<DiagnosticTroubleCode>
): DtcClearOutcome =
    when {
        status == DiagnosticTroubleCodeClearStatus.ERR -> DtcClearOutcome.REFUSED
        codesAfterClear.any { it.standardCode.isNotEmpty() && !it.isPermanentOnly() } -> DtcClearOutcome.CODES_REMAIN
        codesAfterClear.any { it.standardCode.isNotEmpty() } -> DtcClearOutcome.PERMANENT_REMAIN
        else -> DtcClearOutcome.CLEARED
    }

// How many codes a clear outcome message counts: permanent-only ones are not "still reported" faults.
fun Collection<DiagnosticTroubleCode>.remainingAfterClear(permanent: Boolean): Int =
    count { it.standardCode.isNotEmpty() && it.isPermanentOnly() == permanent }
