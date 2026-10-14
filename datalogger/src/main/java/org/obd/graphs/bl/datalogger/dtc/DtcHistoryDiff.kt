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

// One code's record across a vehicle's scans. coveredIn: scans that looked at its module.
data class DtcCodeHistory(
    val firstSeen: Long,
    val lastSeen: Long,
    val seenIn: Int,
    val coveredIn: Int
)

// What the newest scan of a vehicle changed. Keys are DiagnosticTroubleCode.historyKey().
// newCodes: in the newest scan, absent from every earlier scan that looked at their module - not merely
// absent from the previous one, or an intermittent code would be "new" every other drive.
// gone: codes present the last time their module was scanned before, absent now although the newest scan
// looked at their module. A code from a module left out of this scan has not gone, it was not looked at.
data class DtcHistoryDiff(
    val newCodes: Set<String> = emptySet(),
    val gone: List<DiagnosticTroubleCode> = emptyList(),
    val codes: Map<String, DtcCodeHistory> = emptyMap()
) {
    fun isNew(code: DiagnosticTroubleCode) = code.historyKey() in newCodes

    fun history(code: DiagnosticTroubleCode) = codes[code.historyKey()]

    companion object {
        val EMPTY = DtcHistoryDiff()
    }
}

// scans: one vehicle's history, oldest first; the last one is the scan on screen.
fun dtcHistoryDiff(scans: List<DtcScan>): DtcHistoryDiff {
    val current = scans.lastOrNull() ?: return DtcHistoryDiff.EMPTY
    val earlier = scans.dropLast(1)

    // Latest occurrence of every code, with the scan that reported it.
    val occurrences = linkedMapOf<String, Pair<DiagnosticTroubleCode, DtcScan>>()
    scans.forEach { scan -> scan.codes.forEach { occurrences[it.historyKey()] = it to scan } }

    val currentKeys = current.codes.map { it.historyKey() }.toSet()

    val newCodes =
        current.codes
            .filter { code ->
                val covering = earlier.filter { it.covers(code, current) }
                covering.isNotEmpty() && covering.none { scan -> scan.codes.any { it.historyKey() == code.historyKey() } }
            }.map { it.historyKey() }
            .toSet()

    val gone =
        occurrences
            .filterKeys { it !in currentKeys }
            .values
            .filter { (code, reportedBy) ->
                current.covers(code, reportedBy) &&
                    earlier.lastOrNull { it.covers(code, reportedBy) }?.let { last ->
                        last.codes.any { it.historyKey() == code.historyKey() }
                    } == true
            }.map { it.first }

    val codes =
        occurrences.mapValues { (key, occurrence) ->
            val (code, reportedBy) = occurrence
            // A scan that reported the code looked at it, whatever its module tag says.
            val seen = scans.filter { scan -> scan.codes.any { it.historyKey() == key } }
            val covering = scans.filter { it in seen || it.covers(code, reportedBy) }
            DtcCodeHistory(
                firstSeen = seen.first().timestamp,
                lastSeen = seen.last().timestamp,
                seenIn = seen.size,
                coveredIn = covering.size
            )
        }

    return DtcHistoryDiff(newCodes = newCodes, gone = gone, codes = codes)
}
