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

import android.app.PendingIntent
import android.content.Context
import android.util.Log
import org.obd.graphs.EXTRA_NAVIGATE_TO
import org.obd.graphs.NAVIGATE_TO_DTC
import org.obd.graphs.Notifications
import org.obd.graphs.bl.datalogger.DATA_LOGGER_DTC_NEW_CODES
import org.obd.graphs.commons.R
import org.obd.graphs.getContext
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.sendBroadcastEvent
import org.obd.metrics.api.model.DiagnosticTroubleCode

const val PREF_DTC_NOTIFY_NEW_CODES = "pref.dtc.notify_new_codes"

private const val LOG_TAG = "DtcNewCodes"
private const val NOTIFICATION_ID = 12347
private const val CHANNEL_ID = "dtc_new_codes"

// The codes to tell the driver about after a scan: only the automatic scan on connect (a read the user
// started is on screen anyway), and only codes the vehicle's history calls new. The diff is empty for an
// unknown vehicle and for the vehicle's first scan, so neither announces anything.
fun dtcCodesToAnnounce(
    kind: DtcScanKind,
    dtc: Collection<DiagnosticTroubleCode>,
    diff: DtcHistoryDiff
): List<DiagnosticTroubleCode> = if (kind == DtcScanKind.CONNECT) dtc.filter { diff.isNew(it) }.sortedForDisplay() else emptyList()

// "P0123-1A · Engine ECU"; the bare code when the module is the default one.
fun DiagnosticTroubleCode.announcementLine(): String = listOfNotNull(displayCode(), moduleNameOrNull()).joinToString(" · ")

// Phone notification (posted from the service, so it shows with the app in the background) and a broadcast
// carrying the lines (ArrayList<String>) for AA. One setting gates both; on by default.
internal object DtcNewCodesAnnouncer {
    fun announce(
        kind: DtcScanKind,
        dtc: Collection<DiagnosticTroubleCode>,
        diff: DtcHistoryDiff
    ) {
        val codes = dtcCodesToAnnounce(kind, dtc, diff)
        if (codes.isEmpty()) return

        val lines = codes.map { it.announcementLine() }
        if (!Prefs.getBoolean(PREF_DTC_NOTIFY_NEW_CODES, true)) {
            Log.i(LOG_TAG, "New DTC found, notifications are off: $lines")
            return
        }

        Log.i(LOG_TAG, "New DTC found: $lines")
        sendBroadcastEvent(DATA_LOGGER_DTC_NEW_CODES, ArrayList(lines))
        // Never let the notification break storing the scan (updateDTC calls this).
        try {
            getContext()?.let { notify(it, lines) }
        } catch (e: Throwable) {
            Log.e(LOG_TAG, "Failed to show the new DTC notification", e)
        }
    }

    private fun notify(
        context: Context,
        lines: List<String>
    ) {
        Notifications.show(
            context = context,
            notificationId = NOTIFICATION_ID,
            channelId = CHANNEL_ID,
            channelName = context.getString(R.string.dtc_new_codes_channel),
            channelDescription = context.getString(R.string.dtc_new_codes_channel_description),
            title = context.getString(R.string.dtc_new_codes_title, lines.size),
            text = lines.joinToString("\n"),
            pendingIntent = openDtcList(context)
        )
    }

    // Its own request code: PendingIntents differing only in extras are the same one, and the launch intent
    // with code 0 (service, GPS notifications) would drop the navigation extra.
    private fun openDtcList(context: Context): PendingIntent? =
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                it.putExtra(EXTRA_NAVIGATE_TO, NAVIGATE_TO_DTC),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
}
