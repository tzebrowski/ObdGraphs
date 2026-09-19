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
package org.obd.graphs.preferences.dtc

import org.obd.metrics.api.model.DiagnosticTroubleCode

internal const val DTC_DEFAULT_MODULE = "ecu"

internal sealed class DtcListItem {
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
internal fun List<DiagnosticTroubleCode>.toDtcListItems(
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
