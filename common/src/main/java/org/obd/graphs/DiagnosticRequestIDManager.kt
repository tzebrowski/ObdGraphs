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
package org.obd.graphs

import android.util.Log
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.updateString

const val DRI_LOG_KEY = "DRI"
private const val DRI_MAX_MAPPINGS_ALLOWED = 100

data class DiagnosticMappingItem(
    val modeIndex: Int,
    val requestKey: String,
    val headerValue: String,
    // Friendly label shown everywhere in the UI. requestKey remains the internal lookup ID
    // (matched against a PID's mode / used as the DTC module key) and is never itself displayed.
    val description: String = ""
) {
    val displayName: String
        get() = description.ifBlank { requestKey }
}

object DiagnosticRequestIDManager {
    private const val PREFIX_ID = "pref.adapter.init.mode.id_value.mode_"
    private const val PREFIX_HEADER = "pref.adapter.init.mode.header_value.mode_"
    private const val PREFIX_DESCRIPTION = "pref.adapter.init.mode.description_value.mode_"

    private const val ACTIVE_ID_KEY = "pref.adapter.init.mode.id"
    private const val ACTIVE_HEADER_KEY = "pref.adapter.init.mode.header"

    private val values = mutableSetOf<String>()

    fun getValuePreferenceName() = ACTIVE_HEADER_KEY

    fun getMappings(): List<DiagnosticMappingItem> {
        val mappings = mutableListOf<DiagnosticMappingItem>()
        for (i in 1..DRI_MAX_MAPPINGS_ALLOWED) {
            val key = Prefs.getString("$PREFIX_ID$i", null)
            val value = Prefs.getString("$PREFIX_HEADER$i", null)
            val description = Prefs.getString("$PREFIX_DESCRIPTION$i", null)

            if (key != null && value != null) {
                val cleanKey = key.replace("\"", "")
                val cleanValue = value.replace("\"", "")
                val cleanDescription = description?.replace("\"", "") ?: ""
                mappings.add(DiagnosticMappingItem(i, cleanKey, cleanValue, cleanDescription))
            }
        }
        return mappings
    }

    fun getMapping(): Map<String, String> {
        val mapping = getMappings().associate { it.requestKey to it.headerValue }.filter { it.value.isNotEmpty() }
        Log.i(DRI_LOG_KEY, "Available mapping: $mapping")
        return mapping
    }

    fun saveMapping(item: DiagnosticMappingItem) {
        val activeId = Prefs.getString(ACTIVE_ID_KEY, "")?.replace("\"", "")

        Prefs.updateString("$PREFIX_ID${item.modeIndex}", item.requestKey)
        Prefs.updateString("$PREFIX_HEADER${item.modeIndex}", item.headerValue)
        Prefs.updateString("$PREFIX_DESCRIPTION${item.modeIndex}", item.description)

        if (activeId == item.requestKey) {
            Prefs.updateString(ACTIVE_HEADER_KEY, item.headerValue)
        }
    }

    fun addMapping(requestKey: String, headerValue: String, description: String = "") {
        val existingIndices = getMappings().map { it.modeIndex }
        val nextIndex = if (existingIndices.isEmpty()) 1 else existingIndices.maxOrNull()!! + 1

        val newItem = DiagnosticMappingItem(nextIndex, requestKey, headerValue, description)
        saveMapping(newItem)
    }

    fun deleteMapping(item: DiagnosticMappingItem) {
        Prefs.edit()
            .remove("$PREFIX_ID${item.modeIndex}")
            .remove("$PREFIX_HEADER${item.modeIndex}")
            .remove("$PREFIX_DESCRIPTION${item.modeIndex}")
            .apply()
    }

    fun reset() {
        Prefs.edit().let { editor ->
            for (i in 1..DRI_MAX_MAPPINGS_ALLOWED) {
                editor.putString("$PREFIX_HEADER$i", "")
                editor.putString("$PREFIX_ID$i", "")
                editor.putString("$PREFIX_DESCRIPTION$i", "")
            }
            editor.putString(ACTIVE_HEADER_KEY, "")
            editor.putString(ACTIVE_ID_KEY, "")
            editor.apply()
        }
        values.clear()
    }

    fun updateSettings(preferences: MutableMap<String, Any?>) {
        val canIDS = preferences.filter { entry -> entry.key.contains(ACTIVE_HEADER_KEY) }
            .values.map { it.toString() }.toSet()
        Log.i(DRI_LOG_KEY, "Registered following CAN IDS: $canIDS")
        values.addAll(canIDS)
    }
}
