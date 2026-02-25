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
package org.obd.graphs.bl.datalogger

import android.content.Context
import android.os.Build
import android.util.Log
import org.obd.graphs.DATA_LOGGER_AUTO_CONNECT_EVENT
import org.obd.graphs.Network
import org.obd.graphs.sendBroadcastEvent
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

private const val SCHEDULE_DELAY_SEC = 2L

private const val TAG = "AutoConnect"

object AutoConnect {
    private val scheduleService: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
    private var future: ScheduledFuture<*>? = null

    fun schedule(
        context: Context,
        autoConnectEnabled: Boolean = dataLoggerSettings.instance().adapter.autoConnectEnabled,
        scheduleDelaySec: Long = SCHEDULE_DELAY_SEC,
    ) {
        try {
            val macAddress =
                dataLoggerSettings
                    .instance()
                    .adapter.deviceAddress
                    .uppercase()

            Log.i(
                TAG,
                "Auto connect is enabled=$autoConnectEnabled. MacAddress of device=$macAddress",
            )

            if (autoConnectEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && macAddress.isNotEmpty()) {
                Network.startBackgroundBleScanForMac(
                    context,
                    macAddress,
                ) {
                    Log.i(
                        TAG,
                        "Found device=$macAddress. Scheduling data logger connection",
                    )

                    val task =
                        Runnable {
                            sendBroadcastEvent(DATA_LOGGER_AUTO_CONNECT_EVENT)
                        }
                    future?.cancel(true)

                    future =
                        scheduleService.schedule(
                            task,
                            scheduleDelaySec,
                            TimeUnit.SECONDS,
                        )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule DataLogger connection", e)
        }
    }
}
