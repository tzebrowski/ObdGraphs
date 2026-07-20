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
package org.obd.graphs.ui.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import org.obd.graphs.R

/**
 * A dialog for failures the user may need to relay back verbatim (API error details,
 * exception messages) - a toast auto-dismisses too fast to read/copy for these.
 */
fun Context.showCopyableErrorDialog(
    title: String,
    message: String,
    clipLabel: String = "Error details"
) {
    AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(message)
        .setCancelable(true)
        .setPositiveButton(R.string.dtc_action_copy) { dialog, _ ->
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(clipLabel, message))
            Toast.makeText(this, R.string.error_dialog_copied_to_clipboard, Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }.setNegativeButton(R.string.nav_close) { dialog, _ -> dialog.dismiss() }
        .show()
}

fun Context.showCopyableErrorDialog(
    @StringRes titleRes: Int,
    message: String,
    clipLabel: String = "Error details"
) = showCopyableErrorDialog(getString(titleRes), message, clipLabel)
