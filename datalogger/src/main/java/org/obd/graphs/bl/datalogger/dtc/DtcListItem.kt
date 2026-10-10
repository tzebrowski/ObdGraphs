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

// Shared by the phone DTC dialog and the AA DTC screen - keep one copy of the grouping/ordering.

const val DTC_DEFAULT_MODULE = "ecu"

// Stores the *deselected* module keys so a module added later starts out checked.
const val PREF_DTC_DESELECTED_MODULES = "pref.dtc.module_picker.deselected"

sealed class DtcListItem {
    data class ModuleHeader(val module: String) : DtcListItem()
    data class DtcRow(val dtc: DiagnosticTroubleCode) : DtcListItem()

    // A plain informational line (no code) - the "no DTC found" empty state, or a scanned module
    // that reported nothing. Keyed so DiffUtil can tell per-module messages apart.
    data class Message(val key: String, val text: String) : DtcListItem()
}

private const val DTC_DEFAULT_MODULE_LABEL = "ECU"

// The default-ECU read tags codes "ecu", while some codecs leave module unset - both mean the
// same default ECU and must land in one group, not two headers that both render as "ECU".
private fun DiagnosticTroubleCode.moduleLabel(): String =
    module?.takeUnless { it.isBlank() || it.equals(DTC_DEFAULT_MODULE, ignoreCase = true) } ?: DTC_DEFAULT_MODULE_LABEL

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
    noCodesForModuleMessage: String
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
    // tell which one they belong to, so they keep the default label.
    val singleScannedModule = scannedModules.filter { it.isNotBlank() }.distinct().singleOrNull()

    fun DiagnosticTroubleCode.groupLabel(): String =
        moduleLabel().let { label ->
            if (label == DTC_DEFAULT_MODULE_LABEL && singleScannedModule != null) singleScannedModule else label
        }

    // Within a group, the same code can arrive twice (once tagged "ecu", once untagged) - show it once.
    val codesByModule =
        groupBy { it.groupLabel() }
            .mapValues { (_, codes) -> codes.distinctBy { it.standardCode to it.failureType?.code } }
    val labels = (codesByModule.keys + scannedModules.filter { it.isNotBlank() }).distinct().sorted()

    return labels.flatMap { label ->
        val codes = codesByModule[label].orEmpty()
        listOf(DtcListItem.ModuleHeader(label)) +
            if (codes.isEmpty()) {
                listOf(DtcListItem.Message(key = label, text = noCodesForModuleMessage))
            } else {
                codes.map { DtcListItem.DtcRow(it) }
            }
    }
}

// Module first, then known descriptions before unknown ones, then code.
fun Collection<DiagnosticTroubleCode>.sortedForDisplay(): List<DiagnosticTroubleCode> =
    sortedWith(
        compareBy<DiagnosticTroubleCode> { it.module ?: "" }
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

// One block of the list: a module header (null for the flat default-ECU list) and its rows.
data class DtcSection(val header: String?, val items: List<DtcListItem>)

// Folds the flat header/row stream into sections, for list UIs that render headers natively
// (the AA ListTemplate's sectioned lists) instead of as rows.
fun List<DtcListItem>.toDtcSections(): List<DtcSection> {
    val sections = mutableListOf<DtcSection>()
    var header: String? = null
    var items = mutableListOf<DtcListItem>()

    fun flush() {
        if (header != null || items.isNotEmpty()) sections.add(DtcSection(header, items))
    }

    forEach { item ->
        if (item is DtcListItem.ModuleHeader) {
            flush()
            header = item.module
            items = mutableListOf()
        } else {
            items.add(item)
        }
    }
    flush()
    return sections
}
