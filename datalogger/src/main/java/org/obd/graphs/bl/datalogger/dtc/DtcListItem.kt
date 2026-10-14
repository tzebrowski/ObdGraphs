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

import org.obd.graphs.DiagnosticMappingItem
import org.obd.metrics.api.model.DiagnosticTroubleCode
import org.obd.metrics.api.model.SnapshotPID

// Shared by the phone DTC dialog and the AA DTC screen - keep one copy of the grouping/ordering.

const val DTC_DEFAULT_MODULE = "ecu"

// Stores the *deselected* module keys so a module added later starts out checked.
const val PREF_DTC_DESELECTED_MODULES = "pref.dtc.module_picker.deselected"

sealed class DtcListItem {
    // codeCount: the module's code rows, 0 for a module scanned clean. summary / expanded come from
    // withModuleSummaries / collapsedBy. None of them is part of the row identity.
    data class ModuleHeader(
        val module: String,
        val codeCount: Int = 0,
        val summary: DtcModuleSummary = DtcModuleSummary(),
        val expanded: Boolean = true
    ) : DtcListItem()

    // isNew / isGone / history come from the vehicle's DTC history (DtcHistoryDiff); none of them is part
    // of the row identity. isGone: a row of the "no longer reported" section.
    data class DtcRow(
        val dtc: DiagnosticTroubleCode,
        val isNew: Boolean = false,
        val isGone: Boolean = false,
        val history: DtcCodeHistory? = null
    ) : DtcListItem()

    // Header of the "no longer reported" section: codes the previous scan of their module had and this one
    // has not (healed or cleared). Collapsed by default; expanded is part of the content, so a toggle rebinds it.
    data class GoneHeader(val codeCount: Int, val expanded: Boolean = false) : DtcListItem()

    // A plain informational line (no code) - the "no DTC found" empty state, or a scanned module
    // that reported nothing. Keyed so DiffUtil can tell per-module messages apart.
    data class Message(val key: String, val text: String) : DtcListItem()

    // Every subclass is a data class; declared here so DiffUtil's areContentsTheSame (and lint's
    // DiffUtilEquals check, which cannot see subclasses across modules) rely on value equality.
    abstract override fun equals(other: Any?): Boolean

    abstract override fun hashCode(): Int
}

private const val DTC_DEFAULT_MODULE_LABEL = "ECU"

// The default-ECU read tags codes "ecu", while some codecs leave module unset - both mean the
// same default ECU and must land in one group, not two headers that both render as "ECU".
private fun DiagnosticTroubleCode.moduleLabel(): String =
    module?.takeUnless { it.isBlank() || it.equals(DTC_DEFAULT_MODULE, ignoreCase = true) } ?: DTC_DEFAULT_MODULE_LABEL

// Identity of a list row across reloads (DiffUtil's areItemsTheSame, the expanded row). A code row
// includes its module: the same code reported by two modules is two rows. The raw hex is left out -
// it carries the status byte, and a status change is a content change of the same row.
fun DtcListItem.itemKey(): String =
    when (this) {
        is DtcListItem.ModuleHeader -> "header:$module"
        is DtcListItem.DtcRow -> "dtc:${dtc.moduleLabel()}:${dtc.displayCode()}"
        is DtcListItem.Message -> "message:$key"
        is DtcListItem.GoneHeader -> "gone-header"
    }

// What a module header shows about its codes, so a collapsed module still says what it holds:
// codes per state (no-state codes left out), how many request the MIL, how many are new.
data class DtcModuleSummary(
    val byState: Map<DtcState, Int> = emptyMap(),
    val milCount: Int = 0,
    val newCount: Int = 0
)

