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

 import android.content.res.AssetManager
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Test
import org.obd.graphs.preferences.updatePreference
import android.os.Environment
 import org.junit.Before
 import org.obd.graphs.preferences.Prefs
 import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties

internal class ProfileServiceTest :TestSetup() {

    @Before
    override fun setup() {
        super.setup()

        mockkStatic("org.obd.graphs.preferences.PreferencesKt")
        every { Prefs } returns sharedPrefs
        every { sharedPrefs.edit() } returns editor
        every { sharedPrefs.all } returns emptyMap()
        every { sharedPrefs.getString(any(), any()) } answers { secondArg() }
        every { sharedPrefs.getBoolean(any(), any()) } returns false // Default to false for installation check
        every { sharedPrefs.registerOnSharedPreferenceChangeListener(any()) } just Runs
    }

    @Test
    fun `getAvailableProfiles returns map of default profiles`() {
        // Arrange
        // When the code asks for a profile name, return a specific value
        every { sharedPrefs.getString("pref.profile.names.profile_1", any()) } returns "User Profile 1"

        // Act
        val profiles = profileBackend.getAvailableProfiles()

        // Assert
        assertEquals(20, profiles.size)
        assertEquals("User Profile 1", profiles["profile_1"])
        assertEquals("Profile 2", profiles["profile_2"])
    }

    @Test
    fun `getCurrentProfile returns default if not set`() {
        // Arrange
        every { sharedPrefs.getString("pref.profile.id", "profile_1") } returns "profile_1"

        // Act
        val current = profileBackend.getCurrentProfile()

        // Assert
        assertEquals("profile_1", current)
    }

    @Test
    fun `updateCurrentProfileName saves name to prefs`() {
        // Arrange
        val newName = "My Race Mode"
        every { sharedPrefs.getString("pref.profile.id", any()) } returns "profile_1"

        // Act
        profileBackend.updateCurrentProfileName(newName)

        // Assert
        verify {
            editor.putString("pref.profile.names.profile_1", newName)
        }
    }

    @Test
    fun `saveCurrentProfile filters reserved keys and saves rest with prefix`() {
        // Arrange
        val currentProfile = "profile_1"
        every { sharedPrefs.getString("pref.profile.id", any()) } returns currentProfile

        // Simulate existing preferences
        val mockMap = mapOf(
            "user_setting_1" to "value1",
            "profile_2.setting" to "ignore_me",
            "pref.profile.names.p1" to "ignore_me",
            "pref.about.build" to "value2"
        )
        every { sharedPrefs.all } returns mockMap

        // Act
        profileBackend.saveCurrentProfile()

        // Assert
        verify {
            editor.updatePreference("profile_1.user_setting_1", "value1")
            editor.updatePreference("profile_1.pref.about.build", "value2")
        }

        // Verify reserved keys were NOT saved
        verify(exactly = 0) {
            editor.updatePreference(match { it.startsWith("profile_1.profile_") }, any())
        }
    }

    @Test
    fun `loadProfile loads values from prefix and resets others`() {
        // Arrange
        val targetProfile = "profile_2"

        val mockMap = mapOf(
            "profile_2.engine_type" to "V8",
            "profile_2.color" to "Red",
            "profile_1.engine_type" to "V6",
            "generic_setting" to "default"
        )
        every { sharedPrefs.all } returns mockMap

        // Act
        profileBackend.loadProfile(targetProfile)

        // Assert
        verify { editor.remove("generic_setting") }
        verify { editor.updatePreference("engine_type", "V8") }
        verify { editor.updatePreference("color", "Red") }

        // Verify we ignored profile_1
        verify(exactly = 0) { editor.updatePreference("engine_type", "V6") }

        // Verify current profile name update
        verify { editor.putString("pref.profile.current_name", any()) }
    }

