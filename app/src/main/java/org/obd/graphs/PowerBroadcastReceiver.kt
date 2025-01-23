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

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.util.Log
import org.obd.graphs.activity.LOG_TAG
import org.obd.graphs.activity.MainActivity
import org.obd.graphs.bl.datalogger.DATA_LOGGER_SCHEDULED_START_EVENT
import org.obd.graphs.bl.datalogger.dataLogger

const val SCREEN_OFF_EVENT = "power.screen.off"
const val SCREEN_ON_EVENT = "power.screen.on"

internal class PowerBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context?,
        intent: Intent,
    ) {
        val powerPreferences: PowerPreferences = getPowerPreferences()
        Log.d(
            LOG_TAG,
            "Received Power Event: ${intent.action}, powerPreferences.connectOnPower=${powerPreferences.connectOnPower}",
        )

        if (intent.action === Intent.ACTION_POWER_CONNECTED) {
            if (powerPreferences.switchNetworkOffOn) {
                true.run {
                    network.bluetooth(this)
                    network.wifi(this)
                    sendBroadcastEvent(DATA_LOGGER_SCHEDULED_START_EVENT)
                }
            } else {
                if (powerPreferences.connectOnPower) {
                    sendBroadcastEvent(DATA_LOGGER_SCHEDULED_START_EVENT)
                }
            }

            if (powerPreferences.screenOnOff) {
                startMainActivity(context!!)
                sendBroadcastEvent(SCREEN_ON_EVENT)
            }
        } else if (intent.action === Intent.ACTION_POWER_DISCONNECTED) {
            if (powerPreferences.switchNetworkOffOn) {
                network.bluetooth(false)
                network.wifi(false)
            }

            if (powerPreferences.connectOnPower) {
                Log.d(
                    LOG_TAG,
                    "Stop data logging",
                )
                dataLogger.stop()
                dataLogger.scheduledStop()
            }

            if (powerPreferences.screenOnOff) {
                sendBroadcastEvent(SCREEN_OFF_EVENT)
            }
        }
    }

    private fun startMainActivity(context: Context) {
        if (!isActivityVisibleOnTheScreen(context, MainActivity::class.java)) {
            val i = Intent(context, MainActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }
    }

    private fun isActivityVisibleOnTheScreen(
        context: Context,
        activityClass: Class<*>,
    ): Boolean {
        val activityManager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val taskInfo = activityManager.getRunningTasks(1)
        Log.d(LOG_TAG, "Current top activity ${taskInfo[0].topActivity!!.className}")
        val componentInfo = taskInfo[0].topActivity
        return activityClass.canonicalName.equals(componentInfo!!.className, ignoreCase = true)
    }
}
