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
package org.obd.graphs.preferences.dri

import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.updateString

data class DiagnosticMappingItem(
    val modeIndex: Int,
    val requestKey: String,
    val headerValue: String
)

object DiagnosticRequestIDManager {
    private const val PREFIX_ID = "pref.adapter.init.mode.id_value.mode_"
    private const val PREFIX_HEADER = "pref.adapter.init.mode.header_value.mode_"

    private const val ACTIVE_ID_KEY = "pref.adapter.init.mode.id"
    private const val ACTIVE_HEADER_KEY = "pref.adapter.init.mode.header"

    fun getMappings(): List<DiagnosticMappingItem> {
        val mappings = mutableListOf<DiagnosticMappingItem>()
        for (i in 1..100) {
            val key = Prefs.getString("$PREFIX_ID$i", null)
            val value = Prefs.getString("$PREFIX_HEADER$i", null)

            if (key != null && value != null) {
                val cleanKey = key.replace("\"", "")
                val cleanValue = value.replace("\"", "")
                mappings.add(DiagnosticMappingItem(i, cleanKey, cleanValue))
            }
        }
        return mappings
    }

    fun saveMapping(item: DiagnosticMappingItem) {
        val activeId = Prefs.getString(ACTIVE_ID_KEY, "")?.replace("\"", "")

        Prefs.updateString("$PREFIX_ID${item.modeIndex}", item.requestKey)
        Prefs.updateString("$PREFIX_HEADER${item.modeIndex}", item.headerValue)

        if (activeId == item.requestKey) {
            Prefs.updateString(ACTIVE_HEADER_KEY, item.headerValue)
        }
    }

    fun addMapping(requestKey: String, headerValue: String) {
        val existingIndices = getMappings().map { it.modeIndex }
        val nextIndex = if (existingIndices.isEmpty()) 1 else existingIndices.maxOrNull()!! + 1

        val newItem = DiagnosticMappingItem(nextIndex, requestKey, headerValue)
        saveMapping(newItem)
    }

    fun deleteMapping(item: DiagnosticMappingItem) {
        Prefs.edit()
            .remove("$PREFIX_ID${item.modeIndex}")
            .remove("$PREFIX_HEADER${item.modeIndex}")
            .apply()
    }
}
