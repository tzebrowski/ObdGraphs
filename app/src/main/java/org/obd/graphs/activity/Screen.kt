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
package org.obd.graphs.activity

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.obd.graphs.AdminReceiver
import org.obd.graphs.getPowerPreferences

internal val screen = Screen()

internal class Screen {
    fun lockScreen(activity: Activity) {
        if (getPowerPreferences().screenOnOff) {
            val pm = activity.getSystemService(AppCompatActivity.POWER_SERVICE) as PowerManager
            if (pm.isInteractive) {
                val policy =
                    activity.getSystemService(AppCompatActivity.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                try {
                    policy.lockNow()
                } catch (ex: SecurityException) {
                    Toast
                        .makeText(
                            activity,
                            "Must enable device administrator",
                            Toast.LENGTH_LONG,
                        ).show()
                    val admin = ComponentName(activity, AdminReceiver::class.java)
                    val intent: Intent =
                        Intent(
                            DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN,
                        ).putExtra(
                            DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                            admin,
                        )
                    activity.startActivity(intent)
                }
            }
        }
    }

    fun changeScreenBrightness(
        activity: Activity,
        value: Float,
    ) {
        if (getPowerPreferences().screenOnOff) {
            val pm = activity.getSystemService(AppCompatActivity.POWER_SERVICE) as PowerManager
            val wl =
                pm.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "data_logger:wakeLock",
                )
            try {
                wl.acquire(5000) // wait 5s
                val params: WindowManager.LayoutParams =
                    activity.window.attributes
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                params.screenBrightness = value
                activity.window.attributes = params
            } catch (e: Throwable) {
                Log.e(LOG_TAG, "Failed to change screen brightness", e)
            } finally {
                wl.release()
            }
        }
    }

    fun hideSystemUI(activity: Activity) {
        val decorView = activity.window.decorView
        decorView.systemUiVisibility =
            (
                View.SYSTEM_UI_FLAG_IMMERSIVE
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
    }

    fun setupWindowManager(activity: Activity) {
        // keeps screen on
        activity.window.run {
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
    }
}
