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
package org.obd.graphs.activity

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.obd.graphs.R
import org.obd.graphs.SCREEN_LOCK_PROGRESS_CONTEXT_PARAM
import org.obd.graphs.ui.common.toast

internal fun Intent.getContextExtraParam(): String? = extras?.getString(SCREEN_LOCK_PROGRESS_CONTEXT_PARAM)

class ScreenLockManager(
    private val activity: Activity
) : DefaultLifecycleObserver {

    private var lockScreenDialog: AlertDialog? = null
    private var onCancelAction: (() -> Unit)? = null

    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null

    // Cached view references
    private var cancelButton: Button? = null
    private var messageTextView: TextView? = null

    fun setup() {
        if (lockScreenDialog != null) return

        AlertDialog.Builder(activity).run {
            setCancelable(false)
            val dialogView: View = activity.layoutInflater.inflate(R.layout.dialog_screen_lock, null)

            cancelButton = dialogView.findViewById(R.id.dialog_screen_lock_cancel_btn)
            messageTextView = dialogView.findViewById(R.id.dialog_screen_lock_message_id)

            cancelButton?.setOnClickListener {
                onCancelAction?.invoke()
                dismiss()
            }

            setView(dialogView)
            lockScreenDialog = create().apply {
                window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            }
        }
    }

    fun show(
        message: String,
        timeoutMs: Long = 10000L,
        showCancelButton: Boolean = false,
        onCancel: (() -> Unit)? = null
    ) {
        this.onCancelAction = onCancel
        this.cancelButton?.isVisible = showCancelButton

        if (message.isNotEmpty()) {
            messageTextView?.text = message
        }

        lockScreenDialog?.let { dialog ->
            if (!dialog.isShowing && !activity.isFinishing) {
                dialog.show()
            }
        }

        timeoutRunnable?.let { handler.removeCallbacks(it) }

        if (timeoutMs > 0) {
            timeoutRunnable = Runnable {
                if (lockScreenDialog?.isShowing == true) {
                    toast(R.string.pref_dialog_screen_lock_timeout_message)
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
        cancelButton = null
        messageTextView = null
        super.onDestroy(owner)
    }
}