// Fills each module header's summary from the code rows below it (call after markedWith, for the new count).
fun List<DtcListItem>.withModuleSummaries(): List<DtcListItem> {
    val rowsByHeader = mutableMapOf<Int, MutableList<DtcListItem.DtcRow>>()
    var header = -1
    forEachIndexed { index, item ->
        when (item) {
            is DtcListItem.ModuleHeader -> header = index
            is DtcListItem.GoneHeader -> header = -1
            is DtcListItem.DtcRow ->
                if (header >= 0 && item.dtc.standardCode.isNotEmpty()) rowsByHeader.getOrPut(header) { mutableListOf() }.add(item)
            is DtcListItem.Message -> Unit
        }
    }
    return mapIndexed { index, item ->
        if (item is DtcListItem.ModuleHeader) {
            val rows = rowsByHeader[index].orEmpty()
            val statuses = rows.map { it.dtc.dtcStatus() }
            item.copy(
                summary =
                DtcModuleSummary(
                    byState = statuses.filter { it.state != DtcState.UNKNOWN }.groupingBy { it.state }.eachCount(),
                    milCount = statuses.count { it.warningLamp },
                    newCount = rows.count { it.isNew }
                )
            )
        } else {
            item
        }
    }
}

// Modules start expanded when they reported codes, collapsed when clean: a 13-module scan was mostly
// "no trouble codes reported" lines. A collapsed module keeps its header (count + summary) and drops its rows.
fun DtcListItem.ModuleHeader.isExpandedByDefault(): Boolean = codeCount > 0

// expanded: the user's choice per module name, null where the user has not toggled it.
fun List<DtcListItem>.collapsedBy(expanded: (String) -> Boolean?): List<DtcListItem> {
    var hide = false
    return mapNotNull { item ->
        when (item) {
            is DtcListItem.ModuleHeader -> {
                val isExpanded = expanded(item.module) ?: item.isExpandedByDefault()
                hide = !isExpanded
                item.copy(expanded = isExpanded)
            }
            // The gone section has its own toggle.
            is DtcListItem.GoneHeader -> {
                hide = false
                item
            }
            else -> item.takeUnless { hide }
        }
    }
}

// Marks the rows of the list on screen with what the vehicle's history says about them.
fun List<DtcListItem>.markedWith(diff: DtcHistoryDiff): List<DtcListItem> =
    map { item ->
        if (item is DtcListItem.DtcRow && item.dtc.standardCode.isNotEmpty()) {
            item.copy(isNew = diff.isNew(item.dtc), history = diff.history(item.dtc))
        } else {
            item
        }
    }

// Appends the "no longer reported" section (header + rows when expanded); nothing when no code has gone.
fun List<DtcListItem>.withGoneSection(
    diff: DtcHistoryDiff,
    expanded: Boolean
): List<DtcListItem> {
    if (diff.gone.isEmpty()) return this
    val header = DtcListItem.GoneHeader(diff.gone.size, expanded)
    val rows =
        if (expanded) {
            diff.gone.sortedForDisplay().map { DtcListItem.DtcRow(it, isGone = true, history = diff.history(it)) }
        } else {
            emptyList()
        }
    return this + header + rows
}

// The module a gone row came from, null for the default ECU: the gone section mixes modules.
fun DiagnosticTroubleCode.moduleNameOrNull(): String? = moduleLabel().takeUnless { it == DTC_DEFAULT_MODULE_LABEL }

