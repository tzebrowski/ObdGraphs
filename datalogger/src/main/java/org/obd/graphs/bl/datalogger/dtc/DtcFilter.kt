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

// The phone DTC dialog's filter chips, in display order. ALL leaves the list as it is.
enum class DtcFilter(
    val state: DtcState?
) {
    ALL(null),
    ACTIVE(DtcState.ACTIVE),
    STORED(DtcState.STORED),
    PENDING(DtcState.PENDING)
}

fun DtcFilter.matches(dtc: DiagnosticTroubleCode): Boolean = state == null || dtc.dtcStatus().state == state

// Codes on the list per filter, for the chip labels. Counts the rows toDtcListItems built (one per module and code).
fun List<DtcListItem>.dtcFilterCounts(): Map<DtcFilter, Int> {
    val codes = filterIsInstance<DtcListItem.DtcRow>().filter { !it.isGone && it.dtc.standardCode.isNotEmpty() }.map { it.dtc }
    return DtcFilter.entries.associateWith { filter -> codes.count { filter.matches(it) } }
}

// Keeps the code rows the filter matches, under their module's header with the count of what is shown. Modules without
// a match are left out, clean and unanswered ones included: under a filter they hold nothing to look at. emptyMessage
// when nothing matches. Call before withModuleSummaries / collapsedBy / withModuleClearActions, and leave the gone
// section out under a filter (its codes have no current state).
fun List<DtcListItem>.filteredBy(
    filter: DtcFilter,
    emptyMessage: String
): List<DtcListItem> {
    if (filter == DtcFilter.ALL) return this

    val result = mutableListOf<DtcListItem>()
    var header: DtcListItem.ModuleHeader? = null
    var kept = mutableListOf<DtcListItem.DtcRow>()

    fun flush() {
        if (kept.isNotEmpty()) {
            header?.let { result.add(it.copy(codeCount = kept.size)) }
            result.addAll(kept)
        }
        kept = mutableListOf()
    }

    forEach { item ->
        when (item) {
            is DtcListItem.ModuleHeader -> {
                flush()
                header = item
            }
            is DtcListItem.DtcRow ->
                if (!item.isGone && item.dtc.standardCode.isNotEmpty() && filter.matches(item.dtc)) kept.add(item)
            is DtcListItem.Message, is DtcListItem.ModuleClear, is DtcListItem.GoneHeader -> Unit
        }
    }
    flush()
    return result.ifEmpty { listOf(DtcListItem.Message(key = "filter", text = emptyMessage)) }
}
