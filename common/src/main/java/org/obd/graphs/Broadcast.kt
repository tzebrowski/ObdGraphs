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
package org.obd.graphs

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build

private const val EXTRA_PARAM_NAME = "extra"

fun Intent.getExtraParam(): String = extras?.get(EXTRA_PARAM_NAME) as String

fun sendBroadcastEvent(
    actionName: String,
    extra: String? = "",
) {
    getContext()?.run {
        sendBroadcast(
            Intent().apply {
                action = actionName
                putExtra(EXTRA_PARAM_NAME, extra)
            },
        )
    }
}

fun registerReceiver(
    context: Context?,
    receiver: BroadcastReceiver,
    exportReceiver: Boolean = true,
    func: (filter: IntentFilter) -> Unit,
) {
    context?.let {
        val intentFilter = IntentFilter()
        func(intentFilter)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val flags =
                if (exportReceiver) {
                    Context.RECEIVER_EXPORTED
                } else {
                    Context.RECEIVER_NOT_EXPORTED
                }
            it.registerReceiver(receiver, intentFilter, flags)
        } else {
            it.registerReceiver(receiver, intentFilter)
        }
    }
}
