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
}

// Only worth a section header once there's more than one distinct configured module in the
// result - keeps the default/no-multi-module-scan case visually identical to before this feature.
internal fun List<DiagnosticTroubleCode>.toDtcListItems(): List<DtcListItem> {
    val distinctModules =
        mapNotNull { it.module }
            .filter { it.isNotBlank() && it != DTC_DEFAULT_MODULE }
            .distinct()

    if (distinctModules.size < 2) {
        return map { DtcListItem.DtcRow(it) }
    }

    val items = mutableListOf<DtcListItem>()
    var lastModule: String? = null

    forEach { dtc ->
        val label = dtc.module?.takeIf { it.isNotBlank() } ?: "ECU"
        if (label != lastModule) {
            items.add(DtcListItem.ModuleHeader(label))
            lastModule = label
        }
        items.add(DtcListItem.DtcRow(dtc))
    }

    return items
}
