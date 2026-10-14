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

import android.util.Log
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.obd.graphs.getContext
import java.io.File

private const val LOG_TAG = "DtcHistory"
private const val DTC_HISTORY_FILE = "dtc_history.json"

// files/dtc_history.json: {vin -> [DtcScan]}. Not a pref: freeze frames make it tens of kB, and
// SharedPreferences are held in memory from startup. A missing or corrupt file is an empty history -
// it never blocks storing the current list (pref.datalogger.dtc).
object DtcHistoryStore {
    private val mapper =
        ObjectMapper().apply {
            registerModule(KotlinModule())
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }

    internal var file: () -> File? = { getContext()?.filesDir?.let { File(it, DTC_HISTORY_FILE) } }

    private var cache: Map<String, List<DtcScan>>? = null

    @Synchronized
    fun append(
        vin: String?,
        scan: DtcScan
    ) {
        try {
            val updated = load().append(vin, scan)
            cache = updated
            write(updated)
        } catch (e: Throwable) {
            Log.e(LOG_TAG, "Failed to store DTC history", e)
        }
    }

    @Synchronized
    fun scans(vin: String?): List<DtcScan> = load()[historyKey(vin)].orEmpty()

    // The markers for the list on screen: readInfo is the stored list's stamp. Nothing for a list without
    // a VIN reply (its codes may come from another car) or one that is not the vehicle's newest scan
    // (stored before history existed, or the history write failed).
    fun diff(readInfo: DtcReadInfo?): DtcHistoryDiff {
        if (readInfo == null || historyKey(readInfo.vin) == UNKNOWN_VEHICLE) return DtcHistoryDiff.EMPTY
        val scans = scans(readInfo.vin)
        if (scans.lastOrNull()?.timestamp != readInfo.timestamp) return DtcHistoryDiff.EMPTY
        return dtcHistoryDiff(scans)
    }

    @Synchronized
    internal fun reset() {
        cache = null
    }

    private fun load(): Map<String, List<DtcScan>> =
        cache ?: read().also { cache = it }

    private fun read(): Map<String, List<DtcScan>> =
        try {
            file()?.takeIf { it.exists() }?.let { mapper.readValue<Map<String, List<DtcScan>>>(it) } ?: emptyMap()
        } catch (e: Throwable) {
            Log.e(LOG_TAG, "DTC history is unreadable, starting over", e)
            emptyMap()
        }

    // Temp file + rename, so a crash mid-write leaves the previous history, not a truncated one.
    private fun write(history: Map<String, List<DtcScan>>) {
        val target = file() ?: return
        val temp = File(target.parentFile, "${target.name}.tmp")
        mapper.writeValue(temp, history)
        if (!temp.renameTo(target)) {
            target.delete()
            temp.renameTo(target)
        }
    }
}
