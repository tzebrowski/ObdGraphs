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
package org.obd.graphs.ui

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import org.obd.graphs.SCREEN_LOCK_MSG_EXTRA_PARAM_NAME
import org.obd.graphs.R
import org.obd.graphs.SCREEN_LOCK_PROGRESS_EVENT
import org.obd.graphs.activity.SCREEN_LOCK_CONTEXT_EXTRA_PARAM_NAME
import org.obd.graphs.activity.FabButtons
import org.obd.graphs.activity.SCREEN_LOCK_SHOW_CANCEL_BUTTON_EXTRA_PARAM_NAME
import org.obd.graphs.bl.datalogger.DataLoggerRepository
import org.obd.graphs.bl.datalogger.DataLoggerService
import org.obd.graphs.bl.query.Query
import org.obd.graphs.sendBroadcastEvent

 fun DialogFragment.withDataLogger(action: DataLoggerService.() -> Unit) {
    org.obd.graphs.bl.datalogger
        .withDataLogger(requireContext(), action)
}

fun Fragment.withDataLogger(action: DataLoggerService.() -> Unit) {
    org.obd.graphs.bl.datalogger
        .withDataLogger(requireContext(), action)
}

fun ComponentActivity.withDataLogger(action: DataLoggerService.() -> Unit) {
    org.obd.graphs.bl.datalogger
        .withDataLogger(this, action)
}

fun Fragment.configureActionButton(query: Query) {
    activity?.let {
        val connectBtn = FabButtons.view(it).connectFab

        connectBtn.setOnClickListener {
            if (DataLoggerRepository.isRunning()) {
                withDataLogger {
                    Log.i("Fragment", "Stop data logging")
                    stop()
                }
            } else {

                sendBroadcastEvent(SCREEN_LOCK_PROGRESS_EVENT, mapOf(
                    SCREEN_LOCK_SHOW_CANCEL_BUTTON_EXTRA_PARAM_NAME to true,
                    SCREEN_LOCK_MSG_EXTRA_PARAM_NAME to getText(R.string.pref_dialog_screen_lock_logger_connect_message) as String,
                    SCREEN_LOCK_CONTEXT_EXTRA_PARAM_NAME to "datalogger.connect"))

                withDataLogger {
                    Log.i("Fragment", "Start data logging")
                    start(query)
                }
            }
        }

        connectBtn.backgroundTintList =
            ContextCompat.getColorStateList(
                it,
                if (DataLoggerRepository.isRunning()) {
                    org.obd.graphs.commons.R.color.cardinal
                } else {
                    org.obd.graphs.commons.R.color.philippine_green
                },
            )
    }
}
