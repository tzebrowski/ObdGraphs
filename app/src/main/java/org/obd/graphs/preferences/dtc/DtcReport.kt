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

import java.util.Locale

// Localized format strings for the shared report's header: "Scan duration: %1$.1f s", "Modules without an answer: %1$s".
internal data class DtcReportHeaderLabels(
    val duration: String,
    val noResponse: String
)

// The lines above the codes in the shared report: when and from which vehicle the list was read (the dialog's
// read info line), how long the last DTC action took, and the modules that did not answer. A part that is
// unknown (list stored before it was recorded) is left out rather than shown as N/A.
internal fun dtcReportHeader(
    readInfo: String?,
    durationMs: Long?,
    unansweredModules: List<String>,
    labels: DtcReportHeaderLabels,
    locale: Locale = Locale.getDefault()
): List<String> =
    listOfNotNull(
        readInfo,
        durationMs?.let { String.format(locale, labels.duration, it / 1000.0) },
        unansweredModules.takeIf { it.isNotEmpty() }?.let { labels.noResponse.format(it.joinToString(", ")) }
    )
