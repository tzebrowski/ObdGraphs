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

import org.obd.metrics.api.model.Init

// A DTC action the app asked ObdMetrics for: what it is (READ, CLEAR, CONNECT) and the modules it targets
// (empty = the default single-ECU read).
data class DtcRequest(
    val kind: DtcScanKind,
    val modules: List<Init.Header> = emptyList()
) {
    // The labels the codes are tagged with (DiagnosticTroubleCode.module), stored as the scanned modules.
    val moduleLabels: List<String> get() = modules.map { it.mode }
}

// What a completion belonged to, and the request to start next (if one waited).
data class DtcCompletion(
    val finished: DtcRequest?,
    val next: DtcRequest?
)

// One DTC action at a time. ObdMetrics' scheduleDTCAction clears its command buffer, so a second action
// scheduled while one runs cuts the first short, and both completions arrive for the app to tell apart.
// Seen on a car: Clear pressed during the connect scan - the scan's partial result was taken for the clear,
// and the clear's read was stored with no modules (the list collapsed into one "ECU" group).
// So a request made while another is in flight waits, and starts when the running one completes.
// Only the newest waiting request is kept, except that a read never replaces a waiting clear (a clear ends
// with a read anyway). An in-flight action older than its timeout counts as lost (e.g. the link dropped
// without a completion) and no longer blocks.
class DtcActionQueue(
    private val now: () -> Long = System::currentTimeMillis
) {
    private var inFlight: DtcRequest? = null
    private var inFlightSince = 0L
    private var waiting: DtcRequest? = null

    // True when the request should be sent to ObdMetrics now; false when it waits for the running one.
    @Synchronized
    fun submit(request: DtcRequest): Boolean {
        val running = inFlight
        if (running == null || now() - inFlightSince > dtcActionTimeoutMs(running.modules.size)) {
            start(request)
            return true
        }
        if (waiting?.kind != DtcScanKind.CLEAR || request.kind == DtcScanKind.CLEAR) waiting = request
        return false
    }

    // Called on every completion: the finished request (null for a completion nobody asked for), and the
    // waiting one, which is now in flight and must be sent.
    @Synchronized
    fun complete(): DtcCompletion {
        val finished = inFlight
        val next = waiting
        waiting = null
        inFlight = null
        next?.let { start(it) }
        return DtcCompletion(finished, next)
    }

    // A new or closed connection: nothing in flight any more.
    @Synchronized
    fun reset() {
        inFlight = null
        waiting = null
    }

    private fun start(request: DtcRequest) {
        inFlight = request
        inFlightSince = now()
    }
}
