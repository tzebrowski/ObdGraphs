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
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal class SetupProfilesTest : TestSetup() {


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
    fun `setupProfiles should load assets and update preferences when fresh install`() {
        // GIVEN
        val testProfileFile = "profile_1.properties"
        val profileContent = """
            pref.engine.type=petrol
            pref.adapter.logging.enabled=true
            pref.custom.list=[1, 2, 3]
            pref.adapter.baud=38400
        """.trimIndent()

        // Initialize the class
        profileService = ProfilePreferencesBackend()
        profileService.init(1, "profile_1",  SimpleDateFormat("yyyyMMdd.HHmm",
            Locale.getDefault()).format(Date()))

        // Mock Assets to return our test file
        every { assets.list("") } returns arrayOf(testProfileFile)
        every { assets.open(testProfileFile) } returns ByteArrayInputStream(profileContent.toByteArray())

        // Mock Prefs to appear empty (fresh install)
        every { Prefs.all } returns emptyMap()
        // Mock getBoolean for installation check (returns false)
        every { Prefs.getBoolean(any(), false) } returns false

        // WHEN
        profileService.setupProfiles(forceOverrideRecommendation = false)

        // THEN
        // 1. Verify editor was cleared (logic: if keys empty, forceOverride becomes true)
        verify { editor.clear() }

        // 2. Verify String parsing
        verify { editor.putString("pref.engine.type", "petrol") }

        // 3. Verify Boolean parsing (StringExt.kt logic)
        verify { editor.putBoolean("pref.adapter.logging.enabled", true) }

        // 4. Verify Array/Set parsing
        verify {
            editor.putStringSet("pref.custom.list", match {
                it.contains("1") && it.contains("2") && it.contains("3")
            })
        }

        // 5. Verify Numeric parsing
        verify { editor.putInt("pref.adapter.baud", 38400) }

        // 6. Verify default profile was set
        verify { editor.putString("pref.profile.id", "profile_1") }

        // 7. Verify installation key was set to true
        verify { editor.putBoolean(match { it.startsWith("prefs.installed.profiles") }, true) }

        // 8. Verify apply was called
        verify(atLeast = 1) { editor.apply() }
    }


    @Test
    fun `setupProfiles should force reload if forceOverride is true`() {
        // GIVEN
        val testProfileFile = "profile_1.properties"
        val profileContent = "pref.some.key=value"
        val installationKey = "prefs.installed.profiles.1"

        // Even if installed...
        every { Prefs.all } returns mapOf(installationKey to true)
        every { Prefs.getBoolean(installationKey, false) } returns false

        // Mock Assets
        every { assets.list("") } returns arrayOf(testProfileFile)
        every { assets.open(testProfileFile) } returns ByteArrayInputStream(profileContent.toByteArray())

        // WHEN
        profileService.setupProfiles(forceOverrideRecommendation = true)

        // THEN
        // It SHOULD clear and load
        verify { editor.clear() }
        verify { assets.open(testProfileFile) }
        verify { editor.putString("pref.some.key", "value") }
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
}
