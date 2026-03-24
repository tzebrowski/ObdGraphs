 /**
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
package org.obd.graphs.activity

import android.app.Activity
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.obd.graphs.R
import androidx.core.graphics.drawable.toDrawable
import org.obd.graphs.ui.common.toast

 class ScreenLockManager(
    private val activity: Activity,
) : DefaultLifecycleObserver {
    private var lockScreenDialog: AlertDialog? = null
    private var onCancelAction: (() -> Unit)? = null

    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null

    fun setup() {
        AlertDialog.Builder(activity).run {
            setCancelable(false)
            val dialogView: View = activity.layoutInflater.inflate(R.layout.dialog_screen_lock, null)
            val cancelButton = dialogView.findViewById<Button>(R.id.dialog_screen_lock_cancel_btn)

            cancelButton.setOnClickListener {
                onCancelAction?.invoke()
                dismiss()
            }

            setView(dialogView)
            lockScreenDialog = create()

            lockScreenDialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }
    }

    fun show(
        message: String,
        timeoutMs: Long = 5000L,
        onCancel: (() -> Unit)? = null,
    ) {
        this.onCancelAction = onCancel

        lockScreenDialog?.let { dialog ->
            val dialogTitle = dialog.findViewById<TextView>(R.id.dialog_screen_lock_message_id)
            if (dialogTitle != null && message.isNotEmpty()) {
                dialogTitle.text = message
            }
            if (!dialog.isShowing) {
                dialog.show()
            }
        }

        timeoutRunnable?.let { handler.removeCallbacks(it) }
        if (timeoutMs > 0) {
            timeoutRunnable =
                Runnable {
                    if (lockScreenDialog?.isShowing == true) {
                        toast(R.string.dialog_screen_lock_timeout_message)
                        dismiss()
                    }
                }
            handler.postDelayed(timeoutRunnable!!, timeoutMs)
        }
    }

    fun dismiss() {
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        if (lockScreenDialog?.isShowing == true) {
            lockScreenDialog?.dismiss()
        }
        onCancelAction = null
    }

    override fun onDestroy(owner: LifecycleOwner) {
        dismiss()
        lockScreenDialog = null
        super.onDestroy(owner)
    }
}
