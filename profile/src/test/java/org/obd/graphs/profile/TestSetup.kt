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
package org.obd.graphs.profile

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.AssetManager
import android.os.Environment
import android.util.Log
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.runAsync
import org.obd.graphs.sendBroadcastEvent
import java.io.File
import java.lang.ref.WeakReference

internal abstract class TestSetup {
    protected lateinit var profileService: Profile
    protected val context = mockk<android.content.ContextWrapper>(relaxed = true)
    protected val sharedPrefs = mockk<SharedPreferences>(relaxed = true)
    protected val editor = mockk<SharedPreferences.Editor>(relaxed = true)
    protected val assets = mockk<AssetManager>()

    protected val alfaProfileContent =
        """
        profile_3.pref.adapter.connection.type=bluetooth
        profile_3.pref.adapter.power.switch_network_on_off=false
        profile_3.pref.gauge_display_scale=true
        profile_3.pref.gauge.fps="4"
        profile_3.pref.adapter.id=
        profile_3.pref.graph.view.enabled=true
        profile_3.pref.pids.generic.high=[22, 7002, 13, 15]
        profile_3.pref.adapter.init.protocol=CAN_29
        profile_3.pref.dash.background_color_1=-6697984
        pref.profile.names.profile_3=Alfa 2.0 GME (BT)
        profile_3.pref.adapter.init.mode.header_value.mode_2=DA10F1
        profile_3.pref.adapter.init.mode.id_value.mode_2="22"
        profile_3.pref.aa.pids.selected=[7002, 7003, 7014, 7025]
        """.trimIndent()

    protected val ALFA_2_0_GME_CONTENT = """
        profile_3.pref.adapter.connection.type=bluetooth
        profile_3.pref.adapter.power.switch_network_on_off=false
        profile_3.pref.pids.generic.high=[22, 7002, 13, 15]
        profile_3.pref.adapter.init.delay="500"
        profile_3.pref.profile.id=profile_3
        pref.profile.names.profile_3=Alfa 2.0 GME (BT)
        profile_3.pref.profile.about=This profile contains Alfa Romeo 2.0 GME ECU specific settings.
    """.trimIndent()

    protected val ALFA_175_TBI_CONTENT = """
        profile_2.pref.adapter.connection.type=wifi
        profile_2.pref.adapter.batch.size="8"
        profile_2.pref.gauge.pids.selected=[22, 6075, 6011]
        profile_2.pref.profile.id=profile_2
        pref.profile.names.profile_2=Alfa 1.75 TBI (BT)
        profile_2.pref.adapter.power.connect_adapter=true
    """.trimIndent()

    @Before
    open fun setup() {
        mockLog()
        mockAsync()
        mockContext()
        every { context.assets } returns assets
        mockFunction()
        mockIntent()
        mockEnvironment()
        profileService = ProfileService()
//        profileService = ProfilePreferencesBackend()
  }


    protected fun mockPrefsAll() =
        every { Prefs.all } returns mapOf(
            "pref.adapter.connection.type" to "bluetooth",
            "pref.adapter.init.delay" to "500", // stored as string in map usually
            "pref.pids.generic.high" to "[22, 7002, 13, 15]",
            "pref.profile.names.profile_3" to "Alfa 2.0 GME (BT)"
        )

    private fun mockIntent() {
        mockkConstructor(Intent::class)
        every { anyConstructed<Intent>().setAction(any()) } returns mockk()
        // Stub other methods if needed
        every { anyConstructed<Intent>().putExtra(any<String>(), any<String>()) } returns mockk()
    }

    private fun mockEnvironment() {
        mockkStatic(Environment::class)
        every { Environment.getExternalStorageState() } returns Environment.MEDIA_MOUNTED
        every { Environment.getExternalStorageState(any()) } returns Environment.MEDIA_MOUNTED
        every { Environment.getExternalStorageDirectory() } returns File("/tmp/mock_storage")
    }

    private fun mockFunction() {
        mockkStatic("org.obd.graphs.BroadcastKt") // For sendBroadcastEvent
        every { sendBroadcastEvent(any()) } just Runs
    }

    private fun mockContext() {
        mockkStatic("org.obd.graphs.ContextKt")
        val field =
            Class
                .forName("org.obd.graphs.ContextKt")
                .getDeclaredField("activityContext")
        field.isAccessible = true
        field.set(null, WeakReference(context))
    }

    private fun mockLog() {
        mockkStatic(Log::class)
        every { Log.v(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
    }


    private fun mockAsync() {
        mockkStatic("org.obd.graphs.AsyncKt")

        every {
            runAsync<Any?>(any(), any())
        } answers {
            // Retrieve the arguments passed to the function
            val wait = arg<Boolean>(0)
            val handler = arg<() -> Any?>(1)

            // Execute the handler immediately on the TEST thread
            val result = handler.invoke()

            // Mimic the original logic:
            // If wait=true, return the result. If wait=false, return null.
            if (wait) {
                result
            } else {
                null
            }
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }
}
