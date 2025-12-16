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

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.updatePreference
import org.obd.graphs.sendBroadcastEvent
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal class ProfileServiceTestExtended : TestSetup() {

    private val ALFA_2_0_GME_CONTENT = """
        profile_3.pref.adapter.connection.type=bluetooth
        profile_3.pref.adapter.power.switch_network_on_off=false
        profile_3.pref.pids.generic.high=[22, 7002, 13, 15]
        profile_3.pref.adapter.init.delay="500"
        profile_3.pref.profile.id=profile_3
        pref.profile.names.profile_3=Alfa 2.0 GME (BT)
        profile_3.pref.profile.about=This profile contains Alfa Romeo 2.0 GME ECU specific settings.
    """.trimIndent()

    private val ALFA_175_TBI_CONTENT = """
        profile_2.pref.adapter.connection.type=wifi
        profile_2.pref.adapter.batch.size="8"
        profile_2.pref.gauge.pids.selected=[22, 6075, 6011]
        profile_2.pref.profile.id=profile_2
        pref.profile.names.profile_2=Alfa 1.75 TBI (BT)
        profile_2.pref.adapter.power.connect_adapter=true
    """.trimIndent()

    @Before
    override fun setup() {
        super.setup()

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

        // Mock AssetManager to return our file names and content
        every { assets.list("") } returns arrayOf("alfa_2_0_gme.properties", "alfa_175_tbi.properties")

        every { assets.open("alfa_2_0_gme.properties") } answers {
            ByteArrayInputStream(ALFA_2_0_GME_CONTENT.toByteArray())
        }
        every { assets.open("alfa_175_tbi.properties") } answers {
            ByteArrayInputStream(ALFA_175_TBI_CONTENT.toByteArray())
        }

        profileService = ProfileService()

    }

    @Test
    fun `loadProfile('profile_3') loads specific Alfa GME settings into active preferences`() {
        // Given
        val targetProfile = "profile_3"
        // Simulate that setupProfiles has already run and populated Prefs.all with the raw file data
        val simulatedPrefs = mapOf(
            "profile_3.pref.adapter.connection.type" to "bluetooth",
            "profile_3.pref.adapter.init.delay" to "500", // stored as string in map usually
            "profile_3.pref.pids.generic.high" to "[22, 7002, 13, 15]",
            "pref.profile.names.profile_3" to "Alfa 2.0 GME (BT)"
        )
        every { Prefs.all } returns simulatedPrefs

        // When
        profileService.loadProfile(targetProfile)

        // Then
        // 1. Verify connection type is promoted to root preference
        // "pref.adapter.connection.type" is the suffix after "profile_3."
        verify { editor.putString("pref.adapter.connection.type", "bluetooth") }

        // 2. Verify init delay
        verify { editor.putString("pref.adapter.init.delay", "500") }

        // 3. Verify Array is moved as string (loadProfile logic copies value directly)
        // Note: The logic in loadProfile iterates Prefs.all and calls updatePreference.
        // If the source map has it as String "[...]", it copies it as String.
        verify { editor.putString("pref.pids.generic.high", "[22, 7002, 13, 15]") }

        // 4. Verify Current Profile Name is updated
//        verify { editor.updatePreference("pref.profile.current_name", "Alfa 2.0 GME (BT)") }
    }

    @Test
    fun `loadProfile('profile_2') correctly ignores unrelated profiles`() {
        // Given
        val targetProfile = "profile_2"
        val simulatedPrefs = mapOf(
            "profile_2.pref.adapter.connection.type" to "wifi",
            "profile_3.pref.adapter.connection.type" to "bluetooth" // Should be ignored
        )
        every { Prefs.all } returns simulatedPrefs

        // When
        profileService.loadProfile(targetProfile)

        // Then
        // Verify we loaded the WIFI setting from profile 2
        verify { editor.updatePreference("pref.adapter.connection.type", "wifi") }

        // Verify we did NOT load the Bluetooth setting from profile 3
        verify(exactly = 0) { editor.updatePreference("pref.adapter.connection.type", "bluetooth") }
    }

    @Test
    fun `setupProfiles parses 'Alfa 175 TBI' properties correctly`() {
        profileService.init(1, "profile_2", SimpleDateFormat("yyyyMMdd.HHmm",
            Locale.getDefault()).format(Date()))

        // When
        profileService.setupProfiles(forceOverrideRecommendation = true)

        // Then
        // Verify key specific to file 2: profile_2.pref.adapter.batch.size="8"
        verify { editor.putString("profile_2.pref.adapter.batch.size", "8") }
//
        // Verify boolean: profile_2.pref.adapter.power.connect_adapter=true
        verify { editor.putBoolean("profile_2.pref.adapter.power.connect_adapter", true) }
    }

    @Test
    fun `setupProfiles parses 'Alfa 2_0 GME' properties and loads them into preferences`() {
        profileService.init(1, "profile_3", SimpleDateFormat("yyyyMMdd.HHmm",
            Locale.getDefault()).format(Date()))

        // When
        profileService.setupProfiles(forceOverrideRecommendation = true)

        // Then
        verify {
            // 1. Verify standard string property
            editor.putString("profile_3.pref.adapter.connection.type", "bluetooth")

            // 2. Verify numeric property (parsed as Int)
            // profile_3.pref.adapter.init.delay="500"
            editor.putString("profile_3.pref.adapter.init.delay", "500")

            // 3. Verify boolean property
            // profile_3.pref.adapter.power.switch_network_on_off=false
            editor.putBoolean("profile_3.pref.adapter.power.switch_network_on_off", false)



            // 5. Verify Profile Name registration
            // pref.profile.names.profile_3=Alfa 2.0 GME (BT)
//            editor.updatePreference("pref.profile.names.profile_3", "Alfa 2.0 GME (BT)")
        }
    }

    @Test
    fun `test setupProfiles loads Alfa Romeo properties file correctly`() {
        // Arrange: Mock the file system to return the Alfa profile

        every { assets.list("") } returns arrayOf("alfa_2_0_gme.properties")
        every { assets.open("alfa_2_0_gme.properties") } returns
            ByteArrayInputStream(alfaProfileContent.toByteArray(StandardCharsets.UTF_8))

        profileService.init(1, "profile_2", SimpleDateFormat("yyyyMMdd.HHmm",
            Locale.getDefault()).format(Date()))

        // Act
        profileService.setupProfiles(forceOverrideRecommendation = true)

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
        profileService.loadProfile(profileName)

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
        profileService.saveCurrentProfile()

        // Assert
        // Verify the user's change was written back to "profile_3.pref.adapter.init.protocol"
        verify { editor.updatePreference("profile_3.pref.adapter.init.protocol", "CAN_11_MODIFIED") }
        verify { editor.updatePreference("profile_3.pref.gauge.fps", "10") }
    }
}
