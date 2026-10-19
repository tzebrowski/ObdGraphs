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

import android.content.Context
import android.content.SharedPreferences
import android.text.format.DateUtils
import android.util.AttributeSet
import androidx.preference.DialogPreference
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.PREF_DTC_READ_INFO
import org.obd.graphs.bl.datalogger.VehicleCapabilitiesManager
import org.obd.graphs.bl.datalogger.dtc.DtcReadInfo
import org.obd.graphs.bl.datalogger.dtc.DtcReadInfoLabels
import org.obd.graphs.bl.datalogger.dtc.summary
import org.obd.graphs.preferences.Prefs

// The row opening the DTC list. Its summary tells what the list holds (code count, when and from which vehicle it was
// read) and follows every read: a scan on connect stores the list while this screen is open.
internal class DiagnosticTroubleCodeListPreferences(
    context: Context,
    attrs: AttributeSet?
) : DialogPreference(context, attrs) {
    // SharedPreferences keeps listeners weakly: a field, not a lambda passed inline.
    private val readInfoListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PREF_DTC_READ_INFO) refreshSummary()
        }

    override fun onAttached() {
        super.onAttached()
        Prefs.registerOnSharedPreferenceChangeListener(readInfoListener)
        refreshSummary()
    }

    override fun onDetached() {
        Prefs.unregisterOnSharedPreferenceChangeListener(readInfoListener)
        super.onDetached()
    }

    private fun refreshSummary() {
        val info = VehicleCapabilitiesManager.getDtcReadInfo()
        summary =
            dtcListSummary(
                codeCount = VehicleCapabilitiesManager.getDiagnosticTroubleCodes().size,
                readInfo = info?.let { readInfoSummary(context, it) },
                labels =
                DtcListSummaryLabels(
                    notReadYet = context.getString(R.string.pref_dtc_list_not_read),
                    noCodes = context.getString(R.string.pref_dtc_list_no_codes),
                    codes = { count -> context.resources.getQuantityString(R.plurals.pref_dtc_list_codes, count, count) }
                )
            )
    }
}

// codes formats a non-zero count ("3 codes", plurals live in the resources).
internal class DtcListSummaryLabels(
    val notReadYet: String,
    val noCodes: String,
    val codes: (Int) -> String
)

// Before the first read there is no list to describe, whatever the stored count says.
internal fun dtcListSummary(
    codeCount: Int,
    readInfo: String?,
    labels: DtcListSummaryLabels
): String =
    when {
        readInfo == null -> labels.notReadYet
        codeCount == 0 -> "${labels.noCodes} · $readInfo"
        else -> "${labels.codes(codeCount)} · $readInfo"
    }

// "Last read: <time> · VIN …", shown on the list row and at the top of the DTC dialog.
internal fun readInfoSummary(
    context: Context,
    info: DtcReadInfo
): String =
    info.summary(
        formattedTime =
        DateUtils.formatDateTime(
            context,
            info.timestamp,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_YEAR
        ),
        currentVin = VehicleCapabilitiesManager.getCurrentVin(),
        labels =
        DtcReadInfoLabels(
            lastRead = context.getString(R.string.pref_dtc_read_info),
            vin = context.getString(R.string.pref_dtc_read_info_vin),
            otherVehicle = context.getString(R.string.pref_dtc_read_info_other_vehicle),
            noVin = context.getString(R.string.pref_dtc_read_info_no_vin)
        )
    )