// Always label results with a module header once a real (non-default) module is present, even if
// only one module ended up reporting codes this scan (eg. you picked 2 modules but only one had
// codes) - the user explicitly picked modules and wants to see which one each result came from.
// Modules that were scanned (scannedModules) but reported no codes still get a header with a
// "no codes" line, so they aren't silently missing from the list.
// Falls back to a flat list only for the plain default single-ECU case (no module scanning
// configured at all), keeping that visually identical to before this feature.
fun List<DiagnosticTroubleCode>.toDtcListItems(
    scannedModules: List<String>,
    noCodesMessage: String,
    noCodesForModuleMessage: String,
    unknownModuleLabel: String
): List<DtcListItem> {
    val realModules =
        (mapNotNull { it.module } + scannedModules)
            .filter { it.isNotBlank() && !it.equals(DTC_DEFAULT_MODULE, ignoreCase = true) }
            .distinct()

    if (realModules.isEmpty()) {
        return if (isEmpty()) {
            listOf(DtcListItem.Message(key = "", text = noCodesMessage))
        } else {
            map { DtcListItem.DtcRow(it) }
        }
    }

    // A scan targeting a single module can still return codes tagged with the default "ecu" (or
    // untagged) - they came back from that scan, so file them under the module the user picked
    // rather than a separate generic "ECU" group. With several modules scanned there's no way to
    // tell which one they belong to: "ECU" would read as a real module (and sit next to e.g.
    // "Engine ECU"), so they go under unknownModuleLabel instead.
    val distinctScannedModules = scannedModules.filter { it.isNotBlank() }.distinct()
    val singleScannedModule = distinctScannedModules.singleOrNull()

    fun DiagnosticTroubleCode.groupLabel(): String =
        moduleLabel().let { label ->
            when {
                label != DTC_DEFAULT_MODULE_LABEL -> label
                singleScannedModule != null -> singleScannedModule
                distinctScannedModules.size > 1 -> unknownModuleLabel
                else -> label
            }
        }

    // Within a group, the same code can arrive twice (once tagged "ecu", once untagged) - show it once.
    val codesByModule =
        groupBy { it.groupLabel() }
            .mapValues { (_, codes) -> codes.distinctBy { it.standardCode to it.failureType?.code } }
    // Modules reporting codes first - they are what the list is opened for - then the clean ones, each by name.
    val labels =
        (codesByModule.keys + scannedModules.filter { it.isNotBlank() })
            .distinct()
            .sortedWith(compareBy<String> { if (codesByModule[it].isNullOrEmpty()) 1 else 0 }.thenBy { it })

    return labels.flatMap { label ->
        val codes = codesByModule[label].orEmpty()
        listOf(DtcListItem.ModuleHeader(label, codes.size)) +
            if (codes.isEmpty()) {
                listOf(DtcListItem.Message(key = label, text = noCodesForModuleMessage))
            } else {
                codes.map { DtcListItem.DtcRow(it) }
            }
    }
}

// Module first, then by state (active, stored, pending, history, none), known descriptions before
// unknown ones, then code.
fun Collection<DiagnosticTroubleCode>.sortedForDisplay(): List<DiagnosticTroubleCode> =
    sortedWith(
        compareBy<DiagnosticTroubleCode> { it.module ?: "" }
            .thenBy { it.dtcStatus().state }
            .thenBy { if (it.isDescriptionUnknown()) 1 else 0 }
            .thenBy { it.standardCode }
    )

// "P0123-1A" when the code carries a failure type, the bare standard code otherwise.
fun DiagnosticTroubleCode.displayCode(): String =
    failureType?.code?.takeUnless { it.isEmpty() }?.let { "$standardCode-$it" } ?: standardCode

// Request keys to scan: the configured modules (with a header) the user has not deselected in the
// phone's module picker. Empty means the plain default-ECU read.
fun dtcScanModules(
    mappings: List<DiagnosticMappingItem>,
    deselected: Collection<String>
): Set<String> =
    mappings
        .filter { it.headerValue.isNotEmpty() && it.requestKey !in deselected }
        .map { it.requestKey }
        .toSet()

// Modules the automatic DTC read on connect scans: every configured module with a header - the
// picker's deselection applies to manual scans only. Empty means no modules are configured and
// ObdMetrics' own default-header read on connect stays in charge.
fun dtcConnectScanModules(mappings: List<DiagnosticMappingItem>): Set<String> = dtcScanModules(mappings, emptySet())

