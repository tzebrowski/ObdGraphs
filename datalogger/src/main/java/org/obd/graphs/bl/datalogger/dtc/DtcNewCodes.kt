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

import android.app.NotificationManager
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
import java.io.Serializable

const val PREF_DTC_NOTIFY_NEW_CODES = "pref.dtc.notify_new_codes"

private const val LOG_TAG = "DtcNewCodes"
private const val NOTIFICATION_ID = 12347

// High importance: a fault alert pops up (heads-up); default importance only lists it. A channel's importance is
// fixed once created on a device, so changing it needs a new id ("dtc_new_codes" was the default-importance one).
private const val CHANNEL_ID = "dtc_alerts"
private const val MAX_NOTIFICATION_LINES = 8

// What the scan on connect tells the driver. firstScan: the vehicle had no earlier scan, so every code
// counts ("Trouble codes found"); otherwise only the codes its history calls new ("New trouble codes").
// lines: one per code, "P0123-1A · Engine ECU". Broadcast to AA as the DATA_LOGGER_DTC_NEW_CODES extras.
data class DtcAnnouncement(
    val lines: List<String>,
    val firstScan: Boolean
) : Serializable {
    // At most max lines, the rest as a count (the caller's "+N more" text).
    fun shown(max: Int): Pair<List<String>, Int> = lines.take(max) to (lines.size - max).coerceAtLeast(0)
}

// Only the automatic scan on connect (a read the user started is on screen anyway), and only for a vehicle
// with a VIN reply: an unknown vehicle's codes may come from any car. scans: the vehicle's history, this scan last.
// firstScan must be known before the scan is stored: history replaces a connect scan equal to the previous one, so a
// vehicle whose codes never change keeps a single scan, and "one scan stored" would announce them on every connect.
fun dtcAnnouncement(
    kind: DtcScanKind,
    vin: String?,
    dtc: Collection<DiagnosticTroubleCode>,
    scans: List<DtcScan>,
    firstScan: Boolean
): DtcAnnouncement? {
    if (kind != DtcScanKind.CONNECT || historyKey(vin) == UNKNOWN_VEHICLE) return null
    val diff = dtcHistoryDiff(scans)
    val codes = dtc.filter { firstScan || diff.isNew(it) }.sortedForDisplay()
    return if (codes.isEmpty()) null else DtcAnnouncement(codes.map { it.announcementLine() }, firstScan)
}

// "P0123-1A · Engine ECU"; the bare code when the module is the default one.
fun DiagnosticTroubleCode.announcementLine(): String = listOfNotNull(displayCode(), moduleNameOrNull()).joinToString(" · ")

// Phone notification (posted from the service, so it shows with the app in the background) and a broadcast
// for AA. One setting gates both; on by default.
internal object DtcNewCodesAnnouncer {
    fun announce(
        kind: DtcScanKind,
        vin: String?,
        dtc: Collection<DiagnosticTroubleCode>,
        firstScan: Boolean
    ) {
        if (kind != DtcScanKind.CONNECT) return
        val announcement = dtcAnnouncement(kind, vin, dtc, DtcHistoryStore.scans(vin), firstScan)
        if (announcement == null) {
            // Silence looks like a bug: say why.
            Log.i(LOG_TAG, "No DTC to announce: codes=${dtc.size}, vehicle=${historyKey(vin)}")
            return
        }

        if (!Prefs.getBoolean(PREF_DTC_NOTIFY_NEW_CODES, true)) {
            Log.i(LOG_TAG, "DTC to announce, notifications are off: $announcement")
            return
        }

        Log.i(LOG_TAG, "Announcing DTC: $announcement")
        sendBroadcastEvent(DATA_LOGGER_DTC_NEW_CODES, announcement)
        // Never let the notification break storing the scan (updateDTC calls this).
        try {
            getContext()?.let { notify(it, announcement) }
        } catch (e: Throwable) {
            Log.e(LOG_TAG, "Failed to show the DTC notification", e)
        }
    }

    private fun notify(
        context: Context,
        announcement: DtcAnnouncement
    ) {
        val (lines, more) = announcement.shown(MAX_NOTIFICATION_LINES)
        val title = if (announcement.firstScan) R.string.dtc_codes_found_title else R.string.dtc_new_codes_title
        Notifications.show(
            context = context,
            notificationId = NOTIFICATION_ID,
            channelId = CHANNEL_ID,
            channelName = context.getString(R.string.dtc_new_codes_channel),
            channelDescription = context.getString(R.string.dtc_new_codes_channel_description),
            title = context.getString(title, announcement.lines.size),
            text = (lines + listOfNotNull(more.takeIf { it > 0 }?.let { context.getString(R.string.dtc_codes_more, it) })).joinToString("\n"),
            pendingIntent = openDtcList(context),
            importance = NotificationManager.IMPORTANCE_HIGH
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
