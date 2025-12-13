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

import android.content.SharedPreferences
import android.content.res.AssetManager
import android.util.Log
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.updatePreference
import org.obd.graphs.runAsync
import org.obd.graphs.sendBroadcastEvent
import java.io.ByteArrayInputStream
import java.lang.ref.WeakReference
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileServiceTestExtended {
    private lateinit var profileBackend: ProfileService
    private val mockContext = mockk<android.content.ContextWrapper>(relaxed = true)

    private val editor = mockk<SharedPreferences.Editor>(relaxed = true)
    private val assetsMock = mockk<AssetManager>()

    private val alfaProfileContent =
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

    @Before
    fun setup() {
        // 1. Mock Android Log
        mockkStatic(Log::class)
        every { Log.v(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        mockkStatic("org.obd.graphs.ContextKt")
        val field = Class.forName("org.obd.graphs.ContextKt").getDeclaredField("activityContext")
        field.isAccessible = true
        field.set(null, WeakReference(mockContext))

        every { mockContext.assets } returns assetsMock

        // 2. Mock external dependencies used during restore
        mockkStatic("org.obd.graphs.BroadcastKt") // For sendBroadcastEvent
        every { sendBroadcastEvent(any()) } just Runs

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

        // 3. Mock Prefs singleton
        mockkObject(Prefs)
        every { Prefs.edit() } returns editor
        every { Prefs.all } returns emptyMap()
        every { Prefs.getString(any(), any()) } answers { secondArg() as String }
        every { Prefs.getBoolean(any(), any()) } returns false // Default to false for installation check
        every { Prefs.registerOnSharedPreferenceChangeListener(any()) } just Runs

        // Mock Editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.putInt(any(), any()) } returns editor
        every { editor.putStringSet(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor
        every { editor.commit() } returns true
        every { editor.apply() } just Runs

        mockkConstructor(android.content.Intent::class)
        every { anyConstructed<android.content.Intent>().setAction(any()) } returns mockk()
        // Stub other methods if needed
        every { anyConstructed<android.content.Intent>().putExtra(any<String>(), any<String>()) } returns mockk()

        profileBackend = ProfileService()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test setupProfiles loads Alfa Romeo properties file correctly`() {
        // Arrange: Mock the file system to return the Alfa profile

        every { assetsMock.list("") } returns arrayOf("alfa_2_0_gme.properties")
        every { assetsMock.open("alfa_2_0_gme.properties") } returns
            ByteArrayInputStream(alfaProfileContent.toByteArray(StandardCharsets.UTF_8))

        val version = SimpleDateFormat("yyyyMMdd.HHmm", Locale.getDefault()).format(Date())
        profileBackend.init(1, "profile_2", version)

        // Act
        profileBackend.setupProfiles(forceOverrideRecommendation = true)

        // Assert: Verify specific keys from the file were saved

        // 1. Verify boolean parsing
        verify { editor.updatePreference("profile_3.pref.adapter.power.switch_network_on_off", false) }

        // 2. Verify string parsing (removing quotes)
        verify { editor.updatePreference("profile_3.pref.gauge.fps", "4") }

        // 3. Verify standard string
        verify { editor.updatePreference("profile_3.pref.adapter.init.protocol", "CAN_29") }

        // 4. Verify Array parsing (converting [x, y, z] to Set)
        val expectedSet = setOf("22", "7002", "13", "15")
        verify { editor.putStringSet("profile_3.pref.pids.generic.high", expectedSet) }

        // 5. Verify the profile name was set
        verify { editor.putString("pref.profile.names.profile_3", "Alfa 2.0 GME (BT)") }
    }

    @Test
    fun `test loadProfile('profile_3') switches to Alfa settings`() {
        val profileName = "profile_3"

        // Arrange: Prefs already contains the loaded profile_3 keys (simulating state after setupProfiles)
        val storedPrefs =
            mapOf(
                "profile_3.pref.adapter.init.protocol" to "CAN_29",
                "profile_3.pref.gauge.fps" to "4",
                "profile_3.pref.pids.generic.high" to "[22, 7002, 13, 15]",
                "profile_1.pref.some.other" to "ignore_me",
            )
        every { Prefs.all } returns storedPrefs

        // Act
        profileBackend.loadProfile(profileName)

        // Assert: The specific keys should be copied to the root (stripping "profile_3.")

        // 1. Verify protocol is set to root
        verify { editor.updatePreference("pref.adapter.init.protocol", "CAN_29") }

        // 2. Verify FPS is set to root
        verify { editor.updatePreference("pref.gauge.fps", "4") }

        // 3. Verify Arrays are copied
        verify { editor.updatePreference("pref.pids.generic.high", "[22, 7002, 13, 15]") }

        // 4. Verify broadcast
        verify { sendBroadcastEvent(PROFILE_CHANGED_EVENT) }
    }

    @Test
    fun `test saveCurrentProfile saves root changes back to profile_3`() {
        // Arrange: We are currently in profile_3
        every { Prefs.getString("pref.profile.id", any()) } returns "profile_3"

        // Simulating the root preferences (what the app is currently using)
        val currentRootPrefs =
            mapOf(
                "pref.adapter.init.protocol" to "CAN_11_MODIFIED", // User changed this
                "pref.gauge.fps" to "10",
                "profile_3.pref.original" to "original", // Should be ignored
            )
        every { Prefs.all } returns currentRootPrefs

        // Act
        profileBackend.saveCurrentProfile()

        // Assert
        // Verify the user's change was written back to "profile_3.pref.adapter.init.protocol"
        verify { editor.updatePreference("profile_3.pref.adapter.init.protocol", "CAN_11_MODIFIED") }
        verify { editor.updatePreference("profile_3.pref.gauge.fps", "10") }
    }
}