// How long the phone's "please wait" overlay waits for a DTC read / clear. ScreenLock's 10 s default is shorter than
// a real scan: 13 modules with freeze frames and extended data took ~13 s, and ObdMetrics alone may wait 23 s for
// slow follow-ups. The overlay dismissed with "Timeout waiting for response" while the scan went on and succeeded.
// Only a safety net for a result that never arrives (Cancel stays available); 0 modules = the default-ECU read.
private const val DTC_ACTION_BASE_TIMEOUT_MS = 30_000L
private const val DTC_ACTION_PER_MODULE_TIMEOUT_MS = 5_000L

fun dtcActionTimeoutMs(moduleCount: Int): Long =
    DTC_ACTION_BASE_TIMEOUT_MS + maxOf(1, moduleCount) * DTC_ACTION_PER_MODULE_TIMEOUT_MS

fun DiagnosticTroubleCode.isDescriptionUnknown(): Boolean =
    description.isNullOrBlank() || description.contains("Unknown DTC Description", ignoreCase = true)

// The decoded description, or the system/category/subsystem path when the codec has none.
fun DiagnosticTroubleCode.displayDescription(): String {
    if (!isDescriptionUnknown()) return description

    val fallbackParts =
        listOfNotNull(system?.description, category?.description, subsystem?.description)
            .filter { it.isNotBlank() }

    return if (fallbackParts.isNotEmpty()) {
        fallbackParts.joinToString(" → ") + " (Unknown specific fault)"
    } else {
        "Unknown DTC Description"
    }
}

// "Engine ECU (13)" for a module with codes, the bare name for a clean one.
fun DtcListItem.ModuleHeader.displayName(): String = if (codeCount > 0) "$module ($codeCount)" else module

// One block of the list: a module header (null for the flat default-ECU list) and its rows.
// codeCount follows the header's, so list UIs can style sections with codes apart from clean ones.
data class DtcSection(val header: String?, val items: List<DtcListItem>, val codeCount: Int = 0)

// Folds the flat header/row stream into sections, for list UIs that render headers natively
// (the AA ListTemplate's sectioned lists) instead of as rows.
fun List<DtcListItem>.toDtcSections(): List<DtcSection> {
    val sections = mutableListOf<DtcSection>()
    var header: String? = null
    var codeCount = 0
    var items = mutableListOf<DtcListItem>()

    fun flush() {
        if (header != null || items.isNotEmpty()) sections.add(DtcSection(header, items, codeCount))
    }

    forEach { item ->
        if (item is DtcListItem.ModuleHeader) {
            flush()
            header = item.module
            codeCount = item.codeCount
            items = mutableListOf()
        } else {
            items.add(item)
        }
    }
    flush()
    return sections
}

// The table entries a DTC read/clear actually targets: the selected ones that have a header, one per
// header. Two entries may name the same ECU (e.g. "Engine ECU" and "Engine ECU (2.0 GME)", both
// DA10F1); once codes carry their module, scanning both lists every code of that ECU twice.
// The first entry in table order names the section.
fun dtcScanTargets(
    mappings: List<DiagnosticMappingItem>,
    selected: Set<String>
): List<DiagnosticMappingItem> =
    mappings
        .filter { it.headerValue.isNotEmpty() && it.requestKey in selected }
        .distinctBy { it.headerValue.trim().uppercase() }

// "Engine Speed: 750 rpm" per decoded DID of a snapshot or of the extended data (DiagnosticTroubleCode.details).
// PID labels are written for dashboard tiles ("Engine Coolant\nTemp"), so their line breaks are flattened.
fun List<SnapshotPID>?.toDidLines(
    unknownDid: String,
    notAvailable: String
): List<String> =
    orEmpty().map { did ->
        val description =
            did.definition
                ?.description
                ?.replace(Regex("\\s*\n\\s*"), " ")
                ?.trim()
                ?.takeUnless { it.isEmpty() } ?: unknownDid
        val value = did.decodedValue?.toString() ?: notAvailable
        val units = did.definition?.units?.trim().orEmpty()
        listOf("$description:", value, units).filter { it.isNotEmpty() }.joinToString(" ")
    }
