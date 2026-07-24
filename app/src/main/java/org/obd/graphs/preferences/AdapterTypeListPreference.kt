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
package org.obd.graphs.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.app.AlertDialog
import androidx.preference.ListPreference
import org.obd.graphs.DiagnosticMappingItem
import org.obd.graphs.DiagnosticRequestIDManager
import org.obd.graphs.R
import org.obd.graphs.profile.profile

private const val ADAPTER_TYPE_VGATE_VLINKER_MS = "VGATE_VLINKER_MS"

private const val PREF_CONTINUE_ON_ERROR = "pref.adapter.error.continue_on_error"
private const val PREF_INIT_PROTOCOL = "pref.adapter.init.protocol"
private const val PREF_INIT_SEQUENCE = "pref.adapter.init.sequence"

private const val PREF_CONTINUE_ON_ERROR_FALLBACK = false
private const val PREF_INIT_PROTOCOL_FALLBACK = "AUTO"
private const val PREF_INIT_SEQUENCE_FALLBACK = "DEFAULT"

private const val TRW_CLIMATE_CONTROL_MODE_INDEX = 11
private const val TRW_CLIMATE_CONTROL_ID = "TRW_CLIMATE_CONTROL"
private const val TRW_CLIMATE_CONTROL_HEADER = "18DA98F1"

class AdapterTypeListPreference(
    context: Context,
    attrs: AttributeSet?
) : ListPreference(context, attrs) {
    init {
        setOnPreferenceChangeListener { _, newValue ->
            val previousValue = value

            if (newValue == ADAPTER_TYPE_VGATE_VLINKER_MS) {
                applyVLinkerMsSettings()
            } else if (previousValue == ADAPTER_TYPE_VGATE_VLINKER_MS) {
                revertVLinkerMsSettings()
            }
            true
        }
    }

    private fun applyVLinkerMsSettings() {
        Prefs.updateBoolean(PREF_CONTINUE_ON_ERROR, true)
        Prefs.updateString(PREF_INIT_PROTOCOL, "CAN_USER1")
        Prefs.updateString(PREF_INIT_SEQUENCE, "HS_CAN")

        DiagnosticRequestIDManager.saveMapping(
            DiagnosticMappingItem(
                modeIndex = TRW_CLIMATE_CONTROL_MODE_INDEX,
                requestKey = TRW_CLIMATE_CONTROL_ID,
                headerValue = TRW_CLIMATE_CONTROL_HEADER,
                description = "TRW Climate Control"
            )
        )

        AlertDialog
            .Builder(context)
            .setTitle(context.getString(R.string.pref_adapter_type_vlinker_ms_dialog_title))
            .setMessage(context.getString(R.string.pref_adapter_type_vlinker_ms_dialog_message))
            .setCancelable(true)
            .setPositiveButton(context.getString(android.R.string.ok), null)
            .show()
    }

    private fun revertVLinkerMsSettings() {
        Prefs.updateBoolean(PREF_CONTINUE_ON_ERROR, profile.getProfileValue(PREF_CONTINUE_ON_ERROR, PREF_CONTINUE_ON_ERROR_FALLBACK))
        Prefs.updateString(PREF_INIT_PROTOCOL, profile.getProfileValue(PREF_INIT_PROTOCOL, PREF_INIT_PROTOCOL_FALLBACK))
        Prefs.updateString(PREF_INIT_SEQUENCE, profile.getProfileValue(PREF_INIT_SEQUENCE, PREF_INIT_SEQUENCE_FALLBACK))

        DiagnosticRequestIDManager.getMappings()
            .firstOrNull { it.modeIndex == TRW_CLIMATE_CONTROL_MODE_INDEX }
            ?.takeIf { it.requestKey == TRW_CLIMATE_CONTROL_ID && it.headerValue == TRW_CLIMATE_CONTROL_HEADER }
            ?.let { DiagnosticRequestIDManager.deleteMapping(it) }
    }
}