    @Test
    fun `exportBackup creates a backup file with formatted preferences`() {
        // Arrange
        // 1. Mock the getContext() top-level function
        // Note: Check if your 'getContext' is in 'Context.kt' or similar.
        // If the test fails with "not mocked", check the file name where getContext is defined.
        mockkStatic("org.obd.graphs.ContextKt")
        every { org.obd.graphs.getContext() } returns mockContext

        // 2. Setup a real temporary directory for the test
        val tempDir = java.nio.file.Files.createTempDirectory("backup_test").toFile()
        every { mockContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) } returns tempDir

        // 3. Stub the Preferences data to be exported
        val prefsData = mapOf(
            "user.pref.1" to "some_value",
            "user.pref.int" to 123,
            "user.pref.bool" to true
        )
        every { sharedPrefs.all } returns prefsData

        // Act
        val resultFile = profileBackend.exportBackup()

        // Assert
        assertEquals("obd_graphs.backup", resultFile?.name)
        assert(resultFile!!.exists())

        // Verify the content of the written file
        val props = Properties()
        FileInputStream(resultFile).use { props.load(it) }

        // The implementation wraps strings in quotes
        assertEquals("\"some_value\"", props.getProperty("user.pref.1"))
        assertEquals("123", props.getProperty("user.pref.int"))
        assertEquals("true", props.getProperty("user.pref.bool"))
    }

    @Test
    fun `restoreBackup clears preferences and reloads from file`() {
        // Arrange
        // 1. Create a real temporary backup file
        val tempFile = File.createTempFile("test_backup", ".properties")
        val backupProps = Properties().apply {
            setProperty("restored.key.string", "\"restored_value\"") // Strings are quoted in backup
            setProperty("restored.key.int", "999")
            setProperty("restored.key.bool", "true")
        }
        java.io.FileOutputStream(tempFile).use { backupProps.store(it, null) }

        // 2. Mock external dependencies used during restore
        mockkStatic("org.obd.graphs.BroadcastKt") // For sendBroadcastEvent
        every { org.obd.graphs.sendBroadcastEvent(any()) } just Runs

        // Mock the diagnosticRequestIDMapper object used in 'allowedToOverride()'
        mockkObject(org.obd.graphs.diagnosticRequestIDMapper)
        every { org.obd.graphs.diagnosticRequestIDMapper.getValuePreferenceName() } returns "mock_mapper_pref"

        // Mock string extension functions used for parsing (isBoolean, isNumeric, etc.)
        mockkStatic("org.obd.graphs.profile.StringExtKt")
        // Act
        profileBackend.restoreBackup(tempFile)

        // Assert
        // 1. Verify preferences were cleared first
        verify {
            editor.clear()
            editor.putBoolean("restored.key.bool",true) // logic removes quotes
            editor.putString("restored.key.string", "restored_value") // logic removes quotes
            editor.putInt("restored.key.int", 999)
            editor.putString("pref.profile.id", "profile_1")
            editor.putBoolean("prefs.installed.profiles.0", true)
            editor.apply()
        }

        // 2. Verify broadcast was sent
        verify { org.obd.graphs.sendBroadcastEvent("data.logger.profile.changed.event") }
    }

//    @Test
    fun `reset clears state, resets profile, and broadcasts event`() {
        // Arrange
        // Init needed for versionName used in reset->updateBuildSettings
        val version = SimpleDateFormat("yyyyMMdd.HHmm", Locale.getDefault()).format(Date())
        profileBackend.init(1, "profile_1", version)

        // Mock AssetManager to return empty list so setupProfiles finishes quickly
        val assets = mockk<AssetManager>()
        every { mockContext.assets } returns assets
        every { assets.list("") } returns emptyArray()

        // Act
        profileBackend.reset()

        // Assert
        // 1. Verify installation key reset
        verify { editor.putBoolean(match { it.startsWith("prefs.installed.profiles") }, false) }

        // 2. Verify current profile reset (removal of keys)
        verify { editor.remove(any()) }

        // 3. Verify broadcast
        verify { org.obd.graphs.sendBroadcastEvent("data.logger.profile.reset.event") }
    }
}
