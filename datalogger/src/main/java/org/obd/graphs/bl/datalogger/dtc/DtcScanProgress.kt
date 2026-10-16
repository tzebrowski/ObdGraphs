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

import org.obd.graphs.commons.R
import org.obd.metrics.command.dtc.DtcProgress
import java.io.Serializable

// The DTC overlay context: progress updates only the overlay a DTC action put up.
const val DTC_SCREEN_LOCK_CONTEXT = "datalogger.dtc"

// Where the running DTC read / clear is, from ObdMetrics' DtcProgress. module null = per-code requests
// (freeze frames, extended data) for total codes, after every module was read.
data class DtcScanProgress(
    val module: String?,
    val index: Int,
    val total: Int
) : Serializable

fun DtcProgress.toScanProgress(): DtcScanProgress =
    if (phase == DtcProgress.Phase.FOLLOW_UPS) DtcScanProgress(null, 0, total) else DtcScanProgress(module, index, total)

// "Engine ECU · 4 / 13", or "Reading code details · 12" once the modules are done.
fun DtcScanProgress.text(getString: (Int) -> String): String =
    if (module == null) {
        getString(R.string.dtc_progress_follow_ups).format(total)
    } else {
        getString(R.string.dtc_progress_module).format(module, index, total)
    }
