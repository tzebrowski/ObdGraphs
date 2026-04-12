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
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.preference.SwitchPreferenceCompat
import org.obd.graphs.GOOGLE_SIGN_IN_REQUEST
import org.obd.graphs.R
import org.obd.graphs.sendBroadcastEvent

private const val LOG_TAG = "DriveAutoSyncEnablerCheckBoxPreference"

class DriveAutoSyncEnablerCheckBoxPreference(
    context: Context,
    attrs: AttributeSet?
) : SwitchPreferenceCompat(context, attrs) {

    init {
        setOnPreferenceChangeListener { _, newValue ->
            val isEnabling = newValue as Boolean
            if (isEnabling) {
                AlertDialog
                    .Builder(context)
                    .setTitle(context.getString(R.string.pref_trips_drive_auto_sync_enabled_sign_in_dialog_title))
                    .setMessage(context.getString(R.string.pref_trips_drive_auto_sync_enabled_sign_in_dialog_summary))
                    .setCancelable(false)
                    .setPositiveButton(context.getString(R.string.dialog_ask_question_yes)) { _, _ ->
                        sendBroadcastEvent(GOOGLE_SIGN_IN_REQUEST)
                        isChecked = true
                    }.setNegativeButton(context.getString(R.string.dialog_ask_question_no)) { dialog, _ ->
                        Log.i(LOG_TAG, "Disabling google drive auto sync")
                        dialog.dismiss()
                    }.show()
                false
            } else {
                true
            }
        }
    }
}
