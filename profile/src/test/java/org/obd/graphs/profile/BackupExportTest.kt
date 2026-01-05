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
package org.obd.graphs.profile

import android.os.Environment
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.obd.graphs.preferences.Prefs
import java.io.FileInputStream
import java.util.Properties

internal class BackupExportTest : TestSetup() {
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
    fun `exportBackup creates a backup file with formatted preferences`() {
        // Arrange
        // 1. Mock the getContext() top-level function
        // Note: Check if your 'getContext' is in 'Context.kt' or similar.
        // If the test fails with "not mocked", check the file name where getContext is defined.
        mockkStatic("org.obd.graphs.ContextKt")
        every { org.obd.graphs.getContext() } returns context

        // 2. Setup a real temporary directory for the test
        val tempDir =
            java.nio.file.Files
                .createTempDirectory("backup_test")
                .toFile()
        every { context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) } returns tempDir

        // 3. Stub the Preferences data to be exported
        val prefsData =
            mapOf(
                "user.pref.1" to "some_value",
                "user.pref.int" to 123,
                "user.pref.bool" to true,
            )
        every { sharedPrefs.all } returns prefsData

        // Act
        val resultFile = profileService.exportBackup()

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
}
