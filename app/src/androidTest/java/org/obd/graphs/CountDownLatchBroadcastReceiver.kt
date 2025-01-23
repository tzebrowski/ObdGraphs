 /**
 * Copyright 2019-2025, Tomasz Żebrowski
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class CountDownLatchBroadcastReceiver(
    val broadcastEvent: String,
    private val timeout: Long = 5,
) {
    val eventGate = CountDownLatch(1)
    val eventReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                when (intent?.action) {
                    broadcastEvent -> {
                        eventGate.countDown()
                    }
                }
            }
        }

    fun waitOnEvent() {
        eventGate.await(timeout, TimeUnit.SECONDS)
    }
}
